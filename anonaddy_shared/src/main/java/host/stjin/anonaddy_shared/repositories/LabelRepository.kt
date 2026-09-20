package host.stjin.anonaddy_shared.repositories

import android.content.Context
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

    suspend fun getAllLabels(search: String? = null): NetworkResult<PaginatedResponse<Labels>> {
        waitForInit()

        val parameters = arrayListOf<Pair<String, String>>()
        if (!search.isNullOrEmpty()) parameters.add("filter[search]" to search)

        val response = executeGet(API_URL_LABELS, parameters)

        return handleResponse(response, "getAllLabels") { gson.fromJson(it) }
    }

    suspend fun addNewLabel(newLabelEntry: NewLabelEntry): NetworkResult<Labels> {
        return addNewLabel(newLabelEntry.name, newLabelEntry.colour)
    }

    suspend fun addNewLabel(name: String, colour: String): NetworkResult<Labels> {
        waitForInit()

        val json = JSONObject().apply {
            put("name", name)
            put("colour", colour)
        }

        val response = executePost(API_URL_LABELS, json.toString())

        return handleResponse(response, "addNewLabel") { gson.fromJson(it, SingleLabel::class.java).data }
    }

    suspend fun updateLabel(labelId: String, newLabelEntry: NewLabelEntry): NetworkResult<Labels> {
        return updateLabel(labelId, newLabelEntry.name, newLabelEntry.colour)
    }

    suspend fun updateLabel(labelId: String, name: String, colour: String): NetworkResult<Labels> {
        waitForInit()

        val json = JSONObject().apply {
            put("name", name)
            put("colour", colour)
        }

        val response = executePatch("$API_URL_LABELS/$labelId", json.toString())

        return handleResponse(response, "updateLabel") { gson.fromJson(it, SingleLabel::class.java).data }
    }

    suspend fun deleteLabel(labelId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_LABELS/$labelId")

        return handleStatusResponse(response, "deleteLabel", expectedCode = 204)
    }
}
