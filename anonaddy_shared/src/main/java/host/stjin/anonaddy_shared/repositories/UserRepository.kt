package host.stjin.anonaddy_shared.repositories

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import host.stjin.anonaddy_shared.AddyIo.API_BASE_URL
import host.stjin.anonaddy_shared.AddyIo.API_URL_ACCOUNT_DETAILS
import host.stjin.anonaddy_shared.AddyIo.API_URL_API_TOKEN_DETAILS
import host.stjin.anonaddy_shared.AddyIo.API_URL_DELETE_ACCOUNT
import host.stjin.anonaddy_shared.AddyIo.API_URL_LOGIN
import host.stjin.anonaddy_shared.AddyIo.API_URL_LOGIN_MFA
import host.stjin.anonaddy_shared.AddyIo.API_URL_LOGIN_VERIFY
import host.stjin.anonaddy_shared.AddyIo.API_URL_LOGOUT
import host.stjin.anonaddy_shared.AddyIo.API_URL_NOTIFY_SUBSCRIPTION
import host.stjin.anonaddy_shared.AddyIo.API_URL_REGISTER
import host.stjin.anonaddy_shared.AddyIo.lazyMgr
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.ApiTokenDetails
import host.stjin.anonaddy_shared.models.Error
import host.stjin.anonaddy_shared.models.Login
import host.stjin.anonaddy_shared.models.LoginMfaRequired
import host.stjin.anonaddy_shared.models.SingleUserResource
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.network.BaseNetworkClient
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.DefaultDispatcherProvider
import host.stjin.anonaddy_shared.utils.DispatcherProvider
import org.json.JSONObject

sealed class LoginResult {
    data class Success(val login: Login, val statusCode: Int = 200) : LoginResult()
    data class MfaRequired(val mfa: LoginMfaRequired, val statusCode: Int = 422) : LoginResult()
    data class Error(val error: String?, val statusCode: Int = 0) : LoginResult()
}

class UserRepository(
    context: Context,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BaseNetworkClient(context, dispatchers) {

    suspend fun registration(
        username: String,
        email: String,
        password: String,
        apiExpiration: String
    ): NetworkResult<String> {
        waitForInit()

        val json = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
            put("device_name", "addy.io for Android")
            put("expiration", if (apiExpiration == "never") null else apiExpiration)
        }

        val (_, response, result) = Fuel.post(API_URL_REGISTER)
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            204 -> NetworkResult.Success("204", response.statusCode)
            422 -> {
                val data = response.data.toString(Charsets.UTF_8)
                val addyIoData = gson.fromJson(data, Error::class.java)
                NetworkResult.Error(addyIoData.message, response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "registration")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun verifyRegistration(query: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.post("${API_URL_LOGIN_VERIFY}?${query}")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, Login::class.java)
                NetworkResult.Success(addyIoData.api_key, response.statusCode)
            }
            422, 404, 403 -> {
                val data = response.data.toString(Charsets.UTF_8)
                val addyIoData = gson.fromJson(data, Error::class.java)
                NetworkResult.Error(addyIoData.message, response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "verifyRegistration")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun loginMfa(
        baseUrl: String,
        mfaKey: String,
        otp: String,
        apiExpiration: String,
        cookies: Collection<String>
    ): NetworkResult<Login> {
        waitForInit()

        lazyMgr.reset()
        API_BASE_URL = baseUrl

        val json = JSONObject().apply {
            put("mfa_key", mfaKey)
            put("otp", otp)
            put("device_name", "addy.io for Android")
            put("expiration", if (apiExpiration == "never") null else apiExpiration)
        }

        val (_, response, result) = Fuel.post(API_URL_LOGIN_MFA)
            .header(Headers.COOKIE to cookies)
            .appendHeader(
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
            )
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, Login::class.java)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                val data = response.data.toString(Charsets.UTF_8)
                val addyIoData = gson.fromJson(data, Error::class.java)
                NetworkResult.Error(addyIoData.message, response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "loginMfa")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun login(
        baseUrl: String,
        username: String,
        password: String,
        apiExpiration: String
    ): LoginResult {
        waitForInit()

        lazyMgr.reset()
        API_BASE_URL = baseUrl

        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
            put("device_name", "addy.io for Android")
            put("expiration", if (apiExpiration == "never") null else apiExpiration)
        }

        val (_, response, result) = Fuel.post(API_URL_LOGIN)
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, Login::class.java)
                LoginResult.Success(addyIoData, response.statusCode)
            }
            422 -> {
                val data = response.data.toString(Charsets.UTF_8)
                val addyIoData = gson.fromJson(data, LoginMfaRequired::class.java)
                addyIoData.cookie = response.headers["Set-Cookie"]
                LoginResult.MfaRequired(addyIoData, response.statusCode)
            }
            401, 403 -> {
                val data = response.data.toString(Charsets.UTF_8)
                val addyIoData = gson.fromJson(data, Error::class.java)
                LoginResult.Error(addyIoData.message, response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "login")
                LoginResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun logout(): NetworkResult<Unit> {
        waitForInit()
        val (_, response, result) = Fuel.post(API_URL_LOGOUT)
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleResponse(response, result, "logout") { }
    }

    suspend fun deleteAccount(password: String): NetworkResult<Unit> {
        waitForInit()

        val json = JSONObject().apply {
            put("password", password)
        }

        val (_, response, result) = Fuel.post(API_URL_DELETE_ACCOUNT)
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "deleteAccount") { }
    }

    suspend fun verifyApiKey(baseUrl: String, apiKey: String): NetworkResult<UserResource> {
        waitForInit()

        lazyMgr.reset()
        API_BASE_URL = baseUrl

        val (_, response, result) = Fuel.get(API_URL_ACCOUNT_DETAILS)
            .appendHeader(*getHeaders(apiKey))
            .awaitStringResponseResult()

        return handleResponse(response, result, "verifyApiKey") { data ->
            gson.fromJson(data, SingleUserResource::class.java).data
        }
    }

    suspend fun getUserResource(): NetworkResult<UserResource> {
        waitForInit()

        val (_, response, result) = Fuel.get(API_URL_ACCOUNT_DETAILS)
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleResponse(response, result, "getUserResource") { data ->
            gson.fromJson(data, SingleUserResource::class.java).data
        }
    }

    suspend fun getApiTokenDetails(): NetworkResult<ApiTokenDetails> {
        waitForInit()

        val (_, response, result) = Fuel.get(API_URL_API_TOKEN_DETAILS)
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleResponse(response, result, "getApiTokenDetails") { data ->
            gson.fromJson(data, ApiTokenDetails::class.java)
        }
    }

    suspend fun notifyServerForSubscriptionChange(
        purchaseToken: String,
        subscriptionId: String
    ): NetworkResult<UserResource> {
        waitForInit()

        val json = JSONObject().apply {
            put("purchaseToken", purchaseToken)
            put("subscriptionId", subscriptionId)
        }

        val (_, response, result) = Fuel.post(API_URL_NOTIFY_SUBSCRIPTION)
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "notifyServerForSubscriptionChange") { data ->
            gson.fromJson(data, SingleUserResource::class.java).data
        }
    }

    suspend fun cacheUserResourceForWidget(): NetworkResult<Boolean> {
        return when (val userResourceResult = getUserResource()) {
            is NetworkResult.Success -> {
                val data = gson.toJson(userResourceResult.data)
                encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_USER_RESOURCE, data)
                NetworkResult.Success(true)
            }
            is NetworkResult.Error -> NetworkResult.Error(userResourceResult.error, userResourceResult.statusCode)
        }
    }
}
