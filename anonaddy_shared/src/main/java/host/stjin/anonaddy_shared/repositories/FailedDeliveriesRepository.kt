package host.stjin.anonaddy_shared.repositories

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import host.stjin.anonaddy_shared.ServiceLocator
import host.stjin.anonaddy_shared.AddyIo.API_URL_FAILED_DELIVERIES
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.FailedDeliveries
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.network.BaseNetworkClient
import host.stjin.anonaddy_shared.network.NetworkResult
import org.json.JSONArray
import org.json.JSONObject
import host.stjin.anonaddy_shared.utils.DefaultDispatcherProvider
import host.stjin.anonaddy_shared.utils.DispatcherProvider
import host.stjin.anonaddy_shared.utils.fromJson

class FailedDeliveriesRepository(
    context: Context,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BaseNetworkClient(context, dispatchers) {

    suspend fun getAllFailedDeliveries(
        page: Int? = 1,
        size: Int? = 25,
        filter: String? = null
    ): NetworkResult<PaginatedResponse<FailedDeliveries>> {
        waitForInit()

        val parameters = ArrayList<Pair<String, Any>>()
        if (page != null) parameters.add(Pair("page[number]", page.toString()))
        if (size != null) parameters.add(Pair("page[size]", size.toString()))
        if (filter != null) parameters.add(Pair("filter[email_type]", filter))

        val (_, response, result) = Fuel.get(API_URL_FAILED_DELIVERIES, parameters)
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData: PaginatedResponse<FailedDeliveries> = gson.fromJson(data)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            404 -> {
                NetworkResult.Error("404", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "getAllFailedDeliveries")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun downloadSpecificFailedDelivery(id: String): NetworkResult<ByteArray> {
        waitForInit()

        val (_, response, result) = Fuel.get("${API_URL_FAILED_DELIVERIES}/$id/download")
            .appendHeader(*getHeaders())
            .awaitByteArrayResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                NetworkResult.Success(data, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericErrorByteArray(response, result, "downloadSpecificFailedDelivery")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun resendFailedDelivery(id: String, recipientIds: List<String>? = null): NetworkResult<Unit> {
        waitForInit()

        val json = JSONObject().apply {
            if (recipientIds != null) {
                put("recipient_ids", JSONArray(recipientIds))
            }
        }

        val (_, response, result) = Fuel.post("${API_URL_FAILED_DELIVERIES}/$id/resend")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200, 204 -> NetworkResult.Success(Unit, response.statusCode)
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "resendFailedDelivery")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun deleteFailedDelivery(id: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("${API_URL_FAILED_DELIVERIES}/$id")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            204 -> NetworkResult.Success("204", response.statusCode)
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "deleteFailedDelivery")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun cacheFailedDeliveryCountForWidgetAndBackgroundService(previousId: String?): NetworkResult<Pair<Int, String?>> {
        waitForInit()

        val settingsManager = ServiceLocator().apply { init(context) }.settingsManager
        val filterType = settingsManager.getSettingsString(SettingsManager.PREFS.NOTIFY_FAILED_DELIVERIES_TYPE) ?: "all"

        return when (val deliveriesResult = getAllFailedDeliveries(1, 25, null)) {
            is NetworkResult.Success -> {
                val result = deliveriesResult.data
                val totalCount = result.meta?.total ?: result.data.size
                encryptedSettingsManager.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT, totalCount)

                val latestId = result.data.firstOrNull()?.id ?: ""
                var newDeliveriesCount = 0
                if (previousId != null) {
                    for (delivery in result.data) {
                        if (delivery.id == previousId) break
                        if (filterType == "all" || delivery.type == filterType) {
                            newDeliveriesCount++
                        }
                    }
                }

                NetworkResult.Success(Pair(newDeliveriesCount, latestId))
            }
            is NetworkResult.Error -> NetworkResult.Error(deliveriesResult.error, deliveriesResult.statusCode)
        }
    }
}
