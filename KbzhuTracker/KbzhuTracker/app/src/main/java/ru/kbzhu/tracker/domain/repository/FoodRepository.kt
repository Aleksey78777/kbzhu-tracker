package ru.kbzhu.tracker.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.kbzhu.tracker.domain.model.BarcodeLookupResult
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.domain.model.FoodCategory
import ru.kbzhu.tracker.domain.model.RecipeIngredient

interface FoodRepository {
    fun search(query: String, category: FoodCategory?, onlyFavorites: Boolean, limit: Int): Flow<List<Food>>
    fun observeRecent(limit: Int = 20): Flow<List<Food>>
    fun observeFavorites(): Flow<List<Food>>
    /** Свои продукты и рецепты */
    fun observeMyFoods(): Flow<List<Food>>
    fun observeFood(id: Long): Flow<Food?>
    suspend fun getFood(id: Long): Food?

    /** Вставка (id = 0) или обновление. Возвращает id. */
    suspend fun saveFood(food: Food): Long
    suspend fun deleteFood(id: Long)
    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun markUsed(id: Long)

    suspend fun getRecipeIngredients(recipeId: Long): List<RecipeIngredient>
    /** Сохраняет блюдо и его состав в одной транзакции. */
    suspend fun saveRecipe(recipe: Food, ingredients: List<RecipeIngredient>): Long

    /** Сохраняет в БД продукт из онлайн-поиска (или возвращает id уже существующего с тем же штрих-кодом). */
    suspend fun cacheRemoteFood(food: Food): Long
}

interface BarcodeRepository {
    /** Сначала ищет в локальной БД, затем в Open Food Facts, найденное кэширует. */
    suspend fun lookup(barcode: String): BarcodeLookupResult
    /** Онлайн-поиск по названию. Ничего не сохраняет. */
    suspend fun searchOnline(query: String): Result<List<Food>>
}
