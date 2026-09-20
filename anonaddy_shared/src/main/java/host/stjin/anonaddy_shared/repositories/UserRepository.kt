package host.stjin.anonaddy_shared.repositories

import android.content.Context
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
import host.stjin.anonaddy_shared.network.NetworkResponse
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.DefaultDispatcherProvider
import host.stjin.anonaddy_shared.utils.DispatcherProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        apiExpiration: String,
        newsletter: Boolean = false
    ): NetworkResult<String> {
        waitForInit()

        val json = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
            put("device_name", "addy.io for Android")
            put("expiration", if (apiExpiration == "never") null else apiExpiration)
            put("newsletter", newsletter)
        }

        return when (val networkResponse = executePost(API_URL_REGISTER, json.toString())) {
            is NetworkResponse.Failure -> {
                val errorMessage = handleGenericError(0, "", "registration", networkResponse.exception)
                NetworkResult.Error(errorMessage, 0, networkResponse.exception)
            }
            is NetworkResponse.Success -> {
                val response = networkResponse.response
                val code = response.code
                val bodyString = try { response.body.string() } catch (e: Exception) { "" }

                when (code) {
                    204 -> NetworkResult.Success("204", code)
                    422 -> {
                        val addyIoData = gson.fromJson(bodyString, Error::class.java)
                        NetworkResult.Error(addyIoData.message, code)
                    }
                    else -> {
                        val errorMessage = handleGenericError(code, bodyString, "registration")
                        NetworkResult.Error(errorMessage, code)
                    }
                }
            }
        }
    }

    suspend fun verifyRegistration(query: String): NetworkResult<String> {
        waitForInit()

        return when (val networkResponse = executePost("${API_URL_LOGIN_VERIFY}?${query}")) {
            is NetworkResponse.Failure -> {
                val errorMessage = handleGenericError(0, "", "verifyRegistration", networkResponse.exception)
                NetworkResult.Error(errorMessage, 0, networkResponse.exception)
            }
            is NetworkResponse.Success -> {
                val response = networkResponse.response
                val code = response.code
                val bodyString = try { response.body.string() } catch (e: Exception) { "" }

                when (code) {
                    200 -> {
                        val addyIoData = gson.fromJson(bodyString, Login::class.java)
                        NetworkResult.Success(addyIoData.api_key, code)
                    }
                    422, 404, 403 -> {
                        val addyIoData = gson.fromJson(bodyString, Error::class.java)
                        NetworkResult.Error(addyIoData.message, code)
                    }
                    else -> {
                        val errorMessage = handleGenericError(code, bodyString, "verifyRegistration")
                        NetworkResult.Error(errorMessage, code)
                    }
                }
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

        val cookieHeader = cookies.joinToString("; ") { it.substringBefore(";") }
        val customHeaders = arrayOf<Pair<String, Any>>(
            "Content-Type" to "application/json",
            "X-Requested-With" to "XMLHttpRequest",
            "Accept" to "application/json",
            "User-Agent" to userAgent,
            "Cookie" to cookieHeader
        )

        return when (val networkResponse = executePost(API_URL_LOGIN_MFA, json.toString(), customHeaders)) {
            is NetworkResponse.Failure -> {
                val errorMessage = handleGenericError(0, "", "loginMfa", networkResponse.exception)
                NetworkResult.Error(errorMessage, 0, networkResponse.exception)
            }
            is NetworkResponse.Success -> {
                val response = networkResponse.response
                val code = response.code
                val bodyString = try { response.body.string() } catch (e: Exception) { "" }

                when (code) {
                    200 -> {
                        val addyIoData = gson.fromJson(bodyString, Login::class.java)
                        NetworkResult.Success(addyIoData, code)
                    }
                    401 -> {
                        val addyIoData = gson.fromJson(bodyString, Error::class.java)
                        NetworkResult.Error(addyIoData.message, code)
                    }
                    else -> {
                        val errorMessage = handleGenericError(code, bodyString, "loginMfa")
                        NetworkResult.Error(errorMessage, code)
                    }
                }
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

        return when (val networkResponse = executePost(API_URL_LOGIN, json.toString())) {
            is NetworkResponse.Failure -> {
                val errorMessage = handleGenericError(0, "", "login", networkResponse.exception)
                LoginResult.Error(errorMessage, 0)
            }
            is NetworkResponse.Success -> {
                val response = networkResponse.response
                val code = response.code
                val bodyString = try { response.body.string() } catch (e: Exception) { "" }

                when (code) {
                    200 -> {
                        val addyIoData = gson.fromJson(bodyString, Login::class.java)
                        LoginResult.Success(addyIoData, code)
                    }
                    422 -> {
                        val addyIoData = gson.fromJson(bodyString, LoginMfaRequired::class.java)
                        addyIoData.cookie = response.headers("Set-Cookie")
                        LoginResult.MfaRequired(addyIoData, code)
                    }
                    401, 403 -> {
                        val addyIoData = gson.fromJson(bodyString, Error::class.java)
                        LoginResult.Error(addyIoData.message, code)
                    }
                    else -> {
                        val errorMessage = handleGenericError(code, bodyString, "login")
                        LoginResult.Error(errorMessage, code)
                    }
                }
            }
        }
    }

    suspend fun logout(): NetworkResult<Unit> {
        waitForInit()
        val response = executePost(API_URL_LOGOUT)
        return handleResponse(response, "logout") { }
    }

    suspend fun deleteAccount(password: String): NetworkResult<Unit> {
        waitForInit()

        val json = JSONObject().apply {
            put("password", password)
        }

        val response = executePost(API_URL_DELETE_ACCOUNT, json.toString())
        return handleResponse(response, "deleteAccount") { }
    }

    suspend fun verifyApiKey(baseUrl: String, apiKey: String): NetworkResult<UserResource> {
        waitForInit()

        lazyMgr.reset()
        API_BASE_URL = baseUrl

        val response = executeGet(API_URL_ACCOUNT_DETAILS, headers = getHeaders(apiKey))

        return handleResponse(response, "verifyApiKey") { data ->
            gson.fromJson(data, SingleUserResource::class.java).data
        }
    }

    companion object {
        private const val USER_RESOURCE_CACHE_TTL_MS = 60_000L
        private const val API_TOKEN_DETAILS_CACHE_TTL_MS = 60_000L
    }

    private val userResourceMutex = Mutex()
    private var cachedUserResource: UserResource? = null
    private var lastUserResourceFetchTime: Long = 0L

    private val apiTokenDetailsMutex = Mutex()
    private var cachedApiTokenDetails: ApiTokenDetails? = null
    private var lastApiTokenDetailsFetchTime: Long = 0L

    suspend fun getUserResource(forceRefresh: Boolean = false): NetworkResult<UserResource> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedUserResource != null && (now - lastUserResourceFetchTime < USER_RESOURCE_CACHE_TTL_MS)) {
            return NetworkResult.Success(cachedUserResource!!)
        }

        return userResourceMutex.withLock {
            val lockNow = System.currentTimeMillis()
            if (!forceRefresh && cachedUserResource != null && (lockNow - lastUserResourceFetchTime < USER_RESOURCE_CACHE_TTL_MS)) {
                return@withLock NetworkResult.Success(cachedUserResource!!)
            }

            waitForInit()

            val response = executeGet(API_URL_ACCOUNT_DETAILS)

            val networkResult = handleResponse(response, "getUserResource") { data ->
                gson.fromJson(data, SingleUserResource::class.java).data
            }

            if (networkResult is NetworkResult.Success) {
                cachedUserResource = networkResult.data
                lastUserResourceFetchTime = System.currentTimeMillis()
                (context as? host.stjin.anonaddy_shared.AddyIoApp)?.userResource = networkResult.data
            }

            networkResult
        }
    }

    suspend fun getApiTokenDetails(forceRefresh: Boolean = false): NetworkResult<ApiTokenDetails> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedApiTokenDetails != null && (now - lastApiTokenDetailsFetchTime < API_TOKEN_DETAILS_CACHE_TTL_MS)) {
            return NetworkResult.Success(cachedApiTokenDetails!!)
        }

        return apiTokenDetailsMutex.withLock {
            val lockNow = System.currentTimeMillis()
            if (!forceRefresh && cachedApiTokenDetails != null && (lockNow - lastApiTokenDetailsFetchTime < API_TOKEN_DETAILS_CACHE_TTL_MS)) {
                return@withLock NetworkResult.Success(cachedApiTokenDetails!!)
            }

            waitForInit()

            val response = executeGet(API_URL_API_TOKEN_DETAILS)

            val networkResult = handleResponse(response, "getApiTokenDetails") { data ->
                gson.fromJson(data, ApiTokenDetails::class.java)
            }

            if (networkResult is NetworkResult.Success) {
                cachedApiTokenDetails = networkResult.data
                lastApiTokenDetailsFetchTime = System.currentTimeMillis()
            }

            networkResult
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

        val response = executePost(API_URL_NOTIFY_SUBSCRIPTION, json.toString())

        return handleResponse(response, "notifyServerForSubscriptionChange") { data ->
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
