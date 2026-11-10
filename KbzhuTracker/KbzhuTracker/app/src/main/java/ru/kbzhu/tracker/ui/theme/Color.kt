package ru.kbzhu.tracker.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Спокойная зелёная палитра (Material 3, тон 40 / 80)
val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F0C8),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF4F6354),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E8D5),
    onSecondaryContainer = Color(0xFF0C1F13),
    tertiary = Color(0xFF3B6470),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBFE9F7),
    onTertiaryContainer = Color(0xFF001F27),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6FBF4),
    onBackground = Color(0xFF171D18),
    surface = Color(0xFFF6FBF4),
    onSurface = Color(0xFF171D18),
    surfaceVariant = Color(0xFFDDE5DB),
    onSurfaceVariant = Color(0xFF414941),
    outline = Color(0xFF717970),
    outlineVariant = Color(0xFFC1C9BF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5EE),
    surfaceContainer = Color(0xFFEAEFE8),
    surfaceContainerHigh = Color(0xFFE5EAE3),
    surfaceContainerHighest = Color(0xFFDFE4DD)
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD4AE),
    onPrimary = Color(0xFF00391E),
    primaryContainer = Color(0xFF10532F),
    onPrimaryContainer = Color(0xFFB7F0C8),
    secondary = Color(0xFFB5CCB9),
    onSecondary = Color(0xFF213527),
    secondaryContainer = Color(0xFF374B3D),
    onSecondaryContainer = Color(0xFFD1E8D5),
    tertiary = Color(0xFFA3CDDB),
    onTertiary = Color(0xFF033541),
    tertiaryContainer = Color(0xFF214C58),
    onTertiaryContainer = Color(0xFFBFE9F7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101411),
    onBackground = Color(0xFFDFE4DD),
    surface = Color(0xFF101411),
    onSurface = Color(0xFFDFE4DD),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BF),
    outline = Color(0xFF8B9389),
    outlineVariant = Color(0xFF414941),
    surfaceContainerLowest = Color(0xFF0B0F0C),
    surfaceContainerLow = Color(0xFF181D19),
    surfaceContainer = Color(0xFF1C211D),
    surfaceContainerHigh = Color(0xFF262B27),
    surfaceContainerHighest = Color(0xFF313631)
)

/** Цвета БЖУ и воды — одинаковые по смыслу во всём приложении. */
@Immutable
data class NutrientColors(
    val calories: Color,
    val protein: Color,
    val fat: Color,
    val carbs: Color,
    val water: Color,
    /** Цвета графиков (проверены валидатором палитр на контраст и насыщенность к фону карточки) */
    val chartCalories: Color,
    val chartWeight: Color
)

val LightNutrientColors = NutrientColors(
    calories = Color(0xFF2E7D4F),
    protein = Color(0xFFD9694A),
    fat = Color(0xFFD39E1F),
    carbs = Color(0xFF4A7FE0),
    water = Color(0xFF2B98C9),
    chartCalories = Color(0xFF2E7D4F),
    chartWeight = Color(0xFF4A7FE0)
)

val DarkNutrientColors = NutrientColors(
    calories = Color(0xFF9BD4AE),
    protein = Color(0xFFF2A48F),
    fat = Color(0xFFF4D27A),
    carbs = Color(0xFF9DB9F5),
    water = Color(0xFF7FCDEB),
    chartCalories = Color(0xFF3FA866),
    chartWeight = Color(0xFF5B93F0)
)

val LocalNutrientColors = staticCompositionLocalOf { LightNutrientColors }
