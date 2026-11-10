package ru.kbzhu.tracker.ui.foods

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.domain.model.FoodValidationError
import ru.kbzhu.tracker.domain.model.Nutrition
import ru.kbzhu.tracker.domain.model.RecipeIngredient
import ru.kbzhu.tracker.domain.model.SaveFoodResult
import ru.kbzhu.tracker.domain.model.total
import ru.kbzhu.tracker.domain.repository.FoodRepository
import ru.kbzhu.tracker.domain.repository.PhotoStorage
import ru.kbzhu.tracker.domain.usecase.DeleteFoodUseCase
import ru.kbzhu.tracker.domain.usecase.GetRecipeUseCase
import ru.kbzhu.tracker.domain.usecase.SaveRecipeUseCase
import ru.kbzhu.tracker.domain.usecase.SearchFoodsUseCase
import ru.kbzhu.tracker.ui.navigation.RecipeEditRoute
import ru.kbzhu.tracker.ui.util.parseDecimal
import ru.kbzhu.tracker.ui.util.sanitizeDecimal
import ru.kbzhu.tracker.ui.util.sanitizeInt
import ru.kbzhu.tracker.ui.util.toInputString
import javax.inject.Inject

data class IngredientInput(val food: Food, val gramsText: String) {
    val grams: Double get() = gramsText.parseDecimal() ?: 0.0
    val nutrition: Nutrition get() = food.per100.forGrams(grams)
}

data class RecipeEditUiState(
    val isLoading: Boolean = false,
    val original: Food? = null,
    val name: String = "",
    val ingredients: List<IngredientInput> = emptyList(),
    val cookedWeight: String = "",
    val portions: String = "1",
    val photoPath: String? = null,
    val isPhotoProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val error: FoodValidationError? = null
) {
    val isNew: Boolean get() = original == null
    val rawWeight: Double get() = ingredients.sumOf { it.grams }
    /** Вес готового блюда: введённый или сумма ингредиентов */
    val totalWeight: Double get() = cookedWeight.parseDecimal()?.takeIf { it > 0 } ?: rawWeight
    val total: Nutrition get() = ingredients.map { it.nutrition }.total()
    val per100: Nutrition get() = if (totalWeight > 0) total * (100.0 / totalWeight) else Nutrition.ZERO
    val portionsCount: Int get() = portions.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val perPortion: Nutrition get() = total * (1.0 / portionsCount)
}

sealed interface RecipeEditEvent {
    data object Saved : RecipeEditEvent
    data object Deleted : RecipeEditEvent
    data class Message(@StringRes val res: Int) : RecipeEditEvent
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipeEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
    private val photoStorage: PhotoStorage,
    private val getRecipe: GetRecipeUseCase,
    private val saveRecipe: SaveRecipeUseCase,
    private val deleteFood: DeleteFoodUseCase,
    private val searchFoods: SearchFoodsUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<RecipeEditRoute>()
    private val photos = PendingPhotoTracker(photoStorage)
    private var finished = false

    private val _state = MutableStateFlow(RecipeEditUiState(isLoading = route.recipeId != null))
    val state: StateFlow<RecipeEditUiState> = _state.asStateFlow()

    private val _events = Channel<RecipeEditEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // ---------- Выбор ингредиента ----------
    private val _pickerQuery = MutableStateFlow("")
    val pickerQuery: StateFlow<String> = _pickerQuery.asStateFlow()

