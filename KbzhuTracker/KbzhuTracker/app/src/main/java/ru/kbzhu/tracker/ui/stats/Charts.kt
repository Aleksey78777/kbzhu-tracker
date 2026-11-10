package ru.kbzhu.tracker.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.kbzhu.tracker.domain.model.DailyStat
import ru.kbzhu.tracker.domain.model.WeightRecord
import ru.kbzhu.tracker.ui.util.format1
import ru.kbzhu.tracker.ui.util.formatInt
import ru.kbzhu.tracker.ui.util.formatShort
import ru.kbzhu.tracker.ui.util.formatWeekdayShort
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/*
 * Графики нарисованы на Canvas без сторонних библиотек.
 * Оформление: столбцы ≤ 24 dp со скруглением 4 dp сверху, линия 2 dp,
 * маркеры ≥ 8 dp с кольцом цвета фона, тонкая сплошная сетка, подписи — цветами текста.
 * По нажатию на столбец/точку показывается подсказка со значением.
 */

private val AxisWidth = 40.dp
private val BottomAxis = 24.dp
private val TopPad = 28.dp

/** Столбчатый график калорий по дням с линией нормы. */
@Composable
fun CaloriesBarChart(
    days: List<DailyStat>,
    target: Int,
    barColor: Color,
    targetLabel: String,
    kcalUnit: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp
) {
    val measurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(color = colors.inverseOnSurface)
    val surface = colors.surfaceContainerLow
    var selected by remember(days) { mutableStateOf<Int?>(null) }

    // Анимация роста столбцов при смене периода
    val grow = remember { Animatable(0f) }
    LaunchedEffect(days) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(600))
    }

    val maxValue = max(days.maxOfOrNull { it.nutrition.calories } ?: 0.0, target.toDouble())
    val step = niceStep(maxValue)
    val yMax = (ceil(maxValue * 1.1 / step) * step).coerceAtLeast(step)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(days) {
                detectTapGestures { offset ->
                    val plotLeft = AxisWidth.toPx()
                    val slot = (size.width - plotLeft) / days.size
                    val index = ((offset.x - plotLeft) / slot).toInt()
                    selected = if (index in days.indices && selected != index) index else null
                }
            }
    ) {
        val plotLeft = AxisWidth.toPx()
        val plotTop = TopPad.toPx()
        val plotBottom = size.height - BottomAxis.toPx()
        val plotHeight = plotBottom - plotTop
        val plotWidth = size.width - plotLeft
        fun y(v: Double) = plotBottom - (v / yMax * plotHeight).toFloat()

        // Сетка и подписи оси Y
        var tick = 0.0
        while (tick <= yMax + 0.1) {
            val ty = y(tick)
            drawLine(colors.outlineVariant, Offset(plotLeft, ty), Offset(size.width, ty), strokeWidth = 1.dp.toPx())
            drawLabel(measurer, tick.formatInt(), labelStyle, Offset(0f, ty), alignRight = plotLeft - 6.dp.toPx())
            tick += step
        }

        // Столбцы
        val slot = plotWidth / days.size
        val barWidth = min(slot * 0.62f, 24.dp.toPx())
        val radius = 4.dp.toPx()
        days.forEachIndexed { i, day ->
            val value = day.nutrition.calories * grow.value
            if (value <= 0) return@forEachIndexed
            val cx = plotLeft + slot * i + slot / 2
            val top = y(value)
            val rect = Rect(cx - barWidth / 2, top, cx + barWidth / 2, plotBottom)
            val alpha = if (selected == null || selected == i) 1f else 0.45f
            drawPath(
                Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect,
                            topLeft = CornerRadius(radius), topRight = CornerRadius(radius),
                            bottomRight = CornerRadius.Zero, bottomLeft = CornerRadius.Zero
                        )
                    )
                },
                barColor.copy(alpha = alpha)
            )
        }

        // Линия нормы (поверх столбцов) с подписью справа
        if (target > 0) {
            val ty = y(target.toDouble())
            drawLine(colors.onSurface.copy(alpha = 0.7f), Offset(plotLeft, ty), Offset(size.width, ty), strokeWidth = 1.5.dp.toPx())
            val text = "$targetLabel ${target.formatInt()}"
            val layout = measurer.measure(text, labelStyle.copy(color = colors.onSurface))
            drawRect(surface, Offset(size.width - layout.size.width - 8.dp.toPx(), ty - layout.size.height - 2.dp.toPx()),
                Size(layout.size.width + 8.dp.toPx(), layout.size.height.toFloat()))
            drawText(layout, topLeft = Offset(size.width - layout.size.width - 4.dp.toPx(), ty - layout.size.height - 2.dp.toPx()))
        }

        // Подписи оси X: неделя — все дни, месяц — каждый 5-й и последний
        days.forEachIndexed { i, day ->
            val show = days.size <= 7 || i % 5 == 0 || i == days.lastIndex
            if (!show) return@forEachIndexed
            val text = if (days.size <= 7) day.date.formatWeekdayShort() else day.date.dayOfMonth.toString()
            val cx = plotLeft + slot * i + slot / 2
            val layout = measurer.measure(text, labelStyle)
            drawText(layout, topLeft = Offset(cx - layout.size.width / 2, plotBottom + 6.dp.toPx()))
        }

        // Подсказка выбранного дня
        selected?.let { i ->
            val day = days[i]
            val cx = plotLeft + slot * i + slot / 2
            val text = "${day.date.formatShort()}: ${day.nutrition.calories.formatInt()} $kcalUnit"
            drawTooltip(measurer, text, tooltipStyle, colors.inverseSurface, anchor = Offset(cx, plotTop - 4.dp.toPx()))
        }
    }
}

