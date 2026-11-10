package ru.kbzhu.tracker.ui.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Анализатор кадров CameraX → ML Kit. Распознаёт только товарные коды:
 * EAN-13, EAN-8, UPC-A, UPC-E — так меньше ложных срабатываний на QR и прочем.
 * Результат приходит в главный поток (так работает Task ML Kit по умолчанию).
 */
class BarcodeAnalyzer(
    private val onDetected: (rawValue: String, format: Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
    )

    /** Пока идёт запрос к серверу или показан результат — кадры не анализируем. */
    @Volatile
    var enabled: Boolean = true

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (!enabled || mediaImage == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.let { code ->
                    if (enabled) onDetected(code.rawValue!!, code.format)
                }
            }
            // Кадр обязательно закрываем, иначе CameraX перестанет присылать новые
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() = scanner.close()
}

/** Проверка контрольной цифры GTIN (EAN-13, EAN-8, UPC-A) — отсекает ошибочно считанные коды. */
object BarcodeValidator {

    fun isValidGtin(code: String): Boolean {
        if (code.length !in setOf(8, 12, 13) || !code.all { it.isDigit() }) return false
        val digits = code.map { it - '0' }
        val check = digits.last()
        val body = digits.dropLast(1).reversed()
        val sum = body.mapIndexed { i, d -> if (i % 2 == 0) d * 3 else d }.sum()
        return (10 - sum % 10) % 10 == check
    }

    /**
     * Приводит код к виду, в котором храним в БД: UPC-A (12 цифр) → EAN-13 с ведущим нулём.
     * null — код некорректен.
     */
    fun normalize(raw: String, format: Int?): String? {
        val code = raw.trim()
        if (!code.all { it.isDigit() }) return null
        return when {
            // UPC-E имеет свою схему контрольной цифры — доверяем ML Kit
            format == Barcode.FORMAT_UPC_E -> code.takeIf { it.length in 6..8 }
            !isValidGtin(code) -> null
            code.length == 12 -> "0$code"
            else -> code
        }
    }
}
