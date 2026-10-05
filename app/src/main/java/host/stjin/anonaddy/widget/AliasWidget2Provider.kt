package host.stjin.anonaddy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.net.toUri
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.ActivityTargets
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy.ui.aliases.manage.ManageAliasActivity
import host.stjin.anonaddy.widget.AliasWidget2Provider.AliasWidget2Values.NAVIGATE
import host.stjin.anonaddy.widget.AliasWidget2Provider.AliasWidget2Values.OPEN_APP
import host.stjin.anonaddy.widget.AliasWidget2Provider.AliasWidget2Values.OPEN_APP_ADD_ALIAS_SHEET
import host.stjin.anonaddy.widget.AliasWidget2Provider.AliasWidget2Values.OPEN_APP_TARGET
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.utils.CacheHelper
import kotlin.random.Random


/**
 * Implementation of App Widget functionality.
 */
class AliasWidget2Provider : AppWidgetProvider() {

    object AliasWidget2Values {
        const val OPEN_APP = "host.stjin.anonaddy.widget.OPEN_APP"
        const val OPEN_APP_TARGET = "host.stjin.anonaddy.widget.OPEN_APP.target"
        const val OPEN_APP_ADD_ALIAS_SHEET = "host.stjin.anonaddy.widget.OPEN_APP_ADD_ALIAS_SHEET"
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
        context?.let { ServiceLocator.settingsManager.putSettingsInt(SettingsManager.PREFS.WIDGETS_ACTIVE, 1) }
        // Since a widget was added, call scheduleBackgroundWorker. This method will Schedule if its still required
        context?.let { BackgroundWorkerHelper(it).scheduleBackgroundWorker() }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        var amountOfWidgets = 0
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, appWidgetManager.getAppWidgetOptions(appWidgetId))
            amountOfWidgets++
        }

        // Store the amount of widgets
        ServiceLocator.settingsManager.putSettingsInt(SettingsManager.PREFS.WIDGETS_ACTIVE, amountOfWidgets)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (context != null && intent != null) {
            when (intent.action) {
                OPEN_APP -> {
                    val mainIntent = Intent(context, SplashActivity::class.java).apply {
                        addFlags(FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(mainIntent)
                }

                OPEN_APP_ADD_ALIAS_SHEET -> {
                    val mainIntent = Intent(context, AliasWidget2BottomSheetAddActivity::class.java).apply {
                        addFlags(FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    }
                    context.startActivity(mainIntent)
                }

                OPEN_APP_TARGET -> {
                    val mainIntent = Intent(context, SplashActivity::class.java).apply {
                        putExtra("target", intent.getStringExtra(OPEN_APP_TARGET))
                        addFlags(FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(mainIntent)
                }

                NAVIGATE -> {
                    if (intent.hasExtra(AliasWidget2Values.COPY_ACTION)) {
                        val alias = intent.getStringExtra(AliasWidget2Values.COPY_ACTION)
                        val clipboard: ClipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("alias", alias)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, context.resources.getString(R.string.copied_alias), Toast.LENGTH_LONG).show()
                    } else if (intent.hasExtra(AliasWidget2Values.OPEN_ACTION)) {
                        val manageAliasIntent = Intent(context, ManageAliasActivity::class.java).apply {
                            putExtra("alias_id", intent.getStringExtra(AliasWidget2Values.OPEN_ACTION))
                            addFlags(FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(manageAliasIntent)
                    } else if (intent.hasExtra(OPEN_APP)) {
                        val mainIntent = Intent(context, SplashActivity::class.java).apply {
                            addFlags(FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(mainIntent)
                    }
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
    val userResource = CacheHelper.getBackgroundServiceCacheUserResource(context)
    val isSetup = ServiceLocator.encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.API_KEY) != null

    val views = RemoteViews(context.packageName, R.layout.widget_2_alias)

    if (!isSetup) {
        views.setViewVisibility(R.id.widget_2_layout_1, View.GONE)
        views.setViewVisibility(R.id.widget_2_layout_2, View.GONE)
        views.setViewVisibility(R.id.widget_2_layout_3, View.GONE)
        views.setViewVisibility(R.id.widget_2_layout_not_setup, View.VISIBLE)
        views.setOnClickPendingIntent(android.R.id.background, getPendingSelfIntent(context, OPEN_APP))
        appWidgetManager.updateAppWidget(appWidgetId, views)
        return
    }

    views.setViewVisibility(R.id.widget_2_layout_not_setup, View.GONE)

    val options = newOptions ?: appWidgetManager.getAppWidgetOptions(appWidgetId)
    val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)?.takeIf { it > 0 } ?: 160
    val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)?.takeIf { it > 0 } ?: 100

    val isSmallHeight = minHeight < 100
    val isLargeWidget = minHeight >= 180 && minWidth >= 180

    when {
        isSmallHeight -> {
            // Layout 2 (Compact bar, e.g. 1 row: 2x1, 3x1, 4x1, 5x1)
            views.setViewVisibility(R.id.widget_2_layout_2, View.VISIBLE)
            views.setViewVisibility(R.id.widget_2_layout_1, View.GONE)
            views.setViewVisibility(R.id.widget_2_layout_3, View.GONE)

            views.setTextViewText(
                R.id.widget_2_layout_2_aliases_statistics_forwarded_count,
                (userResource?.total_emails_forwarded ?: 0).toString()
            )

            // If wide enough (>= 220dp, typically 4+ columns)
            if (minWidth >= 220) {
                views.setViewVisibility(R.id.widget_2_layout_2_additional, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_2_layout_2_aliases_statistics_blocked_count,
                    (userResource?.total_emails_blocked ?: 0).toString()
                )
                views.setTextViewText(
                    R.id.widget_2_layout_2_aliases_statistics_sent_count,
                    (userResource?.total_emails_sent ?: 0).toString()
                )
                views.setTextViewText(
                    R.id.widget_2_layout_2_aliases_statistics_replied_count,
                    (userResource?.total_emails_replied ?: 0).toString()
                )
            } else {
                views.setViewVisibility(R.id.widget_2_layout_2_additional, View.GONE)
            }
            views.setOnClickPendingIntent(android.R.id.background, getPendingSelfIntent(context, OPEN_APP))
        }
        isLargeWidget -> {
            // Layout 3 (Expanded list and actions: 3x3+, 4x3+, 4x4+, 5x4+)
            views.setViewVisibility(R.id.widget_2_layout_3, View.VISIBLE)
            views.setViewVisibility(R.id.widget_2_layout_1, View.GONE)
            views.setViewVisibility(R.id.widget_2_layout_2, View.GONE)

            views.setOnClickPendingIntent(R.id.widget_2_layout_3_aliases_add, getPendingSelfIntent(context, OPEN_APP_ADD_ALIAS_SHEET))
            views.setOnClickPendingIntent(
                R.id.widget_2_layout_3_aliases_aliases,
                getPendingSelfIntent(context, OPEN_APP_TARGET, ActivityTargets.ALIASES.activity)
            )
            views.setOnClickPendingIntent(
                R.id.widget_2_layout_3_aliases_recipients,
                getPendingSelfIntent(context, OPEN_APP_TARGET, ActivityTargets.RECIPIENTS.activity)
            )
            views.setOnClickPendingIntent(
                R.id.widget_2_layout_3_aliases_domains,
                getPendingSelfIntent(context, OPEN_APP_TARGET, ActivityTargets.DOMAINS.activity)
            )
            views.setOnClickPendingIntent(
                R.id.widget_2_layout_3_aliases_rules,
                getPendingSelfIntent(context, OPEN_APP_TARGET, ActivityTargets.RULES.activity)
            )
            views.setOnClickPendingIntent(
                R.id.widget_2_layout_3_aliases_usernames,
                getPendingSelfIntent(context, OPEN_APP_TARGET, ActivityTargets.USERNAMES.activity)
            )

            val intent = Intent(context, AliasWidget2RemoteViewsService::class.java)
            views.setRemoteAdapter(R.id.widget_2_layout_3_aliases_listview, intent)

            val clickIntent = Intent(context, AliasWidget2Provider::class.java).apply {
                action = NAVIGATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = toUri(Intent.URI_INTENT_SCHEME).toUri()
            }

            val onClickPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, clickIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            views.setPendingIntentTemplate(R.id.widget_2_layout_3_aliases_listview, onClickPendingIntent)
            appWidgetManager.notifyAppWidgetViewDataChanged(intArrayOf(appWidgetId), R.id.widget_2_layout_3_aliases_listview)
        }
        else -> {
            // Layout 1 (Medium overview card: 2x2, 3x2, 4x2, 2x3, etc.)
            views.setViewVisibility(R.id.widget_2_layout_1, View.VISIBLE)
            views.setViewVisibility(R.id.widget_2_layout_2, View.GONE)
            views.setViewVisibility(R.id.widget_2_layout_3, View.GONE)

            views.setTextViewText(
                R.id.widget_2_layout_1_aliases_statistics_forwarded_count,
                (userResource?.total_emails_forwarded ?: 0).toString()
            )

            // If wide enough (>= 200dp, typically 3+ columns)
            if (minWidth >= 200) {
                views.setViewVisibility(R.id.widget_2_layout_1_additional, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_2_layout_1_aliases_statistics_blocked_count,
                    (userResource?.total_emails_blocked ?: 0).toString()
                )
                views.setTextViewText(
                    R.id.widget_2_layout_1_aliases_statistics_sent_count,
                    (userResource?.total_emails_sent ?: 0).toString()
                )
                views.setTextViewText(
                    R.id.widget_2_layout_1_aliases_statistics_replied_count,
                    (userResource?.total_emails_replied ?: 0).toString()
                )
            } else {
                views.setViewVisibility(R.id.widget_2_layout_1_additional, View.GONE)
            }
            views.setOnClickPendingIntent(android.R.id.background, getPendingSelfIntent(context, OPEN_APP))
        }
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private fun getPendingSelfIntent(context: Context, action: String, target: String? = null): PendingIntent {
    val intent = Intent(context, AliasWidget2Provider::class.java).apply {
        this.action = action
        putExtra(OPEN_APP_TARGET, target)
    }
    return PendingIntent.getBroadcast(
        context,
        Random.nextInt(0, 999),
        intent,
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}