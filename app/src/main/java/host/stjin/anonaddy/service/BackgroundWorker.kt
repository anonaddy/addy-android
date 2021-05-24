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
import host.stjin.anonaddy.widget.AliasWidget1Provider
import host.stjin.anonaddy.widget.AliasWidget2Provider
import host.stjin.anonaddy.widget.AliasWidget3Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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

        // Update widget 3
        val updateWidget3Intent = Intent(applicationContext, AliasWidget3Provider::class.java)
        updateWidget3Intent.action = ACTION_APPWIDGET_UPDATE
        val ids3 = AppWidgetManager.getInstance(applicationContext)
            .getAppWidgetIds(ComponentName(applicationContext, AliasWidget3Provider::class.java))
        updateWidget2Intent.putExtra(EXTRA_APPWIDGET_IDS, ids3)
        applicationContext.sendBroadcast(updateWidget3Intent)
    }

    override fun doWork(): Result {

        if (BuildConfig.DEBUG) {
            println("doWork() called")
        }

        val appContext = applicationContext
        val backgroundWorkerHelper = BackgroundWorkerHelper(appContext)

        // True if there are aliases to be watched or there are widgets to be updated
        if (backgroundWorkerHelper.isThereWorkTodo()) {
            val networkHelper = NetworkHelper(appContext)

            // Stored if the network call succeeds its task
            var aliasNetworkCallResult = false
            var domainNetworkCallResult = false

            // Block the thread until this is finished
            runBlocking(Dispatchers.Default) {
                networkHelper.cacheAliasDataForWidget { result ->
                    // Store the result if the data succeeded to update in a boolean
                    aliasNetworkCallResult = result
                }

                networkHelper.cacheDomainsDataForWidget { result ->
                    // Store the result if the data succeeded to update in a boolean
                    domainNetworkCallResult = result
                }
            }

            // If the aliasNetwork call was successful, perform the check
            if (aliasNetworkCallResult) {
                // Now the data has been updated, perform the AliasWatcher check
                AliasWatcher(appContext).watchAliasesForDifferences()

                // Now the data has been updated, we can update the widget as well
                updateWidgets()
            }

            // If both tasks are successful return a success()
            return if (aliasNetworkCallResult && domainNetworkCallResult) {
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
        val settingsManager = SettingsManager(true, context)

        // Count amount of aliases to be watched
        val aliasToWatch = settingsManager.getStringSet(SettingsManager.PREFS.BACKGROUND_SERVICE_WATCH_ALIAS_LIST)
        // Count amount of widgets
        val amountOfWidgets = SettingsManager(false, context).getSettingsInt(SettingsManager.PREFS.WIDGETS_ACTIVE)

        if (BuildConfig.DEBUG) {
            println("isThereWorkTodo: aliasToWatch=$aliasToWatch;amountOfWidgets=$amountOfWidgets")
        }

        // If there are no aliases to be watched and there are no widgets to be updated, return false
        return !(aliasToWatch.isNullOrEmpty() && amountOfWidgets == 0)
    }

    fun cancelScheduledBackgroundWorker() {
        WorkManager.getInstance(context).cancelAllWorkByTag(CONSTANT_PERIODIC_WORK_REQUEST_TAG)

        if (BuildConfig.DEBUG) {
            println("Cancelled work with tag $CONSTANT_PERIODIC_WORK_REQUEST_TAG")
        }
    }
}

