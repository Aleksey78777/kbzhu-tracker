package ru.kbzhu.tracker.ui.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

val RuLocale: Locale = Locale.forLanguageTag("ru-RU")

private val integerFormat: NumberFormat = NumberFormat.getIntegerInstance(RuLocale)
private val oneDecimal = DecimalFormat("0.#", DecimalFormatSymbols(RuLocale))

/** 1234.6 → «1 235» */
fun Double.formatInt(): String = integerFormat.format(roundToInt())
fun Int.formatInt(): String = integerFormat.format(this)

/** 12.34 → «12,3», 12.0 → «12» */
fun Double.format1(): String = oneDecimal.format(this)

/** Граммы БЖУ: до 10 г — с десятыми, дальше — целые. */
fun Double.formatMacro(): String = if (this < 10) format1() else formatInt()

/** Для полей ввода: без лишнего «,0». */
fun Double.toInputString(): String = format1().replace(' ', ' ')

/** Принимает и запятую, и точку. */
fun String.parseDecimal(): Double? = replace(',', '.').replace(" ", "").trim().toDoubleOrNull()

/**
 * Фильтр ввода чисел: цифры и один разделитель, не больше [maxDecimals] знаков после него.
 * Возвращает null, если ввод недопустим (тогда поле не меняется).
 */
fun sanitizeDecimal(input: String, maxDecimals: Int = 1, maxLength: Int = 7): String? {
    val normalized = input.replace('.', ',')
    if (normalized.length > maxLength) return null
    if (normalized.count { it == ',' } > 1) return null
    if (!normalized.all { it.isDigit() || it == ',' }) return null
    val decimals = normalized.substringAfter(',', "")
    if (decimals.length > maxDecimals) return null
    return normalized
}

fun sanitizeInt(input: String, maxLength: Int = 4): String? =
    if (input.length <= maxLength && input.all { it.isDigit() }) input else null

private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", RuLocale)
private val dayMonthWeekday = DateTimeFormatter.ofPattern("EEEE, d MMMM", RuLocale)
private val shortDay = DateTimeFormatter.ofPattern("d.MM", RuLocale)
private val weekdayShort = DateTimeFormatter.ofPattern("EE", RuLocale)

fun LocalDate.formatDayMonth(): String = format(dayMonth)
fun LocalDate.formatFull(): String = format(dayMonthWeekday).replaceFirstChar { it.titlecase(RuLocale) }
fun LocalDate.formatShort(): String = format(shortDay)
fun LocalDate.formatWeekdayShort(): String = format(weekdayShort).replaceFirstChar { it.titlecase(RuLocale) }
