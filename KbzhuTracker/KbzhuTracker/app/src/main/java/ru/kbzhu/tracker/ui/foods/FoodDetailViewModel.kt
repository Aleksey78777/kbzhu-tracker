package ru.kbzhu.tracker.ui.foods

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.core.util.toLocalDate
import ru.kbzhu.tracker.core.util.today
import ru.kbzhu.tracker.domain.model.DiaryEntry
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.domain.model.FoodSource
import ru.kbzhu.tracker.domain.model.MealType
import ru.kbzhu.tracker.domain.model.Nutrition
import ru.kbzhu.tracker.domain.model.RecipeIngredient
import ru.kbzhu.tracker.domain.repository.DiaryRepository
import ru.kbzhu.tracker.domain.repository.FoodRepository
import ru.kbzhu.tracker.domain.repository.PhotoStorage
import ru.kbzhu.tracker.domain.usecase.AddFoodToDiaryUseCase
import ru.kbzhu.tracker.domain.usecase.DeleteDiaryEntryUseCase
import ru.kbzhu.tracker.domain.usecase.UpdateDiaryEntryUseCase
import ru.kbzhu.tracker.ui.navigation.FoodDetailRoute
import ru.kbzhu.tracker.ui.util.defaultMealForNow
import ru.kbzhu.tracker.ui.util.parseDecimal
import ru.kbzhu.tracker.ui.util.sanitizeDecimal
import ru.kbzhu.tracker.ui.util.toInputString
import java.time.LocalDate
import javax.inject.Inject

enum class AmountMode { GRAMS, SERVINGS }

data class FoodDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val food: Food? = null,
    /** Не null — редактируем существующую запись дневника */
    val entry: DiaryEntry? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val mode: AmountMode = AmountMode.GRAMS,
    val gramsText: String = "100",
    val servingsText: String = "1",
    val meal: MealType = defaultMealForNow(),
    val date: LocalDate = today(),
    val photoPath: String? = null,
    val isPhotoProcessing: Boolean = false,
    val isSaving: Boolean = false
) {
    val isEditingEntry: Boolean get() = entry != null
    val servingSize: Double? get() = food?.servingSize

    val grams: Double?
        get() = when (mode) {
            AmountMode.GRAMS -> gramsText.parseDecimal()
            AmountMode.SERVINGS -> servingsText.parseDecimal()?.let { it * (servingSize ?: 100.0) }
        }

    /** Живой пересчёт КБЖУ под выбранную граммовку */
    val nutrition: Nutrition
        get() {
            val f = food ?: return Nutrition.ZERO
            return grams?.let { f.per100.forGrams(it) } ?: Nutrition.ZERO
        }

    val isAmountValid: Boolean get() = grams?.let { it > 0 && it <= 10_000 } == true
    val isIncomplete: Boolean get() = food?.per100?.calories?.let { it <= 0.0 } == true
    val canSave: Boolean get() = food != null && isAmountValid && !isSaving && !isPhotoProcessing
}

