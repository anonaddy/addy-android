package host.stjin.anonaddy

import android.content.Context
import host.stjin.anonaddy.utils.YDGooglePlayUtils

object Updater {
    // This bit is getting called by default, it checks the Gitlab RSS feed for the latest version
    suspend fun isUpdateAvailable(
        callback: (Boolean, String?) -> Unit, context: Context
    ) {
        NetworkHelper(context).getGitlabTags { result ->
            // Get the title (version name) of the first (thus latest) entry
            val version = result?.items?.get(0)?.title
            if (version != null) {
                // Latest version and current version names do not match, thus true
                // Get only the version name BEFORE | and trim it.
                // This way "v3.0.0 | Material You" becomes "v3.0.0"
                callback(BuildConfig.VERSION_NAME.substringBefore("|").trim() != version, version)
            } else {
                // If version is null something must have gone wrong with checking for updates. Return false to make the app think its up-to-date
                callback(false, null)
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