package host.stjin.anonaddy_shared.managers

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE
import androidx.security.crypto.MasterKey.DEFAULT_MASTER_KEY_ALIAS


class SettingsManager(encrypt: Boolean, private val context: Context) {

    enum class PREFTYPES {
        BOOLEAN,
        STRING,
        INT,
        FLOAT,
        STRINGSET
    }

    enum class PREFS(val encrypted: Boolean, val type: PREFTYPES, val key: String) {
        //region Not encrypted
        DARK_MODE(false, PREFTYPES.BOOLEAN, "dark_mode"),
        DYNAMIC_COLORS(false, PREFTYPES.BOOLEAN, "dynamic_colors"),
        STORE_LOGS(false, PREFTYPES.BOOLEAN, "store_logs"),
        VERSION_CODE(false, PREFTYPES.INT, "version_code"),
        BACKGROUND_SERVICE_INTERVAL(false, PREFTYPES.INT, "background_service_interval"),
        WIDGETS_ACTIVE(false, PREFTYPES.INT, "widgets_active"),
        NOTIFY_UPDATES(false, PREFTYPES.BOOLEAN, "notify_updates"),
        PERIODIC_BACKUPS(false, PREFTYPES.BOOLEAN, "periodic_backups"),
        BACKUPS_LOCATION(false, PREFTYPES.STRING, "backups_location"),
        NOTIFY_FAILED_DELIVERIES(false, PREFTYPES.BOOLEAN, "notify_failed_deliveries"),
        MANAGE_MULTIPLE_ALIASES(false, PREFTYPES.BOOLEAN, "manage_multiple_aliases"),
        NOTIFY_API_TOKEN_EXPIRY(false, PREFTYPES.BOOLEAN, "notify_api_token_expiry"),
        NOTIFY_SUBSCRIPTION_EXPIRY(false, PREFTYPES.BOOLEAN, "notify_subscription_expiry"),
        MAILTO_ACTIVITY_SHOW_SUGGESTIONS(false, PREFTYPES.BOOLEAN, "mailto_activity_show_suggestions"),

        // Sorting and Filtering for aliasFragment
        ALIAS_SORT_FILTER(false, PREFTYPES.STRING, "alias_sort_filter"),
        //endregion

        // Encrypted
        BIOMETRIC_ENABLED(true, PREFTYPES.BOOLEAN, "biometric_enabled"),
        PRIVACY_MODE(true, PREFTYPES.BOOLEAN, "privacy_mode"),
        API_KEY(true, PREFTYPES.STRING, "API_KEY"),
        BASE_URL(true, PREFTYPES.STRING, "BASE_URL"),
        RECENT_SEARCHES(true, PREFTYPES.STRINGSET, "recent_searches"),
        BACKUPS_PASSWORD(true, PREFTYPES.STRING, "backups_password"),

        // USER_RESOURCE is also being used by the background service to store the user_resource in
        USER_RESOURCE(true, PREFTYPES.STRING, "user_resource"),
        USER_RESOURCE_EXTENDED(true, PREFTYPES.STRING, "user_resource_extended"),

        //region Wear OS
        WEAROS_SKIP_ALIAS_CREATE_GUIDE(false, PREFTYPES.BOOLEAN, "wearos_skip_alias_create_guide"),
        WEAROS_FAVORITE_ALIASES(true, PREFTYPES.STRINGSET, "wearos_favorite_aliases"),
        DISABLE_WEAROS_QUICK_SETUP_DIALOG(false, PREFTYPES.STRING, "disable_wearos_quick_setup_dialog"),
        SELECTED_WEAROS_DEVICE(false, PREFTYPES.STRING, "selected_wearos_device"),
        BACKGROUND_SERVICE_CACHE_FAVORITE_ALIASES_DATA(true, PREFTYPES.STRING, "cache_favorite_aliases_data"),
        //endregion

        //region Background service
        // Locally stored data
        BACKGROUND_SERVICE_CACHE_MOST_ACTIVE_ALIASES_DATA(true, PREFTYPES.STRING, "cache_most_active_aliases_data"),
        BACKGROUND_SERVICE_CACHE_LAST_UPDATED_ALIASES_DATA(true, PREFTYPES.STRING, "cache_last_updated_aliases_data"),

