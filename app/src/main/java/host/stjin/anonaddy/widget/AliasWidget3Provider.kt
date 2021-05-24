package host.stjin.anonaddy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.widget.AliasWidget3Provider.AliasWidget3Values.COPY_ACTION
import host.stjin.anonaddy.widget.AliasWidget3Provider.AliasWidget3Values.NAVIGATE
import host.stjin.anonaddy.widget.AliasWidget3Provider.AliasWidget3Values.OPEN_ACTION

/**
 * Implementation of App Widget functionality.
 */
class AliasWidget3Provider : AppWidgetProvider() {

    object AliasWidget3Values {
        const val COPY_ACTION = "host.stjin.anonaddy.widget.COPY_ACTION"
        const val OPEN_ACTION = "host.stjin.anonaddy.widget.OPEN_ACTION"
        const val NAVIGATE = "host.stjin.anonaddy.widget.NAVIGATE"
    }

    /*
    Called in response to the AppWidgetManager#ACTION_APPWIDGET_DISABLED broadcast,
    which is sent when the last AppWidget instance for this provider is deleted. Override this method to implement your own AppWidget functionality.
     */
    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        // Since this might be the last widget is that is removed, call scheduleBackgroundWorker. This method will remove the BackgroundWorker and reschedule if its still required
        context?.let { BackgroundWorkerHelper(it).scheduleBackgroundWorker() }
    }


    override fun onEnabled(context: Context?) {
        super.onEnabled(context)

        // Set widgets to 1 (onEnabled gets called if this is the first widget) to allow the backgroundworker to be scheduled
        context?.let { SettingsManager(false, it).putSettingsInt(SettingsManager.PREFS.WIDGETS_ACTIVE, 1) }
        // Since a widget was added, call scheduleBackgroundWorker. This method will Schedule if its still required
        context?.let { BackgroundWorkerHelper(it).scheduleBackgroundWorker() }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {

        if (BuildConfig.DEBUG) {
            println("onUpdate() called")
        }

        // There may be multiple widgets active, so update all of them
        var amountOfWidgets = 0
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
            amountOfWidgets++
        }

        // Store the amount of widgets
        SettingsManager(false, context).putSettingsInt(SettingsManager.PREFS.WIDGETS_ACTIVE, amountOfWidgets)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (context != null && intent != null) {
            when (intent.action) {
                NAVIGATE -> {
                    if (intent.hasExtra(COPY_ACTION)) {
                        val alias = intent.getStringExtra(COPY_ACTION)
                        val clipboard: ClipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("alias", alias)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, context.resources.getString(R.string.copied_alias), Toast.LENGTH_LONG).show()
                    } else if (intent.hasExtra(OPEN_ACTION)) {
                        val manageAliasIntent = Intent(context, ManageAliasActivity::class.java)
                        manageAliasIntent.putExtra("alias_id", intent.getStringExtra(OPEN_ACTION))
                        manageAliasIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                        startActivity(context, manageAliasIntent, null)
                    }
                }
            }
        }
    }

}


private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.widget_3_alias)

    val intent = Intent(context, AliasWidget3RemoteViewsService::class.java)
    views.setRemoteAdapter(R.id.widget_3_alias_list_view, intent)


    val clickIntent = Intent(context, AliasWidget3Provider::class.java)
    clickIntent.action = NAVIGATE
    clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    clickIntent.data = Uri.parse(clickIntent.toUri(Intent.URI_INTENT_SCHEME))

    val onClickPendingIntent = PendingIntent
        .getBroadcast(
            context, appWidgetId, clickIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    views.setPendingIntentTemplate(R.id.widget_3_alias_list_view, onClickPendingIntent)

    // Tell every widget there is new data for the listview
    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_3_alias_list_view)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}