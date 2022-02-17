package host.stjin.anonaddy.service

import android.content.Context
import androidx.wear.tiles.TileService
import androidx.work.*
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit


/*
This BackgroundWorker is used for obtaining data in the background, this data is then being used to "Watch" aliases and updating the data the widget uses.
 */

class BackgroundWorker(private val ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    private fun updateTiles() {

        TileService.getUpdater(ctx)
            .requestUpdate(FavoriteAliasesTileService::class.java)

        /*// Update widget 1
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
        applicationContext.sendBroadcast(updateWidget2Intent)*/
    }

    override fun doWork(): Result {

        if (BuildConfig.DEBUG) {
            println("doWork() called")
        }

        val appContext = applicationContext

        val networkHelper = NetworkHelper(appContext)
        // Stored if the network call succeeds its task
        var userResourceNetworkCallResult = false
        var aliasNetworkCallResult = false
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

            networkHelper.cache15MostPopularAliasesDataForWidget { result ->
                // Store the result if the data succeeded to update in a boolean
                aliasNetworkCallResult = result
            }


            /*
            FAILED DELIVERIES
             */

            networkHelper.cacheFailedDeliveryCountForWidgetAndBackgroundService { result ->
                // Store the result if the data succeeded to update in a boolean
                failedDeliveriesNetworkCallResult = result
            }
        }


        // If both tasks are successful return a success()
        return if (userResourceNetworkCallResult &&
            aliasNetworkCallResult &&
            failedDeliveriesNetworkCallResult
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
        // Cancel the work to prevent it from being scheduled twice
        cancelScheduledBackgroundWorker()

        //define constraints
        val myConstraints: Constraints = Constraints.Builder()
            .setRequiresDeviceIdle(false)
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        // Get the amount of minutes from the settings

        //TODO make this a setting?
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


    private fun cancelScheduledBackgroundWorker() {
        WorkManager.getInstance(context).cancelAllWorkByTag(CONSTANT_PERIODIC_WORK_REQUEST_TAG)

        if (BuildConfig.DEBUG) {
            println("Cancelled work with tag $CONSTANT_PERIODIC_WORK_REQUEST_TAG")
        }
    }
}