sealed interface FoodDetailEvent {
    data object Done : FoodDetailEvent
    data class Message(@StringRes val res: Int) : FoodDetailEvent
}

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
    private val diaryRepository: DiaryRepository,
    private val photoStorage: PhotoStorage,
    private val addFoodToDiary: AddFoodToDiaryUseCase,
    private val updateDiaryEntry: UpdateDiaryEntryUseCase,
    private val deleteDiaryEntry: DeleteDiaryEntryUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<FoodDetailRoute>()
    private val photos = PendingPhotoTracker(photoStorage)
    private var saved = false
    private var amountInitialized = false

    private val _state = MutableStateFlow(
        FoodDetailUiState(
            meal = route.meal?.let { m -> MealType.entries.firstOrNull { it.name == m } } ?: defaultMealForNow(),
            date = route.date?.toLocalDate() ?: today()
        )
    )
    val state: StateFlow<FoodDetailUiState> = _state.asStateFlow()

    private val _events = Channel<FoodDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val entryId = route.entryId
            if (entryId != null) loadEntry(entryId) else loadFood(route.foodId)
        }
    }

    private suspend fun loadEntry(entryId: Long) {
        val entry = diaryRepository.getEntry(entryId)
        if (entry == null) {
            _state.update { it.copy(isLoading = false, notFound = true) }
            return
        }
        amountInitialized = true
        _state.update {
            it.copy(
                entry = entry,
                meal = entry.mealType,
                date = entry.date,
                photoPath = entry.photoPath,
                mode = AmountMode.GRAMS,
                gramsText = entry.grams.toInputString()
            )
        }
        val foodFlow = entry.foodId?.let { foodRepository.observeFood(it) } ?: flowOf(null)
        foodFlow.collect { food ->
            // Продукт удалён — работаем со снимком КБЖУ из записи
            val effective = food ?: Food(
                name = entry.name,
                brand = entry.brand,
                per100 = entry.per100,
                source = FoodSource.USER
            )
            onFoodLoaded(effective)
        }
    }

    private suspend fun loadFood(foodId: Long?) {
        if (foodId == null) {
            _state.update { it.copy(isLoading = false, notFound = true) }
            return
        }
        // Подписка: после правки продукта или отметки «избранное» карточка обновится сама
        foodRepository.observeFood(foodId).collect { food ->
            if (food == null) {
                _state.update { it.copy(isLoading = false, notFound = true) }
            } else {
                onFoodLoaded(food)
            }
        }
    }

    private suspend fun onFoodLoaded(food: Food) {
        val ingredients = if (food.isRecipe && food.isSaved) foodRepository.getRecipeIngredients(food.id) else emptyList()
        _state.update { s ->
            var next = s.copy(isLoading = false, notFound = false, food = food, ingredients = ingredients)
            if (!amountInitialized) {
                amountInitialized = true
                next = if (food.servingSize != null) {
                    next.copy(mode = AmountMode.SERVINGS, servingsText = "1")
                } else {
                    next.copy(mode = AmountMode.GRAMS, gramsText = "100")
                }
            }
            // Если у продукта пропала порция (её удалили при редактировании) — переключаемся на граммы
            if (next.mode == AmountMode.SERVINGS && food.servingSize == null) {
                next = next.copy(mode = AmountMode.GRAMS, gramsText = (s.grams ?: 100.0).toInputString())
            }
            next
        }
    }

    // ---------- Количество ----------
    fun setMode(mode: AmountMode) = _state.update { s ->
        if (s.mode == mode) return@update s
        val grams = s.grams
        when (mode) {
            AmountMode.GRAMS -> s.copy(mode = mode, gramsText = grams?.toInputString() ?: s.gramsText)
            AmountMode.SERVINGS -> {
                val size = s.servingSize ?: return@update s
                s.copy(mode = mode, servingsText = grams?.let { (it / size).toInputString() } ?: "1")
            }
        }
    }

    fun onGramsChange(value: String) {
        sanitizeDecimal(value, maxDecimals = 1, maxLength = 6)?.let { v -> _state.update { it.copy(gramsText = v) } }
    }

    fun onServingsChange(value: String) {
        sanitizeDecimal(value, maxDecimals = 2, maxLength = 5)?.let { v -> _state.update { it.copy(servingsText = v) } }
    }

    fun setQuickAmount(value: Double) = _state.update { s ->
        when (s.mode) {
            AmountMode.GRAMS -> s.copy(gramsText = value.toInputString())
            AmountMode.SERVINGS -> s.copy(servingsText = value.toInputString())
        }
    }

    fun selectMeal(meal: MealType) = _state.update { it.copy(meal = meal) }

    fun toggleFavorite() {
        val food = _state.value.food ?: return
        if (!food.isSaved) return
        viewModelScope.launch { foodRepository.setFavorite(food.id, !food.isFavorite) }
    }

    // ---------- Фото к записи ----------
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
                _events.send(FoodDetailEvent.Message(R.string.error_photo))
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
        viewModelScope.launch { _events.send(FoodDetailEvent.Message(R.string.error_camera_permission)) }
    }

    // ---------- Сохранение ----------
    fun save() {
        val s = _state.value
        val food = s.food ?: return
        val grams = s.grams ?: return
        if (!s.canSave) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val entry = s.entry
            if (entry != null) {
                updateDiaryEntry(
                    entry.copy(
                        grams = grams,
                        mealType = s.meal,
                        photoPath = s.photoPath,
                        // Если продукт с тех пор исправили — запись получит актуальные КБЖУ
                        per100 = food.per100,
                        name = food.name,
                        brand = food.brand
                    )
                )
            } else {
                addFoodToDiary(food, grams, s.meal, s.date, s.photoPath)
            }
            photos.commit(kept = s.photoPath)
            saved = true
            _state.update { it.copy(isSaving = false) }
            _events.send(FoodDetailEvent.Done)
        }
    }

    fun deleteEntry() {
        val entry = _state.value.entry ?: return
        viewModelScope.launch {
            deleteDiaryEntry(entry)
            photos.commit(kept = null)
            saved = true
            _events.send(FoodDetailEvent.Done)
        }
    }

    override fun onCleared() {
        if (!saved) photos.discardAll()
        super.onCleared()
    }

    companion object {
        val QUICK_GRAMS = listOf(50.0, 100.0, 150.0, 200.0, 250.0)
        val QUICK_SERVINGS = listOf(0.5, 1.0, 1.5, 2.0, 3.0)
    }
}
