package host.stjin.anonaddy

import android.content.Context
import host.stjin.anonaddy.utils.YDGooglePlayUtils

object Updater {
    // This bit is getting called by default, it checks the Gitlab RSS feed for the latest version
    suspend fun isUpdateAvailable(
        callback: (Boolean, String?, Boolean) -> Unit, context: Context
    ) {
        NetworkHelper(context).getGitlabTags { feed, _ ->
            // Get the title (version name) of the first (thus latest) entry
            val version = feed?.items?.get(0)?.title
            if (version != null) {
                // Take the latest server version and remove the prefix (v) and version separators (.)
                // Turn the server version into an int.

                val serverVersionCodeAsInt = version.replace("v", "").replace(".", "").toInt()
                val appVersionCodeAsInt = BuildConfig.VERSION_NAME.replace("v", "").replace(".", "").toInt()
                callback(serverVersionCodeAsInt > appVersionCodeAsInt, version, appVersionCodeAsInt > serverVersionCodeAsInt)
            } else {
                // If version is null something must have gone wrong with checking for updates. Return false to make the app think its up-to-date
                callback(false, null, false)
            }

        }
    }

    fun figureOutDownloadUrl(context: Context): String {
        return when {
            YDGooglePlayUtils.isInstalledViaGooglePlay(context) -> "https://play.google.com/store/apps/details?id=host.stjin.anonaddy"
            YDGooglePlayUtils.isInstalledViaFDroid(context) -> "https://f-droid.org/en/packages/host.stjin.anonaddy"
            else -> "https://gitlab.com/Stjin/anonaddy-android/-/releases"
        }
    }
}