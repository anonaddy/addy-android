package host.stjin.anonaddy.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy.ui.alias.ManageAliasActivity
import host.stjin.anonaddy_shared.controllers.LauncherIconController
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.utils.GsonTools


class WearableListenerService : WearableListenerService() {

    /*
    Messages are delivered to connected network nodes. A message is considered successful if it has been queued for delivery to the specified node.
    A message will only be queued if the specified node is connected
     */
    override fun onMessageReceived(p0: MessageEvent) {
        super.onMessageReceived(p0)
        when (p0.path) {
            "/start" -> {
                val intent = Intent(this@WearableListenerService, SplashActivity::class.java)
                intent.flags = FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            "/setup" -> {
                // a serialized wearOSConfiguration is being passed containing the API_KEY and the BASE_URL
                storeSettings(String(p0.data))
            }
            "/reset" -> {
                Toast.makeText(this, this.resources.getString(R.string.app_reset_requested_by, String(p0.data)), Toast.LENGTH_LONG).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    (this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                }, 2500)
            }
            "/showAlias" -> {
                val intent = Intent(this@WearableListenerService, ManageAliasActivity::class.java)
                intent.putExtra("alias", String(p0.data))
                intent.flags = FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            "/setIcon" -> {
                val iconToSet = LauncherIconController.LauncherIcon.entries.firstOrNull { it.key == String(p0.data) }
                if (iconToSet != null) {
                    LauncherIconController(this).setIcon(iconToSet)
                }
            }
        }
    }

    private fun storeSettings(wearOSConfiguration: String) {
        // Deserialize the configuration
        val configuration = GsonTools.jsonToWearOSSettingsObject(this, wearOSConfiguration)

        if (configuration != null) {
            // If the configuration is valid, set the API_KEY and BASE_URL to encrypted sharedpref
            val encryptedSettingsManager = SettingsManager(true, this)
            encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.API_KEY, configuration.api_key)
            encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.BASE_URL, configuration.base_url)
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } else {
            // Configuration could not be parsed thus is not valid
            Toast.makeText(this, this.resources.getString(R.string.app_configuration_not_valid),Toast.LENGTH_LONG).show()
        }
    }
}