package ru.kbzhu.tracker.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ru.kbzhu.tracker.core.util.today
import ru.kbzhu.tracker.domain.model.DailyStat
import ru.kbzhu.tracker.domain.model.Nutrition
import ru.kbzhu.tracker.domain.model.StatsData
import ru.kbzhu.tracker.domain.model.StatsPeriod
import ru.kbzhu.tracker.domain.model.total
import ru.kbzhu.tracker.domain.repository.DiaryRepository
import ru.kbzhu.tracker.domain.repository.SettingsRepository
import ru.kbzhu.tracker.domain.repository.WeightRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs

class ObserveStatisticsUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val weightRepository: WeightRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(period: StatsPeriod, endDate: LocalDate = today()): Flow<StatsData> {
        val from = endDate.minusDays(period.days - 1L)
        return combine(
            diaryRepository.observeDailyTotals(from, endDate),
            weightRepository.observeRange(from, endDate),
            settingsRepository.settings
        ) { totals, weights, settings ->
            val byDate = totals.associateBy { it.date }
            val days = (0 until period.days).map { offset ->
                val date = from.plusDays(offset.toLong())
                byDate[date] ?: DailyStat(date, Nutrition.ZERO)
            }
            val tracked = totals.filter { it.nutrition.calories > 0 }
            val target = settings.targets.calories
            val average = if (tracked.isEmpty()) Nutrition.ZERO
            else tracked.map { it.nutrition }.total() * (1.0 / tracked.size)
            val onTarget = tracked.count { abs(it.nutrition.calories - target) <= target * 0.1 }

            StatsData(
                period = period,
                days = days,
                targetCalories = target,
                averageNutrition = average,
                daysTracked = tracked.size,
                daysOnTarget = onTarget,
                weights = weights,
                weightChange = if (weights.size >= 2) weights.last().weightKg - weights.first().weightKg else null
            )
        }
    }
}
