package ru.kbzhu.tracker.ui.util

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.ui.graphics.vector.ImageVector
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.domain.model.ActivityLevel
import ru.kbzhu.tracker.domain.model.FoodCategory
import ru.kbzhu.tracker.domain.model.Gender
import ru.kbzhu.tracker.domain.model.Goal
import ru.kbzhu.tracker.domain.model.MealType
import ru.kbzhu.tracker.domain.model.ThemeMode
import java.time.LocalTime

@get:StringRes
val MealType.titleRes: Int
    get() = when (this) {
        MealType.BREAKFAST -> R.string.meal_breakfast
        MealType.LUNCH -> R.string.meal_lunch
        MealType.DINNER -> R.string.meal_dinner
        MealType.SNACK -> R.string.meal_snack
    }

val MealType.icon: ImageVector
    get() = when (this) {
        MealType.BREAKFAST -> Icons.Filled.FreeBreakfast
        MealType.LUNCH -> Icons.Filled.LunchDining
        MealType.DINNER -> Icons.Filled.DinnerDining
        MealType.SNACK -> Icons.Filled.Cookie
    }

/** Приём пищи по текущему времени — подставляется по умолчанию. */
fun defaultMealForNow(time: LocalTime = LocalTime.now()): MealType = when (time.hour) {
    in 5..10 -> MealType.BREAKFAST
    in 11..15 -> MealType.LUNCH
    in 18..21 -> MealType.DINNER
    else -> MealType.SNACK
}

@get:StringRes
val FoodCategory.titleRes: Int
    get() = when (this) {
        FoodCategory.VEGETABLES -> R.string.cat_vegetables
        FoodCategory.FRUITS -> R.string.cat_fruits
        FoodCategory.MEAT -> R.string.cat_meat
        FoodCategory.FISH -> R.string.cat_fish
        FoodCategory.EGGS -> R.string.cat_eggs
        FoodCategory.DAIRY -> R.string.cat_dairy
        FoodCategory.GRAINS -> R.string.cat_grains
        FoodCategory.BAKERY -> R.string.cat_bakery
        FoodCategory.NUTS -> R.string.cat_nuts
        FoodCategory.OILS_SAUCES -> R.string.cat_oils
        FoodCategory.SWEETS -> R.string.cat_sweets
        FoodCategory.DRINKS -> R.string.cat_drinks
        FoodCategory.READY_MEALS -> R.string.cat_ready
        FoodCategory.OTHER -> R.string.cat_other
    }

@get:StringRes
val Gender.titleRes: Int
    get() = if (this == Gender.MALE) R.string.gender_male else R.string.gender_female

@get:StringRes
val Goal.titleRes: Int
    get() = when (this) {
        Goal.LOSE -> R.string.goal_lose
        Goal.MAINTAIN -> R.string.goal_maintain
        Goal.GAIN -> R.string.goal_gain
    }

@get:StringRes
val Goal.descriptionRes: Int
    get() = when (this) {
        Goal.LOSE -> R.string.goal_lose_desc
        Goal.MAINTAIN -> R.string.goal_maintain_desc
        Goal.GAIN -> R.string.goal_gain_desc
    }

val Goal.icon: ImageVector
    get() = when (this) {
        Goal.LOSE -> Icons.AutoMirrored.Filled.TrendingDown
        Goal.MAINTAIN -> Icons.AutoMirrored.Filled.TrendingFlat
        Goal.GAIN -> Icons.AutoMirrored.Filled.TrendingUp
    }

@get:StringRes
val ActivityLevel.titleRes: Int
    get() = when (this) {
        ActivityLevel.SEDENTARY -> R.string.activity_sedentary
        ActivityLevel.LIGHT -> R.string.activity_light
        ActivityLevel.MODERATE -> R.string.activity_moderate
        ActivityLevel.HIGH -> R.string.activity_high
        ActivityLevel.EXTREME -> R.string.activity_extreme
    }

@get:StringRes
val ActivityLevel.descriptionRes: Int
    get() = when (this) {
        ActivityLevel.SEDENTARY -> R.string.activity_sedentary_desc
        ActivityLevel.LIGHT -> R.string.activity_light_desc
        ActivityLevel.MODERATE -> R.string.activity_moderate_desc
        ActivityLevel.HIGH -> R.string.activity_high_desc
        ActivityLevel.EXTREME -> R.string.activity_extreme_desc
    }

val ActivityLevel.icon: ImageVector
    get() = when (this) {
        ActivityLevel.SEDENTARY -> Icons.Filled.Weekend
        ActivityLevel.LIGHT -> Icons.AutoMirrored.Filled.DirectionsWalk
        ActivityLevel.MODERATE -> Icons.AutoMirrored.Filled.DirectionsBike
        ActivityLevel.HIGH -> Icons.AutoMirrored.Filled.DirectionsRun
        ActivityLevel.EXTREME -> Icons.Filled.FitnessCenter
    }

@get:StringRes
val ThemeMode.titleRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }
