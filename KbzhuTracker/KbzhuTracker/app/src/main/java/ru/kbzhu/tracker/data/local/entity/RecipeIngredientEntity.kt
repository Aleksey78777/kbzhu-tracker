package ru.kbzhu.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Ингредиент рецепта: ссылка на блюдо (recipe_id) и на продукт (food_id) с весом. */
@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recipe_id"), Index("food_id")]
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    @ColumnInfo(name = "food_id") val foodId: Long,
    val grams: Double
)

/** Ингредиент вместе с данными продукта — для экрана редактирования рецепта. */
data class IngredientWithFood(
    @Embedded val ingredient: RecipeIngredientEntity,
    @Relation(parentColumn = "food_id", entityColumn = "id")
    val food: FoodEntity
)
