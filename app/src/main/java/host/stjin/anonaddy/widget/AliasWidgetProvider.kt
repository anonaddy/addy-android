package host.stjin.anonaddy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.widget.AliasWidgetProvider.AliasWidgetValues.COPY_ACTION
import host.stjin.anonaddy.widget.AliasWidgetProvider.AliasWidgetValues.NAVIGATE
import host.stjin.anonaddy.widget.AliasWidgetProvider.AliasWidgetValues.OPEN_ACTION
import host.stjin.anonaddy.widget.AliasWidgetProvider.AliasWidgetValues.OPEN_APP
import host.stjin.anonaddy.widget.AliasWidgetProvider.AliasWidgetValues.REFRESH_ACTION


/**
 * Implementation of App Widget functionality.
 */
class AliasWidgetProvider : AppWidgetProvider() {

    object AliasWidgetValues {
        val REFRESH_ACTION = "host.stjin.anonaddy.widget.REFRESH_ACTION"
        val COPY_ACTION = "host.stjin.anonaddy.widget.COPY_ACTION"
        val OPEN_ACTION = "host.stjin.anonaddy.widget.OPEN_ACTION"
        val OPEN_APP = "host.stjin.anonaddy.widget.OPEN_APP"
        val NAVIGATE = "host.stjin.anonaddy.widget.NAVIGATE"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (intent != null) {
            println("ONRECEIVE ${intent.action}")
        }

        if (context != null && intent != null) {
            when {
                REFRESH_ACTION == intent.action -> {
                    onUpdate(context)
                }
                OPEN_APP == intent.action -> {
                    val mainIntent = Intent(context, SplashActivity::class.java)
                    mainIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                    startActivity(context, mainIntent, null)
                }
                NAVIGATE == intent.action -> {
                    if (intent.hasExtra(COPY_ACTION)) {
                        val alias = intent.getStringExtra(COPY_ACTION)
                        val clipboard: ClipboardManager? =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("alias", alias)
                        clipboard?.setPrimaryClip(clip)
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

    private fun onUpdate(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisAppWidgetComponentName = ComponentName(context.packageName, AliasWidgetProvider::class.java.name)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_alias_list_view)
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

}


internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.alias_widget)

    val intent = Intent(context, AliasWidgetRemoteViewsService::class.java)
    views.setRemoteAdapter(R.id.widget_alias_list_view, intent)


    val clickIntent = Intent(context, AliasWidgetProvider::class.java)
    clickIntent.action = NAVIGATE
    clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    clickIntent.data = Uri.parse(clickIntent.toUri(Intent.URI_INTENT_SCHEME))

    val onClickPendingIntent = PendingIntent
        .getBroadcast(
            context, appWidgetId, clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

    views.setPendingIntentTemplate(R.id.widget_alias_list_view, onClickPendingIntent)
    views.setOnClickPendingIntent(R.id.widget_aliases_listview_list_refresh, getPendingSelfIntent(context, REFRESH_ACTION))
    views.setOnClickPendingIntent(R.id.widget_aliases_listview_list_open_app, getPendingSelfIntent(context, OPEN_APP))

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
    val intent = Intent(context, AliasWidgetProvider::class.java)
    intent.action = action
    return PendingIntent.getBroadcast(context, 0, intent, 0)
}