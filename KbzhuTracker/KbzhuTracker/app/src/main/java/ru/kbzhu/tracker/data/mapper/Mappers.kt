package ru.kbzhu.tracker.data.mapper

import ru.kbzhu.tracker.core.util.buildSearchKey
import ru.kbzhu.tracker.core.util.toDbValue
import ru.kbzhu.tracker.core.util.toLocalDate
import ru.kbzhu.tracker.data.local.entity.DailyTotalsTuple
import ru.kbzhu.tracker.data.local.entity.DiaryEntryEntity
import ru.kbzhu.tracker.data.local.entity.DiaryEntryWithFoodImage
import ru.kbzhu.tracker.data.local.entity.FoodEntity
import ru.kbzhu.tracker.data.local.entity.WeightEntity
import ru.kbzhu.tracker.domain.model.DailyStat
import ru.kbzhu.tracker.domain.model.DiaryEntry
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.domain.model.Nutrition
import ru.kbzhu.tracker.domain.model.WeightRecord

fun FoodEntity.toDomain() = Food(
    id = id,
    name = name,
    brand = brand,
    category = category,
    per100 = Nutrition(calories, protein, fat, carbs),
    servingSize = servingSize,
    servingName = servingName,
    barcode = barcode,
    photoPath = photoPath,
    imageUrl = imageUrl,
    source = source,
    isFavorite = isFavorite,
    lastUsedAt = lastUsedAt,
    recipeYield = recipeYield,
    recipePortions = recipePortions
)

/** Новая сущность из доменной модели. */
fun Food.toNewEntity(now: Long) = FoodEntity(
    id = 0,
    name = name,
    brand = brand,
    category = category,
    calories = per100.calories,
    protein = per100.protein,
    fat = per100.fat,
    carbs = per100.carbs,
    servingSize = servingSize,
    servingName = servingName,
    barcode = barcode,
    photoPath = photoPath,
    imageUrl = imageUrl,
    source = source,
    isFavorite = isFavorite,
    lastUsedAt = lastUsedAt,
    useCount = 0,
    recipeYield = recipeYield,
    recipePortions = recipePortions,
    searchKey = buildSearchKey(name, brand),
    createdAt = now
)

/** Обновление существующей строки: статистика использования и дата создания сохраняются. */
fun FoodEntity.updatedFrom(food: Food) = copy(
    name = food.name,
    brand = food.brand,
    category = food.category,
    calories = food.per100.calories,
    protein = food.per100.protein,
    fat = food.per100.fat,
    carbs = food.per100.carbs,
    servingSize = food.servingSize,
    servingName = food.servingName,
    barcode = food.barcode,
    photoPath = food.photoPath,
    imageUrl = food.imageUrl,
    source = food.source,
    isFavorite = food.isFavorite,
    recipeYield = food.recipeYield,
    recipePortions = food.recipePortions,
    searchKey = buildSearchKey(food.name, food.brand)
)

fun DiaryEntryWithFoodImage.toDomain() = entry.toDomain().copy(
    foodPhotoPath = foodPhotoPath,
    foodImageUrl = foodImageUrl
)

fun DiaryEntryEntity.toDomain() = DiaryEntry(
    id = id,
    date = date.toLocalDate(),
    mealType = mealType,
    foodId = foodId,
    name = name,
    brand = brand,
    grams = grams,
    per100 = Nutrition(caloriesPer100, proteinPer100, fatPer100, carbsPer100),
    photoPath = photoPath,
    createdAt = createdAt
)

fun DiaryEntry.toEntity() = DiaryEntryEntity(
    id = id,
    date = date.toDbValue(),
    mealType = mealType,
    foodId = foodId,
    name = name,
    brand = brand,
    grams = grams,
    caloriesPer100 = per100.calories,
    proteinPer100 = per100.protein,
    fatPer100 = per100.fat,
    carbsPer100 = per100.carbs,
    photoPath = photoPath,
    createdAt = createdAt
)

fun DailyTotalsTuple.toDomain() = DailyStat(
    date = date.toLocalDate(),
    nutrition = Nutrition(calories, protein, fat, carbs)
)

fun WeightEntity.toDomain() = WeightRecord(date.toLocalDate(), weightKg)
