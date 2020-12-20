package host.stjin.anonaddy.service

import android.content.Context
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.utils.GsonTools
import host.stjin.anonaddy.utils.notifications.NotificationHelper

class AliasWatcher(private val context: Context) {
    val settingsManager = SettingsManager(true, context)

    fun watchAliasesForDifferences() {
        val aliasesToWatch = settingsManager.getStringSet(SettingsManager.PREFS.BACKGROUND_SERVICE_WATCH_ALIAS_LIST)
        val aliasesJson = settingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DATA_ALIASES)
        val previousAliasesJson = settingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DATA_ALIASES_PREVIOUS)

        val aliasesList = aliasesJson?.let { GsonTools.jsonToAliasObject(context, it) }
        val aliasesListPrevious = previousAliasesJson?.let { GsonTools.jsonToAliasObject(context, it) }

        // Iterate through the new list, if a alias is on the watchlist, try to look up the emails_forwarded amount from the old list and compare it with
        // the new one
        if (aliasesList != null) {
            for (alias in aliasesList) {
                if (aliasesToWatch != null) {
                    if (aliasesToWatch.contains(alias.id)) {
                        // The alias is on the watchlist.
                        val currentEmailsForwarded = alias.emails_forwarded

                        // Find this alias in the previous version of the list.
                        val index = aliasesListPrevious?.indexOfFirst { it.id == alias.id }

                        // If index is null there is no alias like this in the previous list, alias must be new
                        if (index != null && index > 0) {
                            val previousEmailsForwarded = aliasesListPrevious[index].emails_forwarded
                            if (currentEmailsForwarded > previousEmailsForwarded) {
                                // There are currently more emails forwarded than last time, send a notification
                                NotificationHelper(context).createAliasWatcherNotification(
                                    currentEmailsForwarded - previousEmailsForwarded,
                                    alias.id,
                                    alias.email
                                )
                            }
                        }

                    }
                }
            }
        }
    }

    fun getAliasesToWatch(): MutableSet<String>? {
        return settingsManager.getStringSet(SettingsManager.PREFS.BACKGROUND_SERVICE_WATCH_ALIAS_LIST)
    }

    fun removeAliasToWatch(alias: String) {
        val aliasList = getAliasesToWatch()
        aliasList?.remove(alias)
        aliasList?.let { settingsManager.putStringSet(SettingsManager.PREFS.BACKGROUND_SERVICE_WATCH_ALIAS_LIST, it) }

        // Since an alias was removed from the watchlist, call scheduleBackgroundWorker. This method will schedule if its still required
        BackgroundWorkerHelper(context).scheduleBackgroundWorker()
    }

    fun addAliasToWatch(alias: String) {
        val aliasList = getAliasesToWatch()
        aliasList?.add(alias)
        aliasList?.let { settingsManager.putStringSet(SettingsManager.PREFS.BACKGROUND_SERVICE_WATCH_ALIAS_LIST, it) }

        // Since an alias was added to the watchlist, call scheduleBackgroundWorker. This method will schedule if its required
        BackgroundWorkerHelper(context).scheduleBackgroundWorker()
    }
}