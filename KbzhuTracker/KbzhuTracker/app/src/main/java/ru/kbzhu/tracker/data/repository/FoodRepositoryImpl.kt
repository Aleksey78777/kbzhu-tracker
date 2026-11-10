package ru.kbzhu.tracker.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.kbzhu.tracker.core.util.escapeLike
import ru.kbzhu.tracker.core.util.toSearchKey
import ru.kbzhu.tracker.data.local.AppDatabase
import ru.kbzhu.tracker.data.local.dao.FoodDao
import ru.kbzhu.tracker.data.local.dao.RecipeDao
import ru.kbzhu.tracker.data.local.entity.RecipeIngredientEntity
import ru.kbzhu.tracker.data.mapper.toDomain
import ru.kbzhu.tracker.data.mapper.toNewEntity
import ru.kbzhu.tracker.data.mapper.updatedFrom
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.domain.model.FoodCategory
import ru.kbzhu.tracker.domain.model.FoodSource
import ru.kbzhu.tracker.domain.model.RecipeIngredient
import ru.kbzhu.tracker.domain.repository.FoodRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val foodDao: FoodDao,
    private val recipeDao: RecipeDao
) : FoodRepository {

    override fun search(
        query: String,
        category: FoodCategory?,
        onlyFavorites: Boolean,
        limit: Int
    ): Flow<List<Food>> =
        foodDao.search(query.toSearchKey().escapeLike(), category?.name, onlyFavorites, limit)
            .map { list -> list.map { it.toDomain() } }

    override fun observeRecent(limit: Int): Flow<List<Food>> =
        foodDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun observeFavorites(): Flow<List<Food>> =
        foodDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    override fun observeMyFoods(): Flow<List<Food>> =
        foodDao.observeBySources(listOf(FoodSource.USER.name, FoodSource.RECIPE.name))
            .map { list -> list.map { it.toDomain() } }

    override fun observeFood(id: Long): Flow<Food?> = foodDao.observeById(id).map { it?.toDomain() }

    override suspend fun getFood(id: Long): Food? = foodDao.getById(id)?.toDomain()

    override suspend fun saveFood(food: Food): Long {
        val existing = if (food.isSaved) foodDao.getById(food.id) else null
        return if (existing == null) {
            foodDao.insert(food.toNewEntity(System.currentTimeMillis()))
        } else {
            foodDao.update(existing.updatedFrom(food))
            existing.id
        }
    }

    override suspend fun deleteFood(id: Long) = foodDao.deleteById(id)

    override suspend fun setFavorite(id: Long, favorite: Boolean) = foodDao.setFavorite(id, favorite)

    override suspend fun markUsed(id: Long) = foodDao.markUsed(id, System.currentTimeMillis())

    override suspend fun getRecipeIngredients(recipeId: Long): List<RecipeIngredient> =
        recipeDao.getIngredients(recipeId).map { RecipeIngredient(it.food.toDomain(), it.ingredient.grams) }

    override suspend fun saveRecipe(recipe: Food, ingredients: List<RecipeIngredient>): Long =
        db.withTransaction {
            val id = saveFood(recipe)
            recipeDao.replaceIngredients(
                id,
                ingredients.map { RecipeIngredientEntity(recipeId = id, foodId = it.food.id, grams = it.grams) }
            )
            id
        }

    override suspend fun cacheRemoteFood(food: Food): Long {
        food.barcode?.let { code -> foodDao.getByBarcode(code)?.let { return it.id } }
        return foodDao.insert(food.copy(id = 0).toNewEntity(System.currentTimeMillis()))
    }
}