        // Used for the shimmerview and widget 2
        BACKGROUND_SERVICE_CACHE_DOMAIN_COUNT(true, PREFTYPES.INT, "cache_domain_count"),
        BACKGROUND_SERVICE_CACHE_USER_RESOURCE(true, USER_RESOURCE.type, USER_RESOURCE.key),
        BACKGROUND_SERVICE_CACHE_USERNAME_COUNT(true, PREFTYPES.INT, "cache_username_count"),
        BACKGROUND_SERVICE_CACHE_RULES_COUNT(true, PREFTYPES.INT, "cache_rules_count"),
        BACKGROUND_SERVICE_CACHE_RECIPIENT_COUNT(true, PREFTYPES.INT, "cache_recipient_count"),

        // Also used for background service failed delivery notifications
        BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT(true, PREFTYPES.INT, "cache_failed_deliveries_count"),

        // Used to limit the amount of expiry notifications to 1 a day
        BACKGROUND_SERVICE_CACHE_API_KEY_EXPIRY_LEFT_COUNT(true, PREFTYPES.INT, "cache_api_key_expiry_left_count"),
        BACKGROUND_SERVICE_CACHE_SUBSCRIPTION_EXPIRY_LEFT_COUNT(true, PREFTYPES.INT, "cache_subscription_expiry_left_count"),

        // This value keeps track of the previous amount of failed deliveries so comparisons can be made in the BackgroundWorker
        BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT_PREVIOUS(true, PREFTYPES.INT, "cache_failed_deliveries_count_previous"),

        // When BACKGROUND_SERVICE_CACHE_DATA_ALIASES gets updated the current list will move moved to BACKGROUND_SERVICE_CACHE_DATA_ALIASES_PREVIOUS for the AliasWatcher to compare
        BACKGROUND_SERVICE_WATCH_ALIAS_LIST(true, PREFTYPES.STRINGSET, "background_service_watch_alias_list"),
        BACKGROUND_SERVICE_CACHE_WATCH_ALIAS_DATA(true, PREFTYPES.STRING, "cache_watch_alias_data"),
        BACKGROUND_SERVICE_CACHE_WATCH_ALIAS_DATA_PREVIOUS(true, PREFTYPES.STRING, "cache_watch_alias_data_previous"),
        //endregion
    }

    /*
    This user val is made for possible multiple user support. Defaulting to 1 for now.
     */
    private val user = 1
    val prefs: SharedPreferences = if (!encrypt) {
        PreferenceManager.getDefaultSharedPreferences(context)
    } else {
        val masterKeyAlias = getMasterKey()
        EncryptedSharedPreferences.create(
            context,
            "host.stjin.anonaddy_enc_user$user",
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }


    private fun getMasterKey(): MasterKey {
        // this is equivalent to using deprecated MasterKeys.AES256_GCM_SPEC
        val spec = KeyGenParameterSpec.Builder(
            DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(DEFAULT_AES_GCM_MASTER_KEY_SIZE)
            .build()

        return MasterKey.Builder(context)
            .setKeyGenParameterSpec(spec)
            .build()
    }

    fun putSettingsBool(key: PREFS, boolean: Boolean) {
        prefs.edit().putBoolean(key.key, boolean).apply()
    }

    fun getSettingsBool(key: PREFS, default: Boolean = false): Boolean {
        return prefs.getBoolean(key.key, default)
    }

    fun putSettingsString(key: PREFS, string: String) {
        prefs.edit().putString(key.key, string).apply()
    }

    fun getSettingsString(key: PREFS): String? {
        return prefs.getString(key.key, null)
    }

    fun putSettingsInt(key: PREFS, int: Int) {
        prefs.edit().putInt(key.key, int).apply()
    }

    fun getSettingsInt(key: PREFS, default: Int = 0): Int {
        return prefs.getInt(key.key, default)
    }

    fun putSettingsFloat(key: PREFS, float: Float) {
        prefs.edit().putFloat(key.key, float).apply()
    }

    fun getSettingsFloat(key: PREFS): Float {
        return prefs.getFloat(key.key, 0f)
    }

    fun putStringSet(key: PREFS, mutableset: MutableSet<String>) {
        prefs.edit().remove(key.key).apply()
        prefs.edit().putStringSet(key.key, mutableset).apply()
    }

    fun getStringSet(key: PREFS): MutableSet<String>? {
        return prefs.getStringSet(key.key, HashSet())
    }


    fun removeSetting(value: PREFS) {
        prefs.edit().remove(value.key).apply()
    }

    fun clearAllData() {
        SettingsManager(true, context).prefs.edit().clear().apply()
        SettingsManager(false, context).prefs.edit().clear().apply()
    }


    /*
    Clears all the settings and closes the app
     */

    fun clearSettingsAndCloseApp() {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
    }
}