/** Линейный график веса: линия 2 dp, лёгкая заливка, точки с кольцом, подпись последнего значения. */
@Composable
fun WeightLineChart(
    records: List<WeightRecord>,
    goalWeight: Double?,
    lineColor: Color,
    goalLabel: String,
    kgUnit: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp
) {
    if (records.size < 2) return
    val measurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = colors.onSurface)
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(color = colors.inverseOnSurface)
    val surface = colors.surfaceContainerLow
    var selected by remember(records) { mutableStateOf<Int?>(null) }

    val dataMin = records.minOf { it.weightKg }
    val dataMax = records.maxOf { it.weightKg }
    // Цель показываем, только если она недалеко от данных — иначе график сплющится
    val showGoal = goalWeight != null && goalWeight in (dataMin - 3)..(dataMax + 3)
    val lo = min(dataMin, if (showGoal) goalWeight!! else dataMin) - 0.5
    val hi = max(dataMax, if (showGoal) goalWeight!! else dataMax) + 0.5
    val firstDay = records.first().date.toEpochDay()
    val span = max(1L, records.last().date.toEpochDay() - firstDay).toFloat()

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(records) {
                detectTapGestures { offset ->
                    val plotLeft = AxisWidth.toPx()
                    val plotWidth = size.width - plotLeft - 12.dp.toPx()
                    val nearest = records.indices.minByOrNull { i ->
                        val x = plotLeft + (records[i].date.toEpochDay() - firstDay) / span * plotWidth
                        abs(x - offset.x)
                    }
                    selected = if (nearest != selected) nearest else null
                }
            }
    ) {
        val plotLeft = AxisWidth.toPx()
        val plotTop = TopPad.toPx()
        val plotBottom = size.height - BottomAxis.toPx()
        val plotWidth = size.width - plotLeft - 12.dp.toPx()
        val plotHeight = plotBottom - plotTop
        fun x(r: WeightRecord) = plotLeft + (r.date.toEpochDay() - firstDay) / span * plotWidth
        fun y(v: Double) = plotBottom - ((v - lo) / (hi - lo) * plotHeight).toFloat()

        // Сетка: 3 уровня
        listOf(lo, (lo + hi) / 2, hi).forEach { v ->
            val ty = y(v)
            drawLine(colors.outlineVariant, Offset(plotLeft, ty), Offset(size.width, ty), strokeWidth = 1.dp.toPx())
            drawLabel(measurer, v.format1(), labelStyle, Offset(0f, ty), alignRight = plotLeft - 6.dp.toPx())
        }

        if (showGoal) {
            val gy = y(goalWeight!!)
            drawLine(colors.onSurface.copy(alpha = 0.6f), Offset(plotLeft, gy), Offset(size.width, gy), strokeWidth = 1.5.dp.toPx())
            val layout = measurer.measure("$goalLabel ${goalWeight.format1()}", labelStyle.copy(color = colors.onSurface))
            drawText(layout, topLeft = Offset(plotLeft + 4.dp.toPx(), gy - layout.size.height - 2.dp.toPx()))
        }

        // Заливка под линией (~10%) и сама линия
        val line = Path()
        records.forEachIndexed { i, r -> if (i == 0) line.moveTo(x(r), y(r.weightKg)) else line.lineTo(x(r), y(r.weightKg)) }
        val area = Path().apply {
            addPath(line)
            lineTo(x(records.last()), plotBottom)
            lineTo(x(records.first()), plotBottom)
            close()
        }
        drawPath(area, lineColor.copy(alpha = 0.10f))
        drawPath(line, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Точки: r = 4 dp + кольцо 2 dp цвета фона
        records.forEachIndexed { i, r ->
            val c = Offset(x(r), y(r.weightKg))
            val rr = if (selected == i) 6.dp.toPx() else 4.dp.toPx()
            drawCircle(surface, radius = rr + 2.dp.toPx(), center = c)
            drawCircle(lineColor, radius = rr, center = c)
        }

        // Подпись последнего значения
        val last = records.last()
        val lastLayout = measurer.measure("${last.weightKg.format1()} $kgUnit", valueStyle)
        val lx = (x(last) - lastLayout.size.width).coerceAtLeast(plotLeft)
        drawText(lastLayout, topLeft = Offset(lx, y(last.weightKg) - lastLayout.size.height - 8.dp.toPx()))

        // Даты на оси X: первая и последняя
        val firstLayout = measurer.measure(records.first().date.formatShort(), labelStyle)
        drawText(firstLayout, topLeft = Offset(plotLeft, plotBottom + 6.dp.toPx()))
        val endLayout = measurer.measure(last.date.formatShort(), labelStyle)
        drawText(endLayout, topLeft = Offset(size.width - endLayout.size.width, plotBottom + 6.dp.toPx()))

        selected?.let { i ->
            val r = records[i]
            drawTooltip(
                measurer, "${r.date.formatShort()}: ${r.weightKg.format1()} $kgUnit",
                tooltipStyle, colors.inverseSurface, anchor = Offset(x(r), plotTop - 4.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawLabel(
    measurer: TextMeasurer, text: String, style: TextStyle, center: Offset, alignRight: Float
) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(alignRight - layout.size.width, center.y - layout.size.height / 2))
}

private fun DrawScope.drawTooltip(
    measurer: TextMeasurer, text: String, style: TextStyle, background: Color, anchor: Offset
) {
    val layout = measurer.measure(text, style)
    val padH = 8.dp.toPx()
    val padV = 4.dp.toPx()
    val w = layout.size.width + padH * 2
    val h = layout.size.height + padV * 2
    val left = (anchor.x - w / 2).coerceIn(0f, size.width - w)
    val top = (anchor.y - h).coerceAtLeast(0f)
    drawRoundRect(background, Offset(left, top), Size(w, h), CornerRadius(8.dp.toPx()))
    drawText(layout, topLeft = Offset(left + padH, top + padV))
}

/** «Круглый» шаг сетки: 250 / 500 / 1000 ккал. */
private fun niceStep(max: Double): Double = when {
    max <= 1200 -> 250.0
    max <= 3500 -> 500.0
    else -> 1000.0
}
