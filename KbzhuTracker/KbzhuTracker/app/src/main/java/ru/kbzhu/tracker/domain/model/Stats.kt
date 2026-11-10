package ru.kbzhu.tracker.domain.model

import java.time.LocalDate

enum class StatsPeriod(val days: Int) { WEEK(7), MONTH(30) }

data class DailyStat(val date: LocalDate, val nutrition: Nutrition)

data class WeightRecord(val date: LocalDate, val weightKg: Double)

data class StatsData(
    val period: StatsPeriod,
    /** Все дни периода по порядку, дни без записей — с нулями */
    val days: List<DailyStat>,
    val targetCalories: Int,
    /** Средние значения по дням, в которые что-то записано */
    val averageNutrition: Nutrition,
    val daysTracked: Int,
    /** Дни, когда калорийность в пределах ±10% от нормы */
    val daysOnTarget: Int,
    val weights: List<WeightRecord>,
    /** Изменение веса за период (последняя отметка − первая) */
    val weightChange: Double?
)

/** Результат поиска по штрих-коду. */
sealed interface BarcodeLookupResult {
    /** [isIncomplete] — в базе OFF нет КБЖУ, стоит дополнить вручную */
    data class Found(val food: Food, val isIncomplete: Boolean) : BarcodeLookupResult
    data class NotFound(val barcode: String) : BarcodeLookupResult
    data class Error(val barcode: String, val reason: NetworkError) : BarcodeLookupResult
}

enum class NetworkError { NO_CONNECTION, TIMEOUT, SERVER }

class NetworkException(val reason: NetworkError) : Exception(reason.name)
