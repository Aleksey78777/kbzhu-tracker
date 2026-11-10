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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.domain.model.FoodCategory
import ru.kbzhu.tracker.domain.model.FoodSource
import ru.kbzhu.tracker.domain.model.FoodValidationError
import ru.kbzhu.tracker.domain.model.Nutrition
import ru.kbzhu.tracker.domain.model.SaveFoodResult
import ru.kbzhu.tracker.domain.repository.FoodRepository
import ru.kbzhu.tracker.domain.repository.PhotoStorage
import ru.kbzhu.tracker.domain.usecase.DeleteFoodUseCase
import ru.kbzhu.tracker.domain.usecase.SaveFoodUseCase
import ru.kbzhu.tracker.ui.navigation.FoodEditRoute
import ru.kbzhu.tracker.ui.util.parseDecimal
import ru.kbzhu.tracker.ui.util.sanitizeDecimal
import ru.kbzhu.tracker.ui.util.toInputString
import javax.inject.Inject
import kotlin.math.roundToInt

data class FoodEditUiState(
    val isLoading: Boolean = false,
    val isNew: Boolean = true,
    val original: Food? = null,
    val name: String = "",
    val brand: String = "",
    val category: FoodCategory = FoodCategory.OTHER,
    val calories: String = "",
    val protein: String = "",
    val fat: String = "",
    val carbs: String = "",
    val servingSize: String = "",
    val servingName: String = "",
    val barcode: String = "",
    val photoPath: String? = null,
    val isPhotoProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val error: FoodValidationError? = null,
    val showErrors: Boolean = false
) {
    /** Калорийность по БЖУ (4/9/4) — подсказка, если пользователь не знает ккал. */
    val caloriesFromMacros: Int?
        get() {
            val p = protein.parseDecimal() ?: return null
            val f = fat.parseDecimal() ?: return null
            val c = carbs.parseDecimal() ?: return null
            return (p * 4 + f * 9 + c * 4).roundToInt()
        }

    val nameError: Boolean get() = showErrors && name.isBlank()
    val caloriesError: Boolean get() = showErrors && calories.parseDecimal() == null
}

sealed interface FoodEditEvent {
    data class Saved(val id: Long, val isNew: Boolean) : FoodEditEvent
    data object Deleted : FoodEditEvent
    data class Message(@StringRes val res: Int) : FoodEditEvent
}

@HiltViewModel
class FoodEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
    private val photoStorage: PhotoStorage,
    private val saveFood: SaveFoodUseCase,
    private val deleteFood: DeleteFoodUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<FoodEditRoute>()
    private val photos = PendingPhotoTracker(photoStorage)
    private var finished = false

    private val _state = MutableStateFlow(FoodEditUiState(barcode = route.barcode.orEmpty()))
    val state: StateFlow<FoodEditUiState> = _state.asStateFlow()

    private val _events = Channel<FoodEditEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        route.foodId?.let { id ->
            _state.update { it.copy(isLoading = true, isNew = false) }
            viewModelScope.launch {
                val food = foodRepository.getFood(id)
                if (food == null) {
                    _state.update { it.copy(isLoading = false, isNew = true) }
                    return@launch
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        // Встроенный продукт сохранится как новый «свой» (копия)
                        isNew = food.source == FoodSource.BUILT_IN,
                        original = food,
                        name = food.name,
                        brand = food.brand.orEmpty(),
                        category = food.category,
                        calories = food.per100.calories.toInputString(),
                        protein = food.per100.protein.toInputString(),
                        fat = food.per100.fat.toInputString(),
                        carbs = food.per100.carbs.toInputString(),
                        servingSize = food.servingSize?.toInputString().orEmpty(),
                        servingName = food.servingName.orEmpty(),
                        barcode = food.barcode.orEmpty(),
                        photoPath = food.photoPath
                    )
                }
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v.take(100), error = null) }
    fun onBrand(v: String) = _state.update { it.copy(brand = v.take(60)) }
    fun onCategory(c: FoodCategory) = _state.update { it.copy(category = c) }
    fun onCalories(v: String) = decimal(v, 6) { s, x -> s.copy(calories = x) }
    fun onProtein(v: String) = decimal(v, 5) { s, x -> s.copy(protein = x) }
    fun onFat(v: String) = decimal(v, 5) { s, x -> s.copy(fat = x) }
    fun onCarbs(v: String) = decimal(v, 5) { s, x -> s.copy(carbs = x) }
    fun onServingSize(v: String) = decimal(v, 6) { s, x -> s.copy(servingSize = x) }
    fun onServingName(v: String) = _state.update { it.copy(servingName = v.take(30)) }
    fun onBarcode(v: String) {
        if (v.length <= 14 && v.all { it.isDigit() }) _state.update { it.copy(barcode = v) }
    }

    fun applyCaloriesFromMacros() {
        val kcal = _state.value.caloriesFromMacros ?: return
        _state.update { it.copy(calories = kcal.toString(), error = null) }
    }

    private inline fun decimal(v: String, maxLen: Int, crossinline apply: (FoodEditUiState, String) -> FoodEditUiState) {
        val clean = sanitizeDecimal(v, maxDecimals = 1, maxLength = maxLen) ?: return
        _state.update { apply(it, clean).copy(error = null) }
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
                _events.send(FoodEditEvent.Message(R.string.error_photo))
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
        viewModelScope.launch { _events.send(FoodEditEvent.Message(R.string.error_camera_permission)) }
    }

    // ---------- Сохранение / удаление ----------
    fun save() {
        val s = _state.value
        val kcal = s.calories.parseDecimal()
        if (s.name.isBlank() || kcal == null) {
            _state.update { it.copy(showErrors = true, error = if (s.name.isBlank()) FoodValidationError.EMPTY_NAME else FoodValidationError.INVALID_NUMBERS) }
            return
        }
        val original = s.original
        val food = Food(
            id = original?.id ?: 0,
            name = s.name,
            brand = s.brand,
            category = s.category,
            per100 = Nutrition(
                calories = kcal,
                protein = s.protein.parseDecimal() ?: 0.0,
                fat = s.fat.parseDecimal() ?: 0.0,
                carbs = s.carbs.parseDecimal() ?: 0.0
            ),
            servingSize = s.servingSize.parseDecimal()?.takeIf { it > 0 },
            servingName = s.servingName.takeIf { it.isNotBlank() && s.servingSize.isNotBlank() },
            barcode = s.barcode,
            photoPath = s.photoPath,
            // OFF-продукт после правки становится «своим»
            source = if (original?.source == FoodSource.RECIPE) FoodSource.RECIPE else FoodSource.USER,
            imageUrl = original?.imageUrl,
            isFavorite = original?.isFavorite ?: false,
            lastUsedAt = original?.lastUsedAt
        )
        _state.update { it.copy(isSaving = true, showErrors = true) }
        viewModelScope.launch {
            when (val result = saveFood(food)) {
                is SaveFoodResult.Success -> {
                    photos.commit(kept = s.photoPath)
                    finished = true
                    _state.update { it.copy(isSaving = false) }
                    _events.send(FoodEditEvent.Saved(result.id, isNew = s.isNew))
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
            _events.send(FoodEditEvent.Deleted)
        }
    }

    override fun onCleared() {
        if (!finished) photos.discardAll()
        super.onCleared()
    }
}
