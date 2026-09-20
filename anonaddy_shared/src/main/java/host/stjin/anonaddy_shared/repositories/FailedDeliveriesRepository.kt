package host.stjin.anonaddy_shared.repositories

import android.content.Context
import host.stjin.anonaddy_shared.AddyIo.API_URL_FAILED_DELIVERIES
import host.stjin.anonaddy_shared.ServiceLocator
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.FailedDeliveries
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.network.BaseNetworkClient
import host.stjin.anonaddy_shared.models.ErrorHelper
import host.stjin.anonaddy_shared.network.NetworkResponse
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.DefaultDispatcherProvider
import host.stjin.anonaddy_shared.utils.DispatcherProvider
import host.stjin.anonaddy_shared.utils.fromJson
import org.json.JSONArray
import org.json.JSONObject

class FailedDeliveriesRepository(
    context: Context,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BaseNetworkClient(context, dispatchers) {

    suspend fun getAllFailedDeliveries(
        page: Int? = 1,
        size: Int? = 25,
        filter: String? = null
    ): NetworkResult<PaginatedResponse<FailedDeliveries>> {
        val parameters = ArrayList<Pair<String, Any?>>()
        if (page != null) parameters.add(Pair("page[number]", page.toString()))
        if (size != null) parameters.add(Pair("page[size]", size.toString()))
        if (filter != null) parameters.add(Pair("filter[email_type]", filter))

        val response = executeGet(API_URL_FAILED_DELIVERIES, parameters)
        return handleResponse(response, "getAllFailedDeliveries") { gson.fromJson(it) }
    }

    suspend fun downloadSpecificFailedDelivery(id: String): NetworkResult<ByteArray> {
        val response = executeGet("${API_URL_FAILED_DELIVERIES}/$id/download")
        return handleByteArrayResponse(response, "downloadSpecificFailedDelivery")
    }

    suspend fun resendFailedDelivery(id: String, recipientIds: List<String>? = null): NetworkResult<Unit> {
        val json = JSONObject().apply {
            if (recipientIds != null) {
                put("recipient_ids", JSONArray(recipientIds))
            }
        }

        val response = executePost("${API_URL_FAILED_DELIVERIES}/$id/resend", json.toString())
        return handleResponse(response, "resendFailedDelivery") { }
    }

    suspend fun deleteFailedDelivery(id: String): NetworkResult<String> {
        val response = executeDelete("${API_URL_FAILED_DELIVERIES}/$id")
        return handleStatusResponse(response, "deleteFailedDelivery", expectedCode = 204)
    }

    suspend fun cacheFailedDeliveryCountForWidgetAndBackgroundService(previousId: String?): NetworkResult<Pair<Int, String?>> {
        val settingsManager = ServiceLocator.getInstance(context).settingsManager
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
