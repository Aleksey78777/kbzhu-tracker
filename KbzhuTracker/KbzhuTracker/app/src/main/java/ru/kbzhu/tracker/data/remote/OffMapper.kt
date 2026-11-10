package ru.kbzhu.tracker.data.remote

import ru.kbzhu.tracker.data.remote.dto.OffProduct
import ru.kbzhu.tracker.data.remote.dto.asDouble
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.domain.model.FoodCategory
import ru.kbzhu.tracker.domain.model.FoodSource
import ru.kbzhu.tracker.domain.model.Nutrition

private const val KJ_PER_KCAL = 4.184

data class OffMappedFood(val food: Food, val hasNutrition: Boolean)

/** null — данных слишком мало (нет ни названия, ни калорийности). */
fun OffProduct.toFood(fallbackBarcode: String?): OffMappedFood? {
    val kcal = nutriment("energy-kcal_100g")
        ?: nutriment("energy_100g")?.div(KJ_PER_KCAL)
    val protein = nutriment("proteins_100g")
    val fat = nutriment("fat_100g")
    val carbs = nutriment("carbohydrates_100g")

    val name = productNameRu?.trim()?.takeIf { it.isNotEmpty() }
        ?: productName?.trim()?.takeIf { it.isNotEmpty() }
    if (name == null && kcal == null) return null

    val barcode = code?.takeIf { it.isNotBlank() } ?: fallbackBarcode
    val food = Food(
        name = name ?: "Товар ${barcode.orEmpty()}".trim(),
        brand = brands?.split(',')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() },
        category = FoodCategory.OTHER,
        per100 = Nutrition(
            calories = kcal ?: 0.0,
            protein = protein ?: 0.0,
            fat = fat ?: 0.0,
            carbs = carbs ?: 0.0
        ),
        servingSize = servingQuantity.asDouble()?.takeIf { it > 0 },
        servingName = servingSize?.trim()?.takeIf { it.isNotEmpty() }?.take(40),
        barcode = barcode,
        imageUrl = imageFrontUrl ?: imageFrontSmallUrl,
        source = FoodSource.OPEN_FOOD_FACTS
    )
    return OffMappedFood(food, hasNutrition = kcal != null)
}
