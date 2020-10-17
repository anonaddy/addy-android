package host.stjin.anonaddy

import android.app.ActivityManager
import android.content.Context
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys


class SettingsManager(encrypt: Boolean, private val context: Context) {
    enum class PREFS(val filename: String) {
        DARK_MODE("dark_mode"),
        STORE_LOGS("store_logs"),
        VERSION_CODE("version_code"),
        SHOW_BETA_FEATURES("show_beta_features"),

        // Encrypted
        BIOMETRIC_ENABLED("biometric_enabled"),
        API_KEY("API_KEY"),
        BASE_URL("BASE_URL"),
        RECENT_SEARCHES("recent_searches"),
        STAT_CURRENT_EMAILS_FORWARDED_TOTAL_COUNT("stat_current_emails_forwarded_total_count"),
        STAT_CURRENT_EMAILS_BLOCKED_TOTAL_COUNT("stat_current_emails_blocked_total_count"),
        STAT_CURRENT_EMAILS_REPLIED_TOTAL_COUNT("stat_current_emails_replied_total_count"),
        STAT_CURRENT_EMAILS_SENT_TOTAL_COUNT("stat_current_emails_sent_total_count")
    }

    /*
    This user val is made for possible multiple user support. Defaulting to 1 for now.
     */
    private val user = 1
    private val prefs = if (!encrypt) {
        PreferenceManager.getDefaultSharedPreferences(context)
    } else {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "host.stjin.anonaddy_enc_user$user",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun putSettingsBool(key: PREFS, boolean: Boolean) {
        prefs.edit().putBoolean(key.filename, boolean).apply()
    }

    fun getSettingsBool(key: PREFS): Boolean {
        return prefs.getBoolean(key.filename, false)
    }

    fun putSettingsString(key: PREFS, string: String) {
        prefs.edit().putString(key.filename, string).apply()
    }

    fun getSettingsString(key: PREFS): String? {
        return prefs.getString(key.filename, null)
    }

    fun putSettingsInt(key: PREFS, int: Int) {
        prefs.edit().putInt(key.filename, int).apply()
    }

    fun getSettingsInt(key: PREFS, default: Int = 0): Int {
        return prefs.getInt(key.filename, default)
    }

    fun putSettingsFloat(key: PREFS, float: Float) {
        prefs.edit().putFloat(key.filename, float).apply()
    }

    fun getSettingsFloat(key: PREFS): Float {
        return prefs.getFloat(key.filename, 0f)
    }

    fun putStringSet(key: PREFS, mutableset: MutableSet<String>) {
        prefs.edit().remove(key.filename).apply()
        prefs.edit().putStringSet(key.filename, mutableset).apply()
    }

    fun getStringSet(key: PREFS): MutableSet<String>? {
        return prefs.getStringSet(key.filename, HashSet())
    }

    fun removeSetting(value: PREFS) {
        prefs.edit().remove(value.filename).apply()
    }


    /*
    Clears all the settings and closes the app
     */

    fun clearSettingsAndCloseApp() {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
    }
}
