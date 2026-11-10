package ru.kbzhu.tracker.ui.diary

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.domain.model.DaySummary
import ru.kbzhu.tracker.domain.model.DiaryEntry
import ru.kbzhu.tracker.domain.model.MealGroup
import ru.kbzhu.tracker.domain.model.MealType
import ru.kbzhu.tracker.ui.components.CalorieRing
import ru.kbzhu.tracker.ui.components.FoodImage
import ru.kbzhu.tracker.ui.components.NutrientProgress
import ru.kbzhu.tracker.ui.theme.AppTheme
import ru.kbzhu.tracker.ui.util.formatInt
import ru.kbzhu.tracker.ui.util.formatMacro
import ru.kbzhu.tracker.ui.util.icon
import ru.kbzhu.tracker.ui.util.titleRes

@Composable
fun DayContent(
    summary: DaySummary,
    onAddToMeal: (MealType) -> Unit,
    onScanToMeal: (MealType) -> Unit,
    onEditEntry: (DiaryEntry) -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit,
    onWaterChange: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Снизу запас под FAB, чтобы он не перекрывал последнюю запись
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "summary") { SummaryCard(summary) }
        item(key = "water") {
            WaterCard(
                waterMl = summary.waterMl,
                goalMl = summary.waterGoalMl,
                portionMl = summary.waterPortionMl,
                onChange = onWaterChange
            )
        }
        if (summary.isEmpty) {
            item(key = "hint") { EmptyDayHint() }
        }
        summary.meals.forEach { group ->
            item(key = "meal_${group.mealType.name}") {
                MealCard(
                    group = group,
                    onAdd = { onAddToMeal(group.mealType) },
                    onScan = { onScanToMeal(group.mealType) },
                    onEdit = onEditEntry,
                    onDelete = onDeleteEntry
                )
            }
        }
    }
}

// ---------------- Итоги дня ----------------
@Composable
private fun SummaryCard(summary: DaySummary) {
    val colors = AppTheme.nutrients
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SideStat(
                    label = stringResource(R.string.diary_eaten),
                    value = summary.eaten.calories.formatInt(),
                    modifier = Modifier.weight(1f)
                )
                CalorieRing(eaten = summary.eaten.calories, target = summary.targets.calories, size = 168.dp)
                SideStat(
                    label = stringResource(R.string.diary_target),
                    value = summary.targets.calories.formatInt(),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NutrientProgress(stringResource(R.string.protein), summary.eaten.protein, summary.targets.protein, colors.protein, Modifier.weight(1f))
                NutrientProgress(stringResource(R.string.fat), summary.eaten.fat, summary.targets.fat, colors.fat, Modifier.weight(1f))
                NutrientProgress(stringResource(R.string.carbs), summary.eaten.carbs, summary.targets.carbs, colors.carbs, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SideStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            "$label, ${stringResource(R.string.unit_kcal)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------- Вода ----------------
@Composable
private fun WaterCard(waterMl: Int, goalMl: Int, portionMl: Int, onChange: (Int) -> Unit) {
    val waterColor = AppTheme.nutrients.water
    val progress by animateFloatAsState(
        targetValue = if (goalMl > 0) (waterMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(500),
        label = "water"
    )
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(waterColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = waterColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.diary_water), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.diary_water_value, waterMl.formatInt(), goalMl.formatInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = waterColor,
                    trackColor = waterColor.copy(alpha = 0.18f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalIconButton(
                onClick = { onChange(-portionMl) },
                enabled = waterMl > 0,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Filled.Remove, stringResource(R.string.diary_water_remove, portionMl))
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = { onChange(portionMl) },
                modifier = Modifier.size(52.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = waterColor, contentColor = Color.White)
            ) {
                Icon(Icons.Filled.Add, stringResource(R.string.diary_water_add, portionMl))
            }
        }
    }
}

@Composable
private fun EmptyDayHint() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.diary_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.diary_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ---------------- Приём пищи ----------------
@Composable
private fun MealCard(
    group: MealGroup,
    onAdd: () -> Unit,
    onScan: () -> Unit,
    onEdit: (DiaryEntry) -> Unit,
    onDelete: (DiaryEntry) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column {
            // Заголовок: иконка, название, сумма
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAdd)
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(group.mealType.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(group.mealType.titleRes), style = MaterialTheme.typography.titleMedium)
                    if (group.entries.isNotEmpty()) {
                        Text(
                            macroLine(group.total.protein, group.total.fat, group.total.carbs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    stringResource(R.string.value_kcal, group.total.calories.formatInt()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onScan) {
                    Icon(Icons.Filled.QrCodeScanner, stringResource(R.string.diary_scan_barcode))
                }
            }

            group.entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                // key обязателен: иначе состояние свайпа «переедет» на соседнюю запись после удаления
                key(entry.id) {
                    SwipeableEntry(entry = entry, onEdit = { onEdit(entry) }, onDelete = { onDelete(entry) })
                }
            }

            TextButton(
                onClick = onAdd,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.diary_add_to_meal))
            }
        }
    }
}

/**
 * Свайп влево — удалить (с возможностью отмены), свайп вправо — редактировать.
 * Для «редактировать» возвращаем false, чтобы строка вернулась на место.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableEntry(entry: DiaryEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
                SwipeToDismissBoxValue.StartToEnd -> { onEdit(); false }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val direction = state.dismissDirection
            val bg by animateColorAsState(
                when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                label = "swipeBg"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.EndToStart ->
                        Icon(Icons.Filled.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.onErrorContainer)
                    SwipeToDismissBoxValue.StartToEnd ->
                        Icon(Icons.Filled.Edit, stringResource(R.string.action_edit), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    SwipeToDismissBoxValue.Settled -> Unit
                }
            }
        }
    ) {
        EntryRow(entry, onClick = onEdit)
    }
}

@Composable
private fun EntryRow(entry: DiaryEntry, onClick: () -> Unit) {
    val n = entry.nutrition
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FoodImage(entry.displayImage, size = 44.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${stringResource(R.string.value_g, entry.grams.formatInt())} · ${macroLine(n.protein, n.fat, n.carbs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(n.calories.formatInt(), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun macroLine(protein: Double, fat: Double, carbs: Double): String =
    stringResource(R.string.macro_line, protein.formatMacro(), fat.formatMacro(), carbs.formatMacro())
