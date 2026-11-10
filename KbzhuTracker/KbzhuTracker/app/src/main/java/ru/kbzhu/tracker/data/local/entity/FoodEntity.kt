package ru.kbzhu.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.kbzhu.tracker.domain.model.FoodCategory
import ru.kbzhu.tracker.domain.model.FoodSource

/**
 * Продукт или блюдо. Все значения КБЖУ — на 100 г.
 * Для рецептов (source = RECIPE) КБЖУ на 100 г вычисляется из ингредиентов,
 * а serving_size хранит общий вес готового блюда.
 */
@Entity(
    tableName = "foods",
    indices = [
        Index("barcode"),
        Index("search_key"),
        Index("source"),
        Index("is_favorite"),
        Index("last_used_at")
    ]
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String?,
    val category: FoodCategory,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    @ColumnInfo(name = "serving_size") val servingSize: Double?,
    @ColumnInfo(name = "serving_name") val servingName: String?,
    val barcode: String?,
    /** Локальный путь к сжатому фото во внутреннем хранилище */
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    /** URL картинки из Open Food Facts (грузится через Coil, кэшируется) */
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    val source: FoodSource,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long? = null,
    @ColumnInfo(name = "use_count") val useCount: Int = 0,
    /** Только для рецептов: вес готового блюда, г */
    @ColumnInfo(name = "recipe_yield") val recipeYield: Double? = null,
    /** Только для рецептов: на сколько порций рассчитано блюдо */
    @ColumnInfo(name = "recipe_portions") val recipePortions: Int? = null,
    @ColumnInfo(name = "search_key") val searchKey: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
