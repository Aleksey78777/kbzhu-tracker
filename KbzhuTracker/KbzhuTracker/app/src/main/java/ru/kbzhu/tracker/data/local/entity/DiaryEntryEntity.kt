package ru.kbzhu.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.kbzhu.tracker.domain.model.MealType

/**
 * Запись дневника. КБЖУ на 100 г копируется из продукта в момент добавления,
 * поэтому удаление или правка продукта не «ломает» историю.
 * date — LocalDate.toEpochDay().
 */
@Entity(
    tableName = "diary_entries",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("date"), Index("food_id")]
)
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    @ColumnInfo(name = "meal_type") val mealType: MealType,
    @ColumnInfo(name = "food_id") val foodId: Long?,
    val name: String,
    val brand: String?,
    val grams: Double,
    @ColumnInfo(name = "calories_per_100") val caloriesPer100: Double,
    @ColumnInfo(name = "protein_per_100") val proteinPer100: Double,
    @ColumnInfo(name = "fat_per_100") val fatPer100: Double,
    @ColumnInfo(name = "carbs_per_100") val carbsPer100: Double,
    /** Фото, прикреплённое именно к этой записи */
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

/** Запись + фото продукта (если своего фото у записи нет — показываем фото продукта). */
data class DiaryEntryWithFoodImage(
    @Embedded val entry: DiaryEntryEntity,
    @ColumnInfo(name = "food_photo_path") val foodPhotoPath: String?,
    @ColumnInfo(name = "food_image_url") val foodImageUrl: String?
)

/** Агрегат КБЖУ за день — для статистики. */
data class DailyTotalsTuple(
    val date: Long,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double
)
