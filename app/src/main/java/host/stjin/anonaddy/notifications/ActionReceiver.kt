package host.stjin.anonaddy.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.service.AliasWatcher


class ActionReceiver : BroadcastReceiver() {

    object NOTIFICATIONACTIONS {
        const val STOP_WATCHING = "stop_watching"
        const val STOP_DOWNLOAD_UPDATE_CHECK = "stop_download_update_check"
    }

    override fun onReceive(context: Context, intent: Intent) {
        //Toast.makeText(context,"received",Toast.LENGTH_SHORT).show();
        val action = intent.action
        val extra = intent.getStringExtra("extra")
        val notificationManager = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        when (action) {
            NOTIFICATIONACTIONS.STOP_WATCHING -> {
                extra?.let {
                    AliasWatcher(context).removeAliasToWatch(it)
                    // Dismiss notification
                    notificationManager.cancel(NotificationHelper.ALIAS_WATCHER_NOTIFICATION_NOTIFICATION_ID)

                }
            }
            NOTIFICATIONACTIONS.STOP_DOWNLOAD_UPDATE_CHECK -> {
                SettingsManager(false, context).putSettingsBool(SettingsManager.PREFS.NOTIFY_UPDATES, false)
                // Dismiss notification
                notificationManager.cancel(NotificationHelper.UPDATER_NOTIFICATION_ID)
            }
        }
    }

}