    /** Пустой запрос — недавние продукты; само блюдо в списке не показываем. */
    val pickerResults: StateFlow<List<Food>> = _pickerQuery.debounce(150)
        .flatMapLatest { q ->
            if (q.isBlank()) foodRepository.observeRecent(30) else searchFoods(q, limit = 50)
        }
        .map { list -> list.filter { it.id != route.recipeId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        route.recipeId?.let { id ->
            viewModelScope.launch {
                val recipe = getRecipe(id)
                if (recipe == null) {
                    _state.update { it.copy(isLoading = false) }
                    return@launch
                }
                val food = recipe.food
                _state.update {
                    it.copy(
                        isLoading = false,
                        original = food,
                        name = food.name,
                        ingredients = recipe.ingredients.map { ing -> IngredientInput(ing.food, ing.grams.toInputString()) },
                        cookedWeight = food.recipeYield
                            ?.takeIf { y -> kotlin.math.abs(y - recipe.ingredients.sumOf { i -> i.grams }) > 0.5 }
                            ?.toInputString().orEmpty(),
                        portions = (food.recipePortions ?: 1).toString(),
                        photoPath = food.photoPath
                    )
                }
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v.take(100), error = null) }

    fun onPickerQuery(v: String) {
        _pickerQuery.value = v.take(80)
    }

    fun addIngredient(food: Food) {
        val grams = food.servingSize ?: 100.0
        _state.update { it.copy(ingredients = it.ingredients + IngredientInput(food, grams.toInputString()), error = null) }
        _pickerQuery.value = ""
    }

    fun onIngredientGrams(index: Int, v: String) {
        val clean = sanitizeDecimal(v, 1, 6) ?: return
        _state.update { s ->
            s.copy(ingredients = s.ingredients.mapIndexed { i, ing -> if (i == index) ing.copy(gramsText = clean) else ing }, error = null)
        }
    }

    fun removeIngredient(index: Int) = _state.update { s ->
        s.copy(ingredients = s.ingredients.filterIndexed { i, _ -> i != index })
    }

    fun onCookedWeight(v: String) {
        sanitizeDecimal(v, 1, 6)?.let { clean -> _state.update { it.copy(cookedWeight = clean, error = null) } }
    }

    fun onPortions(v: String) {
        sanitizeInt(v, 2)?.let { clean -> _state.update { it.copy(portions = clean, error = null) } }
    }

    // ---------- Фото ----------
    fun createCameraUri(): String = photoStorage.createCameraUri()
    fun discardCameraUri(uri: String) = photoStorage.discardCameraUri(uri)

    fun onPhotoPicked(uri: String) {
        viewModelScope.launch {
            _state.update { it.copy(isPhotoProcessing = true) }
            try {
                val path = photoStorage.importPhoto(uri)
                photos.track(path)
                photos.discardIfPending(_state.value.photoPath)
                _state.update { it.copy(photoPath = path, isPhotoProcessing = false) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(isPhotoProcessing = false) }
                _events.send(RecipeEditEvent.Message(R.string.error_photo))
            }
        }
    }

    fun removePhoto() {
        viewModelScope.launch {
            photos.discardIfPending(_state.value.photoPath)
            _state.update { it.copy(photoPath = null) }
        }
    }

    fun onCameraPermissionDenied() {
        viewModelScope.launch { _events.send(RecipeEditEvent.Message(R.string.error_camera_permission)) }
    }

    // ---------- Сохранение ----------
    fun save() {
        val s = _state.value
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = saveRecipe(
                recipeId = s.original?.id,
                name = s.name,
                ingredients = s.ingredients.map { RecipeIngredient(it.food, it.grams) },
                cookedWeight = s.cookedWeight.parseDecimal(),
                portions = s.portionsCount,
                photoPath = s.photoPath,
                category = s.original?.category ?: ru.kbzhu.tracker.domain.model.FoodCategory.READY_MEALS
            )
            when (result) {
                is SaveFoodResult.Success -> {
                    photos.commit(kept = s.photoPath)
                    finished = true
                    _state.update { it.copy(isSaving = false) }
                    _events.send(RecipeEditEvent.Saved)
                }
                is SaveFoodResult.Invalid -> _state.update { it.copy(isSaving = false, error = result.error) }
            }
        }
    }

    fun delete() {
        val original = _state.value.original ?: return
        viewModelScope.launch {
            deleteFood(original)
            photos.commit(kept = null)
            finished = true
            _events.send(RecipeEditEvent.Deleted)
        }
    }

    override fun onCleared() {
        if (!finished) photos.discardAll()
        super.onCleared()
    }
}
