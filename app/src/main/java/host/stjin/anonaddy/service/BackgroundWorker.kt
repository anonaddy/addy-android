package host.stjin.anonaddy.service

import android.content.Context
import androidx.work.*
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/*
This BackgroundWorker is used for obtaining data in the background, this data is then being used to "Watch" aliases and updating the data the widget uses.
 */

class BackgroundWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {

        if (BuildConfig.DEBUG) {
            println("doWork() called")
        }

        val appContext = applicationContext
        val backgroundWorkerHelper = BackgroundWorkerHelper(appContext)

        // True if there are aliases to be watched or there are widgets to be updated
        if (backgroundWorkerHelper.isThereWorkTodo()) {
            val networkHelper = NetworkHelper(appContext)
            var networkCallResult = false

            // Block the thread until this is finished
            runBlocking(Dispatchers.Default) {
                networkHelper.cacheDataForWidget { result ->
                    networkCallResult = result
                }
            }

            // No the data has been updated we perform the AliasWatcher check
            AliasWatcher(appContext).watchAliasesForDifferences()

            return if (networkCallResult) {
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
    val CONSTANT_PERIODIC_WORK_REQUEST_TAG = "host.stjin.anonaddy.backgroundworker"
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
                .setRequiresStorageNotLow(true)
                .build()

            val source: Data = Data.Builder()
                .putString("workType", "PeriodicTime")
                .build()

            val minutes = SettingsManager(false, context).getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL).toLong()
            val refreshCpnWork = PeriodicWorkRequest.Builder(BackgroundWorker::class.java, minutes, TimeUnit.MINUTES)
                .setConstraints(myConstraints)
                .setInputData(source)
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

