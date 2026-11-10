package ru.kbzhu.tracker.domain.calculator

import ru.kbzhu.tracker.domain.model.ActivityLevel
import ru.kbzhu.tracker.domain.model.Gender
import ru.kbzhu.tracker.domain.model.Goal
import ru.kbzhu.tracker.domain.model.MacroSplit
import ru.kbzhu.tracker.domain.model.NutritionTargets
import ru.kbzhu.tracker.domain.model.TargetsCalculation
import ru.kbzhu.tracker.domain.model.UserProfile
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Расчёт дневной нормы.
 *
 * 1. BMR (базовый обмен) — формула Миффлина — Сан-Жеора:
 *      мужчины: BMR = 10·вес(кг) + 6,25·рост(см) − 5·возраст + 5
 *      женщины: BMR = 10·вес(кг) + 6,25·рост(см) − 5·возраст − 161
 * 2. TDEE (суточный расход) = BMR × коэффициент активности (1,2 … 1,9).
 * 3. Корректировка под цель: 1 кг жира ≈ 7700 ккал, поэтому
 *      суточный дефицит/профицит = темп(кг/нед) × 7700 / 7.
 *    Ограничения безопасности для похудения: не ниже 1200 ккал (жен.) / 1500 ккал (муж.)
 *    и дефицит не больше 25% от TDEE.
 * 4. БЖУ: белки и углеводы — 4 ккал/г, жиры — 9 ккал/г.
 */
object NutritionCalculator {

    const val KCAL_PER_KG = 7700.0
    const val KCAL_PER_G_PROTEIN = 4.0
    const val KCAL_PER_G_FAT = 9.0
    const val KCAL_PER_G_CARBS = 4.0

    const val MIN_KCAL_FEMALE = 1200
    const val MIN_KCAL_MALE = 1500
    private const val MAX_DEFICIT_SHARE = 0.25

    /** Допустимые темпы, кг/нед — для выбора в онбординге */
    val LOSE_RATES = listOf(0.25, 0.5, 0.75, 1.0)
    val GAIN_RATES = listOf(0.1, 0.25, 0.5)

    fun bmr(gender: Gender, weightKg: Double, heightCm: Int, age: Int): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        return when (gender) {
            Gender.MALE -> base + 5
            Gender.FEMALE -> base - 161
        }
    }

    fun tdee(bmr: Double, activity: ActivityLevel): Double = bmr * activity.factor

    fun dailyAdjustment(goal: Goal, weeklyRateKg: Double): Double {
        val perDay = weeklyRateKg * KCAL_PER_KG / 7.0
        return when (goal) {
            Goal.LOSE -> -perDay
            Goal.MAINTAIN -> 0.0
            Goal.GAIN -> perDay
        }
    }

    fun minimumCalories(gender: Gender): Int =
        if (gender == Gender.MALE) MIN_KCAL_MALE else MIN_KCAL_FEMALE

    /** Граммы БЖУ из калорийности и процентов. */
    fun macros(calories: Int, split: MacroSplit): NutritionTargets = NutritionTargets(
        calories = calories,
        protein = (calories * split.proteinPercent / 100.0 / KCAL_PER_G_PROTEIN).roundToInt(),
        fat = (calories * split.fatPercent / 100.0 / KCAL_PER_G_FAT).roundToInt(),
        carbs = (calories * split.carbsPercent / 100.0 / KCAL_PER_G_CARBS).roundToInt()
    )

    fun calculate(profile: UserProfile): TargetsCalculation {
        val bmr = bmr(profile.gender, profile.weightKg, profile.heightCm, profile.age)
        val tdee = tdee(bmr, profile.activityLevel)
        val rate = if (profile.goal == Goal.MAINTAIN) 0.0 else profile.weeklyRateKg
        val raw = tdee + dailyAdjustment(profile.goal, rate)

        // Нижняя граница безопасна только для похудения; при наборе/поддержании берём расчёт как есть
        val floor = if (profile.goal == Goal.LOSE) {
            max(minimumCalories(profile.gender).toDouble(), tdee * (1 - MAX_DEFICIT_SHARE))
        } else {
            minimumCalories(profile.gender).toDouble()
        }
        val clamped = raw < floor
        val calories = roundTo(max(raw, floor), 10)
        val actualAdjustment = (calories - tdee).roundToInt()

        return TargetsCalculation(
            bmr = bmr,
            tdee = tdee,
            dailyAdjustment = actualAdjustment,
            targets = macros(calories, profile.macroSplit),
            clampedToMinimum = clamped && profile.goal == Goal.LOSE,
            weeksToGoal = weeksToGoal(profile, actualAdjustment)
        )
    }

    /** Сколько недель до цели при фактическом (после ограничений) дефиците/профиците. */
    fun weeksToGoal(profile: UserProfile, dailyAdjustment: Int): Int? {
        if (profile.goal == Goal.MAINTAIN || dailyAdjustment == 0) return null
        val diff = profile.targetWeightKg - profile.weightKg
        val directionOk = (profile.goal == Goal.LOSE && diff < 0) || (profile.goal == Goal.GAIN && diff > 0)
        if (!directionOk) return null
        val kgPerWeek = abs(dailyAdjustment) * 7.0 / KCAL_PER_KG
        return ceil(abs(diff) / kgPerWeek).toInt()
    }

    /** Рекомендованная норма воды: 30 мл на кг, округление до 50 мл, в пределах 1,5–4 л. */
    fun recommendedWaterMl(weightKg: Double): Int =
        roundTo(weightKg * 30, 50).coerceIn(1500, 4000)

    fun roundTo(value: Double, step: Int): Int = ((value / step).roundToInt() * step)
}
