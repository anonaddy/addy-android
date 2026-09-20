package host.stjin.anonaddy_shared.repositories

import android.content.Context
import host.stjin.anonaddy_shared.AddyIo.API_URL_ACTIVE_USERNAMES
import host.stjin.anonaddy_shared.AddyIo.API_URL_CAN_LOGIN_USERNAMES
import host.stjin.anonaddy_shared.AddyIo.API_URL_CATCH_ALL_USERNAMES
import host.stjin.anonaddy_shared.AddyIo.API_URL_USERNAMES
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.SingleUsername
import host.stjin.anonaddy_shared.models.Usernames
import host.stjin.anonaddy_shared.network.BaseNetworkClient
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.DefaultDispatcherProvider
import host.stjin.anonaddy_shared.utils.DispatcherProvider
import host.stjin.anonaddy_shared.utils.fromJson
import org.json.JSONObject

class UsernameRepository(
    context: Context,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BaseNetworkClient(context, dispatchers) {

    suspend fun getAllUsernames(): NetworkResult<PaginatedResponse<Usernames>> {
        waitForInit()

        val response = executeGet(API_URL_USERNAMES)

        return handleResponse(response, "getAllUsernames") { gson.fromJson(it) }
    }

    suspend fun getSpecificUsername(usernameId: String): NetworkResult<Usernames> {
        waitForInit()

        val response = executeGet("$API_URL_USERNAMES/$usernameId")

        return handleResponse(response, "getSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun addUsername(username: String): NetworkResult<Usernames> {
        waitForInit()

        val json = JSONObject().put("username", username)
        val response = executePost(API_URL_USERNAMES, json.toString())

        return handleResponse(response, "addUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun deleteUsername(usernameId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_USERNAMES/$usernameId")

        return handleStatusResponse(response, "deleteUsername", expectedCode = 204)
    }

    suspend fun activateSpecificUsername(usernameId: String): NetworkResult<Usernames> {
        waitForInit()

        val response = executePost(API_URL_ACTIVE_USERNAMES, JSONObject().put("id", usernameId).toString())

        return handleResponse(response, "activateSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun deactivateSpecificUsername(usernameId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_ACTIVE_USERNAMES/$usernameId")

        return handleStatusResponse(response, "deactivateSpecificUsername", expectedCode = 204)
    }

    suspend fun enableCatchAllSpecificUsername(usernameId: String): NetworkResult<Usernames> {
        waitForInit()

        val response = executePost(API_URL_CATCH_ALL_USERNAMES, JSONObject().put("id", usernameId).toString())

        return handleResponse(response, "enableCatchAllSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun disableCatchAllSpecificUsername(usernameId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_CATCH_ALL_USERNAMES/$usernameId")

        return handleStatusResponse(response, "disableCatchAllSpecificUsername", expectedCode = 204)
    }

    suspend fun enableCanLoginSpecificUsername(usernameId: String): NetworkResult<Usernames> {
        waitForInit()

        val response = executePost(API_URL_CAN_LOGIN_USERNAMES, JSONObject().put("id", usernameId).toString())

        return handleResponse(response, "enableCanLoginSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun disableCanLoginSpecificUsername(usernameId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_CAN_LOGIN_USERNAMES/$usernameId")

        return handleStatusResponse(response, "disableCanLoginSpecificUsername", expectedCode = 204)
    }

    suspend fun updateDefaultRecipientForSpecificUsername(usernameId: String, recipientId: String?): NetworkResult<Usernames> {
        waitForInit()

        val json = JSONObject().put("default_recipient", recipientId)
        val response = executePatch("$API_URL_USERNAMES/$usernameId/default-recipient", json.toString())

        return handleResponse(response, "updateDefaultRecipientForSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun updateDescriptionSpecificUsername(usernameId: String, description: String?): NetworkResult<Usernames> {
        waitForInit()

        val json = JSONObject().put("description", description)
        val response = executePatch("$API_URL_USERNAMES/$usernameId", json.toString())

        return handleResponse(response, "updateDescriptionSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun updateAutoCreateRegexSpecificUsername(usernameId: String, autoCreateRegex: String?): NetworkResult<Usernames> {
        waitForInit()

        val json = JSONObject().put("auto_create_regex", autoCreateRegex)
        val response = executePatch("$API_URL_USERNAMES/$usernameId", json.toString())

        return handleResponse(response, "updateAutoCreateRegexSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun updateFromNameSpecificUsername(usernameId: String, fromName: String?): NetworkResult<Usernames> {
        waitForInit()

        val json = JSONObject().put("from_name", fromName)
        val response = executePatch("$API_URL_USERNAMES/$usernameId", json.toString())

        return handleResponse(response, "updateFromNameSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }
}
