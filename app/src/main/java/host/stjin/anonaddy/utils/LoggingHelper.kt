package host.stjin.anonaddy.utils

import android.content.Context
import host.stjin.anonaddy.SettingsManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet

class LoggingHelper(context: Context) {
    private val prefs = context.getSharedPreferences("host.stjin.anonaddy_logs", 0)
    private val settingsManager = SettingsManager(false, context)

    fun addLog(error: String, method: String) {
        if (settingsManager.getSettingsBool("store_logs")) {
            val logs = getLogs()
            logs.add("${getDateTime()} | $method | $error")
            putLogs(logs)
        }
    }

    fun getLogs(): MutableSet<String> {
        return prefs.getStringSet("logs", HashSet())!!
    }

    private fun putLogs(logs: MutableSet<String>) {
        prefs.edit().putStringSet("logs", logs).apply()
    }

    fun clearLogs() {
        prefs.edit().clear().apply()
    }


    private fun getDateTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }


}
