package host.stjin.anonaddy.utils

import android.content.Context
import host.stjin.anonaddy.SettingsManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet

class LoggingHelper(context: Context, sharedPreference: LOGFILES = LOGFILES.DEFAULT) {
    private val prefs = context.getSharedPreferences(sharedPreference.filename, 0)
    private val settingsManager = SettingsManager(false, context)

    enum class LOGFILES(val filename: String) {
        DEFAULT("host.stjin.anonaddy_logs"),
        BACKUP_LOGS("host.stjin.anonaddy_logs_backups")
    }

    // TODO Add warninglevel?
    fun addLog(error: String, method: String, body: String?) {
        if (settingsManager.getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
            val logs = getLogs()
            logs.add("${getDateTime()} | $method | $error | $body")
            putLogs(logs)
        }
    }

    fun getLogs(): MutableSet<String> {
        return prefs.getStringSet("logs", HashSet())!!
    }

    private fun putLogs(logs: MutableSet<String>) {
        // Clear logs first (weird bug)
        // TODO check if this is fixed
        clearLogs()
        prefs.edit().putStringSet("logs", logs).apply()
    }

    fun clearLogs() {
        prefs.edit().clear().apply()
    }


    private fun getDateTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }


}
