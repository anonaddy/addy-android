package host.stjin.anonaddy.utils

import android.content.Context
import android.content.Intent
import android.view.View
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.utils.LoggingHelper


object SnackbarHelper {
    fun createSnackbar(
        context: Context,
        text: String,
        view: View,
        showLogs: LoggingHelper.LOGFILES? = null,
        length: Int = Snackbar.LENGTH_SHORT,
        allowSwipeDismiss: Boolean = true
    ): Snackbar {
        val snackbar = Snackbar.make(
            view,
            text,
            length
        )

        if (!allowSwipeDismiss) {
            snackbar.behavior = NoSwipeBehavior()
        }
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

internal class NoSwipeBehavior : BaseTransientBottomBar.Behavior() {
    override fun canSwipeDismissView(child: View): Boolean {
        return false
    }
}