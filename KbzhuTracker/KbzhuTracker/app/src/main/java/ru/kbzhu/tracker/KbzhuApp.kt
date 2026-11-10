package ru.kbzhu.tracker

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

/**
 * Точка входа Hilt. Заодно настраиваем Coil: картинки из Open Food Facts
 * кэшируются на диск и показываются без интернета.
 */
@HiltAndroidApp
class KbzhuApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .crossfade(true)
        .memoryCache {
            MemoryCache.Builder(this).maxSizePercent(0.20).build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(100L * 1024 * 1024)
                .build()
        }
        .respectCacheHeaders(false)
        .build()
}
