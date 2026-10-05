package host.stjin.anonaddy_shared.repositories

import android.content.Context
import host.stjin.anonaddy_shared.AddyIo.API_URL_ACTIVE_ALIAS
import host.stjin.anonaddy_shared.AddyIo.API_URL_ALIAS
import host.stjin.anonaddy_shared.AddyIo.API_URL_ALIAS_RECIPIENTS
import host.stjin.anonaddy_shared.AddyIo.API_URL_ATTACHED_RECIPIENTS_ONLY
import host.stjin.anonaddy_shared.AddyIo.API_URL_PINNED_ALIASES
import host.stjin.anonaddy_shared.managers.SettingsManager.PREFS
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.BulkActionResponse
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.SingleAlias
import host.stjin.anonaddy_shared.network.BaseNetworkClient
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.DefaultDispatcherProvider
import host.stjin.anonaddy_shared.utils.DispatcherProvider
import host.stjin.anonaddy_shared.utils.fromJson
import org.json.JSONArray
import org.json.JSONObject

class AliasRepository(
    context: Context,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BaseNetworkClient(context, dispatchers) {

    suspend fun addAlias(
        domain: String,
        description: String,
        format: String,
        aliasLocalPart: String,
        recipients: ArrayList<String>?,
        labels: ArrayList<String>?
    ): NetworkResult<Aliases> {
        waitForInit()

        val json = JSONObject().apply {
            put("domain", domain)
            if (description.isNotEmpty()) put("description", description)
            if (format.isNotEmpty()) put("format", format)
            if (aliasLocalPart.isNotEmpty()) put("local_part", aliasLocalPart)
            if (!recipients.isNullOrEmpty()) put("recipient_ids", JSONArray(recipients))
            if (!labels.isNullOrEmpty()) put("label_ids", JSONArray(labels))
        }

        val response = executePost(API_URL_ALIAS, json.toString())

        return handleResponse(response, "addAlias") { gson.fromJson(it, SingleAlias::class.java).data }
    }

    suspend fun getAliases(
        aliasSortFilter: AliasSortFilter,
        page: Int? = null,
        size: Int? = 20,
        recipient: String? = null,
        domain: String? = null,
        username: String? = null,
    ): NetworkResult<PaginatedResponse<Aliases>> {
        waitForInit()

        val parameters = arrayListOf<Pair<String, String>>()

        if (aliasSortFilter.onlyActiveAliases) {
            parameters.add("filter[active]" to "true")
        }
        if (aliasSortFilter.onlyInactiveAliases) {
            parameters.add("filter[active]" to "false")
        }
        if (aliasSortFilter.onlyDeletedAliases) {
            parameters.add("filter[deleted]" to "only")
        }
        if (aliasSortFilter.onlyPinnedAliases) {
            parameters.add("filter[pinned]" to "true")
        }

        if (size != null) {
            parameters.add("page[size]" to size.toString())
        }
        if (!aliasSortFilter.filter.isNullOrEmpty()) {
            parameters.add("filter[search]" to aliasSortFilter.filter.toString())
        }
        if (page != null) {
            parameters.add("page[number]" to page.toString())
        }
        if (!aliasSortFilter.sort.isNullOrEmpty()) {
            val sortFilter: String = if (aliasSortFilter.sortDesc) "-${aliasSortFilter.sort}" else aliasSortFilter.sort.toString()
            parameters.add("sort" to sortFilter)
        }
        if (!recipient.isNullOrEmpty()) {
            parameters.add("recipient" to recipient)
        }
        if (!domain.isNullOrEmpty()) {
            parameters.add("domain" to domain)
        }
        if (!username.isNullOrEmpty()) {
            parameters.add("username" to username)
        }
        if (!aliasSortFilter.label.isNullOrEmpty()) {
            parameters.add("filter[label]" to aliasSortFilter.label.toString())
        }

        // Always include labels
        parameters.add("with" to "labels")

        val response = executeGet(API_URL_ALIAS, parameters)

        return handleResponse(response, "getAliases") { gson.fromJson(it) }
    }

    suspend fun getSpecificAlias(aliasId: String): NetworkResult<Aliases> {
        waitForInit()

        val response = executeGet("$API_URL_ALIAS/$aliasId")

        return handleResponse(response, "getSpecificAlias") { gson.fromJson(it, SingleAlias::class.java).data }
    }

    suspend fun updateDescriptionSpecificAlias(aliasId: String, description: String?): NetworkResult<Aliases> {
        waitForInit()

        val json = JSONObject().apply {
            put("description", description)
        }

        val response = executePatch("$API_URL_ALIAS/$aliasId", json.toString())

        return handleResponse(response, "updateDescriptionSpecificAlias") { gson.fromJson(it, SingleAlias::class.java).data }
    }

    suspend fun updateFromNameSpecificAlias(aliasId: String, fromName: String?): NetworkResult<Aliases> {
        waitForInit()

        val json = JSONObject().apply {
            put("from_name", fromName)
        }

        val response = executePatch("$API_URL_ALIAS/$aliasId", json.toString())

        return handleResponse(response, "updateFromNameSpecificAlias") { gson.fromJson(it, SingleAlias::class.java).data }
    }

    suspend fun updateRecipientsSpecificAlias(aliasId: String, recipientIds: List<String>): NetworkResult<Aliases> {
        waitForInit()

        val array = JSONArray(recipientIds)
        val json = JSONObject().apply {
            put("alias_id", aliasId)
            put("recipient_ids", array)
        }

        val response = executePost(API_URL_ALIAS_RECIPIENTS, json.toString())

        return handleResponse(response, "updateRecipientsSpecificAlias") { gson.fromJson(it, SingleAlias::class.java).data }
    }

    suspend fun bulkGetAlias(aliasIds: List<String>): NetworkResult<PaginatedResponse<Aliases>> {
        waitForInit()

        val array = JSONArray(aliasIds)
        val json = JSONObject().apply {
            put("ids", array)
        }

        val response = executePost("$API_URL_ALIAS/get/bulk", json.toString())

        return handleResponse(response, "bulkGetAlias") { gson.fromJson(it) }
    }

    suspend fun activateSpecificAlias(aliasId: String): NetworkResult<Aliases> {
        waitForInit()

        val response = executePost(API_URL_ACTIVE_ALIAS, JSONObject().put("id", aliasId).toString())

        return handleResponse(response, "activateSpecificAlias") { gson.fromJson(it, SingleAlias::class.java).data }
    }

    suspend fun deactivateSpecificAlias(aliasId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_ACTIVE_ALIAS/$aliasId")

        return handleStatusResponse(response, "deactivateSpecificAlias", expectedCode = 204)
    }

    suspend fun pinSpecificAlias(aliasId: String): NetworkResult<Aliases> {
        waitForInit()

        val response = executePost(API_URL_PINNED_ALIASES, JSONObject().put("id", aliasId).toString())

        return handleResponse(response, "pinSpecificAlias") { gson.fromJson(it, SingleAlias::class.java).data }
    }

    suspend fun unpinSpecificAlias(aliasId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_PINNED_ALIASES/$aliasId")

        return handleStatusResponse(response, "unpinSpecificAlias", expectedCode = 204)
    }

    suspend fun deleteAlias(aliasId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_ALIAS/$aliasId")

        return handleStatusResponse(response, "deleteAlias", expectedCode = 204)
    }

    suspend fun restoreAlias(aliasId: String): NetworkResult<Aliases> {
        waitForInit()

        val response = executePatch("$API_URL_ALIAS/$aliasId/restore")

        return handleResponse(response, "restoreAlias") { gson.fromJson(it, SingleAlias::class.java).data }
    }

    suspend fun forgetAlias(aliasId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_ALIAS/$aliasId/forget")

        return handleStatusResponse(response, "forgetAlias", expectedCode = 204)
    }

    suspend fun bulkDeleteAlias(aliasIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().put("ids", JSONArray(aliasIds))
        val response = executePost("$API_URL_ALIAS/delete/bulk", json.toString())

        return handleResponse(response, "bulkDeleteAlias") { gson.fromJson(it, BulkActionResponse::class.java) }
    }

    suspend fun bulkRestoreAlias(aliasIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().put("ids", JSONArray(aliasIds))
        val response = executePost("$API_URL_ALIAS/restore/bulk", json.toString())

        return handleResponse(response, "bulkRestoreAlias") { gson.fromJson(it, BulkActionResponse::class.java) }
    }

    suspend fun bulkForgetAlias(aliasIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().put("ids", JSONArray(aliasIds))
        val response = executePost("$API_URL_ALIAS/forget/bulk", json.toString())

        return handleResponse(response, "bulkForgetAlias") { gson.fromJson(it, BulkActionResponse::class.java) }
    }

    suspend fun bulkActivateAlias(aliasIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().put("ids", JSONArray(aliasIds))
        val response = executePost("$API_URL_ALIAS/activate/bulk", json.toString())

        return handleResponse(response, "bulkActivateAlias") { gson.fromJson(it, BulkActionResponse::class.java) }
    }

    suspend fun bulkDeactivateAlias(aliasIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().put("ids", JSONArray(aliasIds))
        val response = executePost("$API_URL_ALIAS/deactivate/bulk", json.toString())

        return handleResponse(response, "bulkDeactivateAlias") { gson.fromJson(it, BulkActionResponse::class.java) }
    }

    suspend fun bulkPinAlias(aliasIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().put("ids", JSONArray(aliasIds))
        val response = executePost("$API_URL_ALIAS/pin/bulk", json.toString())

        return handleResponse(response, "bulkPinAlias") { gson.fromJson(it, BulkActionResponse::class.java) }
    }

    suspend fun bulkUnpinAlias(aliasIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().put("ids", JSONArray(aliasIds))
        val response = executePost("$API_URL_ALIAS/unpin/bulk", json.toString())

        return handleResponse(response, "bulkUnpinAlias") { gson.fromJson(it, BulkActionResponse::class.java) }
    }

    suspend fun bulkUpdateAliasesLabels(aliasIds: List<String>, labelIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().apply {
            put("ids", JSONArray(aliasIds))
            put("label_ids", JSONArray(labelIds))
        }

        val response = executePost("$API_URL_ALIAS/labels/bulk", json.toString())

        return handleResponse(response, "bulkUpdateAliasesLabels") { gson.fromJson(it, BulkActionResponse::class.java) }
    }

    suspend fun bulkUpdateAliasesRecipients(aliasIds: List<String>, recipientIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().apply {
            put("ids", JSONArray(aliasIds))
            put("recipient_ids", JSONArray(recipientIds))
        }

        val response = executePost("$API_URL_ALIAS/recipients/bulk", json.toString())

        return handleResponse(response, "bulkUpdateAliasesRecipients") { gson.fromJson(it, BulkActionResponse::class.java) }
    }

    suspend fun activateAttachedRecipientsOnly(aliasId: String): NetworkResult<Aliases> {
        waitForInit()

        val json = JSONObject().put("id", aliasId)
        val response = executePost(API_URL_ATTACHED_RECIPIENTS_ONLY, json.toString())

        return handleResponse(response, "activateAttachedRecipientsOnly") { gson.fromJson(it, SingleAlias::class.java).data }
    }

    suspend fun deactivateAttachedRecipientsOnly(aliasId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_ATTACHED_RECIPIENTS_ONLY/$aliasId")

        return handleStatusResponse(response, "deactivateAttachedRecipientsOnly", expectedCode = 204)
    }

    suspend fun cacheMostPopularAliasesDataForWidget(amountOfAliasesToCache: Int? = 15): NetworkResult<Boolean> {
        val filter = AliasSortFilter(
            onlyActiveAliases = true,
            onlyDeletedAliases = false,
            onlyInactiveAliases = false,
            onlyWatchedAliases = false,
            onlyPinnedAliases = false,
            sort = "emails_forwarded",
            sortDesc = true,
            filter = null
        )
        return when (val aliasesResult = getAliases(filter, size = amountOfAliasesToCache)) {
            is NetworkResult.Success -> {
                val data = gson.toJson(aliasesResult.data.data)
                encryptedSettingsManager.putSettingsString(PREFS.BACKGROUND_SERVICE_CACHE_MOST_ACTIVE_ALIASES_DATA, data)
                NetworkResult.Success(true)
            }
            is NetworkResult.Error -> NetworkResult.Error(aliasesResult.error, aliasesResult.statusCode)
        }
    }

    suspend fun cacheLastUpdatedAliasesData(amountOfAliasesToCache: Int? = 15): NetworkResult<Boolean> {
        val filter = AliasSortFilter(
            onlyActiveAliases = false,
            onlyDeletedAliases = false,
            onlyInactiveAliases = false,
            onlyWatchedAliases = false,
            onlyPinnedAliases = false,
            sort = "updated_at",
            sortDesc = true,
            filter = null
        )
        return when (val aliasesResult = getAliases(filter, size = amountOfAliasesToCache)) {
            is NetworkResult.Success -> {
                val data = gson.toJson(aliasesResult.data.data)
                encryptedSettingsManager.putSettingsString(PREFS.BACKGROUND_SERVICE_CACHE_LAST_UPDATED_ALIASES_DATA, data)
                NetworkResult.Success(true)
            }
            is NetworkResult.Error -> NetworkResult.Error(aliasesResult.error, aliasesResult.statusCode)
        }
    }

    suspend fun cachePinnedAliasesData(): NetworkResult<Boolean> {
        val filter = AliasSortFilter(
            onlyActiveAliases = false,
            onlyDeletedAliases = false,
            onlyInactiveAliases = false,
            onlyWatchedAliases = false,
            onlyPinnedAliases = true,
            sort = "updated_at",
            sortDesc = true,
            filter = null
        )
        return when (val aliasesResult = getAliases(filter)) {
            is NetworkResult.Success -> {
                val data = gson.toJson(aliasesResult.data.data)
                encryptedSettingsManager.putSettingsString(PREFS.BACKGROUND_SERVICE_CACHE_PINNED_ALIASES_DATA, data)
                NetworkResult.Success(true)
            }
            is NetworkResult.Error -> NetworkResult.Error(aliasesResult.error, aliasesResult.statusCode)
        }
    }
}
