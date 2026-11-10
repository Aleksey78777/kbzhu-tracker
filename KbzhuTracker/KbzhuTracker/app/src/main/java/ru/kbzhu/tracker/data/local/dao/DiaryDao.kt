package ru.kbzhu.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.kbzhu.tracker.data.local.entity.DailyTotalsTuple
import ru.kbzhu.tracker.data.local.entity.DiaryEntryEntity
import ru.kbzhu.tracker.data.local.entity.DiaryEntryWithFoodImage

@Dao
interface DiaryDao {

    @Query(
        """
        SELECT d.*, f.photo_path AS food_photo_path, f.image_url AS food_image_url
        FROM diary_entries d
        LEFT JOIN foods f ON d.food_id = f.id
        WHERE d.date = :date
        ORDER BY d.created_at
        """
    )
    fun observeByDate(date: Long): Flow<List<DiaryEntryWithFoodImage>>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getById(id: Long): DiaryEntryEntity?

    @Insert
    suspend fun insert(entry: DiaryEntryEntity): Long

    @Update
    suspend fun update(entry: DiaryEntryEntity)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        SELECT date,
               SUM(calories_per_100 * grams / 100.0) AS calories,
               SUM(protein_per_100  * grams / 100.0) AS protein,
               SUM(fat_per_100      * grams / 100.0) AS fat,
               SUM(carbs_per_100    * grams / 100.0) AS carbs
        FROM diary_entries
        WHERE date BETWEEN :from AND :to
        GROUP BY date
        ORDER BY date
        """
    )
    fun observeDailyTotals(from: Long, to: Long): Flow<List<DailyTotalsTuple>>

    /** Все фото записей — чтобы при удалении записи удалить и файл, если он больше не нужен. */
    @Query("SELECT COUNT(*) FROM diary_entries WHERE photo_path = :path")
    suspend fun countPhotoUsages(path: String): Int
}
