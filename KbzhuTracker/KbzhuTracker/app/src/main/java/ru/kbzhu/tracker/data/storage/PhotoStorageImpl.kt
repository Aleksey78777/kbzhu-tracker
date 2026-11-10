package ru.kbzhu.tracker.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.kbzhu.tracker.domain.repository.PhotoStorage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Сжимает фото до 1280 px по длинной стороне, JPEG 82%, учитывает EXIF-поворот
 * и сохраняет в filesDir/photos (файлы приватны приложению и удаляются вместе с ним).
 */
@Singleton
class PhotoStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PhotoStorage {

    private val authority = "${context.packageName}.fileprovider"
    private val photosDir: File get() = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }
    private val cameraDir: File get() = File(context.cacheDir, CAMERA_DIR).apply { mkdirs() }

    override fun createCameraUri(): String {
        val file = File(cameraDir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, authority, file).toString()
    }

    override fun discardCameraUri(uri: String) {
        cameraTempFile(Uri.parse(uri))?.delete()
    }

    override suspend fun importPhoto(sourceUri: String): String = withContext(Dispatchers.IO) {
        val uri = Uri.parse(sourceUri)
        val resolver = context.contentResolver

        // 1. Узнаём размер, не загружая картинку в память
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IOException("Не удалось открыть изображение")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Файл не является изображением")

        // 2. Декодируем с уменьшением (inSampleSize — степень двойки)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_SIDE)
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Не удалось декодировать изображение")

        // 3. Поворот по EXIF (камеры часто пишут ориентацию тегом, а не поворачивают пиксели)
        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)

        val scaled = scaleToMax(decoded, MAX_SIDE)
        val result = rotate(scaled, rotation)

        val outFile = File(photosDir, "${UUID.randomUUID()}.jpg")
        try {
            FileOutputStream(outFile).use { out ->
                if (!result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                    throw IOException("Не удалось сохранить изображение")
                }
            }
        } catch (e: IOException) {
            outFile.delete()
            throw e
        } finally {
            setOf(decoded, scaled, result).forEach { if (!it.isRecycled) it.recycle() }
        }

        cameraTempFile(uri)?.delete()
        outFile.absolutePath
    }

    override suspend fun delete(path: String) {
        withContext(Dispatchers.IO) {
            val file = File(path)
            // Удаляем только свои файлы
            if (file.canonicalFile.parentFile == photosDir.canonicalFile) file.delete()
        }
    }

    /** Если URI указывает на наш временный файл камеры — возвращает его. */
    private fun cameraTempFile(uri: Uri): File? {
        if (uri.authority != authority) return null
        val segments = uri.pathSegments
        if (segments.size < 2 || segments[0] != CAMERA_DIR) return null
        return File(cameraDir, segments.last())
    }

    private fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxSide) sample *= 2
        return sample
    }

    private fun scaleToMax(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val ratio = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).roundToInt(),
            (bitmap.height * ratio).roundToInt(),
            true
        )
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private companion object {
        const val PHOTOS_DIR = "photos"
        const val CAMERA_DIR = "camera"
        const val MAX_SIDE = 1280
        const val JPEG_QUALITY = 82
    }
}
