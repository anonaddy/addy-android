package host.stjin.anonaddy_shared.repositories

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
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

        val (_, response, result) = Fuel.get(API_URL_USERNAMES)
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleResponse(response, result, "getAllUsernames") { gson.fromJson(it) }
    }

    suspend fun getSpecificUsername(usernameId: String): NetworkResult<Usernames> {
        waitForInit()

        val (_, response, result) = Fuel.get("$API_URL_USERNAMES/$usernameId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleResponse(response, result, "getSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun addUsername(username: String): NetworkResult<Usernames> {
        waitForInit()

        val json = JSONObject().put("username", username)
        val (_, response, result) = Fuel.post(API_URL_USERNAMES)
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "addUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun deleteUsername(usernameId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_USERNAMES/$usernameId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "deleteUsername", expectedCode = 204)
    }

    suspend fun activateSpecificUsername(usernameId: String): NetworkResult<Usernames> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_ACTIVE_USERNAMES)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", usernameId).toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "activateSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun deactivateSpecificUsername(usernameId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_ACTIVE_USERNAMES/$usernameId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "deactivateSpecificUsername", expectedCode = 204)
    }

    suspend fun enableCatchAllSpecificUsername(usernameId: String): NetworkResult<Usernames> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_CATCH_ALL_USERNAMES)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", usernameId).toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "enableCatchAllSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun disableCatchAllSpecificUsername(usernameId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_CATCH_ALL_USERNAMES/$usernameId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "disableCatchAllSpecificUsername", expectedCode = 204)
    }

    suspend fun enableCanLoginSpecificUsername(usernameId: String): NetworkResult<Usernames> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_CAN_LOGIN_USERNAMES)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", usernameId).toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "enableCanLoginSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun disableCanLoginSpecificUsername(usernameId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_CAN_LOGIN_USERNAMES/$usernameId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "disableCanLoginSpecificUsername", expectedCode = 204)
    }

    suspend fun updateDefaultRecipientForSpecificUsername(usernameId: String, recipientId: String?): NetworkResult<Usernames> {
        waitForInit()

        val json = JSONObject().put("default_recipient", recipientId)
        val (_, response, result) = Fuel.patch("$API_URL_USERNAMES/$usernameId/default-recipient")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "updateDefaultRecipientForSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun updateDescriptionSpecificUsername(usernameId: String, description: String?): NetworkResult<Usernames> {
        waitForInit()

        val json = JSONObject().put("description", description)
        val (_, response, result) = Fuel.patch("$API_URL_USERNAMES/$usernameId")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "updateDescriptionSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun updateAutoCreateRegexSpecificUsername(usernameId: String, autoCreateRegex: String?): NetworkResult<Usernames> {
        waitForInit()

        val json = JSONObject().put("auto_create_regex", autoCreateRegex)
        val (_, response, result) = Fuel.patch("$API_URL_USERNAMES/$usernameId")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "updateAutoCreateRegexSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }

    suspend fun updateFromNameSpecificUsername(usernameId: String, fromName: String?): NetworkResult<Usernames> {
        waitForInit()

        val json = JSONObject().put("from_name", fromName)
        val (_, response, result) = Fuel.patch("$API_URL_USERNAMES/$usernameId")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "updateFromNameSpecificUsername") { gson.fromJson(it, SingleUsername::class.java).data }
    }
}
