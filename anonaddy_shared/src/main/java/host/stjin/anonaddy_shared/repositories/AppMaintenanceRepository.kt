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
import host.stjin.anonaddy_shared.models.Version
import host.stjin.anonaddy_shared.network.BaseNetworkClient
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

        val response = executeGet(API_URL_APP_VERSION)
        val code = response.code
        val bodyString = try { response.body?.string() ?: "" } catch (e: Exception) { "" }

        return when (code) {
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

    suspend fun getGithubTags(): NetworkResult<Feed?> {
        waitForInit()

        val response = executeGet(GITHUB_TAGS_RSS_FEED)
        val code = response.code

        return when (code) {
            200 -> {
                try {
                    val inputStream: InputStream? = response.body?.byteStream()
                    val feed = if (inputStream != null) EarlParser.parse(inputStream, 0) else null
                    NetworkResult.Success(feed, code)
                } catch (e: Exception) {
                    NetworkResult.Error(e.message, code, e)
                }
            }
            else -> {
                val bodyString = try { response.body?.string() ?: "" } catch (e: Exception) { "" }
                val errorMessage = handleGenericError(code, bodyString, "getGithubTags")
                NetworkResult.Error(errorMessage, code)
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
}
