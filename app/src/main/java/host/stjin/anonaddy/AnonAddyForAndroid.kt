package host.stjin.anonaddy

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.google.gson.Gson
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.models.UserResourceExtended
import host.stjin.anonaddy_shared.SettingsManager

class AnonAddyForAndroid : Application() {

    lateinit var encryptedSettingsManager: SettingsManager
    var userResource: UserResource
        get() {
            return Gson().fromJson(encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.USER_RESOURCE), UserResource::class.java)
        }
        set(value) {
            encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.USER_RESOURCE, Gson().toJson(value))
        }
    var userResourceExtended: UserResourceExtended
        get() {
            return Gson().fromJson(
                encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.USER_RESOURCE_EXTENDED),
                UserResourceExtended::class.java
            )
        }
        set(value) {
            encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.USER_RESOURCE_EXTENDED, Gson().toJson(value))
        }
    override fun onCreate() {
        super.onCreate()
        val settingsManager = SettingsManager(false, this)
        encryptedSettingsManager = SettingsManager(true, this)

        if (settingsManager.getSettingsBool(SettingsManager.PREFS.DYNAMIC_COLORS)) {
            // Apply dynamic color
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }
}