package host.stjin.anonaddy.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import host.stjin.anonaddy.R
import host.stjin.anonaddy.components.ErrorScreen
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.alias.AliasActivity
import host.stjin.anonaddy_shared.AddyIo.API_BASE_URL
import host.stjin.anonaddy_shared.controllers.LauncherIconController
import host.stjin.anonaddy_shared.managers.SettingsManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadSettings()

        LauncherIconController(this).tryFixLauncherIconIfNeeded()
    }


    private fun loadSettings() {

        /*
        The load procedure in the Wear OS device works a bit different than the handheld app.
        The goal of a watch app is to quickly give the user access to information without having to wait
        for resources to be loaded. In the case of the Watch OS app, only the existence of the API key is checked
        The check if the information is valid as well as retrieving any userResource information is being done on the background while the user
        is using the app
         */

        // This is prone to fail when users have restored the app data from any restore app as the
        // encryption key has changed. So we catch this once in the app and that's at launch
        val encryptedSettingsManager = try {
            SettingsManager(true, this)
        } catch (e: Exception) {
            null
        }

        if (encryptedSettingsManager == null) {
            setTheme(R.style.AppTheme)
            setContent {
                ErrorScreen(this, this.resources.getString(R.string.app_data_corrupted))
            }
            Handler(Looper.getMainLooper()).postDelayed({
                // Clear settings
                SettingsManager(false, this).clearSettingsAndCloseApp()
            }, 15000)
            return
        }


        if (encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.API_KEY) == null) {
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Schedule the background worker (in case this has not been done before) (this will cancel if already scheduled)
            BackgroundWorkerHelper(this).scheduleBackgroundWorker()

            /**
             * MIGRATE FROM APP.ANONADDY.COM TO APP.ADDY.IO
             */
            //migrateFromAnonAddyToAddyIo() // TODO ENABLE

            val intent = Intent(this, AliasActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun migrateFromAnonAddyToAddyIo() {

        val encryptedSettingsManager = SettingsManager(true, this)

        val baseUrl = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.BASE_URL)
        if (baseUrl == "https://app.anonaddy.com") {
            // Change baseUrl to app.addy.io
            encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.BASE_URL, API_BASE_URL)
        }
    }

}