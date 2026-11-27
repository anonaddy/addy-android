package host.stjin.anonaddy.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy_shared.managers.SettingsManager
import java.util.concurrent.TimeUnit

/*
    A difference between the backgroundworker on the paired device and the watch is that the watch
    always needs to refresh data at some interval.

    The goal of the watch app is to instantly show data upon opening the app, thus we need to periodically obtain
    data.
 */

class BackgroundWorkerHelper(private val context: Context) {
    private val CONSTANT_PERIODIC_WORK_REQUEST_TAG = "host.stjin.anonaddy.backgroundworker"
    private val CONSTANT_ONE_TIME_WORK_REQUEST_TAG = "host.stjin.anonaddy.backgroundworker.onetime"

    fun scheduleBackgroundWorker() {
        //define constraints
        val myConstraints: Constraints = Constraints.Builder()
            .setRequiresDeviceIdle(false)
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        // Get the amount of minutes from the settings
        val minutes = ServiceLocator.settingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL, 30).toLong()
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

        // Trigger an immediate background execution to ensure pinned aliases and widget data are cached immediately
        triggerImmediateBackgroundWorker()
    }

    fun triggerImmediateBackgroundWorker() {
        val myConstraints: Constraints = Constraints.Builder()
            .setRequiresDeviceIdle(false)
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<BackgroundWorker>()
            .setConstraints(myConstraints)
            .addTag(CONSTANT_ONE_TIME_WORK_REQUEST_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            CONSTANT_ONE_TIME_WORK_REQUEST_TAG,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
    }

    fun cancelScheduledBackgroundWorker() {
        WorkManager.getInstance(context).cancelUniqueWork(CONSTANT_PERIODIC_WORK_REQUEST_TAG)
    }
}
