package host.stjin.anonaddy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat.startActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy.utils.GsonTools
import host.stjin.anonaddy.widget.AliasWidget2Provider.AliasWidget2Values.OPEN_APP
import kotlin.random.Random


/**
 * Implementation of App Widget functionality.
 */
class AliasWidget2Provider : AppWidgetProvider() {

    object AliasWidget2Values {
        const val OPEN_APP = "host.stjin.anonaddy.widget.OPEN_APP"
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
            updateAppWidget(context, appWidgetManager, appWidgetId, appWidgetManager.getAppWidgetOptions(appWidgetId))
            amountOfWidgets++
        }

        // Store the amount of widgets
        SettingsManager(false, context).putSettingsInt(SettingsManager.PREFS.WIDGETS_ACTIVE, amountOfWidgets)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (context != null && intent != null) {
            when (intent.action) {
                OPEN_APP -> {
                    val mainIntent = Intent(context, SplashActivity::class.java)
                    mainIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                    startActivity(context, mainIntent, null)
                }
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)

        // Widget got resized or moved. Update the widget and send the new nums as bundle
        context?.let { appWidgetManager?.let { it1 -> updateAppWidget(it, it1, appWidgetId, newOptions) } }
    }

}


private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle? = null) {

    val settingsManager = SettingsManager(true, context)
    val aliasesJson = settingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DATA_ALIASES)
    val aliasesList = aliasesJson?.let { GsonTools.jsonToAliasObject(context, it) }
    var emailsForwarded = 0
    var emailsBlocked = 0
    var emailsSent = 0
    var emailsReplied = 0
    // Count the stats from the cache
    if (aliasesList != null) {
        for (alias in aliasesList) {
            emailsForwarded += alias.emails_forwarded
            emailsBlocked += alias.emails_blocked
            emailsSent += alias.emails_sent
            emailsReplied += alias.emails_replied
        }
    }

    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.widget_2_alias)

    // Widget got resized or moved
    if (newOptions != null) {
        // Width is min 300, show the additional info
        if (newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) > 300) {
            views.setViewVisibility(R.id.widget_aliases_statistics_additional, View.VISIBLE)
            views.setTextViewText(R.id.widget_aliases_statistics_blocked_count, emailsBlocked.toString())
            views.setTextViewText(R.id.widget_aliases_statistics_sent_count, emailsSent.toString())
            views.setTextViewText(R.id.widget_aliases_statistics_replied_count, emailsReplied.toString())
        } else {
            views.setViewVisibility(R.id.widget_aliases_statistics_additional, View.GONE)
        }
    }



    views.setTextViewText(R.id.widget_aliases_statistics_count, emailsForwarded.toString())

    views.setOnClickPendingIntent(android.R.id.background, getPendingSelfIntent(context, OPEN_APP))

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
    val intent = Intent(context, AliasWidget2Provider::class.java)
    intent.action = action
    return PendingIntent.getBroadcast(context, Random.nextInt(0, 999), intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
}