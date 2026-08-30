package host.stjin.anonaddy_shared

import android.app.Application
import com.google.android.material.color.DynamicColors
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.UserAgent
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.models.UserResourceExtended

import host.stjin.anonaddy_shared.utils.GsonTools

open class AddyIoApp : Application() {

    protected val serviceLocator: ServiceLocator by lazy {
        ServiceLocator().apply { init(this@AddyIoApp) }
    }

    private val encryptedSettingsManager: SettingsManager by lazy { serviceLocator.encryptedSettingsManager }
    private val settingsManager: SettingsManager by lazy { serviceLocator.settingsManager }

    private var cachedUserResource: UserResource? = null
    private var cachedUserResourceExtended: UserResourceExtended? = null
    private val gson = GsonTools.gson

    val userResourceOrNull: UserResource?
        get() {
            if (cachedUserResource == null) {
                val json = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.USER_RESOURCE)
                cachedUserResource = if (!json.isNullOrEmpty()) gson.fromJson(json, UserResource::class.java) else null
            }
            return cachedUserResource
        }

    val userResourceExtendedOrNull: UserResourceExtended?
        get() {
            if (cachedUserResourceExtended == null) {
                val json = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.USER_RESOURCE_EXTENDED)
                cachedUserResourceExtended = if (!json.isNullOrEmpty()) gson.fromJson(json, UserResourceExtended::class.java) else null
            }
            return cachedUserResourceExtended
        }

    // Retained for backward compatibility
    var userResource: UserResource
        get() {
            return userResourceOrNull ?: throw IllegalStateException("UserResource is not initialized yet")
        }
        set(value) {
            cachedUserResource = value
            encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.USER_RESOURCE, gson.toJson(value))
        }

    var userResourceExtended: UserResourceExtended
        get() {
            return userResourceExtendedOrNull ?: throw IllegalStateException("UserResourceExtended is not initialized yet")
        }
        set(value) {
            cachedUserResourceExtended = value
            encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.USER_RESOURCE_EXTENDED, gson.toJson(value))
        }

    lateinit var userAgent: UserAgent

    override fun onCreate() {
        super.onCreate()

        // set userAgent by default (in case splashActivity has not set it yet)
        // This would happen on direct (eg. widget) actions where splashActivity gets skipped
        val packageName = applicationContext.packageName
        val packageInfo = applicationContext.packageManager.getPackageInfo(packageName, 0)
        val version = packageInfo.versionName
        val versionCode = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(packageInfo).toInt()

        userAgent = UserAgent(
            userAgentApplicationID = packageName,
            userAgentVersion = version.toString(),
            userAgentVersionCode = versionCode,
            userAgentApplicationBuildType = BuildConfig.BUILD_TYPE
        )

        DynamicColors.applyToActivitiesIfAvailable(
            this,
            com.google.android.material.color.DynamicColorsOptions.Builder()
                .setPrecondition { _, _ ->
                    settingsManager.getSettingsBool(SettingsManager.PREFS.DYNAMIC_COLORS)
                }
                .build()
        )
    }
}