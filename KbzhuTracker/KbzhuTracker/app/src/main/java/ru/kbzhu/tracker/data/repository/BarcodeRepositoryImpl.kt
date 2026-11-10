package ru.kbzhu.tracker.data.repository

import retrofit2.HttpException
import ru.kbzhu.tracker.data.local.dao.FoodDao
import ru.kbzhu.tracker.data.mapper.toDomain
import ru.kbzhu.tracker.data.mapper.toNewEntity
import ru.kbzhu.tracker.data.remote.OpenFoodFactsApi
import ru.kbzhu.tracker.data.remote.toFood
import ru.kbzhu.tracker.domain.model.BarcodeLookupResult
import ru.kbzhu.tracker.domain.model.Food
import ru.kbzhu.tracker.domain.model.NetworkError
import ru.kbzhu.tracker.domain.model.NetworkException
import ru.kbzhu.tracker.domain.repository.BarcodeRepository
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BarcodeRepositoryImpl @Inject constructor(
    private val api: OpenFoodFactsApi,
    private val foodDao: FoodDao
) : BarcodeRepository {

    override suspend fun lookup(barcode: String): BarcodeLookupResult {
        // 1. Кэш / свои продукты — работает офлайн
        foodDao.getByBarcode(barcode)?.let {
            return BarcodeLookupResult.Found(it.toDomain(), isIncomplete = it.calories <= 0.0)
        }
        // 2. Open Food Facts
        return try {
            val response = api.getProduct(barcode)
            val mapped = response.product
                ?.takeIf { response.status == 1 }
                ?.toFood(barcode)
                ?: return BarcodeLookupResult.NotFound(barcode)

            val id = foodDao.insert(mapped.food.copy(barcode = barcode).toNewEntity(System.currentTimeMillis()))
            val saved = foodDao.getById(id)?.toDomain() ?: mapped.food.copy(id = id)
            BarcodeLookupResult.Found(saved, isIncomplete = !mapped.hasNutrition)
        } catch (e: HttpException) {
            if (e.code() == 404) BarcodeLookupResult.NotFound(barcode)
            else BarcodeLookupResult.Error(barcode, NetworkError.SERVER)
        } catch (e: SocketTimeoutException) {
            BarcodeLookupResult.Error(barcode, NetworkError.TIMEOUT)
        } catch (e: IOException) {
            BarcodeLookupResult.Error(barcode, NetworkError.NO_CONNECTION)
        } catch (e: kotlinx.serialization.SerializationException) {
            BarcodeLookupResult.Error(barcode, NetworkError.SERVER)
        }
    }

    override suspend fun searchOnline(query: String): Result<List<Food>> = try {
        val foods = api.search(query).products
            .mapNotNull { it.toFood(null) }
            .filter { it.hasNutrition }
            .map { it.food }
            .distinctBy { it.barcode ?: it.name }
        Result.success(foods)
    } catch (e: HttpException) {
        Result.failure(NetworkException(NetworkError.SERVER))
    } catch (e: SocketTimeoutException) {
        Result.failure(NetworkException(NetworkError.TIMEOUT))
    } catch (e: IOException) {
        Result.failure(NetworkException(NetworkError.NO_CONNECTION))
    } catch (e: kotlinx.serialization.SerializationException) {
        Result.failure(NetworkException(NetworkError.SERVER))
    }
}
