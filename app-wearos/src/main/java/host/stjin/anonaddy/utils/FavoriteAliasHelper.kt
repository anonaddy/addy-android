package host.stjin.anonaddy.utils

import android.content.Context
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy_shared.managers.SettingsManager

class FavoriteAliasHelper(private val context: Context) {
    private val settingsManager = SettingsManager(true, context)

    fun getFavoriteAliases(): MutableSet<String>? {
        return settingsManager.getStringSet(SettingsManager.PREFS.WEAROS_FAVORITE_ALIASES)
    }


    fun removeAliasAsFavorite(alias: String) {
        val aliasList = getFavoriteAliases()
        aliasList?.remove(alias)
        aliasList?.let { settingsManager.putStringSet(SettingsManager.PREFS.WEAROS_FAVORITE_ALIASES, it) }

        // Since an alias was removed from the watchlist, call scheduleBackgroundWorker. This method will schedule the service if its still required
        BackgroundWorkerHelper(context).scheduleBackgroundWorker()
    }

    fun addAliasAsFavorite(alias: String): Boolean {
        val aliasList = getFavoriteAliases()
        if (aliasList != null) {
            if (aliasList.size < 3) {
                aliasList.add(alias)
                aliasList.let { settingsManager.putStringSet(SettingsManager.PREFS.WEAROS_FAVORITE_ALIASES, it) }

                // Since an alias was added to the favorites, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(context).scheduleBackgroundWorker()

                return true
            }
        }
        return false
    }
}