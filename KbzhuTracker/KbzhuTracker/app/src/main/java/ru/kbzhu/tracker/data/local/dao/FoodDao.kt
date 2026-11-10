package ru.kbzhu.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.kbzhu.tracker.data.local.entity.FoodEntity

@Dao
interface FoodDao {

    @Query("SELECT * FROM foods WHERE id = :id")
    fun observeById(id: Long): Flow<FoodEntity?>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: Long): FoodEntity?

    @Query("SELECT * FROM foods WHERE barcode = :barcode ORDER BY source = 'USER' DESC LIMIT 1")
    suspend fun getByBarcode(barcode: String): FoodEntity?

    /**
     * Поиск по нормализованной строке. Сначала совпадения с начала названия,
     * затем — по частоте использования.
     * query уже нормализован (toSearchKey) и экранирован (escapeLike).
     */
    @Query(
        """
        SELECT * FROM foods
        WHERE search_key LIKE '%' || :query || '%' ESCAPE '\'
          AND (:category IS NULL OR category = :category)
          AND (:onlyFavorites = 0 OR is_favorite = 1)
        ORDER BY
          CASE WHEN search_key LIKE :query || '%' ESCAPE '\' THEN 0 ELSE 1 END,
          use_count DESC,
          name COLLATE NOCASE
        LIMIT :limit
        """
    )
    fun search(
        query: String,
        category: String?,
        onlyFavorites: Boolean,
        limit: Int
    ): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE last_used_at IS NOT NULL ORDER BY last_used_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE is_favorite = 1 ORDER BY name COLLATE NOCASE")
    fun observeFavorites(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE source IN (:sources) ORDER BY created_at DESC")
    fun observeBySources(sources: List<String>): Flow<List<FoodEntity>>

    /** Внимание: без REPLACE — замена удалила бы строку и каскадно стёрла ингредиенты. */
    @Insert
    suspend fun insert(food: FoodEntity): Long

    @Insert
    suspend fun insertAll(foods: List<FoodEntity>)

    @Update
    suspend fun update(food: FoodEntity)

    @Query("UPDATE foods SET is_favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE foods SET last_used_at = :time, use_count = use_count + 1 WHERE id = :id")
    suspend fun markUsed(id: Long, time: Long)

    @Query("DELETE FROM foods WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int
}
