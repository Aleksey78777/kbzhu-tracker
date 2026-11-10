package ru.kbzhu.tracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.ui.theme.AppTheme
import ru.kbzhu.tracker.ui.theme.BigNumberStyle
import ru.kbzhu.tracker.ui.util.formatInt
import kotlin.math.abs

/**
 * Круговой индикатор калорий. В центре — сколько осталось (или перебор).
 * Дуга на 270°, разрыв снизу — так цифра читается лучше, чем в полном круге.
 */
@Composable
fun CalorieRing(
    eaten: Double,
    target: Int,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 16.dp
) {
    val fraction = if (target > 0) (eaten / target).toFloat() else 0f
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "calorieRing"
    )
    val remaining = target - eaten
    val over = remaining < 0
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val progressColor = if (over) MaterialTheme.colorScheme.error else AppTheme.nutrients.calories
    val description = stringResource(R.string.cd_calorie_ring, eaten.formatInt(), target.formatInt())

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val diameter = this.size.minDimension - stroke
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(diameter, diameter)
            val startAngle = 135f
            val sweep = 270f
            drawArc(
                color = trackColor, startAngle = startAngle, sweepAngle = sweep, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )
            if (animated > 0f) {
                drawArc(
                    color = progressColor, startAngle = startAngle, sweepAngle = sweep * animated,
                    useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = abs(remaining).formatInt(),
                style = BigNumberStyle,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(if (over) R.string.kcal_over else R.string.kcal_left),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
