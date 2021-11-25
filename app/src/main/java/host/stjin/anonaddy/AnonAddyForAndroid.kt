package host.stjin.anonaddy

import android.app.Application
import com.google.android.material.color.DynamicColors
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.models.UserResourceExtended

class AnonAddyForAndroid : Application() {
    override fun onCreate() {
        super.onCreate()

        val settingsManager = SettingsManager(false, this)
        if (settingsManager.getSettingsBool(SettingsManager.PREFS.DYNAMIC_COLORS)) {
            // Apply dynamic color
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }

    companion object User {
        lateinit var userResource: UserResource
        lateinit var userResourceExtended: UserResourceExtended
    }
}