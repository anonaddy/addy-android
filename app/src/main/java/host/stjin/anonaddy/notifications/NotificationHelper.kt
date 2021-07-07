package host.stjin.anonaddy.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PRIVATE
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.ui.appsettings.update.AppSettingsUpdateActivity
import kotlin.random.Random

class NotificationHelper(private val context: Context) {
    private val ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private val UPDATER__NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private var mNotificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val ALIAS_WATCHER_NOTIFICATION_NOTIFICATION_ID = 1
        const val UPDATER_NOTIFICATION_ID = 2
    }


    fun createUpdateNotification(version: String, download: String) {
        createChannel(
            UPDATER__NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.new_update_available),
            context.resources.getString(R.string.notification_channel_update_desc)
        )

        buildUpdateCallNotification(
            context.resources.getString(R.string.new_update_available),
            context.resources.getString(R.string.notification_new_update_available_desc, version), download)
    }

    private fun buildUpdateCallNotification(title: String, text: String, download: String) {

        val stopCheckingUpdateIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.STOP_DOWNLOAD_UPDATE_CHECK
        }
        val stopCheckingUpdatePendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                Random.nextInt(0, 999),
                stopCheckingUpdateIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        val downloadUpdateIntent = Intent(context, AppSettingsUpdateActivity::class.java)
        val downloadUpdatePendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(downloadUpdateIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(Random.nextInt(0, 999), PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            Random.nextInt(0, 999),
            notificationIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setVisibility(VISIBILITY_PUBLIC)
            .setColor(ContextCompat.getColor(context, R.color.primaryColor))
            .setSmallIcon(R.drawable.ic_downloading_24dp)
            .addAction(R.drawable.ic_downloading_24dp, context.resources.getString(R.string.stop_checking), stopCheckingUpdatePendingIntent)
            .setLights(ContextCompat.getColor(context, R.color.primaryColor), 1000, 6000)
            .setContentIntent(downloadUpdatePendingIntent)
            // Auto cancel only in production
            .setAutoCancel(!BuildConfig.DEBUG)
            .build()

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(UPDATER_NOTIFICATION_ID, notification)
        }
    }


    fun createAliasWatcherNotification(emailDifference: Int, id: String, email: String) {
        createChannel(
            ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.watch_alias),
            context.resources.getString(R.string.notification_channel_watch_alias_desc)
        )

        buildAliasWatcherCallNotification(
            context.resources.getString(R.string.notification_new_emails),
            context.resources.getString(R.string.notification_new_emails_desc, emailDifference, email),
            id
        )
    }

    private fun buildAliasWatcherCallNotification(title: String, text: String, aliasId: String) {
        // Decide notification visibility based on if biometrics is enabled
        val visibility =
            if (SettingsManager(true, context).getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED)) VISIBILITY_PRIVATE else VISIBILITY_PUBLIC

        val stopWatchingIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.STOP_WATCHING
            putExtra("extra", aliasId)
        }
        val stopWatchingPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                Random.nextInt(0, 999),
                stopWatchingIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        val editAliasIntent = Intent(context, ManageAliasActivity::class.java).apply {
            putExtra("alias_id", aliasId)
        }
        val editAliasPendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(editAliasIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(Random.nextInt(0, 999), PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            Random.nextInt(0, 999),
            notificationIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setVisibility(visibility)
            .setColor(ContextCompat.getColor(context, R.color.primaryColor))
            .setSmallIcon(R.drawable.notification_ic_comment_eye_outline)
            .addAction(R.drawable.notification_ic_comment_eye_outline, context.resources.getString(R.string.stop_watching), stopWatchingPendingIntent)
            .setLights(ContextCompat.getColor(context, R.color.primaryColor), 1000, 6000)
            .setContentIntent(editAliasPendingIntent)
            // Auto cancel only in production
            .setAutoCancel(!BuildConfig.DEBUG)
            .build()

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(ALIAS_WATCHER_NOTIFICATION_NOTIFICATION_ID, notification)
        }
    }


    private fun createChannel(channelId: String, title: String, description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                title,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = description
            channel.enableLights(true)
            channel.lightColor = ContextCompat.getColor(context, R.color.primaryColor)
            mNotificationManager.createNotificationChannel(channel)
        }
    }

}