package ru.kbzhu.tracker.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.kbzhu.tracker.data.local.AppDatabase
import ru.kbzhu.tracker.data.local.dao.DiaryDao
import ru.kbzhu.tracker.data.local.dao.FoodDao
import ru.kbzhu.tracker.data.local.dao.RecipeDao
import ru.kbzhu.tracker.data.local.dao.WaterDao
import ru.kbzhu.tracker.data.local.dao.WeightDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase = AppDatabase.create(context)

    @Provides fun provideFoodDao(db: AppDatabase): FoodDao = db.foodDao()
    @Provides fun provideRecipeDao(db: AppDatabase): RecipeDao = db.recipeDao()
    @Provides fun provideDiaryDao(db: AppDatabase): DiaryDao = db.diaryDao()
    @Provides fun provideWaterDao(db: AppDatabase): WaterDao = db.waterDao()
    @Provides fun provideWeightDao(db: AppDatabase): WeightDao = db.weightDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings") }
        )
}
