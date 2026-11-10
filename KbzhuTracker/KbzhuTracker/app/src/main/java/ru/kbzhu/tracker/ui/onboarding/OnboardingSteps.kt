package ru.kbzhu.tracker.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.domain.calculator.NutritionCalculator
import ru.kbzhu.tracker.domain.model.ActivityLevel
import ru.kbzhu.tracker.domain.model.Gender
import ru.kbzhu.tracker.domain.model.Goal
import ru.kbzhu.tracker.ui.components.NumberField
import ru.kbzhu.tracker.ui.components.OptionCard
import ru.kbzhu.tracker.ui.theme.AppTheme
import ru.kbzhu.tracker.ui.theme.BigNumberStyle
import ru.kbzhu.tracker.ui.util.descriptionRes
import ru.kbzhu.tracker.ui.util.format1
import ru.kbzhu.tracker.ui.util.formatInt
import ru.kbzhu.tracker.ui.util.icon
import ru.kbzhu.tracker.ui.util.titleRes

/** Общая обёртка шага: заголовок, подзаголовок, прокрутка. */
@Composable
private fun StepContainer(
    title: String,
    subtitle: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

// ---------------- 1. Пол ----------------
@Composable
fun GenderStep(state: OnboardingUiState, onSelect: (Gender) -> Unit) {
    StepContainer(stringResource(R.string.onb_gender_title), stringResource(R.string.onb_gender_subtitle)) {
        OptionCard(
            title = stringResource(R.string.gender_male),
            selected = state.gender == Gender.MALE,
            onClick = { onSelect(Gender.MALE) },
            icon = Icons.Filled.Male
        )
        OptionCard(
            title = stringResource(R.string.gender_female),
            selected = state.gender == Gender.FEMALE,
            onClick = { onSelect(Gender.FEMALE) },
            icon = Icons.Filled.Female
        )
    }
}

// ---------------- 2. Возраст, рост, вес ----------------
@Composable
fun BodyStep(
    state: OnboardingUiState,
    onAge: (String) -> Unit,
    onHeight: (String) -> Unit,
    onWeight: (String) -> Unit,
    onDone: () -> Unit
) {
    StepContainer(stringResource(R.string.onb_body_title), stringResource(R.string.onb_body_subtitle)) {
        NumberField(
            value = state.age,
            onValueChange = onAge,
            label = stringResource(R.string.onb_age),
            suffix = stringResource(R.string.unit_years),
            decimal = false,
            errorText = if (state.age.isNotEmpty() && !state.ageValid) stringResource(R.string.onb_age_error) else null
        )
        NumberField(
            value = state.height,
            onValueChange = onHeight,
            label = stringResource(R.string.onb_height),
            suffix = stringResource(R.string.unit_cm),
            decimal = false,
            errorText = if (state.height.isNotEmpty() && !state.heightValid) stringResource(R.string.onb_height_error) else null
        )
        OutlinedTextField(
            value = state.weight,
            onValueChange = onWeight,
            label = { Text(stringResource(R.string.onb_weight)) },
            suffix = { Text(stringResource(R.string.unit_kg)) },
            singleLine = true,
            isError = state.weight.isNotEmpty() && !state.weightValid,
            supportingText = if (state.weight.isNotEmpty() && !state.weightValid) {
                { Text(stringResource(R.string.onb_weight_error)) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ---------------- 3. Цель ----------------
@Composable
fun GoalStep(state: OnboardingUiState, onSelect: (Goal) -> Unit) {
    StepContainer(stringResource(R.string.onb_goal_title), null) {
        Goal.entries.forEach { goal ->
            OptionCard(
                title = stringResource(goal.titleRes),
                description = stringResource(goal.descriptionRes),
                selected = state.goal == goal,
                onClick = { onSelect(goal) },
                icon = goal.icon
            )
        }
    }
}

// ---------------- 4. Желаемый вес и темп ----------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TargetStep(state: OnboardingUiState, onTargetWeight: (String) -> Unit, onRate: (Double) -> Unit) {
    val lose = state.goal == Goal.LOSE
    StepContainer(
        stringResource(R.string.onb_target_title),
        stringResource(if (lose) R.string.onb_target_subtitle_lose else R.string.onb_target_subtitle_gain)
    ) {
        NumberField(
            value = state.targetWeight,
            onValueChange = onTargetWeight,
            label = stringResource(R.string.onb_target_weight),
            suffix = stringResource(R.string.unit_kg),
            imeAction = ImeAction.Done,
            errorText = if (state.targetWeight.isNotEmpty() && !state.targetWeightValid) {
                stringResource(if (lose) R.string.onb_target_error_lose else R.string.onb_target_error_gain)
            } else null,
            supportingText = state.weightValue?.let {
                stringResource(R.string.onb_current_weight, it.format1())
            }
        )
        Text(stringResource(R.string.onb_rate_title), style = MaterialTheme.typography.titleMedium)
        val rates = if (lose) NutritionCalculator.LOSE_RATES else NutritionCalculator.GAIN_RATES
        val recommended = if (lose) 0.5 else 0.25
        rates.forEach { rate ->
            OptionCard(
                title = stringResource(R.string.onb_rate_value, rate.format1()),
                description = stringResource(rateDescription(lose, rate)),
                selected = state.weeklyRate == rate,
                onClick = { onRate(rate) },
                badge = if (rate == recommended) stringResource(R.string.onb_recommended) else null
            )
        }
    }
}

private fun rateDescription(lose: Boolean, rate: Double): Int = when {
    lose && rate <= 0.25 -> R.string.onb_rate_lose_slow
    lose && rate <= 0.5 -> R.string.onb_rate_lose_normal
    lose && rate <= 0.75 -> R.string.onb_rate_lose_fast
    lose -> R.string.onb_rate_lose_max
    rate <= 0.1 -> R.string.onb_rate_gain_slow
    rate <= 0.25 -> R.string.onb_rate_gain_normal
    else -> R.string.onb_rate_gain_fast
}

// ---------------- 5. Активность ----------------
@Composable
fun ActivityStep(state: OnboardingUiState, onSelect: (ActivityLevel) -> Unit) {
    StepContainer(stringResource(R.string.onb_activity_title), stringResource(R.string.onb_activity_subtitle)) {
        ActivityLevel.entries.forEach { level ->
            OptionCard(
                title = stringResource(level.titleRes),
                description = stringResource(level.descriptionRes),
                selected = state.activity == level,
                onClick = { onSelect(level) },
                icon = level.icon
            )
        }
    }
}

// ---------------- 6. Распределение БЖУ ----------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MacrosStep(
    state: OnboardingUiState,
    onPreset: (MacroPreset) -> Unit,
    onChange: (Macro, Int) -> Unit,
    previewGrams: (Macro) -> Int?
) {
    StepContainer(stringResource(R.string.onb_macros_title), stringResource(R.string.onb_macros_subtitle)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MacroPreset.entries.forEach { preset ->
                FilterChip(
                    selected = state.macroSplit == preset.split,
                    onClick = { onPreset(preset) },
                    label = { Text(stringResource(preset.titleRes)) }
                )
            }
        }
        val colors = AppTheme.nutrients
        MacroRow(stringResource(R.string.protein), state.macroSplit.proteinPercent, previewGrams(Macro.PROTEIN), colors.protein) { onChange(Macro.PROTEIN, it) }
        MacroRow(stringResource(R.string.fat), state.macroSplit.fatPercent, previewGrams(Macro.FAT), colors.fat) { onChange(Macro.FAT, it) }
        MacroRow(stringResource(R.string.carbs), state.macroSplit.carbsPercent, previewGrams(Macro.CARBS), colors.carbs) { onChange(Macro.CARBS, it) }

        val sum = state.macroSplit.proteinPercent + state.macroSplit.fatPercent + state.macroSplit.carbsPercent
        Text(
            text = if (sum == 100) stringResource(R.string.onb_macros_sum_ok)
            else stringResource(R.string.onb_macros_sum_error, sum),
            style = MaterialTheme.typography.bodyMedium,
            color = if (sum == 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium
        )
    }
}

private val MacroPreset.titleRes: Int
    get() = when (this) {
        MacroPreset.BALANCED -> R.string.preset_balanced
        MacroPreset.HIGH_PROTEIN -> R.string.preset_high_protein
        MacroPreset.LOW_CARB -> R.string.preset_low_carb
        MacroPreset.SPORT -> R.string.preset_sport
    }

@Composable
private fun MacroRow(label: String, percent: Int, grams: Int?, color: Color, onChange: (Int) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = color)
                if (grams != null) {
                    Text(
                        stringResource(R.string.onb_grams_per_day, grams),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FilledTonalIconButton(onClick = { onChange(-5) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Remove, stringResource(R.string.action_decrease))
            }
            Text(
                "$percent%",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.width(64.dp),
                textAlign = TextAlign.Center
            )
            FilledTonalIconButton(onClick = { onChange(5) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Add, stringResource(R.string.action_increase))
            }
        }
    }
}

// ---------------- 7. Итог ----------------
@Composable
fun ResultStep(
    state: OnboardingUiState,
    onManualToggle: (Boolean) -> Unit,
    onCalories: (String) -> Unit,
    onMacro: (Macro, String) -> Unit
) {
    val calc = state.calculation ?: return
    val shown = state.manualTargets ?: calc.targets
    val colors = AppTheme.nutrients

    StepContainer(stringResource(R.string.onb_result_title), stringResource(R.string.onb_result_subtitle)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(shown.calories.formatInt(), style = BigNumberStyle, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(stringResource(R.string.kcal_per_day), color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MacroSummary(stringResource(R.string.protein), shown.protein, colors.protein)
                    MacroSummary(stringResource(R.string.fat), shown.fat, colors.fat)
                    MacroSummary(stringResource(R.string.carbs), shown.carbs, colors.carbs)
                }
            }
        }

        // Как получилась норма
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(stringResource(R.string.onb_bmr), stringResource(R.string.value_kcal, calc.bmr.formatInt()))
                InfoRow(stringResource(R.string.onb_tdee), stringResource(R.string.value_kcal, calc.tdee.formatInt()))
                InfoRow(
                    stringResource(
                        when {
                            calc.dailyAdjustment < 0 -> R.string.onb_deficit
                            calc.dailyAdjustment > 0 -> R.string.onb_surplus
                            else -> R.string.onb_adjustment_none
                        }
                    ),
                    stringResource(R.string.value_kcal, kotlin.math.abs(calc.dailyAdjustment).formatInt())
                )
                calc.weeksToGoal?.let {
                    HorizontalDivider()
                    InfoRow(stringResource(R.string.onb_weeks_to_goal), stringResource(R.string.onb_weeks_value, it))
                }
            }
        }

        if (calc.clampedToMinimum) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.WarningAmber, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.onb_clamped_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Ручная корректировка
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.onb_manual_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.onb_manual_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = state.manualEnabled, onCheckedChange = onManualToggle)
        }

        if (state.manualEnabled) {
            NumberField(
                value = state.manualCalories,
                onValueChange = onCalories,
                label = stringResource(R.string.calories),
                suffix = stringResource(R.string.unit_kcal),
                decimal = false,
                errorText = if (!state.manualValid) stringResource(R.string.onb_manual_error) else null
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(state.manualProtein, { onMacro(Macro.PROTEIN, it) }, stringResource(R.string.protein_short),
                    Modifier.weight(1f), suffix = stringResource(R.string.unit_g), decimal = false)
                NumberField(state.manualFat, { onMacro(Macro.FAT, it) }, stringResource(R.string.fat_short),
                    Modifier.weight(1f), suffix = stringResource(R.string.unit_g), decimal = false)
                NumberField(state.manualCarbs, { onMacro(Macro.CARBS, it) }, stringResource(R.string.carbs_short),
                    Modifier.weight(1f), suffix = stringResource(R.string.unit_g), decimal = false, imeAction = ImeAction.Done)
            }
        }

        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.onb_change_later),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MacroSummary(label: String, grams: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.value_g, grams.formatInt()), style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
