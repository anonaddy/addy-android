package host.stjin.anonaddy

import android.app.Application
import com.google.gson.Gson
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.WearOSSettings

class AnonAddyForWearOS : Application() {

    lateinit var encryptedSettingsManager: SettingsManager
    var wearOSSettings: WearOSSettings?
        get() {
            return Gson().fromJson(encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.WEAROS_CONFIGURATION), WearOSSettings::class.java)
        }
        set(value) {
            encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.WEAROS_CONFIGURATION, Gson().toJson(value))
        }

    override fun onCreate() {
        super.onCreate()
        encryptedSettingsManager = SettingsManager(true, this)
    }
}