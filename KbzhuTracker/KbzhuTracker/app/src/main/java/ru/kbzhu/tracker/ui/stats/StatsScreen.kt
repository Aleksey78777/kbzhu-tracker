package ru.kbzhu.tracker.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.domain.model.NutritionTargets
import ru.kbzhu.tracker.domain.model.StatsData
import ru.kbzhu.tracker.domain.model.StatsPeriod
import ru.kbzhu.tracker.domain.model.WeightRecord
import ru.kbzhu.tracker.ui.components.LoadingBox
import ru.kbzhu.tracker.ui.components.NutrientProgress
import ru.kbzhu.tracker.ui.theme.AppTheme
import ru.kbzhu.tracker.ui.util.format1
import ru.kbzhu.tracker.ui.util.formatDayMonth
import ru.kbzhu.tracker.ui.util.formatInt
import ru.kbzhu.tracker.ui.util.formatShort
import ru.kbzhu.tracker.ui.util.formatWeekdayShort
import ru.kbzhu.tracker.ui.util.sanitizeDecimal
import ru.kbzhu.tracker.ui.util.toInputString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val period by viewModel.period.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val targets by viewModel.targets.collectAsStateWithLifecycle()
    val goalWeight by viewModel.goalWeight.collectAsStateWithLifecycle()
    val latestWeight by viewModel.latestWeight.collectAsStateWithLifecycle()
    val history by viewModel.weightHistory.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showWeightDialog by rememberSaveable { mutableStateOf(false) }
    var showCaloriesTable by rememberSaveable { mutableStateOf(false) }
    var showAllWeights by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is StatsEvent.Message -> snackbarHostState.showSnackbar(context.getString(e.res))
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_stats)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val s = stats
        if (s == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "period") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    StatsPeriod.entries.forEachIndexed { i, p ->
                        SegmentedButton(
                            selected = period == p,
                            onClick = { viewModel.selectPeriod(p) },
                            shape = SegmentedButtonDefaults.itemShape(i, StatsPeriod.entries.size),
                            label = { Text(stringResource(if (p == StatsPeriod.WEEK) R.string.stats_week else R.string.stats_month)) }
                        )
                    }
                }
            }

            item(key = "tiles") { SummaryTiles(s) }

            item(key = "calories") {
                CaloriesCard(
                    stats = s,
                    showTable = showCaloriesTable,
                    onToggleTable = { showCaloriesTable = !showCaloriesTable }
                )
            }

            item(key = "macros") { MacrosCard(s, targets) }

            item(key = "weight") {
                WeightCard(
                    stats = s,
                    goalWeight = goalWeight,
                    latest = latestWeight,
                    onLogWeight = { showWeightDialog = true }
                )
            }

            if (history.isNotEmpty()) {
                item(key = "history_title") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.stats_weight_history), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (history.size > 5) {
                            TextButton(onClick = { showAllWeights = !showAllWeights }) {
                                Text(stringResource(if (showAllWeights) R.string.stats_show_less else R.string.stats_show_all))
                            }
                        }
                    }
                }
                val visible = if (showAllWeights) history else history.take(5)
                items(visible, key = { "w_${it.date}" }) { record ->
                    WeightHistoryRow(record, onDelete = { viewModel.deleteWeight(record) })
                }
            }
        }
    }

    if (showWeightDialog) {
        WeightDialog(
            initial = latestWeight?.weightKg,
            onDismiss = { showWeightDialog = false },
            onSave = { text ->
                val ok = viewModel.saveWeight(text)
                if (ok) showWeightDialog = false
                ok
            }
        )
    }
}

