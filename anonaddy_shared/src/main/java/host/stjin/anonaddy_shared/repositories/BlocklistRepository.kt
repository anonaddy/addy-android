package host.stjin.anonaddy_shared.repositories

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import host.stjin.anonaddy_shared.AddyIo.API_URL_BLOCKLIST
import host.stjin.anonaddy_shared.models.BlocklistEntries
import host.stjin.anonaddy_shared.models.NewBlocklistEntry
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.SingleBlocklistEntry
import host.stjin.anonaddy_shared.network.BaseNetworkClient
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.DefaultDispatcherProvider
import host.stjin.anonaddy_shared.utils.DispatcherProvider
import host.stjin.anonaddy_shared.utils.fromJson
import org.json.JSONObject

class BlocklistRepository(
    context: Context,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BaseNetworkClient(context, dispatchers) {

    suspend fun getAllBlocklistEntries(
        page: Int? = 1,
        size: Int? = 25,
        filter: String? = null,
        search: String? = null
    ): NetworkResult<PaginatedResponse<BlocklistEntries>> {
        waitForInit()

        val parameters = ArrayList<Pair<String, Any>>()
        if (page != null) parameters.add(Pair("page[number]", page.toString()))
        if (size != null) parameters.add(Pair("page[size]", size.toString()))
        if (filter != null) parameters.add(Pair("filter[type]", filter))
        if (!search.isNullOrEmpty()) parameters.add(Pair("filter[search]", search))

        val (_, response, result) = Fuel.get(API_URL_BLOCKLIST, parameters)
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData: PaginatedResponse<BlocklistEntries> = gson.fromJson(data)
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
                val errorMessage = handleGenericError(response, result, "getAllBlocklistEntries")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun addBlocklistEntry(entry: NewBlocklistEntry): NetworkResult<BlocklistEntries> {
        waitForInit()

        val json = JSONObject().apply {
            put("type", entry.type)
            put("value", entry.value)
        }
        val (_, response, result) = Fuel.post(API_URL_BLOCKLIST)
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            201 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, SingleBlocklistEntry::class.java)
                NetworkResult.Success(addyIoData.data, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "addBlocklistEntry")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun deleteBlocklistEntry(blocklistId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_BLOCKLIST/$blocklistId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            204 -> NetworkResult.Success("204", response.statusCode)
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "deleteBlocklistEntry")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }
}
