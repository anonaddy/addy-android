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


        // set userAgent by default (in case splashActivity has not set it yet)
        // This would happen on direct (eg. widget) actions where splashActivity gets skipped
        val packageName = applicationContext.packageName // need to put this line
        val version = applicationContext.packageManager.getPackageInfo(packageName, 0).versionName
        val versionCode = applicationContext.packageManager.getPackageInfo(packageName, 0).versionCode

        userAgent = UserAgent(
            userAgentApplicationID = packageName,
            userAgentVersion = version.toString(),
            userAgentVersionCode = versionCode,
            userAgentApplicationBuildType = BuildConfig.BUILD_TYPE
        )

        if (settingsManager.getSettingsBool(SettingsManager.PREFS.DYNAMIC_COLORS)) {
            // Apply dynamic color
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }
}