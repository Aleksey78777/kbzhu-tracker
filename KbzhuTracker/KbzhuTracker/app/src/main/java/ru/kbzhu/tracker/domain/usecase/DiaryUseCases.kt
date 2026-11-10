package ru.kbzhu.tracker.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ru.kbzhu.tracker.domain.model.DaySummary
import ru.kbzhu.tracker.domain.model.DiaryEntry
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.domain.model.MealGroup
import ru.kbzhu.tracker.domain.model.MealType
import ru.kbzhu.tracker.domain.model.total
import ru.kbzhu.tracker.domain.repository.DiaryRepository
import ru.kbzhu.tracker.domain.repository.FoodRepository
import ru.kbzhu.tracker.domain.repository.PhotoStorage
import ru.kbzhu.tracker.domain.repository.SettingsRepository
import ru.kbzhu.tracker.domain.repository.WaterRepository
import java.time.LocalDate
import javax.inject.Inject

/** Всё, что нужно главному экрану за день: приёмы пищи, итоги, норма, вода. */
class ObserveDaySummaryUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(date: LocalDate): Flow<DaySummary> = combine(
        diaryRepository.observeEntries(date),
        waterRepository.observe(date),
        settingsRepository.settings
    ) { entries, waterMl, settings ->
        DaySummary(
            date = date,
            meals = MealType.entries.map { type -> MealGroup(type, entries.filter { it.mealType == type }) },
            eaten = entries.map { it.nutrition }.total(),
            targets = settings.targets,
            waterMl = waterMl,
            waterGoalMl = settings.waterGoalMl,
            waterPortionMl = settings.waterPortionMl
        )
    }
}

class AddFoodToDiaryUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val foodRepository: FoodRepository
) {
    /** [food] должен быть сохранён в БД (для онлайн-результатов сначала CacheRemoteFoodUseCase). */
    suspend operator fun invoke(
        food: Food,
        grams: Double,
        mealType: MealType,
        date: LocalDate,
        photoPath: String? = null
    ): Long {
        require(grams > 0) { "Граммовка должна быть больше нуля" }
        val id = diaryRepository.addEntry(
            DiaryEntry(
                date = date,
                mealType = mealType,
                foodId = food.id.takeIf { food.isSaved },
                name = food.name,
                brand = food.brand,
                grams = grams,
                per100 = food.per100,
                photoPath = photoPath
            )
        )
        if (food.isSaved) foodRepository.markUsed(food.id)
        return id
    }
}

class UpdateDiaryEntryUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val photoStorage: PhotoStorage
) {
    suspend operator fun invoke(entry: DiaryEntry) {
        require(entry.grams > 0) { "Граммовка должна быть больше нуля" }
        val old = diaryRepository.getEntry(entry.id)
        diaryRepository.updateEntry(entry)
        val oldPhoto = old?.photoPath
        if (oldPhoto != null && oldPhoto != entry.photoPath && !diaryRepository.isPhotoUsed(oldPhoto)) {
            photoStorage.delete(oldPhoto)
        }
    }
}

/**
 * Удаление записи. Фото можно не удалять сразу ([deletePhoto] = false),
 * чтобы работала кнопка «Отменить» в снекбаре; тогда вызовите [cleanupPhoto] позже.
 */
class DeleteDiaryEntryUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val photoStorage: PhotoStorage
) {
    suspend operator fun invoke(entry: DiaryEntry, deletePhoto: Boolean = true) {
        diaryRepository.deleteEntry(entry.id)
        if (deletePhoto) cleanupPhoto(entry)
    }

    suspend fun cleanupPhoto(entry: DiaryEntry) {
        val path = entry.photoPath ?: return
        if (!diaryRepository.isPhotoUsed(path)) photoStorage.delete(path)
    }
}

/** Восстановление удалённой записи (кнопка «Отменить»). */
class RestoreDiaryEntryUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository
) {
    suspend operator fun invoke(entry: DiaryEntry): Long = diaryRepository.addEntry(entry.copy(id = 0))
}

class ChangeWaterUseCase @Inject constructor(
    private val waterRepository: WaterRepository
) {
    suspend operator fun invoke(date: LocalDate, deltaMl: Int) = waterRepository.add(date, deltaMl)
}
