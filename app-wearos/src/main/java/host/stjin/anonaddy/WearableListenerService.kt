package host.stjin.anonaddy

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import host.stjin.anonaddy_shared.SettingsManager


class WearableListenerService : WearableListenerService() {

    /*
    Messages are delivered to connected network nodes. A message is considered successful if it has been queued for delivery to the specified node.
    A message will only be queued if the specified node is connected
     */
    override fun onMessageReceived(p0: MessageEvent) {
        super.onMessageReceived(p0)
        when {
            p0.path.equals("/start") -> {
                val intent = Intent(this@WearableListenerService, SplashActivity::class.java)
                intent.flags = FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            p0.path.equals("/setup") -> {
                storeSettings(String(p0.data))

                val intent = Intent(this@WearableListenerService, SplashActivity::class.java)
                intent.flags = FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            p0.path.equals("/reset") -> {
                Toast.makeText(this, this.resources.getString(R.string.app_reset_requested_by, String(p0.data)),Toast.LENGTH_LONG).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    (this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                }, 2500)
            }
        }
    }

    private fun storeSettings(wearOSConfiguration: String?) {
        // Write configuration to settings
        wearOSConfiguration?.let { SettingsManager(true, this).putSettingsString(SettingsManager.PREFS.WEAROS_CONFIGURATION, it) }
    }
}