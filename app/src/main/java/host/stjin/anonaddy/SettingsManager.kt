package host.stjin.anonaddy

import android.app.ActivityManager
import android.content.Context
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys


class SettingsManager(encrypt: Boolean, private val context: Context) {
    //dark_mode
//biometric_enabled
    //store_logs
    private val prefs = if (encrypt) {
        PreferenceManager.getDefaultSharedPreferences(context)
    } else {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "host.stjin.anonaddy_enc",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // TODO consider method overloading

    fun putSettingsBool(key: String, boolean: Boolean) {
        prefs.edit().putBoolean(key, boolean).apply()
    }

    fun getSettingsBool(key: String): Boolean {
        return prefs.getBoolean(key, false)
    }

    fun putSettingsString(key: String, string: String) {
        prefs.edit().putString(key, string).apply()
    }

    fun getSettingsString(key: String): String? {
        return prefs.getString(key, null)
    }

    fun putSettingsInt(key: String, int: Int) {
        prefs.edit().putInt(key, int).apply()
    }

    fun getSettingsInt(key: String, default: Int = 0): Int {
        return prefs.getInt(key, 0)
    }

    fun putSettingsFloat(key: String, float: Float) {
        prefs.edit().putFloat(key, float).apply()
    }

    fun getSettingsFloat(key: String): Float {
        return prefs.getFloat(key, 0f)
    }


    fun putStringSet(key: String, mutableset: MutableSet<String>) {
        prefs.edit().remove(key).apply()
        prefs.edit().putStringSet(key, mutableset).apply()
    }

    fun getStringSet(key: String): MutableSet<String>? {
        return prefs.getStringSet(key, HashSet())
    }


    /*
    Clears all the settings and closes the app
     */
    fun clearSettings() {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
    }
}
