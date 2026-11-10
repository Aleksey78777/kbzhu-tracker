package ru.kbzhu.tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Ответ GET /api/v2/product/{barcode}. status = 1 — найден. */
@Serializable
data class OffProductResponse(
    val code: String? = null,
    val status: Int = 0,
    val product: OffProduct? = null
)

/** Ответ /cgi/search.pl */
@Serializable
data class OffSearchResponse(
    val products: List<OffProduct> = emptyList()
)

/**
 * Продукт OFF. Значения nutriments приходят то числом, то строкой —
 * поэтому храним JsonElement и разбираем вручную.
 */
@Serializable
data class OffProduct(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_ru") val productNameRu: String? = null,
    val brands: String? = null,
    val nutriments: Map<String, JsonElement>? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    @SerialName("serving_quantity") val servingQuantity: JsonElement? = null,
    @SerialName("image_front_url") val imageFrontUrl: String? = null,
    @SerialName("image_front_small_url") val imageFrontSmallUrl: String? = null
) {
    fun nutriment(key: String): Double? = nutriments?.get(key).asDouble()
}

internal fun JsonElement?.asDouble(): Double? =
    (this as? JsonPrimitive)?.contentOrNull
        ?.replace(',', '.')
        ?.trim()
        ?.toDoubleOrNull()
        ?.takeIf { !it.isNaN() && it >= 0 }
