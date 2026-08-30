package host.stjin.anonaddy_shared.repositories

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import host.stjin.anonaddy_shared.AddyIo.API_URL_LABELS
import host.stjin.anonaddy_shared.models.Labels
import host.stjin.anonaddy_shared.models.NewLabelEntry
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.SingleLabel
import host.stjin.anonaddy_shared.network.BaseNetworkClient
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.DefaultDispatcherProvider
import host.stjin.anonaddy_shared.utils.DispatcherProvider
import host.stjin.anonaddy_shared.utils.fromJson
import org.json.JSONObject

class LabelRepository(
    context: Context,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BaseNetworkClient(context, dispatchers) {

    suspend fun getAllLabels(search: String? = null, page: Int? = null, size: Int? = 20): NetworkResult<PaginatedResponse<Labels>> {
        waitForInit()

        val parameters = arrayListOf<Pair<String, String>>()
        if (!search.isNullOrEmpty()) parameters.add("filter[search]" to search)
        if (size != null) parameters.add("page[size]" to size.toString())
        if (page != null) parameters.add("page[number]" to page.toString())

        val (_, response, result) = Fuel.get(API_URL_LABELS, parameters)
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData: PaginatedResponse<Labels> = gson.fromJson(data)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "getAllLabels")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun addNewLabel(newLabelEntry: NewLabelEntry): NetworkResult<Labels> {
        return addNewLabel(newLabelEntry.name, newLabelEntry.colour)
    }

    suspend fun addNewLabel(name: String, color: String?): NetworkResult<Labels> {
        waitForInit()

        val json = JSONObject().apply {
            put("name", name)
            put("color", color)
            put("colour", color)
        }

        val (_, response, result) = Fuel.post(API_URL_LABELS)
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            201 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, SingleLabel::class.java)
                NetworkResult.Success(addyIoData.data, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "addNewLabel")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun updateLabel(labelId: String, newLabelEntry: NewLabelEntry): NetworkResult<Labels> {
        return updateLabel(labelId, newLabelEntry.name, newLabelEntry.colour)
    }

    suspend fun updateLabel(labelId: String, name: String, color: String?): NetworkResult<Labels> {
        waitForInit()

        val json = JSONObject().apply {
            put("name", name)
            put("color", color)
            put("colour", color)
        }

        val (_, response, result) = Fuel.patch("$API_URL_LABELS/$labelId")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, SingleLabel::class.java)
                NetworkResult.Success(addyIoData.data, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "updateLabel")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun deleteLabel(labelId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_LABELS/$labelId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            204 -> NetworkResult.Success("204", response.statusCode)
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "deleteLabel")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }
}
