package host.stjin.anonaddy_shared.utils

import android.content.Context
import androidx.core.content.edit
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy_shared.ServiceLocator
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Logs
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LoggingHelper(private val context: Context, sharedPreference: LOGFILES = LOGFILES.DEFAULT) {
    private val prefs = context.getSharedPreferences(sharedPreference.filename, 0)
    private val settingsManager = ServiceLocator().apply { init(context) }.settingsManager

    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    enum class LOGFILES(val filename: String) {
        DEFAULT("host.stjin.anonaddy_logs"),
        BACKUP_LOGS("host.stjin.anonaddy_logs_backups"),
        WEAROS_LOGS("host.stjin.anonaddy_logs_wearos")
    }

    private fun setList(list: ArrayList<Logs>?) {
        // Only save the 100 last results on saving to prevent more than 100 logs to be stored
        val json = GsonTools.gson.toJson(list?.takeLast(100))
        set("logs", json)
    }

    operator fun set(key: String?, value: String?) {
        prefs.edit {
            putString(key, value)
        }
    }

    fun getLogs(): ArrayList<Logs> {
        return try {
            val string: String? = prefs.getString("logs", null)
            val type: Type = object : TypeToken<ArrayList<Logs>>() {}.type
            var logsList: ArrayList<Logs> = arrayListOf()
            if (string != null) {
                logsList = GsonTools.gson.fromJson(string, type)
            }
            logsList
        } catch (_: Exception) {
            prefs.edit { clear() }
            arrayListOf()
        }
    }

    fun addLog(importance: Int, error: String, method: String, extra: String?) {
        if (settingsManager.getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
            val logs = getLogs()
            logs.add(
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
        prefs.edit { clear() }
    }

    private fun getDateTime(): String {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER)
    }
}
