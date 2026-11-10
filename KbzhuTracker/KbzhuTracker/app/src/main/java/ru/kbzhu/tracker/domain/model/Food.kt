package ru.kbzhu.tracker.domain.model

/** Продукт или блюдо. [per100] — КБЖУ на 100 г. id = 0 — продукт ещё не сохранён (например, из онлайн-поиска). */
data class Food(
    val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val category: FoodCategory = FoodCategory.OTHER,
    val per100: Nutrition,
    val servingSize: Double? = null,
    val servingName: String? = null,
    val barcode: String? = null,
    val photoPath: String? = null,
    val imageUrl: String? = null,
    val source: FoodSource = FoodSource.USER,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long? = null,
    val recipeYield: Double? = null,
    val recipePortions: Int? = null
) {
    val isSaved: Boolean get() = id != 0L
    val isRecipe: Boolean get() = source == FoodSource.RECIPE
    /** Встроенные продукты не редактируются (только в избранное). */
    val isUserEditable: Boolean get() = source != FoodSource.BUILT_IN
    /** Что показывать картинкой: своё фото приоритетнее картинки из интернета. */
    val displayImage: String? get() = photoPath ?: imageUrl
}

data class RecipeIngredient(val food: Food, val grams: Double) {
    val nutrition: Nutrition get() = food.per100.forGrams(grams)
}

data class Recipe(val food: Food, val ingredients: List<RecipeIngredient>)

/** Ошибки валидации при сохранении продукта/рецепта. */
enum class FoodValidationError {
    EMPTY_NAME,
    INVALID_NUMBERS,
    MACROS_EXCEED_100G,
    NO_INGREDIENTS,
    INVALID_WEIGHT
}

sealed interface SaveFoodResult {
    data class Success(val id: Long) : SaveFoodResult
    data class Invalid(val error: FoodValidationError) : SaveFoodResult
}
