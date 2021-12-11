package host.stjin.anonaddy.utils

import android.content.Context
import android.content.Intent
import android.view.View
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity

object SnackbarHelper {
    fun createSnackbar(
        context: Context,
        text: String,
        view: View,
        showLogs: LoggingHelper.LOGFILES? = null,
        length: Int = Snackbar.LENGTH_SHORT
    ): Snackbar {
        val snackbar = Snackbar.make(
            view,
            text,
            length
        )
        if (showLogs != null && SettingsManager(false, context).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
            snackbar.setAction(R.string.logs) {
                val intent = Intent(context, LogViewerActivity::class.java)
                intent.putExtra("logfile", showLogs.filename)
                context.startActivity(intent)
            }
        }
        return snackbar
    }
}