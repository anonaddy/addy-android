package host.stjin.anonaddy.service

import android.content.Context
import android.widget.Toast
import host.stjin.anonaddy.R
import host.stjin.anonaddy.notifications.NotificationHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.utils.GsonTools

class AliasWatcher(private val context: Context) {
    val encryptedSettingsManager = SettingsManager(true, context)


    fun watchAliasesForDifferences() {
        val aliasesToWatch = encryptedSettingsManager.getStringSet(SettingsManager.PREFS.BACKGROUND_SERVICE_WATCH_ALIAS_LIST)
        val aliasesJson = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_WATCH_ALIAS_DATA)
        val previousAliasesJson = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_WATCH_ALIAS_DATA_PREVIOUS)

        // Turn the 2 alias en previousAlias jsons into objects.
        val aliasesList = aliasesJson?.let { GsonTools.jsonToAliasObject(context, it) }
        val aliasesListPrevious = previousAliasesJson?.let { GsonTools.jsonToAliasObject(context, it) }

        // Iterate through the new list, if an alias is on the watchlist, try to look up the emails_forwarded amount from the old list and compare it with
        // the new one

        // if aliasesToWatch is empty, skip everything, don't compare because there is nothing to be compared
        if (!aliasesToWatch.isNullOrEmpty()) {
            // if aliasesList is empty or null, skip everything, don't compare as there is no list
            if (!aliasesList.isNullOrEmpty()) {

                // Loop through all the aliases in the aliasList (this only contains active and non-deleted aliases)
                for (alias in aliasesList) {
                    // The alias is on the watchlist, otherwise it would have been removed in the networkCall
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

    fun getAliasesToWatch(): MutableSet<String> {
        return encryptedSettingsManager.getStringSet(SettingsManager.PREFS.BACKGROUND_SERVICE_WATCH_ALIAS_LIST) ?: HashSet()
    }

    fun removeAliasToWatch(alias: String) {
        val aliasList = getAliasesToWatch()

        // Only remove alias if it is already in the list
        if (aliasList.contains(alias)) {
            aliasList.remove(alias)
            aliasList.let { encryptedSettingsManager.putStringSet(SettingsManager.PREFS.BACKGROUND_SERVICE_WATCH_ALIAS_LIST, it) }

            // Since an alias was removed from the watchlist, call scheduleBackgroundWorker. This method will schedule the service if its still required
            BackgroundWorkerHelper(context).scheduleBackgroundWorker()
        }
    }

    fun addAliasToWatch(alias: String): Boolean {
        val aliasList = getAliasesToWatch()
        // The aliasWatcherlist has a maximum of 25 aliases, the reason for this is to prevent API limitations
        return if (aliasList.count() > 24) {
            Toast.makeText(context, context.resources.getString(R.string.aliaswatcher_max_reached), Toast.LENGTH_LONG).show()
            false
        } else {
            // Only add alias if it is not already in the list
            if (!aliasList.contains(alias)) {
                aliasList.add(alias)
                aliasList.let { encryptedSettingsManager.putStringSet(SettingsManager.PREFS.BACKGROUND_SERVICE_WATCH_ALIAS_LIST, it) }

                // Since an alias was added to the watchlist, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(context).scheduleBackgroundWorker()
            }
            true
        }
    }
}