package host.stjin.anonaddy.service

import android.content.Context
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.notifications.NotificationHelper
import host.stjin.anonaddy.utils.GsonTools

class AliasWatcher(private val context: Context) {
    val settingsManager = SettingsManager(true, context)


    // TODO for aliasWatcher we would have to loop and get aliasinfo for every alias watched
    fun watchAliasesForDifferences() {
        val aliasesToWatch = settingsManager.getStringSet(SettingsManager.PREFS.BACKGROUND_SERVICE_WATCH_ALIAS_LIST)
        val aliasesJson = settingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DATA_ALIASES)
        val previousAliasesJson = settingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DATA_ALIASES_PREVIOUS)

        // Turn the 2 alias en previousAlias jsons into objects.
        val aliasesList = aliasesJson?.let { GsonTools.jsonToAliasObject(context, it) }
        val aliasesListPrevious = previousAliasesJson?.let { GsonTools.jsonToAliasObject(context, it) }

        // Iterate through the new list, if an alias is on the watchlist, try to look up the emails_forwarded amount from the old list and compare it with
        // the new one

        // if aliasesToWatch is empty, skip everything, don't compare because there is nothing to be compared
        if (!aliasesToWatch.isNullOrEmpty()) {
            // if aliasesList is empty or null, skip everything, don't compare ad there is no list
            if (!aliasesList.isNullOrEmpty()) {

                // Loop through all the aliases in the aliasList (this only contains active and non-deleted aliases)
                for (alias in aliasesList) {
                    // If an alias in the list is on the watchlist
                    if (aliasesToWatch.contains(alias.id)) {
                        // The alias is on the watchlist
                        // Get the amount of forwarded emails for this alias
                        val currentEmailsForwarded = alias.emails_forwarded

                        // Find this alias in the previous version of the list.
                        val index = aliasesListPrevious?.indexOfFirst { it.id == alias.id }

                        // If index is null or -1 there is no alias like this in the previous list, alias must be new and thus will have 0 previousEmailsForwarded
                        val previousEmailsForwarded = if (index == null || index == -1) {
                            0
                        } else {
                            aliasesListPrevious[index].emails_forwarded
                        }

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

    fun getAliasesToWatch(): MutableSet<String>? {
        return settingsManager.getStringSet(SettingsManager.PREFS.BACKGROUND_SERVICE_WATCH_ALIAS_LIST)
    }

    fun removeAliasToWatch(alias: String) {
        val aliasList = getAliasesToWatch()
        aliasList?.remove(alias)
        aliasList?.let { settingsManager.putStringSet(SettingsManager.PREFS.BACKGROUND_SERVICE_WATCH_ALIAS_LIST, it) }

        // Since an alias was removed from the watchlist, call scheduleBackgroundWorker. This method will schedule the service if its still required
        BackgroundWorkerHelper(context).scheduleBackgroundWorker()
    }

    fun addAliasToWatch(alias: String) {
        val aliasList = getAliasesToWatch()
        aliasList?.add(alias)
        aliasList?.let { settingsManager.putStringSet(SettingsManager.PREFS.BACKGROUND_SERVICE_WATCH_ALIAS_LIST, it) }

        // Since an alias was added to the watchlist, call scheduleBackgroundWorker. This method will schedule the service if its required
        BackgroundWorkerHelper(context).scheduleBackgroundWorker()
    }
}