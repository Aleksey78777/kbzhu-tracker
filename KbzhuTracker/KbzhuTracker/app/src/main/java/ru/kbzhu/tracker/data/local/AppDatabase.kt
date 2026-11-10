package ru.kbzhu.tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.kbzhu.tracker.data.local.dao.DiaryDao
import ru.kbzhu.tracker.data.local.dao.FoodDao
import ru.kbzhu.tracker.data.local.dao.RecipeDao
import ru.kbzhu.tracker.data.local.dao.WaterDao
import ru.kbzhu.tracker.data.local.dao.WeightDao
import ru.kbzhu.tracker.data.local.entity.DiaryEntryEntity
import ru.kbzhu.tracker.data.local.entity.FoodEntity
import ru.kbzhu.tracker.data.local.entity.RecipeIngredientEntity
import ru.kbzhu.tracker.data.local.entity.WaterEntity
import ru.kbzhu.tracker.data.local.entity.WeightEntity
import ru.kbzhu.tracker.data.local.seed.SeedDatabaseCallback

@Database(
    entities = [
        FoodEntity::class,
        RecipeIngredientEntity::class,
        DiaryEntryEntity::class,
        WaterEntity::class,
        WeightEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun recipeDao(): RecipeDao
    abstract fun diaryDao(): DiaryDao
    abstract fun waterDao(): WaterDao
    abstract fun weightDao(): WeightDao

    companion object {
        const val NAME = "kbzhu.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                // При первом создании БД заливаем 200+ продуктов
                .addCallback(SeedDatabaseCallback())
                .build()
    }
}
