package ru.kbzhu.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import ru.kbzhu.tracker.data.local.entity.IngredientWithFood
import ru.kbzhu.tracker.data.local.entity.RecipeIngredientEntity

@Dao
interface RecipeDao {

    @Transaction
    @Query("SELECT * FROM recipe_ingredients WHERE recipe_id = :recipeId ORDER BY id")
    suspend fun getIngredients(recipeId: Long): List<IngredientWithFood>

    @Insert
    suspend fun insertIngredients(items: List<RecipeIngredientEntity>)

    @Query("DELETE FROM recipe_ingredients WHERE recipe_id = :recipeId")
    suspend fun deleteIngredients(recipeId: Long)

    /** Полная замена состава рецепта одной транзакцией. */
    @Transaction
    suspend fun replaceIngredients(recipeId: Long, items: List<RecipeIngredientEntity>) {
        deleteIngredients(recipeId)
        insertIngredients(items.map { it.copy(id = 0, recipeId = recipeId) })
    }
}
