package host.stjin.anonaddy.utils

import android.app.Activity
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.WearOSSettings

class WearOSHelper(private val activity: Activity) {
    fun createWearOSConfiguration(): WearOSSettings? {
        val settingsManager = SettingsManager(true, activity)
        val baseUrl = settingsManager.getSettingsString(SettingsManager.PREFS.BASE_URL)
        val apiKey = settingsManager.getSettingsString(SettingsManager.PREFS.API_KEY)

        return if (baseUrl != null && apiKey != null) {
            WearOSSettings(
                base_url = baseUrl,
                api_key = apiKey)
        } else {
            null
        }
    }
}