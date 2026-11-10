package ru.kbzhu.tracker.data.local.seed

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.kbzhu.tracker.core.util.buildSearchKey
import ru.kbzhu.tracker.domain.model.FoodSource

/**
 * Заполняет таблицу foods встроенными продуктами при самом первом создании БД.
 * Выполняется синхронно в той же транзакции открытия БД — к первому запросу данные уже есть.
 */
class SeedDatabaseCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val now = System.currentTimeMillis()
        val sql = """
            INSERT INTO foods (
                name, brand, category, calories, protein, fat, carbs,
                serving_size, serving_name, barcode, photo_path, image_url,
                source, is_favorite, last_used_at, use_count, recipe_yield, recipe_portions,
                search_key, created_at
            ) VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, ?, 0, NULL, 0, NULL, NULL, ?, ?)
        """.trimIndent()

        db.beginTransaction()
        try {
            SeedFoods.items.forEach { item ->
                db.execSQL(
                    sql,
                    arrayOf<Any?>(
                        item.name,
                        item.category.name,
                        item.kcal,
                        item.protein,
                        item.fat,
                        item.carbs,
                        item.servingSize,
                        item.servingName,
                        FoodSource.BUILT_IN.name,
                        buildSearchKey(item.name, null),
                        now
                    )
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
