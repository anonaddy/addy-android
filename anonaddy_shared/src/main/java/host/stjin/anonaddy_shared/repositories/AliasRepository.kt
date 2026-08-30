package host.stjin.anonaddy_shared.repositories

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
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

        val array = JSONArray(recipients ?: emptyList<String>())
        val labelsArray = JSONArray(labels ?: emptyList<String>())

        val json = JSONObject().apply {
            put("domain", domain)
            put("description", description)
            put("format", format)
            put("local_part", aliasLocalPart)
            put("recipient_ids", array)
            put("label_ids", labelsArray)
        }

        val (_, response, result) = Fuel.post(API_URL_ALIAS)
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            201 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                NetworkResult.Success(addyIoData.data, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "addAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
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
        } else if (aliasSortFilter.onlyInactiveAliases) {
            parameters.add("filter[active]" to "false")
        } else if (aliasSortFilter.onlyDeletedAliases) {
            parameters.add("filter[deleted]" to "only")
        } else if (aliasSortFilter.onlyPinnedAliases) {
            parameters.add("filter[pinned]" to "true")
        } else {
            parameters.add("filter[deleted]" to "with")
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

        val (_, response, result) = Fuel.get(API_URL_ALIAS, parameters)
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData: PaginatedResponse<Aliases> = gson.fromJson(data)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "getAliases")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun getSpecificAlias(aliasId: String): NetworkResult<Aliases> {
        waitForInit()

        val (_, response, result) = Fuel.get("$API_URL_ALIAS/$aliasId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                NetworkResult.Success(addyIoData.data, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "getSpecificAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun updateDescriptionSpecificAlias(aliasId: String, description: String?): NetworkResult<Aliases> {
        waitForInit()

        val json = JSONObject().apply {
            put("description", description)
        }

        val (_, response, result) = Fuel.patch("$API_URL_ALIAS/$aliasId")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                NetworkResult.Success(addyIoData.data, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "updateDescriptionSpecificAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun updateFromNameSpecificAlias(aliasId: String, fromName: String?): NetworkResult<Aliases> {
        waitForInit()

        val json = JSONObject().apply {
            put("from_name", fromName)
        }

        val (_, response, result) = Fuel.patch("$API_URL_ALIAS/$aliasId")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                NetworkResult.Success(addyIoData.data, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "updateFromNameSpecificAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun updateRecipientsSpecificAlias(aliasId: String, recipientIds: List<String>): NetworkResult<Aliases> {
        waitForInit()

        val array = JSONArray(recipientIds)
        val json = JSONObject().apply {
            put("alias_id", aliasId)
            put("recipient_ids", array)
        }

        val (_, response, result) = Fuel.post(API_URL_ALIAS_RECIPIENTS)
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                NetworkResult.Success(addyIoData.data, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "updateRecipientsSpecificAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun bulkGetAlias(aliasIds: List<String>): NetworkResult<PaginatedResponse<Aliases>> {
        waitForInit()

        val array = JSONArray(aliasIds)
        val json = JSONObject().apply {
            put("ids", array)
        }

        val (_, response, result) = Fuel.post("$API_URL_ALIAS/get/bulk")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData: PaginatedResponse<Aliases> = gson.fromJson(data)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "bulkGetAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun activateSpecificAlias(aliasId: String): NetworkResult<Aliases> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_ACTIVE_ALIAS)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", aliasId).toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                NetworkResult.Success(addyIoData.data, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "activateSpecificAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun deactivateSpecificAlias(aliasId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_ACTIVE_ALIAS/$aliasId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            204 -> NetworkResult.Success("204", response.statusCode)
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "deactivateSpecificAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun pinSpecificAlias(aliasId: String): NetworkResult<Aliases> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_PINNED_ALIASES)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", aliasId).toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                NetworkResult.Success(addyIoData.data, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "pinSpecificAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun unpinSpecificAlias(aliasId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_PINNED_ALIASES/$aliasId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            204 -> NetworkResult.Success("204", response.statusCode)
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "unpinSpecificAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun deleteAlias(aliasId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_ALIAS/$aliasId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            204 -> NetworkResult.Success("204", response.statusCode)
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "deleteAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun restoreAlias(aliasId: String): NetworkResult<Aliases> {
        waitForInit()

        val (_, response, result) = Fuel.patch("$API_URL_ALIAS/$aliasId/restore")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                NetworkResult.Success(addyIoData.data, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "restoreAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun forgetAlias(aliasId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_ALIAS/$aliasId/forget")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            204 -> NetworkResult.Success("204", response.statusCode)
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "forgetAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun bulkDeleteAlias(aliasIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().put("ids", JSONArray(aliasIds))
        val (_, response, result) = Fuel.post("$API_URL_ALIAS/delete/bulk")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, BulkActionResponse::class.java)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "bulkDeleteAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun bulkRestoreAlias(aliasIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().put("ids", JSONArray(aliasIds))
        val (_, response, result) = Fuel.post("$API_URL_ALIAS/restore/bulk")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, BulkActionResponse::class.java)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "bulkRestoreAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun bulkForgetAlias(aliasIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().put("ids", JSONArray(aliasIds))
        val (_, response, result) = Fuel.post("$API_URL_ALIAS/forget/bulk")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, BulkActionResponse::class.java)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "bulkForgetAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun bulkActivateAlias(aliasIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().put("ids", JSONArray(aliasIds))
        val (_, response, result) = Fuel.post("$API_URL_ALIAS/activate/bulk")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, BulkActionResponse::class.java)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "bulkActivateAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun bulkDeactivateAlias(aliasIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().put("ids", JSONArray(aliasIds))
        val (_, response, result) = Fuel.post("$API_URL_ALIAS/deactivate/bulk")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, BulkActionResponse::class.java)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "bulkDeactivateAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun bulkPinAlias(aliasIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().put("ids", JSONArray(aliasIds))
        val (_, response, result) = Fuel.post("$API_URL_ALIAS/pin/bulk")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, BulkActionResponse::class.java)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "bulkPinAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun bulkUnpinAlias(aliasIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().put("ids", JSONArray(aliasIds))
        val (_, response, result) = Fuel.post("$API_URL_ALIAS/unpin/bulk")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, BulkActionResponse::class.java)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "bulkUnpinAlias")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun bulkUpdateAliasesLabels(aliasIds: List<String>, labelIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().apply {
            put("ids", JSONArray(aliasIds))
            put("label_ids", JSONArray(labelIds))
        }

        val (_, response, result) = Fuel.post("$API_URL_ALIAS/labels/bulk")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, BulkActionResponse::class.java)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "bulkUpdateAliasesLabels")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun bulkUpdateAliasesRecipients(aliasIds: List<String>, recipientIds: List<String>): NetworkResult<BulkActionResponse> {
        waitForInit()

        val json = JSONObject().apply {
            put("ids", JSONArray(aliasIds))
            put("recipient_ids", JSONArray(recipientIds))
        }

        val (_, response, result) = Fuel.post("$API_URL_ALIAS/recipients/bulk")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, BulkActionResponse::class.java)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "bulkUpdateAliasesRecipients")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun activateAttachedRecipientsOnly(aliasId: String): NetworkResult<Aliases> {
        waitForInit()

        val json = JSONObject().put("id", aliasId)
        val (_, response, result) = Fuel.post(API_URL_ATTACHED_RECIPIENTS_ONLY)
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                NetworkResult.Success(addyIoData.data, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "activateAttachedRecipientsOnly")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun deactivateAttachedRecipientsOnly(aliasId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_ATTACHED_RECIPIENTS_ONLY/$aliasId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            204 -> NetworkResult.Success("204", response.statusCode)
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "deactivateAttachedRecipientsOnly")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
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
