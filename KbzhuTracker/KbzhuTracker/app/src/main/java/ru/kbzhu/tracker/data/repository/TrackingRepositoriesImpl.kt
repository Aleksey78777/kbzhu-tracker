package ru.kbzhu.tracker.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.kbzhu.tracker.core.util.toDbValue
import ru.kbzhu.tracker.data.local.dao.DiaryDao
import ru.kbzhu.tracker.data.local.dao.WaterDao
import ru.kbzhu.tracker.data.local.dao.WeightDao
import ru.kbzhu.tracker.data.local.entity.WaterEntity
import ru.kbzhu.tracker.data.local.entity.WeightEntity
import ru.kbzhu.tracker.data.mapper.toDomain
import ru.kbzhu.tracker.data.mapper.toEntity
import ru.kbzhu.tracker.domain.model.DailyStat
import ru.kbzhu.tracker.domain.model.DiaryEntry
import ru.kbzhu.tracker.domain.model.WeightRecord
import ru.kbzhu.tracker.domain.repository.DiaryRepository
import ru.kbzhu.tracker.domain.repository.WaterRepository
import ru.kbzhu.tracker.domain.repository.WeightRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class DiaryRepositoryImpl @Inject constructor(
    private val diaryDao: DiaryDao
) : DiaryRepository {

    override fun observeEntries(date: LocalDate): Flow<List<DiaryEntry>> =
        diaryDao.observeByDate(date.toDbValue()).map { list -> list.map { it.toDomain() } }

    override suspend fun getEntry(id: Long): DiaryEntry? = diaryDao.getById(id)?.toDomain()

    override suspend fun addEntry(entry: DiaryEntry): Long = diaryDao.insert(entry.toEntity())

    override suspend fun updateEntry(entry: DiaryEntry) = diaryDao.update(entry.toEntity())

    override suspend fun deleteEntry(id: Long) = diaryDao.deleteById(id)

    override fun observeDailyTotals(from: LocalDate, to: LocalDate): Flow<List<DailyStat>> =
        diaryDao.observeDailyTotals(from.toDbValue(), to.toDbValue())
            .map { list -> list.map { it.toDomain() } }

    override suspend fun isPhotoUsed(path: String): Boolean = diaryDao.countPhotoUsages(path) > 0
}

@Singleton
class WaterRepositoryImpl @Inject constructor(
    private val waterDao: WaterDao
) : WaterRepository {

    override fun observe(date: LocalDate): Flow<Int> =
        waterDao.observe(date.toDbValue()).map { it?.amountMl ?: 0 }

    override suspend fun add(date: LocalDate, deltaMl: Int) {
        val key = date.toDbValue()
        val current = waterDao.get(key)?.amountMl ?: 0
        waterDao.upsert(WaterEntity(key, (current + deltaMl).coerceIn(0, MAX_WATER_ML)))
    }

    private companion object {
        const val MAX_WATER_ML = 10_000
    }
}

@Singleton
class WeightRepositoryImpl @Inject constructor(
    private val weightDao: WeightDao
) : WeightRepository {

    override fun observeAll(): Flow<List<WeightRecord>> =
        weightDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<WeightRecord>> =
        weightDao.observeRange(from.toDbValue(), to.toDbValue()).map { list -> list.map { it.toDomain() } }

    override fun observeLatest(): Flow<WeightRecord?> = weightDao.observeLatest().map { it?.toDomain() }

    override suspend fun log(date: LocalDate, weightKg: Double) {
        // Храним с точностью до 0,1 кг
        val rounded = (weightKg * 10).roundToInt() / 10.0
        weightDao.upsert(WeightEntity(date.toDbValue(), rounded))
    }

    override suspend fun delete(date: LocalDate) = weightDao.delete(date.toDbValue())
}
