package host.stjin.anonaddy_shared.repositories

import android.content.Context
import com.einmalfel.earl.EarlParser
import com.einmalfel.earl.Feed
import host.stjin.anonaddy_shared.AddyIo.API_URL_ACCOUNT_NOTIFICATIONS
import host.stjin.anonaddy_shared.AddyIo.API_URL_APP_VERSION
import host.stjin.anonaddy_shared.AddyIo.GITHUB_TAGS_RSS_FEED
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.AccountNotifications
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.ServiceStatus
import host.stjin.anonaddy_shared.models.Version
import host.stjin.anonaddy_shared.network.BaseNetworkClient
import host.stjin.anonaddy_shared.network.NetworkResponse
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.DefaultDispatcherProvider
import host.stjin.anonaddy_shared.utils.DispatcherProvider
import host.stjin.anonaddy_shared.utils.fromJson
import java.io.InputStream

class AppMaintenanceRepository(
    context: Context,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BaseNetworkClient(context, dispatchers) {

    suspend fun getAddyIoInstanceVersion(): NetworkResult<Version> {
        waitForInit()

        return when (val networkResponse = executeGet(API_URL_APP_VERSION)) {
            is NetworkResponse.Failure -> {
                val errorMessage = handleGenericError(0, "", "getAddyIoInstanceVersion", networkResponse.exception)
                NetworkResult.Error(errorMessage, 0, networkResponse.exception)
            }
            is NetworkResponse.Success -> {
                val response = networkResponse.response
                val code = response.code
                val bodyString = try { response.body.string() } catch (e: Exception) { "" }

                when (code) {
                    200 -> {
                        val addyIoData = gson.fromJson(bodyString, Version::class.java)
                        NetworkResult.Success(addyIoData, code)
                    }
                    401 -> {
                        invalidApiKey()
                        NetworkResult.Error("Unauthorized", code)
                    }
                    404 -> {
                        NetworkResult.Success(Version(0, 0, 0, ""), code)
                    }
                    else -> {
                        val errorMessage = handleGenericError(code, bodyString, "getAddyIoInstanceVersion")
                        NetworkResult.Error(errorMessage, code)
                    }
                }
            }
        }
    }

    suspend fun getGithubTags(): NetworkResult<Feed?> {
        waitForInit()

        return when (val networkResponse = executeGet(GITHUB_TAGS_RSS_FEED)) {
            is NetworkResponse.Failure -> {
                val errorMessage = handleGenericError(0, "", "getGithubTags", networkResponse.exception)
                NetworkResult.Error(errorMessage, 0, networkResponse.exception)
            }
            is NetworkResponse.Success -> {
                val response = networkResponse.response
                val code = response.code

                when (code) {
                    200 -> {
                        try {
                            val inputStream: InputStream? = response.body.byteStream()
                            val feed = if (inputStream != null) EarlParser.parse(inputStream, 0) else null
                            NetworkResult.Success(feed, code)
                        } catch (e: Exception) {
                            NetworkResult.Error(e.message, code, e)
                        }
                    }
                    else -> {
                        val bodyString = try { response.body.string() } catch (e: Exception) { "" }
                        val errorMessage = handleGenericError(code, bodyString, "getGithubTags")
                        NetworkResult.Error(errorMessage, code)
                    }
                }
            }
        }
    }

    suspend fun getAllAccountNotifications(): NetworkResult<PaginatedResponse<AccountNotifications>> {
        waitForInit()

        val response = executeGet(API_URL_ACCOUNT_NOTIFICATIONS)
        return handleResponse(response, "getAllAccountNotifications") { gson.fromJson(it) }
    }

    suspend fun cacheAccountNotificationsCountForWidgetAndBackgroundService(): NetworkResult<Boolean> {
        return when (val notificationsResult = getAllAccountNotifications()) {
            is NetworkResult.Success -> {
                val result = notificationsResult.data
                val currentAccountNotifications = encryptedSettingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT)
                val totalCount = result.meta?.total ?: result.data.size

                encryptedSettingsManager.putSettingsInt(
                    SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT_PREVIOUS,
                    currentAccountNotifications
                )
                encryptedSettingsManager.putSettingsInt(
                    SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT,
                    totalCount
                )
                NetworkResult.Success(true)
            }
            is NetworkResult.Error -> NetworkResult.Error(notificationsResult.error, notificationsResult.statusCode)
        }
    }

    suspend fun getServiceStatus(url: String = host.stjin.anonaddy_shared.AddyIo.STATUS_JSON_URL): NetworkResult<ServiceStatus> {
        waitForInit()

        return when (val networkResponse = executeGet(url)) {
            is NetworkResponse.Failure -> {
                val errorMessage = handleGenericError(0, "", "getServiceStatus", networkResponse.exception)
                NetworkResult.Error(errorMessage, 0, networkResponse.exception)
            }
            is NetworkResponse.Success -> {
                val response = networkResponse.response
                val code = response.code
                val bodyString = try { response.body.string() } catch (e: Exception) { "" }

                if (code in 200..299) {
                    try {
                        val status = gson.fromJson(bodyString, ServiceStatus::class.java)
                        NetworkResult.Success(status, code)
                    } catch (e: Exception) {
                        NetworkResult.Error("Failed to parse status: ${e.message}", code, e)
                    }
                } else {
                    val errorMessage = handleGenericError(code, bodyString, "getServiceStatus")
                    NetworkResult.Error(errorMessage, code)
                }
            }
        }
    }

    suspend fun testServiceStatus(mockJson: String? = null): NetworkResult<ServiceStatus> {
        if (mockJson != null) {
            return try {
                val status = gson.fromJson(mockJson, ServiceStatus::class.java)
                NetworkResult.Success(status, 200)
            } catch (e: Exception) {
                NetworkResult.Error("Failed to parse mock status: ${e.message}", 400, e)
            }
        }
        return getServiceStatus()
    }
}
