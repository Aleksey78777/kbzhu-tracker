package ru.kbzhu.tracker.domain.model

/** Распределение калорий по БЖУ в процентах. Сумма должна быть 100. */
data class MacroSplit(
    val proteinPercent: Int = 30,
    val fatPercent: Int = 30,
    val carbsPercent: Int = 40
) {
    val isValid: Boolean
        get() = proteinPercent + fatPercent + carbsPercent == 100 &&
            listOf(proteinPercent, fatPercent, carbsPercent).all { it >= 5 }
}

data class UserProfile(
    val gender: Gender,
    val age: Int,
    val heightCm: Int,
    val weightKg: Double,
    val goal: Goal,
    val targetWeightKg: Double,
    val weeklyRateKg: Double,
    val activityLevel: ActivityLevel,
    val macroSplit: MacroSplit = MacroSplit()
)

/** Дневная норма. */
data class NutritionTargets(
    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int
) {
    companion object {
        val DEFAULT = NutritionTargets(calories = 2000, protein = 150, fat = 67, carbs = 200)
    }
}

/** Подробный результат расчёта — показываем на итоговом экране онбординга. */
data class TargetsCalculation(
    val bmr: Double,
    val tdee: Double,
    /** Дефицит (<0) или профицит (>0) ккал/сут */
    val dailyAdjustment: Int,
    val targets: NutritionTargets,
    /** true — норма поднята до безопасного минимума, темп будет медленнее заданного */
    val clampedToMinimum: Boolean,
    /** Примерное число недель до желаемого веса (null для поддержания) */
    val weeksToGoal: Int?
)

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val profile: UserProfile? = null,
    val targets: NutritionTargets = NutritionTargets.DEFAULT,
    /** Пользователь задал норму вручную — не пересчитываем при смене веса */
    val manualTargets: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val waterGoalMl: Int = 2000,
    val waterPortionMl: Int = 250
)
