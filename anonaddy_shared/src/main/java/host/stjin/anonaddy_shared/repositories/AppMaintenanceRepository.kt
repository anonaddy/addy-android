package host.stjin.anonaddy_shared.repositories

import android.content.Context
import com.einmalfel.earl.EarlParser
import com.einmalfel.earl.Feed
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
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

        val (_, response, result) = Fuel.get(API_URL_APP_VERSION)
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData = gson.fromJson(data, Version::class.java)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            404 -> {
                NetworkResult.Success(Version(0, 0, 0, ""), response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "getAddyIoInstanceVersion")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun getGithubTags(): NetworkResult<Feed?> {
        waitForInit()

        val (_, response, result) = Fuel.get(GITHUB_TAGS_RSS_FEED)
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                try {
                    val inputStream: InputStream = result.get().byteInputStream()
                    val feed = EarlParser.parse(inputStream, 0)
                    NetworkResult.Success(feed, response.statusCode)
                } catch (e: Exception) {
                    NetworkResult.Error(e.message, response.statusCode, e)
                }
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "getGithubTags")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun getAllAccountNotifications(page: Int? = null, size: Int? = 20): NetworkResult<PaginatedResponse<AccountNotifications>> {
        waitForInit()

        val parameters = ArrayList<Pair<String, Any>>()
        if (page != null) parameters.add(Pair("page[number]", page.toString()))
        if (size != null) parameters.add(Pair("page[size]", size.toString()))

        val (_, response, result) = Fuel.get(API_URL_ACCOUNT_NOTIFICATIONS, parameters)
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return when (response.statusCode) {
            200 -> {
                val data = result.get()
                val addyIoData: PaginatedResponse<AccountNotifications> = gson.fromJson(data)
                NetworkResult.Success(addyIoData, response.statusCode)
            }
            401 -> {
                invalidApiKey()
                NetworkResult.Error("Unauthorized", response.statusCode)
            }
            else -> {
                val errorMessage = handleGenericError(response, result, "getAllAccountNotifications")
                NetworkResult.Error(errorMessage, response.statusCode)
            }
        }
    }

    suspend fun cacheAccountNotificationsCountForWidgetAndBackgroundService(): NetworkResult<Boolean> {
        return when (val notificationsResult = getAllAccountNotifications(1, 25)) {
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
