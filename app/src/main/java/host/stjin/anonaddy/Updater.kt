package host.stjin.anonaddy

import android.content.Context
import host.stjin.anonaddy.utils.YDGooglePlayUtils
import host.stjin.anonaddy_shared.NetworkHelper

object Updater {
    // This bit is getting called by default, it checks the Github RSS feed for the latest version
    suspend fun isUpdateAvailable(
        callback: (Boolean, String?, Boolean, String?) -> Unit, context: Context
    ) {
        NetworkHelper(context).getGithubTags { feed, error ->
            if (feed != null) {
                // Get the title (version name) of the first (thus latest) entry
                val version = feed.items[0]?.title
                if (version != null) {
                    // Take the latest server version and remove the prefix (v) and version separators (.)
                    // Turn the server version into an int.

                    val serverVersionCodeAsInt = version.replace("v", "").replace(".", "").toInt()
                    val appVersionCodeAsInt = BuildConfig.VERSION_NAME.replace("v", "").replace(".", "").toInt()
                    callback(serverVersionCodeAsInt > appVersionCodeAsInt, version, appVersionCodeAsInt > serverVersionCodeAsInt, null)
                } else {
                    // If version is null something must have gone wrong with checking for updates. Return false to make the app think its up-to-date
                    callback(false, null, false, error)
                }
            } else {
                // If feed is null something must have gone wrong with checking for updates. Return false to make the app think its up-to-date
                callback(false, null, false, error)
            }
        }
    }

    fun figureOutDownloadUrl(context: Context): String {
        return when {
            YDGooglePlayUtils.isInstalledViaGooglePlay(context) -> "https://play.google.com/store/apps/details?id=host.stjin.anonaddy"
            YDGooglePlayUtils.isInstalledViaFDroid(context) -> "https://f-droid.org/en/packages/host.stjin.anonaddy"
            else -> "https://github.com/anonaddy/addy-android/releases"
        }
    }
}