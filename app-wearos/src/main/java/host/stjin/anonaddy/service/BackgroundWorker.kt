package host.stjin.anonaddy.service

import android.content.Context
import androidx.wear.tiles.TileService
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.tiles.PinnedAliasesTileService
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import java.util.concurrent.TimeUnit


/*
This BackgroundWorker is used for obtaining data in the background, this data is then being used to "Watch" aliases and updating the data the widget uses.
 */

class BackgroundWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private fun updateTiles() {
        TileService.getUpdater(ctx)
            .requestUpdate(PinnedAliasesTileService::class.java)
    }

    override suspend fun doWork(): Result {

        if (BuildConfig.DEBUG) {
            println("doWork() called")
        }

        val appContext = applicationContext

        val networkHelper = NetworkHelper(appContext)
        // Stored if the network call succeeds its task
        var userResourceNetworkCallResult = false
        var aliasNetworkCallResult = false
        var pinnedAliasNetworkCallResult = false

        /*
        CACHE DATA
         */

        networkHelper.cacheUserResourceForWidget { result ->
            // Store the result if the data succeeded to update in a boolean
            userResourceNetworkCallResult = result
        }

        networkHelper.cacheLastUpdatedAliasesData({ result ->
            // Store the result if the data succeeded to update in a boolean
            aliasNetworkCallResult = result
        })

        networkHelper.cachePinnedAliasesData { result ->
            // Store the result if the data succeeded to update in a boolean
            pinnedAliasNetworkCallResult = result
        }

        // If all tasks are successful return a success()
        return if (userResourceNetworkCallResult &&
            aliasNetworkCallResult && pinnedAliasNetworkCallResult
        ) {
            // Now the data has been updated, we can update the tiles as well
            updateTiles()
            Result.success()
        } else {
            Result.failure()
        }

    }

}

/*
    A difference between the backgroundworker on the paired device and the watch is that the watch
    always needs to refresh data at some interval.

    The goal of the watch app is to instantly show data upon opening the app, thus we need to periodically obtain
    data.
 */

class BackgroundWorkerHelper(private val context: Context) {
    private val CONSTANT_PERIODIC_WORK_REQUEST_TAG = "host.stjin.anonaddy.backgroundworker"
    fun scheduleBackgroundWorker() {
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

        // Use enqueueUniquePeriodicWork with ExistingPeriodicWorkPolicy.UPDATE to avoid cancelling and re-enqueuing
        // when no changes are made. This also prevents WorkerStoppedException when re-scheduling.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CONSTANT_PERIODIC_WORK_REQUEST_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            refreshCpnWork
        )

        if (BuildConfig.DEBUG) {
            println("There is work todo, queued work for every $minutes minutes")
        }

    }


    private fun cancelScheduledBackgroundWorker() {
        WorkManager.getInstance(context).cancelUniqueWork(CONSTANT_PERIODIC_WORK_REQUEST_TAG)

        if (BuildConfig.DEBUG) {
            println("Cancelled work with unique name $CONSTANT_PERIODIC_WORK_REQUEST_TAG")
        }
    }
}
