package host.stjin.anonaddy.service

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.Updater
import host.stjin.anonaddy.notifications.NotificationHelper
import host.stjin.anonaddy.widget.AliasWidget1Provider
import host.stjin.anonaddy.widget.AliasWidget2Provider
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.GsonTools
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.ocpsoft.prettytime.PrettyTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit


/*
This BackgroundWorker is used for obtaining data in the background, this data is then being used to "Watch" aliases and updating the data the widget uses.
 */

class BackgroundWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    private fun updateWidgets() {
        // Update widget 1
        val updateWidget1Intent = Intent(applicationContext, AliasWidget1Provider::class.java)
        updateWidget1Intent.action = ACTION_APPWIDGET_UPDATE
        val ids1 = AppWidgetManager.getInstance(applicationContext)
            .getAppWidgetIds(ComponentName(applicationContext, AliasWidget1Provider::class.java))
        updateWidget1Intent.putExtra(EXTRA_APPWIDGET_IDS, ids1)
        applicationContext.sendBroadcast(updateWidget1Intent)


        // Update widget 2
        val updateWidget2Intent = Intent(applicationContext, AliasWidget2Provider::class.java)
        updateWidget2Intent.action = ACTION_APPWIDGET_UPDATE
        val ids2 = AppWidgetManager.getInstance(applicationContext)
            .getAppWidgetIds(ComponentName(applicationContext, AliasWidget2Provider::class.java))
        updateWidget2Intent.putExtra(EXTRA_APPWIDGET_IDS, ids2)
        applicationContext.sendBroadcast(updateWidget2Intent)
    }

    override fun doWork(): Result {

        if (BuildConfig.DEBUG) {
            println("doWork() called")
        }

        val appContext = applicationContext
        val backgroundWorkerHelper = BackgroundWorkerHelper(appContext)
        val settingsManager = SettingsManager(false, appContext)
        val encryptedSettingsManager = SettingsManager(true, appContext)

        // True if there are aliases to be watched, widgets to be updated or checked for updates
        if (backgroundWorkerHelper.isThereWorkTodo()) {
            val networkHelper = NetworkHelper(appContext)

            // Stored if the network call succeeds its task
            var userResourceNetworkCallResult = false
            var aliasNetworkCallResult = false
            var aliasWatcherNetworkCallResult = false
            var failedDeliveriesNetworkCallResult = false

            // Block the thread until this is finished
            runBlocking(Dispatchers.Default) {

                /*
                CACHE DATA
                 */

                networkHelper.cacheUserResourceForWidget { result ->
                    // Store the result if the data succeeded to update in a boolean
                    userResourceNetworkCallResult = result
                }

                networkHelper.cacheMostPopularAliasesDataForWidget({ result ->
                    // Store the result if the data succeeded to update in a boolean
                    aliasNetworkCallResult = result
                })

                /**
                ALIAS_WATCHER FUNCTIONALITY
                 **/

                aliasWatcherNetworkCallResult = aliasWatcherTask(appContext, networkHelper, encryptedSettingsManager)



                /*
                UPDATES
                 */

                if (settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_UPDATES)) {
                    Updater.isUpdateAvailable({ updateAvailable: Boolean, latestVersion: String?, _: Boolean, _ :String? ->
                        if (updateAvailable) {
                            latestVersion?.let {
                                NotificationHelper(appContext).createUpdateNotification(
                                    it
                                )
                            }
                        }
                    }, appContext)
                }

                /*
                API TOKEN
                 */

                if (settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_API_TOKEN_EXPIRY, true)) {
                    networkHelper.getApiTokenDetails { apiTokenDetails, error ->
                        if (apiTokenDetails?.expires_at != null) {
                            val expiryDate = DateTimeUtils.turnStringIntoLocalDateTime(apiTokenDetails.expires_at) // Get the expiry date
                            val currentDateTime = LocalDateTime.now() // Get the current date
                            val deadLineDate = expiryDate?.minusDays(5) // Subtract 5 days from the expiry date
                            if (currentDateTime.isAfter(deadLineDate)) {
                                // The current date is suddenly after the deadline date. It will expire within 5 days
                                // Show the api is about to expire card

                                // Check if the notification has already been fired for this day
                                val previousNotificationLeftDays =
                                    encryptedSettingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_API_KEY_EXPIRY_LEFT_COUNT)
                                val currentLeftDays = ChronoUnit.DAYS.between(currentDateTime, deadLineDate).toInt()

                                if (previousNotificationLeftDays != currentLeftDays) {
                                    encryptedSettingsManager.putSettingsInt(
                                        SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_API_KEY_EXPIRY_LEFT_COUNT,
                                        currentLeftDays
                                    )
                                    val text = PrettyTime().format(expiryDate)
                                    NotificationHelper(appContext).createApiTokenExpiryNotification(text)
                                }

                            } else {
                                // The current date is not yet after the deadline date.
                            }
                        }
                        // If expires_at is null it will never expire

                    }
                }

                /*
                DOMAIN ERRORS
                 */

                if (settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_DOMAIN_ERROR, false)) {
                    networkHelper.getAllDomains { domains, _ ->
                        if (!domains.isNullOrEmpty()) {
                            // Check the amount of domains with MX errors
                            val amountOfDomainsWithErrors = domains.count { it.domain_mx_validated_at == null }
                            if (amountOfDomainsWithErrors > 0) {

                                // Check if the notification has already been fired for this count of domains
                                val previousNotificationLeftDays =
                                    encryptedSettingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DOMAIN_ERROR_COUNT)

                                // If the domains with errors have been changed, fire a notification
                                if (previousNotificationLeftDays != amountOfDomainsWithErrors) {
                                    encryptedSettingsManager.putSettingsInt(
                                        SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DOMAIN_ERROR_COUNT,
                                        amountOfDomainsWithErrors
                                    )
                                    NotificationHelper(appContext).createDomainErrorNotification(amountOfDomainsWithErrors)
                                }

                            }
                        }

                    }
                }

                /*
                SUBSCRIPTION EXPIRY
                 */

                if (settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_SUBSCRIPTION_EXPIRY, false)) {
                    networkHelper.getUserResource { user, _ ->
                        if (user?.subscription_ends_at != null) {
                            val expiryDate = DateTimeUtils.turnStringIntoLocalDateTime(user.subscription_ends_at) // Get the expiry date
                            val currentDateTime = LocalDateTime.now() // Get the current date
                            val deadLineDate = expiryDate?.minusDays(7) // Subtract 7 days from the expiry date
                            if (currentDateTime.isAfter(deadLineDate)) {
                                // The current date is suddenly after the deadline date. It will expire within 7 days
                                // Show the subscription is about to expire card

                                // Check if the notification has already been fired for this day
                                val previousNotificationLeftDays =
                                    encryptedSettingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_SUBSCRIPTION_EXPIRY_LEFT_COUNT)
                                val currentLeftDays = ChronoUnit.DAYS.between(currentDateTime, deadLineDate).toInt()

                                if (previousNotificationLeftDays != currentLeftDays) {
                                    encryptedSettingsManager.putSettingsInt(
                                        SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_SUBSCRIPTION_EXPIRY_LEFT_COUNT,
                                        currentLeftDays
                                    )
                                    val text = PrettyTime().format(expiryDate)
                                    NotificationHelper(appContext).createSubscriptionExpiryNotification(text)
                                }
                            } else {
                                // The current date is not yet after the deadline date.
                            }
                        }
                        // If expires_at is null it will never expire

                    }
                }


                /*
                BACKUPS
                 */
                if (settingsManager.getSettingsBool(SettingsManager.PREFS.PERIODIC_BACKUPS)) {
                    BackupHelper(appContext).let {
                        val date: LocalDate? =
                            it.getLatestBackupDate()?.let { it1 -> Instant.ofEpochMilli(it1).atZone(ZoneId.systemDefault()).toLocalDate() }
                        val today: LocalDate = LocalDate.now()
                        // If the previous backup is *older* than 1 day OR if there is no backup at-all. Create a new backup
                        // Else don't make a new backup
                        if (date?.isBefore(today.minusDays(1)) != false) {
                            if (it.createBackup()) {
                                // When the backup is successful delete backups older than 30 days
                                it.deleteBackupsOlderThanXDays(30)
                            } else {
                                NotificationHelper(appContext).createFailedBackupNotification()
                            }
                        }

                    }

                }

                /*
                FAILED DELIVERIES
                 */

                if (settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_FAILED_DELIVERIES)) {
                    networkHelper.cacheFailedDeliveryCountForWidgetAndBackgroundService { result ->
                        // Store the result if the data succeeded to update in a boolean
                        failedDeliveriesNetworkCallResult = result
                    }

                    val currentFailedDeliveries =
                        encryptedSettingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT)
                    val previousFailedDeliveries =
                        encryptedSettingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT_PREVIOUS)
                    // If the current failed delivery count is bigger than the previous list. That means there are new failed deliveries
                    if (currentFailedDeliveries > previousFailedDeliveries) {
                        NotificationHelper(appContext).createFailedDeliveryNotification(
                            currentFailedDeliveries - previousFailedDeliveries
                        )
                    }
                } else {
                    // Not required so always success
                    failedDeliveriesNetworkCallResult = true
                }
            }

            // If the aliasNetwork call was successful, perform the check
            if (aliasWatcherNetworkCallResult) {
                // Now the data has been updated, perform the AliasWatcher check
                AliasWatcher(appContext).watchAliasesForDifferences()
            }

            // If all tasks are successful return a success()
            return if (userResourceNetworkCallResult &&
                aliasNetworkCallResult &&
                aliasWatcherNetworkCallResult &&
                failedDeliveriesNetworkCallResult
            ) {
                // Now the data has been updated, we can update the widget as well
                updateWidgets()

                Result.success()
            } else {
                Result.failure()
            }
        } else {
            // Cancel the work as there is no work to do
            backgroundWorkerHelper.cancelScheduledBackgroundWorker()
            return Result.success()
        }
    }

    private suspend fun aliasWatcherTask(appContext: Context, networkHelper: NetworkHelper, settingsManager: SettingsManager): Boolean {

        /*
        This method loops through all the aliases that need to be watched and caches those aliases locally
         */

        val aliasWatcher = AliasWatcher(appContext)
        val aliasesToWatch = aliasWatcher.getAliasesToWatch().toList()

        if (aliasesToWatch.isNotEmpty()) {
            // Get all aliases from the watchList
            networkHelper.bulkGetAlias({ result, _ ->
                if (result != null) {

                    // Get a copy of the current list
                    val aliasesJson = settingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_WATCH_ALIAS_DATA)
                    val aliasesList = aliasesJson?.let { GsonTools.jsonToAliasObject(appContext, it) }


                    //region Save a copy of the list

                    // When the call is successful, save a copy of the current CACHED version to `currentList`
                    val currentList = settingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_WATCH_ALIAS_DATA)

                    // If the current CACHED list is not null, move the current list to the PREV position for AliasWatcher to compare
                    // This CACHED list could be null if this would be the first time the service is running
                    currentList?.let {
                        settingsManager.putSettingsString(
                            SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_WATCH_ALIAS_DATA_PREVIOUS,
                            it
                        )
                    }
                    //endregion


                    //region CLEANUP DELETED ALIASES
                    // Let's say a user forgets this alias using the web-app, but this alias is watched. We need to make sure that the aliases we request
                    // Are actually returned. If aliases requested are not returned we can assume the alias has been deleted thus we can delete this alias from the watchlist

                    for (id in aliasesToWatch) {
                        if (result.data.none { it.id == id }) {
                            // This alias is being watched but not returned, delete it from the watcher

                            LoggingHelper(appContext, LoggingHelper.LOGFILES.DEFAULT).addLog(
                                LOGIMPORTANCE.WARNING.int,
                                appContext.resources.getString(
                                    R.string.notification_alias_watches_alias_does_not_exist_anymore_desc,
                                    aliasesList?.first { it.id == id }?.email ?: id
                                ),
                                "aliasWatcherTask",
                                null
                            )

                            NotificationHelper(appContext).createAliasWatcherAliasDoesNotExistAnymoreNotification(
                                aliasesList?.first { it.id == id }?.email ?: id
                            )

                            aliasWatcher.removeAliasToWatch(id)
                        }
                    }
                    //endregion

                    // Turn the list into a json object
                    val data = Gson().toJson(result.data)

                    // Store a copy of the just received data locally
                    settingsManager.putSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_WATCH_ALIAS_DATA, data)

                } else {
                    // The call failed, it will be logged in NetworkHelper. Try again later
                }
            }, aliasesToWatch)
        }

        return true
    }

}


