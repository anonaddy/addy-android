package host.stjin.anonaddy.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy_shared.managers.SettingsManager


class ActionReceiver : BroadcastReceiver() {

    object NOTIFICATIONACTIONS {
        const val STOP_WATCHING = "stop_watching"
        const val DISABLE_ALIAS = "disable_alias"
        const val STOP_UPDATE_CHECK = "stop_update_check"
        const val STOP_FAILED_DELIVERY_CHECK = "stop_failed_delivery_check"
        const val STOP_DOMAIN_ERROR_CHECK = "stop_domain_error_check"
        const val STOP_API_EXPIRY_CHECK = "stop_api_expiry_check"
        const val STOP_SUBSCRIPTION_EXPIRY_CHECK = "stop_subscription_expiry_check"
        const val STOP_PERIODIC_BACKUPS = "stop_periodic_backups"
        const val DISABLE_WEAROS_QUICK_SETUP = "disable_wearos_quick_setup"
    }

    override fun onReceive(context: Context, intent: Intent) {
        //Toast.makeText(context,"received",Toast.LENGTH_SHORT).show();
        val action = intent.action
        val extra = intent.getStringExtra("extra")
        val notificationID = intent.getIntExtra("notificationID", 0)
        val notificationManager = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        when (action) {
            NOTIFICATIONACTIONS.STOP_WATCHING -> {
                extra?.let {
                    AliasWatcher(context).removeAliasToWatch(it)
                    // Dismiss notification
                    notificationManager.cancel(notificationID)
                }
            }
            NOTIFICATIONACTIONS.DISABLE_ALIAS -> {
                extra?.let {
                    val manageAliasIntent = Intent(context, ManageAliasActivity::class.java)
                    manageAliasIntent.putExtra("alias_id", it)
                    manageAliasIntent.putExtra("shouldDeactivateThisAlias", true)
                    manageAliasIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ContextCompat.startActivity(context, manageAliasIntent, null)
                    // Dismiss notification
                    notificationManager.cancel(notificationID)
                }
            }
            NOTIFICATIONACTIONS.STOP_UPDATE_CHECK -> {
                SettingsManager(false, context).putSettingsBool(SettingsManager.PREFS.NOTIFY_UPDATES, false)
                // Dismiss notification
                notificationManager.cancel(NotificationHelper.UPDATER_NOTIFICATION_ID)
            }

            NOTIFICATIONACTIONS.STOP_FAILED_DELIVERY_CHECK -> {
                SettingsManager(false, context).putSettingsBool(SettingsManager.PREFS.NOTIFY_FAILED_DELIVERIES, false)
                // Dismiss notification
                notificationManager.cancel(NotificationHelper.FAILED_DELIVERIES_NOTIFICATION_ID)
            }

            NOTIFICATIONACTIONS.STOP_API_EXPIRY_CHECK -> {
                SettingsManager(false, context).putSettingsBool(SettingsManager.PREFS.NOTIFY_API_TOKEN_EXPIRY, false)
                // Dismiss notification
                notificationManager.cancel(NotificationHelper.API_KEY_EXPIRE_NOTIFICATION_ID)
            }

            NOTIFICATIONACTIONS.STOP_DOMAIN_ERROR_CHECK -> {
                SettingsManager(false, context).putSettingsBool(SettingsManager.PREFS.NOTIFY_DOMAIN_ERROR, false)
                // Dismiss notification
                notificationManager.cancel(NotificationHelper.DOMAIN_ERROR_NOTIFICATION_ID)
            }

            NOTIFICATIONACTIONS.STOP_SUBSCRIPTION_EXPIRY_CHECK -> {
                SettingsManager(false, context).putSettingsBool(SettingsManager.PREFS.NOTIFY_SUBSCRIPTION_EXPIRY, false)
                // Dismiss notification
                notificationManager.cancel(NotificationHelper.SUBSCRIPTION_EXPIRE_NOTIFICATION_ID)
            }

            NOTIFICATIONACTIONS.STOP_PERIODIC_BACKUPS -> {
                SettingsManager(false, context).putSettingsBool(SettingsManager.PREFS.PERIODIC_BACKUPS, false)
                // Dismiss notification
                notificationManager.cancel(NotificationHelper.FAILED_BACKUP_NOTIFICATION_ID)
            }
            NOTIFICATIONACTIONS.DISABLE_WEAROS_QUICK_SETUP -> {
                SettingsManager(false, context).putSettingsBool(SettingsManager.PREFS.DISABLE_WEAROS_QUICK_SETUP_DIALOG, true)
                // Dismiss notification
                notificationManager.cancel(NotificationHelper.NEW_WEARABLE_PAIRING_REQUEST_NOTIFICATION_ID)
            }
        }
    }

}