package ru.kbzhu.tracker.ui.foods

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.domain.model.FoodSource
import ru.kbzhu.tracker.domain.model.Nutrition
import ru.kbzhu.tracker.ui.components.FoodImage
import ru.kbzhu.tracker.ui.util.formatInt
import ru.kbzhu.tracker.ui.util.formatMacro

/** Строка продукта в списках: фото, название, бренд, КБЖУ на 100 г, избранное. */
@Composable
fun FoodRow(
    food: Food,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 68.dp)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FoodImage(food.displayImage, size = 48.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(food.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val subtitle = listOfNotNull(
                food.brand,
                when (food.source) {
                    FoodSource.RECIPE -> stringResource(R.string.food_badge_recipe)
                    FoodSource.USER -> stringResource(R.string.food_badge_mine)
                    else -> null
                }
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                per100Line(food.per100),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        if (onEdit != null) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, stringResource(R.string.action_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onToggleFavorite != null) {
            IconButton(onClick = onToggleFavorite) {
                if (food.isFavorite) {
                    Icon(Icons.Filled.Star, stringResource(R.string.food_unfavorite), tint = MaterialTheme.colorScheme.tertiary)
                } else {
                    Icon(Icons.Outlined.StarBorder, stringResource(R.string.food_favorite), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun per100Line(n: Nutrition): String = stringResource(
    R.string.food_per100_line,
    n.calories.formatInt(), n.protein.formatMacro(), n.fat.formatMacro(), n.carbs.formatMacro()
)
