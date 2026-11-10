package ru.kbzhu.tracker.core.util

import java.util.Locale

/**
 * Нормализация строки для поиска.
 * SQLite LIKE не умеет регистронезависимо сравнивать кириллицу,
 * поэтому храним отдельную колонку search_key в нижнем регистре и с «ё» → «е».
 */
fun String.toSearchKey(): String =
    lowercase(Locale.ROOT)
        .replace('ё', 'е')
        .replace(Regex("\\s+"), " ")
        .trim()

fun buildSearchKey(name: String, brand: String?): String =
    listOfNotNull(name, brand?.takeIf { it.isNotBlank() })
        .joinToString(" ")
        .toSearchKey()

/** Экранирует спецсимволы LIKE, чтобы ввод «%» или «_» не ломал поиск. */
fun String.escapeLike(): String =
    replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
