package host.stjin.anonaddy_shared.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy_shared.R
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.Logs
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*


class LoggingHelper(private val context: Context, sharedPreference: LOGFILES = LOGFILES.DEFAULT) {
    private val prefs = context.getSharedPreferences(sharedPreference.filename, 0)
    private val settingsManager = SettingsManager(false, context)

    enum class LOGFILES(val filename: String) {
        DEFAULT("host.stjin.anonaddy_logs"),
        BACKUP_LOGS("host.stjin.anonaddy_logs_backups")
    }

    private fun <Logs> setList(list: ArrayList<Logs>?) {
        val gson = Gson()
        // Only save the 100 last results on saving to prevent more than 100 logs to be stored
        val json = gson.toJson(list?.takeLast(100))
        set("logs", json)
    }

    operator fun set(key: String?, value: String?) {
        if (prefs != null) {
            val prefsEditor: SharedPreferences.Editor = prefs.edit()
            prefsEditor.putString(key, value)
            prefsEditor.apply()
        }
    }

    fun getLogs(): ArrayList<Logs>? {
        if (prefs != null) {
            try {
                val gson = Gson()
                var logsList: ArrayList<Logs> = arrayListOf()
                val string: String? = prefs.getString("logs", null)
                val type: Type = object : TypeToken<ArrayList<Logs?>?>() {}.type
                if (string != null) {
                    logsList = gson.fromJson(string, type)
                }
                return logsList
            } catch (e: Exception) {
                clearLogs()
                addLog(LOGIMPORTANCE.WARNING.int, context.resources.getString(R.string.logs_reset_due_to_error), "getLogs()", null)
            }

        }
        return null
    }

    fun addLog(importance: Int, error: String, method: String, extra: String?) {
        if (settingsManager.getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
            val logs = getLogs()
            logs?.add(
                Logs(
                    importance = importance,
                    dateTime = getDateTime(),
                    method = method,
                    message = error,
                    extra = extra
                )
            )
            setList(logs)
        }
    }


    fun clearLogs() {
        prefs.edit().clear().apply()
        addLog(LOGIMPORTANCE.INFO.int, context.resources.getString(R.string.logs_cleared), "getLogs()", null)
    }


    private fun getDateTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }


}
