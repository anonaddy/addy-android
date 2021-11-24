package host.stjin.anonaddy.service

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.*
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.Updater
import host.stjin.anonaddy.notifications.NotificationHelper
import host.stjin.anonaddy.widget.AliasWidget1Provider
import host.stjin.anonaddy.widget.AliasWidget2Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
            var aliasNetworkCallResult = false
            var domainNetworkCallResult = false
            var usernameNetworkCallResult = false
            var rulesNetworkCallResult = false
            var recipientNetworkCallResult = false
            var failedDeliveriesNetworkCallResult = false

            // Block the thread until this is finished
            runBlocking(Dispatchers.Default) {

                /*
                CACHE DATA
                 */

                networkHelper.cacheAliasDataForWidget { result ->
                    // Store the result if the data succeeded to update in a boolean
                    aliasNetworkCallResult = result
                }

                networkHelper.cacheDomainCountForWidget { result ->
                    // Store the result if the data succeeded to update in a boolean
                    domainNetworkCallResult = result
                }

                networkHelper.cacheUsernamesCountForWidget { result ->
                    // Store the result if the data succeeded to update in a boolean
                    usernameNetworkCallResult = result
                }

                networkHelper.cacheRulesCountForWidget { result ->
                    // Store the result if the data succeeded to update in a boolean
                    rulesNetworkCallResult = result
                }

                networkHelper.cacheRecipientCountForWidget { result ->
                    // Store the result if the data succeeded to update in a boolean
                    recipientNetworkCallResult = result
                }

                networkHelper.cacheFailedDeliveryCountForWidgetAndBackgroundService { result ->
                    // Store the result if the data succeeded to update in a boolean
                    failedDeliveriesNetworkCallResult = result
                }

                /*
                UPDATES
                 */

                if (settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_UPDATES)) {
                    Updater.isUpdateAvailable({ updateAvailable: Boolean, latestVersion: String?, _: Boolean ->
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
                }
            }

            // If the aliasNetwork call was successful, perform the check
            if (aliasNetworkCallResult) {
                // Now the data has been updated, perform the AliasWatcher check
                AliasWatcher(appContext).watchAliasesForDifferences()
            }

            // If both tasks are successful return a success()
            return if (aliasNetworkCallResult &&
                domainNetworkCallResult &&
                usernameNetworkCallResult &&
                rulesNetworkCallResult &&
                recipientNetworkCallResult &&
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
        val encryptedSettingsManager = SettingsManager(true, context)
        val settingsManager = SettingsManager(false, context)

        // Count amount of aliases to be watched
        val aliasToWatch = encryptedSettingsManager.getStringSet(SettingsManager.PREFS.BACKGROUND_SERVICE_WATCH_ALIAS_LIST)
        // Count amount of widgets
        val amountOfWidgets = SettingsManager(false, context).getSettingsInt(SettingsManager.PREFS.WIDGETS_ACTIVE)

        val shouldCheckForUpdates = settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_UPDATES)
        val shouldCheckForFailedDeliveries = settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_FAILED_DELIVERIES)
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
        return (!aliasToWatch.isNullOrEmpty() || amountOfWidgets > 0 || shouldCheckForUpdates || shouldCheckForFailedDeliveries || shouldMakePeriodicBackups)
    }

    fun cancelScheduledBackgroundWorker() {
        WorkManager.getInstance(context).cancelAllWorkByTag(CONSTANT_PERIODIC_WORK_REQUEST_TAG)

        if (BuildConfig.DEBUG) {
            println("Cancelled work with tag $CONSTANT_PERIODIC_WORK_REQUEST_TAG")
        }
    }
}

