package host.stjin.anonaddy.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PRIVATE
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.ui.MainActivity
import kotlin.random.Random

class NotificationHelper(private val context: Context) {
    private val ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private var mNotificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val ALIAS_WATCHER_NOTIFICATION_NOTIFICATION_ID = 1
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
            if (SettingsManager(true, context).getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED)) VISIBILITY_PRIVATE else VISIBILITY_PRIVATE

        val stopWatchingIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.STOP_WATCHING
            putExtra("extra", aliasId)
        }
        val stopWatchingPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, Random.nextInt(0, 999), stopWatchingIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val editAliasIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.EDIT_ALIAS
            putExtra("extra", aliasId)
        }
        val editAliasPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, Random.nextInt(0, 999), editAliasIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, Random.nextInt(0, 999), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
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