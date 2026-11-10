package ru.kbzhu.tracker.domain.model

/** Приём пищи. Порядок важен — в таком порядке секции идут в дневнике. */
enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

/** Откуда взялся продукт. */
enum class FoodSource {
    /** Предзаполненная база приложения */
    BUILT_IN,
    /** Создан пользователем */
    USER,
    /** Блюдо/рецепт из нескольких ингредиентов */
    RECIPE,
    /** Найден по штрих-коду в Open Food Facts и закэширован */
    OPEN_FOOD_FACTS
}

/** Категории продуктов — используются в фильтрах поиска. */
enum class FoodCategory {
    VEGETABLES, FRUITS, MEAT, FISH, EGGS, DAIRY, GRAINS, BAKERY,
    NUTS, OILS_SAUCES, SWEETS, DRINKS, READY_MEALS, OTHER
}

enum class Gender { MALE, FEMALE }

enum class Goal { LOSE, MAINTAIN, GAIN }

/** Уровень активности и коэффициент для расчёта TDEE. */
enum class ActivityLevel(val factor: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    HIGH(1.725),
    EXTREME(1.9)
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
