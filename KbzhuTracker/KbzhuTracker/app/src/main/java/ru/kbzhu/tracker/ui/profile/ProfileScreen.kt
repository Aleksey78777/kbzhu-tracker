package ru.kbzhu.tracker.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kbzhu.tracker.BuildConfig
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.domain.model.Goal
import ru.kbzhu.tracker.domain.model.ThemeMode
import ru.kbzhu.tracker.domain.model.UserProfile
import ru.kbzhu.tracker.domain.model.UserSettings
import ru.kbzhu.tracker.ui.components.LoadingBox
import ru.kbzhu.tracker.ui.components.PrimaryButton
import ru.kbzhu.tracker.ui.theme.AppTheme
import ru.kbzhu.tracker.ui.util.format1
import ru.kbzhu.tracker.ui.util.formatInt
import ru.kbzhu.tracker.ui.util.sanitizeInt
import ru.kbzhu.tracker.ui.util.titleRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showWaterDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_profile)) }) }) { padding ->
        val s = settings
        if (s == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileCard(s.profile, onEditProfile)
            TargetsCard(s, onEditProfile)
            WaterCard(
                settings = s,
                onEditGoal = { showWaterDialog = true },
                onPortion = viewModel::setWaterPortion
            )
            ThemeCard(selected = s.themeMode, onSelect = viewModel::setTheme)
            AboutCard()
        }
    }

    if (showWaterDialog) {
        WaterGoalDialog(
            initial = settings?.waterGoalMl ?: 2000,
            recommended = viewModel.recommendedWater(),
            onDismiss = { showWaterDialog = false },
            onSave = { value ->
                val ok = viewModel.setWaterGoal(value)
                if (ok) showWaterDialog = false
                ok
            }
        )
    }
}

@Composable
private fun SectionCard(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun ProfileCard(profile: UserProfile?, onEdit: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    if (profile == null) {
                        Text(stringResource(R.string.profile_empty), style = MaterialTheme.typography.titleMedium)
                    } else {
                        Text(
                            stringResource(
                                R.string.profile_body_line,
                                stringResource(profile.gender.titleRes),
                                pluralStringResource(R.plurals.age_years, profile.age, profile.age),
                                profile.heightCm,
                                profile.weightKg.format1()
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            goalLine(profile),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            stringResource(R.string.profile_activity, stringResource(profile.activityLevel.titleRes)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = stringResource(R.string.profile_edit),
                onClick = onEdit,
                icon = Icons.Filled.Edit
            )
        }
    }
}

@Composable
private fun goalLine(p: UserProfile): String = when (p.goal) {
    Goal.MAINTAIN -> stringResource(R.string.goal_maintain)
    else -> stringResource(
        R.string.profile_goal_line,
        stringResource(p.goal.titleRes),
        p.targetWeightKg.format1(),
        p.weeklyRateKg.format1()
    )
}

@Composable
private fun TargetsCard(s: UserSettings, onEdit: () -> Unit) {
    val colors = AppTheme.nutrients
    Card(
        onClick = onEdit,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.profile_targets), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(if (s.manualTargets) R.string.profile_targets_manual else R.string.profile_targets_auto),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.value_kcal, s.targets.calories.formatInt()),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MacroChip(stringResource(R.string.protein_short), s.targets.protein, colors.protein)
                    MacroChip(stringResource(R.string.fat_short), s.targets.fat, colors.fat)
                    MacroChip(stringResource(R.string.carbs_short), s.targets.carbs, colors.carbs)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MacroChip(label: String, grams: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text("$label ${stringResource(R.string.value_g, grams.formatInt())}", style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WaterCard(settings: UserSettings, onEditGoal: () -> Unit, onPortion: (Int) -> Unit) {
    SectionCard(Icons.Filled.WaterDrop, stringResource(R.string.profile_water)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onEditGoal)
                .heightIn(min = 48.dp)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.profile_water_goal), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.value_ml, settings.waterGoalMl.formatInt()), style = MaterialTheme.typography.titleMedium)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(stringResource(R.string.profile_water_portion), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileViewModel.WATER_PORTIONS.forEach { ml ->
                FilterChip(
                    selected = settings.waterPortionMl == ml,
                    onClick = { onPortion(ml) },
                    label = { Text(stringResource(R.string.value_ml, ml.formatInt())) }
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    SectionCard(Icons.Filled.Palette, stringResource(R.string.profile_theme)) {
        ThemeMode.entries.forEach { mode ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .selectable(selected = selected == mode, onClick = { onSelect(mode) }, role = Role.RadioButton)
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == mode, onClick = null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(mode.titleRes), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun AboutCard() {
    SectionCard(Icons.Filled.Info, stringResource(R.string.profile_about)) {
        Text(
            stringResource(R.string.profile_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.profile_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        // Условие лицензии ODbL: указывать источник данных
        Text(
            stringResource(R.string.profile_off_attribution),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WaterGoalDialog(initial: Int, recommended: Int?, onDismiss: () -> Unit, onSave: (Int?) -> Boolean) {
    var text by rememberSaveable { mutableStateOf(initial.toString()) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_water_goal)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> sanitizeInt(v, 4)?.let { text = it; error = false } },
                    suffix = { Text(stringResource(R.string.unit_ml)) },
                    singleLine = true,
                    isError = error,
                    supportingText = { Text(stringResource(if (error) R.string.profile_water_error else R.string.profile_water_range)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (recommended != null) {
                    Surface(
                        onClick = { text = recommended.toString(); error = false },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            stringResource(R.string.profile_water_recommended, recommended.formatInt()),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { error = !onSave(text.toIntOrNull()) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
