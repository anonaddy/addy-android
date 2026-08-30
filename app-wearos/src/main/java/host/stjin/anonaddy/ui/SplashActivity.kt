package host.stjin.anonaddy.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.components.ErrorScreen
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.aliases.AliasesActivity
import host.stjin.anonaddy_shared.AddyIo.API_BASE_URL
import host.stjin.anonaddy_shared.controllers.LauncherIconController
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.utils.NetworkUtils
import kotlinx.coroutines.launch

import host.stjin.anonaddy.ui.base.BaseComponentActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseComponentActivity() {

    override fun requiresSetup(): Boolean = false

    private var localNetworkPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission granted, proceed to app
            startApp()
        } else {
            // Permission denied, show error screen
            setTheme(R.style.AppTheme)
            setContent {
                ErrorScreen(this@SplashActivity, this@SplashActivity.resources.getString(R.string.local_network_permission_required))
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

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
            ServiceLocator.encryptedSettingsManager
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
                ServiceLocator.settingsManager.clearSettingsAndCloseApp()
            }, 15000)
            return
        }


        if (encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.API_KEY) == null) {
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Proactive local network permission check for existing installs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                lifecycleScope.launch {
                    if (NetworkUtils.isLocalAddressRobust(API_BASE_URL)) {
                        if (ContextCompat.checkSelfPermission(this@SplashActivity, Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
                            localNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                        } else {
                            startApp()
                        }
                    } else {
                        startApp()
                    }
                }
            } else {
                startApp()
            }
        }
    }

    private fun startApp() {
        // Schedule the background worker (in case this has not been done before) (this will cancel if already scheduled)
        BackgroundWorkerHelper(this).scheduleBackgroundWorker()

        val intent = Intent(this, AliasesActivity::class.java)
        startActivity(intent)
        finish()
    }

}