package ru.kbzhu.tracker.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.kbzhu.tracker.domain.model.DailyStat
import ru.kbzhu.tracker.domain.model.DiaryEntry
import ru.kbzhu.tracker.domain.model.NutritionTargets
import ru.kbzhu.tracker.domain.model.ThemeMode
import ru.kbzhu.tracker.domain.model.UserProfile
import ru.kbzhu.tracker.domain.model.UserSettings
import ru.kbzhu.tracker.domain.model.WeightRecord
import java.time.LocalDate

interface DiaryRepository {
    fun observeEntries(date: LocalDate): Flow<List<DiaryEntry>>
    suspend fun getEntry(id: Long): DiaryEntry?
    suspend fun addEntry(entry: DiaryEntry): Long
    suspend fun updateEntry(entry: DiaryEntry)
    suspend fun deleteEntry(id: Long)
    /** Только дни, в которые есть записи */
    fun observeDailyTotals(from: LocalDate, to: LocalDate): Flow<List<DailyStat>>
    suspend fun isPhotoUsed(path: String): Boolean
}

interface WaterRepository {
    fun observe(date: LocalDate): Flow<Int>
    /** Прибавляет (или убавляет при отрицательном значении) воду, не уходя ниже нуля. */
    suspend fun add(date: LocalDate, deltaMl: Int)
}

interface WeightRepository {
    fun observeAll(): Flow<List<WeightRecord>>
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<WeightRecord>>
    fun observeLatest(): Flow<WeightRecord?>
    suspend fun log(date: LocalDate, weightKg: Double)
    suspend fun delete(date: LocalDate)
}

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun saveProfile(profile: UserProfile, targets: NutritionTargets, manualTargets: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setWaterSettings(goalMl: Int, portionMl: Int)
}

/** Работа с фото во внутреннем хранилище. Пути и URI передаются строками — домен не зависит от Android. */
interface PhotoStorage {
    /** Сжимает изображение по URI (камера/галерея) и сохраняет; возвращает абсолютный путь файла. */
    suspend fun importPhoto(sourceUri: String): String
    /** URI временного файла для системной камеры (FileProvider). */
    fun createCameraUri(): String
    /** Удаляет временный файл, если съёмка отменена. */
    fun discardCameraUri(uri: String)
    suspend fun delete(path: String)
}
