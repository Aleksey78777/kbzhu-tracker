package ru.kbzhu.tracker.domain.model

/** Набор КБЖУ. Для продуктов — значения на 100 г, для записей — на фактическую граммовку. */
data class Nutrition(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0
) {
    operator fun plus(other: Nutrition) = Nutrition(
        calories + other.calories,
        protein + other.protein,
        fat + other.fat,
        carbs + other.carbs
    )

    operator fun times(factor: Double) = Nutrition(
        calories * factor,
        protein * factor,
        fat * factor,
        carbs * factor
    )

    /** Если this — значения на 100 г, возвращает КБЖУ для [grams] грамм. */
    fun forGrams(grams: Double): Nutrition = this * (grams / 100.0)

    companion object {
        val ZERO = Nutrition()
    }
}

fun Iterable<Nutrition>.total(): Nutrition = fold(Nutrition.ZERO) { acc, n -> acc + n }