@Composable
private fun SummaryTiles(s: StatsData) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(
            value = if (s.daysTracked > 0) s.averageNutrition.calories.formatInt() else "—",
            label = stringResource(R.string.stats_avg_kcal),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = if (s.daysTracked > 0) "${s.daysOnTarget}/${s.daysTracked}" else "—",
            label = stringResource(R.string.stats_days_on_target),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = s.weightChange?.let { (if (it > 0) "+" else "") + it.format1() } ?: "—",
            label = stringResource(R.string.stats_weight_change),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChartCard(title: String, subtitle: String?, trailing: @Composable (() -> Unit)? = null, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                trailing?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun CaloriesCard(stats: StatsData, showTable: Boolean, onToggleTable: () -> Unit) {
    ChartCard(
        title = stringResource(R.string.stats_calories_title),
        subtitle = stringResource(R.string.stats_tap_hint),
        trailing = {
            IconButton(onClick = onToggleTable) {
                Icon(Icons.Filled.TableRows, stringResource(if (showTable) R.string.stats_hide_table else R.string.stats_show_table))
            }
        }
    ) {
        if (stats.daysTracked == 0) {
            Text(
                stringResource(R.string.stats_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            CaloriesBarChart(
                days = stats.days,
                target = stats.targetCalories,
                barColor = AppTheme.nutrients.chartCalories,
                targetLabel = stringResource(R.string.stats_target_label),
                kcalUnit = stringResource(R.string.unit_kcal),
                contentDescription = stringResource(
                    R.string.stats_calories_cd,
                    stats.averageNutrition.calories.formatInt(),
                    stats.targetCalories.formatInt()
                )
            )
        }
        // Табличное представление тех же данных — доступно и без графика
        if (showTable) {
            Spacer(Modifier.height(8.dp))
            stats.days.reversed().forEach { day ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(
                        "${day.date.formatWeekdayShort()}, ${day.date.formatDayMonth()}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (day.nutrition.calories > 0) stringResource(R.string.value_kcal, day.nutrition.calories.formatInt()) else "—",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun MacrosCard(stats: StatsData, targets: NutritionTargets) {
    val colors = AppTheme.nutrients
    ChartCard(title = stringResource(R.string.stats_macros_title), subtitle = stringResource(R.string.stats_macros_subtitle)) {
        if (stats.daysTracked == 0) {
            Text(stringResource(R.string.stats_no_data), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NutrientProgress(stringResource(R.string.protein), stats.averageNutrition.protein, targets.protein, colors.protein, Modifier.weight(1f))
                NutrientProgress(stringResource(R.string.fat), stats.averageNutrition.fat, targets.fat, colors.fat, Modifier.weight(1f))
                NutrientProgress(stringResource(R.string.carbs), stats.averageNutrition.carbs, targets.carbs, colors.carbs, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WeightCard(stats: StatsData, goalWeight: Double?, latest: WeightRecord?, onLogWeight: () -> Unit) {
    ChartCard(
        title = stringResource(R.string.stats_weight_title),
        subtitle = latest?.let { stringResource(R.string.stats_latest_weight, it.weightKg.format1(), it.date.formatDayMonth()) },
        trailing = {
            FilledTonalButton(onClick = onLogWeight, modifier = Modifier.heightIn(min = 44.dp)) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.stats_log_weight))
            }
        }
    ) {
        if (stats.weights.size < 2) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
                Icon(Icons.Filled.MonitorWeight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.stats_weight_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            WeightLineChart(
                records = stats.weights,
                goalWeight = goalWeight,
                lineColor = AppTheme.nutrients.chartWeight,
                goalLabel = stringResource(R.string.stats_goal_label),
                kgUnit = stringResource(R.string.unit_kg),
                contentDescription = stringResource(
                    R.string.stats_weight_cd,
                    stats.weights.first().weightKg.format1(),
                    stats.weights.last().weightKg.format1()
                )
            )
        }
    }
}

@Composable
private fun WeightHistoryRow(record: WeightRecord, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${record.date.formatWeekdayShort()}, ${record.date.formatShort()}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text("${record.weightKg.format1()} ${stringResource(R.string.unit_kg)}", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Close, stringResource(R.string.stats_delete_weight), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeightDialog(initial: Double?, onDismiss: () -> Unit, onSave: (String) -> Boolean) {
    var text by rememberSaveable { mutableStateOf(initial?.toInputString().orEmpty()) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stats_log_weight_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { v -> sanitizeDecimal(v, 1, 5)?.let { text = it; error = false } },
                label = { Text(stringResource(R.string.stats_weight_today)) },
                suffix = { Text(stringResource(R.string.unit_kg)) },
                singleLine = true,
                isError = error,
                supportingText = if (error) ({ Text(stringResource(R.string.onb_weight_error)) }) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(onClick = { error = !onSave(text) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
