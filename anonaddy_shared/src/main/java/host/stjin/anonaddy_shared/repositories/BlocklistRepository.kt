package host.stjin.anonaddy_shared.repositories

import android.content.Context
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
        size: Int? = 100,
        filter: String? = null,
        search: String? = null
    ): NetworkResult<PaginatedResponse<BlocklistEntries>> {
        waitForInit()

        val parameters = ArrayList<Pair<String, Any?>>()
        if (page != null) parameters.add(Pair("page[number]", page.toString()))
        if (size != null) parameters.add(Pair("page[size]", size.toString()))
        if (filter != null) parameters.add(Pair("filter[type]", filter))
        if (!search.isNullOrEmpty()) parameters.add(Pair("filter[search]", search))

        val response = executeGet(API_URL_BLOCKLIST, parameters)

        return handleResponse(response, "getAllBlocklistEntries") { gson.fromJson(it) }
    }

    suspend fun addBlocklistEntry(entry: NewBlocklistEntry): NetworkResult<BlocklistEntries> {
        waitForInit()

        val json = JSONObject().apply {
            put("type", entry.type)
            put("value", entry.value)
        }
        val response = executePost(API_URL_BLOCKLIST, json.toString())

        return handleResponse(response, "addBlocklistEntry") { gson.fromJson(it, SingleBlocklistEntry::class.java).data }
    }

    suspend fun deleteBlocklistEntry(blocklistId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_BLOCKLIST/$blocklistId")

        return handleStatusResponse(response, "deleteBlocklistEntry", expectedCode = 204)
    }
}
