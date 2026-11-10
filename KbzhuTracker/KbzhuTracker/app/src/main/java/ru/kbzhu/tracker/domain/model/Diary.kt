package ru.kbzhu.tracker.domain.model

import java.time.LocalDate

data class DiaryEntry(
    val id: Long = 0,
    val date: LocalDate,
    val mealType: MealType,
    val foodId: Long?,
    val name: String,
    val brand: String?,
    val grams: Double,
    val per100: Nutrition,
    val photoPath: String? = null,
    val foodPhotoPath: String? = null,
    val foodImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val nutrition: Nutrition get() = per100.forGrams(grams)
    val displayImage: String? get() = photoPath ?: foodPhotoPath ?: foodImageUrl
}

data class MealGroup(val mealType: MealType, val entries: List<DiaryEntry>) {
    val total: Nutrition = entries.map { it.nutrition }.total()
}

data class DaySummary(
    val date: LocalDate,
    val meals: List<MealGroup>,
    val eaten: Nutrition,
    val targets: NutritionTargets,
    val waterMl: Int,
    val waterGoalMl: Int,
    val waterPortionMl: Int
) {
    val remainingCalories: Double get() = targets.calories - eaten.calories
    val isEmpty: Boolean get() = meals.all { it.entries.isEmpty() }
}
