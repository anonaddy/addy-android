package host.stjin.anonaddy.service

import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import host.stjin.anonaddy.R
import host.stjin.anonaddy.notifications.NotificationHelper
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.LoggingHelper

class WearableListener : WearableListenerService() {
    override fun onMessageReceived(p0: MessageEvent) {
        super.onMessageReceived(p0)
        if (p0.path.equals("/requestsetup")) {
            LoggingHelper(this).addLog(
                LOGIMPORTANCE.INFO.int,
                this.resources.getString(R.string.log_wearable_requested, String(p0.data)),
                "WearableListener:onMessageReceived()",
                null
            )


            if (!SettingsManager(false, this).getSettingsBool(SettingsManager.PREFS.DISABLE_WEAROS_QUICK_SETUP_DIALOG)) {
                val notificationHelper = NotificationHelper(this)

                if (SettingsManager(true, this).getSettingsString(SettingsManager.PREFS.API_KEY) == null) {
                    // The phone-app has not been setup, let the user know the main app needs to be setup first
                    notificationHelper.createSetupAppFirstNotification()
                } else {
                    //Figure out which node requested this setup and show notification
                    notificationHelper.createSetupWearableAppNotification(
                        p0
                    )
                }
            }


        } else if (p0.path.equals("/showAlias")) {
            val intent = Intent(this, ManageAliasActivity::class.java)
            intent.putExtra("alias_id", String(p0.data))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }
}