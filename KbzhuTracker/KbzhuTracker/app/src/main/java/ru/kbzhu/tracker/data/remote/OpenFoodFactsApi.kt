package ru.kbzhu.tracker.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import ru.kbzhu.tracker.data.remote.dto.OffProductResponse
import ru.kbzhu.tracker.data.remote.dto.OffSearchResponse

/**
 * Open Food Facts — открытая база продуктов, ключ API не нужен.
 * Обязателен осмысленный User-Agent (добавляется интерцептором в NetworkModule).
 */
interface OpenFoodFactsApi {

    @GET("api/v2/product/{barcode}")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = FIELDS
    ): OffProductResponse

    @GET("cgi/search.pl")
    suspend fun search(
        @Query("search_terms") query: String,
        @Query("search_simple") simple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("lc") language: String = "ru",
        @Query("fields") fields: String = FIELDS
    ): OffSearchResponse

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
        const val FIELDS =
            "code,product_name,product_name_ru,brands,nutriments,serving_size," +
                "serving_quantity,image_front_url,image_front_small_url"
    }
}
