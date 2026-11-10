package ru.kbzhu.tracker.core.util

import java.time.LocalDate

/** Даты в БД храним как LocalDate.toEpochDay(). */
fun LocalDate.toDbValue(): Long = toEpochDay()

fun Long.toLocalDate(): LocalDate = LocalDate.ofEpochDay(this)

fun today(): LocalDate = LocalDate.now()
