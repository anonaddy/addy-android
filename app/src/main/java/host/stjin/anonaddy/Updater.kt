package host.stjin.anonaddy

import android.content.Context

object Updater {
    // This bit is getting called by default, it checks the Gitlab RSS feed for the latest version
    suspend fun isUpdateAvailable(
        callback: (Boolean) -> Unit, context: Context
    ) {
        NetworkHelper(context).getGitlabTags { result ->
            // Get the title (version name) of the first (thus latest) entry
            val version = result?.items?.get(0)?.title
            if (version != null) {
                // Latest version and current version names do not match, thus true
                callback(BuildConfig.VERSION_NAME != version)
            } else {
                // If version is null something must have gone wrong with checking for updates. Return false to make the app think its up-to-date
                callback(false)
            }

        }
    }
}