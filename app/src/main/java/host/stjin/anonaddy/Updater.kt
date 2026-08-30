package host.stjin.anonaddy

import android.content.Context
import host.stjin.anonaddy.utils.GooglePlayUtils
import host.stjin.anonaddy_shared.network.NetworkResult

data class UpdateInfo(
    val isServerNewer: Boolean,
    val serverVersion: String?,
    val isAppNewer: Boolean,
    val error: String?
)

object Updater {
    // This bit is getting called by default, it checks the Github RSS feed for the latest version
    suspend fun isUpdateAvailable(): UpdateInfo {
        return when (val result = ServiceLocator.appMaintenanceRepository.getGithubTags()) {
            is NetworkResult.Success -> {
                val feed = result.data
                if (feed != null && feed.items.isNotEmpty()) {
                    val version = feed.items[0]?.title
                    if (version != null) {
                        val isServerNewer = isServerVersionNewer(version, BuildConfig.VERSION_NAME)
                        val isAppNewer = isAppVersionNewer(version, BuildConfig.VERSION_NAME)
                        UpdateInfo(isServerNewer, version, isAppNewer, null)
                    } else {
                        UpdateInfo(false, null, false, null)
                    }
                } else {
                    UpdateInfo(false, null, false, null)
                }
            }
            is NetworkResult.Error -> {
                UpdateInfo(false, null, false, result.error)
            }
        }
    }

    private fun parseVersionParts(version: String): List<Int> {
        val cleanVersion = version.removePrefix("v").substringBefore("-").substringBefore("+")
        return cleanVersion.split(".").mapNotNull { it.toIntOrNull() }
    }

    private fun isServerVersionNewer(serverVersion: String, appVersion: String): Boolean {
        val serverParts = parseVersionParts(serverVersion)
        val appParts = parseVersionParts(appVersion)
        val maxLen = maxOf(serverParts.size, appParts.size)
        for (i in 0 until maxLen) {
            val serverPart = serverParts.getOrElse(i) { 0 }
            val appPart = appParts.getOrElse(i) { 0 }
            if (serverPart > appPart) return true
            if (serverPart < appPart) return false
        }
        return false
    }

    private fun isAppVersionNewer(serverVersion: String, appVersion: String): Boolean {
        return isServerVersionNewer(appVersion, serverVersion)
    }

    fun figureOutDownloadUrl(context: Context): String {
        return when {
            GooglePlayUtils.isInstalledViaGooglePlay(context) -> "https://play.google.com/store/apps/details?id=host.stjin.anonaddy"
            GooglePlayUtils.isInstalledViaFDroid(context) -> "https://f-droid.org/en/packages/host.stjin.anonaddy"
            else -> "https://github.com/anonaddy/addy-android/releases"
        }
    }
}