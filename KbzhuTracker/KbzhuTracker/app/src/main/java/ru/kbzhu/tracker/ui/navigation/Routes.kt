package ru.kbzhu.tracker.ui.navigation

import kotlinx.serialization.Serializable

/*
 * Type-safe маршруты Navigation Compose 2.8+.
 * meal — MealType.name, date — LocalDate.toEpochDay(). Если они заданы —
 * экран открыт «для добавления в дневник» на конкретный приём пищи и дату.
 */

@Serializable
data class OnboardingRoute(val editMode: Boolean = false)

// --- Вкладки нижней навигации ---
@Serializable
data object DiaryRoute

@Serializable
data object FoodsRoute

@Serializable
data object StatsRoute

@Serializable
data object ProfileRoute

// --- Вложенные экраны ---

/** Поиск продукта для добавления в дневник (тот же экран, что и вкладка «Продукты»). */
@Serializable
data class AddFoodRoute(val meal: String, val date: Long)

/**
 * Карточка продукта с выбором порции.
 * foodId — добавить продукт; entryId — редактировать существующую запись дневника.
 */
@Serializable
data class FoodDetailRoute(
    val foodId: Long? = null,
    val entryId: Long? = null,
    val meal: String? = null,
    val date: Long? = null
)

/** Создание (foodId = null) или редактирование своего продукта. barcode — подставить из сканера. */
@Serializable
data class FoodEditRoute(
    val foodId: Long? = null,
    val barcode: String? = null,
    val meal: String? = null,
    val date: Long? = null
)

@Serializable
data class RecipeEditRoute(val recipeId: Long? = null)

@Serializable
data class ScannerRoute(val meal: String? = null, val date: Long? = null)