class BackgroundWorkerHelper(private val context: Context) {
    private val CONSTANT_PERIODIC_WORK_REQUEST_TAG = "host.stjin.anonaddy.backgroundworker"
    fun scheduleBackgroundWorker() {
        // Cancel the work to prevent it from being scheduled twice
        cancelScheduledBackgroundWorker()

        // True if there are aliases to be watched or there are widgets to be updated
        if (isThereWorkTodo()) {
            //define constraints
            val myConstraints: Constraints = Constraints.Builder()
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(false)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            // Get the amount of minutes from the settings
            val minutes = SettingsManager(false, context).getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL, 30).toLong()
            val refreshCpnWork = PeriodicWorkRequest.Builder(BackgroundWorker::class.java, minutes, TimeUnit.MINUTES)
                .setConstraints(myConstraints)
                .addTag(CONSTANT_PERIODIC_WORK_REQUEST_TAG)
                .build()
            WorkManager.getInstance(context).enqueue(refreshCpnWork)

            if (BuildConfig.DEBUG) {
                println("There is work todo, queued work for every $minutes minutes")
            }
        }
    }

    fun isThereWorkTodo(): Boolean {
        val settingsManager = SettingsManager(false, context)

        // Count amount of aliases to be watched
        val aliasToWatch = AliasWatcher(context).getAliasesToWatch()
        // Count amount of widgets
        val amountOfWidgets = settingsManager.getSettingsInt(SettingsManager.PREFS.WIDGETS_ACTIVE)

        val shouldCheckForUpdates = settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_UPDATES)
        val shouldCheckForFailedDeliveries = settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_FAILED_DELIVERIES)
        val shouldCheckApiTokenExpiry = settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_API_TOKEN_EXPIRY, true)
        val shouldMakePeriodicBackups = settingsManager.getSettingsBool(SettingsManager.PREFS.PERIODIC_BACKUPS)

        if (BuildConfig.DEBUG) {
            println("isThereWorkTodo: aliasToWatch=$aliasToWatch;amountOfWidgets=$amountOfWidgets;NOTIFY_UPDATES=$shouldCheckForUpdates;NOTIFY_FAILED_DELIVERIES=$shouldCheckForFailedDeliveries")
        }

        // If there are
        // -aliases to be watched
        // -widgets to be updated
        // -app updates to be checked for in the background
        // -failed deliveries to be checked
        // --return true
        return (!aliasToWatch.isNullOrEmpty() || amountOfWidgets > 0 || shouldCheckForUpdates || shouldCheckForFailedDeliveries || shouldCheckApiTokenExpiry || shouldMakePeriodicBackups)
    }

    fun cancelScheduledBackgroundWorker() {
        WorkManager.getInstance(context).cancelAllWorkByTag(CONSTANT_PERIODIC_WORK_REQUEST_TAG)

        if (BuildConfig.DEBUG) {
            println("Cancelled work with tag $CONSTANT_PERIODIC_WORK_REQUEST_TAG")
        }
    }
}

