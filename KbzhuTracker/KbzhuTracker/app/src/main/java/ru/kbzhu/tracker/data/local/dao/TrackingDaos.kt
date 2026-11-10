package ru.kbzhu.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.kbzhu.tracker.data.local.entity.WaterEntity
import ru.kbzhu.tracker.data.local.entity.WeightEntity

@Dao
interface WaterDao {

    @Query("SELECT * FROM water WHERE date = :date")
    fun observe(date: Long): Flow<WaterEntity?>

    @Query("SELECT * FROM water WHERE date = :date")
    suspend fun get(date: Long): WaterEntity?

    @Upsert
    suspend fun upsert(entity: WaterEntity)

    @Query("SELECT * FROM water WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(from: Long, to: Long): Flow<List<WaterEntity>>
}

@Dao
interface WeightDao {

    @Query("SELECT * FROM weight ORDER BY date")
    fun observeAll(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weight WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(from: Long, to: Long): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weight ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<WeightEntity?>

    @Upsert
    suspend fun upsert(entity: WeightEntity)

    @Query("DELETE FROM weight WHERE date = :date")
    suspend fun delete(date: Long)
}
