package ru.kbzhu.tracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kbzhu.tracker.data.datastore.SettingsRepositoryImpl
import ru.kbzhu.tracker.data.repository.BarcodeRepositoryImpl
import ru.kbzhu.tracker.data.repository.DiaryRepositoryImpl
import ru.kbzhu.tracker.data.repository.FoodRepositoryImpl
import ru.kbzhu.tracker.data.repository.WaterRepositoryImpl
import ru.kbzhu.tracker.data.repository.WeightRepositoryImpl
import ru.kbzhu.tracker.data.storage.PhotoStorageImpl
import ru.kbzhu.tracker.domain.repository.BarcodeRepository
import ru.kbzhu.tracker.domain.repository.DiaryRepository
import ru.kbzhu.tracker.domain.repository.FoodRepository
import ru.kbzhu.tracker.domain.repository.PhotoStorage
import ru.kbzhu.tracker.domain.repository.SettingsRepository
import ru.kbzhu.tracker.domain.repository.WaterRepository
import ru.kbzhu.tracker.domain.repository.WeightRepository
import javax.inject.Singleton

/** Связывает интерфейсы домена с реализациями data-слоя. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository
    @Binds @Singleton abstract fun bindBarcodeRepository(impl: BarcodeRepositoryImpl): BarcodeRepository
    @Binds @Singleton abstract fun bindDiaryRepository(impl: DiaryRepositoryImpl): DiaryRepository
    @Binds @Singleton abstract fun bindWaterRepository(impl: WaterRepositoryImpl): WaterRepository
    @Binds @Singleton abstract fun bindWeightRepository(impl: WeightRepositoryImpl): WeightRepository
    @Binds @Singleton abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun bindPhotoStorage(impl: PhotoStorageImpl): PhotoStorage
}
