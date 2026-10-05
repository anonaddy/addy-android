package host.stjin.anonaddy.ui.base

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy_shared.managers.SettingsManager

abstract class BaseComponentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requiresSetup()) {
            val apiKey = try {
                ServiceLocator.encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.API_KEY)
            } catch (e: Exception) {
                null
            }
            if (apiKey == null) {
                // App not setup, open splash
                val intent = Intent(this, SplashActivity::class.java)
                startActivity(intent)
                finish()
                return
            }
        }
    }

    open fun requiresSetup(): Boolean = true
}
