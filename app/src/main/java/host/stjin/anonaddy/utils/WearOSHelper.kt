package host.stjin.anonaddy.utils

import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.WearOSSettings

object WearOSHelper {
    fun createWearOSConfiguration(): WearOSSettings? {
        val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
        val baseUrl = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.BASE_URL)
        val apiKey = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.API_KEY)

        return if (baseUrl != null && apiKey != null) {
            WearOSSettings(
                base_url = baseUrl,
                api_key = apiKey
            )
        } else {
            null
        }
    }
}