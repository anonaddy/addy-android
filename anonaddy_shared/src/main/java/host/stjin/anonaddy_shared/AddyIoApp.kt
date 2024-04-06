package host.stjin.anonaddy_shared

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.google.gson.Gson
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.UserAgent
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.models.UserResourceExtended

class AddyIoApp : Application() {

    private lateinit var encryptedSettingsManager: SettingsManager

    // Not nullable, the app should crash if these values are not set. That means something is definitely wrong.
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

    lateinit var userAgent: UserAgent

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