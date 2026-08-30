package host.stjin.anonaddy.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy_shared.managers.SettingsManager.PREFS
import java.util.concurrent.TimeUnit

class BackgroundWorkerHelper(private val context: Context) {
    companion object {
        private const val CONSTANT_PERIODIC_WORK_REQUEST_TAG = "host.stjin.anonaddy.backgroundworker"
    }

    fun scheduleBackgroundWorker() {
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
            val minutes = ServiceLocator.settingsManager.getSettingsInt(PREFS.BACKGROUND_SERVICE_INTERVAL, 30).toLong()
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
        } else {
            cancelScheduledBackgroundWorker()
        }
    }

    fun isThereWorkTodo(): Boolean {
        val settingsManager = ServiceLocator.settingsManager
        val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager

        if (encryptedSettingsManager.getSettingsString(PREFS.API_KEY) != null) {
            // Count amount of aliases to be watched
            val aliasToWatch = AliasWatcher(context).getAliasesToWatch()
            // Count amount of widgets
            val amountOfWidgets = settingsManager.getSettingsInt(PREFS.WIDGETS_ACTIVE)

            val shouldCheckForUpdates = settingsManager.getSettingsBool(PREFS.NOTIFY_UPDATES)
            val shouldCheckForFailedDeliveries = settingsManager.getSettingsBool(PREFS.NOTIFY_FAILED_DELIVERIES)
            val shouldCheckForAccountNotifications = settingsManager.getSettingsBool(PREFS.NOTIFY_ACCOUNT_NOTIFICATIONS)
            val shouldCheckApiTokenExpiry = settingsManager.getSettingsBool(PREFS.NOTIFY_API_TOKEN_EXPIRY, true)
            val shouldCheckCertificateExpiry = settingsManager.getSettingsBool(PREFS.NOTIFY_CERTIFICATE_EXPIRY)
            val shouldMakePeriodicBackups = settingsManager.getSettingsBool(PREFS.PERIODIC_BACKUPS)
            return (aliasToWatch.isNotEmpty() || amountOfWidgets > 0 || shouldCheckForUpdates || shouldCheckForFailedDeliveries || shouldCheckForAccountNotifications || shouldCheckApiTokenExpiry || shouldCheckCertificateExpiry || shouldMakePeriodicBackups)
        } else {
            return false
        }
    }

    fun cancelScheduledBackgroundWorker() {
        WorkManager.getInstance(context).cancelUniqueWork(CONSTANT_PERIODIC_WORK_REQUEST_TAG)
    }
}
