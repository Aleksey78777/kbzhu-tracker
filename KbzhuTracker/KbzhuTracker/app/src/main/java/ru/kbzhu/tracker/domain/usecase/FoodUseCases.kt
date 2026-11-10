package ru.kbzhu.tracker.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.kbzhu.tracker.domain.model.BarcodeLookupResult
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.domain.model.FoodCategory
import ru.kbzhu.tracker.domain.model.FoodSource
import ru.kbzhu.tracker.domain.model.FoodValidationError
import ru.kbzhu.tracker.domain.model.Nutrition
import ru.kbzhu.tracker.domain.model.Recipe
import ru.kbzhu.tracker.domain.model.RecipeIngredient
import ru.kbzhu.tracker.domain.model.SaveFoodResult
import ru.kbzhu.tracker.domain.model.total
import ru.kbzhu.tracker.domain.repository.BarcodeRepository
import ru.kbzhu.tracker.domain.repository.FoodRepository
import ru.kbzhu.tracker.domain.repository.PhotoStorage
import javax.inject.Inject

class SearchFoodsUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) {
    operator fun invoke(
        query: String,
        category: FoodCategory? = null,
        onlyFavorites: Boolean = false,
        limit: Int = 100
    ): Flow<List<Food>> = foodRepository.search(query.trim(), category, onlyFavorites, limit)
}

class SearchOnlineUseCase @Inject constructor(
    private val barcodeRepository: BarcodeRepository
) {
    suspend operator fun invoke(query: String): Result<List<Food>> {
        val q = query.trim()
        if (q.length < 2) return Result.success(emptyList())
        return barcodeRepository.searchOnline(q)
    }
}

class LookupBarcodeUseCase @Inject constructor(
    private val barcodeRepository: BarcodeRepository
) {
    suspend operator fun invoke(barcode: String): BarcodeLookupResult =
        barcodeRepository.lookup(barcode.trim())
}

class CacheRemoteFoodUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) {
    /** Возвращает сохранённую копию продукта (с id). */
    suspend operator fun invoke(food: Food): Food {
        if (food.isSaved) return food
        val id = foodRepository.cacheRemoteFood(food)
        return foodRepository.getFood(id) ?: food.copy(id = id)
    }
}

/** Валидация и сохранение своего продукта. При замене фото старый файл удаляется. */
class SaveFoodUseCase @Inject constructor(
    private val foodRepository: FoodRepository,
    private val photoStorage: PhotoStorage
) {
    suspend operator fun invoke(food: Food): SaveFoodResult {
        validate(food)?.let { return SaveFoodResult.Invalid(it) }
        val old = if (food.isSaved) foodRepository.getFood(food.id) else null
        val toSave = food.copy(
            name = food.name.trim(),
            brand = food.brand?.trim()?.takeIf { it.isNotEmpty() },
            barcode = food.barcode?.trim()?.takeIf { it.isNotEmpty() },
            servingName = food.servingName?.trim()?.takeIf { it.isNotEmpty() },
            // Встроенный продукт не меняем — сохраняем как новый свой
            source = if (food.source == FoodSource.BUILT_IN) FoodSource.USER else food.source,
            id = if (old?.source == FoodSource.BUILT_IN) 0 else food.id
        )
        val id = foodRepository.saveFood(toSave)
        val oldPhoto = old?.photoPath
        if (oldPhoto != null && oldPhoto != toSave.photoPath) photoStorage.delete(oldPhoto)
        return SaveFoodResult.Success(id)
    }

    private fun validate(food: Food): FoodValidationError? {
        if (food.name.isBlank()) return FoodValidationError.EMPTY_NAME
        val n = food.per100
        val values = listOf(n.calories, n.protein, n.fat, n.carbs)
        if (values.any { it.isNaN() || it < 0 }) return FoodValidationError.INVALID_NUMBERS
        if (n.calories > 950) return FoodValidationError.INVALID_NUMBERS
        if (n.protein + n.fat + n.carbs > 100.5) return FoodValidationError.MACROS_EXCEED_100G
        food.servingSize?.let { if (it <= 0 || it > 5000) return FoodValidationError.INVALID_WEIGHT }
        return null
    }
}

class DeleteFoodUseCase @Inject constructor(
    private val foodRepository: FoodRepository,
    private val photoStorage: PhotoStorage
) {
    suspend operator fun invoke(food: Food) {
        require(food.isUserEditable) { "Встроенные продукты удалять нельзя" }
        foodRepository.deleteFood(food.id)
        food.photoPath?.let { photoStorage.delete(it) }
    }
}

class GetRecipeUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) {
    suspend operator fun invoke(recipeId: Long): Recipe? {
        val food = foodRepository.getFood(recipeId) ?: return null
        return Recipe(food, foodRepository.getRecipeIngredients(recipeId))
    }
}

/**
 * Рецепт: КБЖУ на 100 г = сумма КБЖУ ингредиентов × 100 / вес готового блюда.
 * Вес готового блюда можно указать (при варке/жарке он меняется), иначе — сумма весов ингредиентов.
 * Порция = вес блюда / число порций.
 */
class SaveRecipeUseCase @Inject constructor(
    private val foodRepository: FoodRepository,
    private val photoStorage: PhotoStorage
) {
    suspend operator fun invoke(
        recipeId: Long?,
        name: String,
        ingredients: List<RecipeIngredient>,
        cookedWeight: Double?,
        portions: Int,
        photoPath: String?,
        category: FoodCategory = FoodCategory.READY_MEALS
    ): SaveFoodResult {
        if (name.isBlank()) return SaveFoodResult.Invalid(FoodValidationError.EMPTY_NAME)
        if (ingredients.isEmpty()) return SaveFoodResult.Invalid(FoodValidationError.NO_INGREDIENTS)
        if (ingredients.any { it.grams <= 0 }) return SaveFoodResult.Invalid(FoodValidationError.INVALID_WEIGHT)

        val rawWeight = ingredients.sumOf { it.grams }
        val totalWeight = cookedWeight?.takeIf { it > 0 } ?: rawWeight
        if (totalWeight <= 0 || portions !in 1..100) {
            return SaveFoodResult.Invalid(FoodValidationError.INVALID_WEIGHT)
        }

        val total: Nutrition = ingredients.map { it.nutrition }.total()
        val per100 = total * (100.0 / totalWeight)
        val old = recipeId?.let { foodRepository.getFood(it) }

        val recipe = Food(
            id = recipeId ?: 0,
            name = name.trim(),
            category = category,
            per100 = per100,
            servingSize = totalWeight / portions,
            servingName = "1 порция",
            photoPath = photoPath,
            source = FoodSource.RECIPE,
            isFavorite = old?.isFavorite ?: false,
            lastUsedAt = old?.lastUsedAt,
            recipeYield = totalWeight,
            recipePortions = portions
        )
        val id = foodRepository.saveRecipe(recipe, ingredients)
        val oldPhoto = old?.photoPath
        if (oldPhoto != null && oldPhoto != photoPath) photoStorage.delete(oldPhoto)
        return SaveFoodResult.Success(id)
    }
}
