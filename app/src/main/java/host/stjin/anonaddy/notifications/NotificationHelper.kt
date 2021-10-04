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
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.appsettings.update.AppSettingsUpdateActivity
import host.stjin.anonaddy.ui.faileddeliveries.FailedDeliveriesActivity
import host.stjin.anonaddy.utils.LoggingHelper
import kotlin.random.Random

class NotificationHelper(private val context: Context) {
    private val ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private val UPDATER_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private val FAILED_DELIVERIES_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private val FAILED_BACKUP_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private var mNotificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val ALIAS_WATCHER_NOTIFICATION_NOTIFICATION_ID = 1
        const val UPDATER_NOTIFICATION_ID = 2
        const val FAILED_DELIVERIES_NOTIFICATION_ID = 3
        const val FAILED_BACKUP_NOTIFICATION_ID = 3
    }


    /*
    Updates
     */
    fun createUpdateNotification(version: String) {
        createChannel(
            UPDATER_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.anonaddy_updater),
            context.resources.getString(R.string.notification_channel_update_desc)
        )

        buildUpdateNotification(
            context.resources.getString(R.string.new_update_available),
            context.resources.getString(R.string.notification_new_update_available_desc, version)
        )
    }

    private fun buildUpdateNotification(title: String, text: String) {
        val stopCheckingUpdateIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.STOP_UPDATE_CHECK
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

        val notification = NotificationCompat.Builder(context, ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(VISIBILITY_PUBLIC)
            .setColor(ContextCompat.getColor(context, R.color.primaryColor))
            .setSmallIcon(R.drawable.ic_cloud_download)
            .addAction(R.drawable.ic_cloud_download, context.resources.getString(R.string.stop_checking), stopCheckingUpdatePendingIntent)
            .setLights(ContextCompat.getColor(context, R.color.primaryColor), 1000, 6000)
            .setContentIntent(downloadUpdatePendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(UPDATER_NOTIFICATION_ID, notification)
        }
    }


    /*
    Failed deliveries
     */
    fun createFailedDeliveryNotification(difference: Int) {
        createChannel(
            FAILED_DELIVERIES_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.failed_deliveries),
            context.resources.getString(R.string.notification_channel_failed_deliveries_desc)
        )

        buildFailedDeliveryNotification(
            context.resources.getString(R.string.notification_new_failed_delivery),
            context.resources.getString(R.string.notification_new_failed_delivery_desc, difference.toString())
        )
    }

    private fun buildFailedDeliveryNotification(title: String, text: String) {
        val stopCheckingFailedDeliveryIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.STOP_FAILED_DELIVERY_CHECK
        }
        val stopCheckingFailedDeliveryPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                Random.nextInt(0, 999),
                stopCheckingFailedDeliveryIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        val openFailedDeliveriesIntent = Intent(context, FailedDeliveriesActivity::class.java)
        val openFailedDeliveriesPendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(openFailedDeliveriesIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(Random.nextInt(0, 999), PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notification = NotificationCompat.Builder(context, ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(VISIBILITY_PUBLIC)
            .setColor(ContextCompat.getColor(context, R.color.primaryColor))
            .setSmallIcon(R.drawable.ic_mail_error)
            .addAction(R.drawable.ic_mail_error, context.resources.getString(R.string.stop_checking), stopCheckingFailedDeliveryPendingIntent)
            .setLights(ContextCompat.getColor(context, R.color.primaryColor), 1000, 6000)
            .setContentIntent(openFailedDeliveriesPendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(FAILED_DELIVERIES_NOTIFICATION_ID, notification)
        }
    }

    /*
    Failed backup
    */
    fun createFailedBackupNotification() {
        createChannel(
            FAILED_BACKUP_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.notification_channel_failed_backups),
            context.resources.getString(R.string.notification_channel_failed_backups_desc)
        )

        buildFailedBackupNotification(
            context.resources.getString(R.string.notification_new_backup_delivery),
            context.resources.getString(R.string.notification_new_backup_delivery_desc)
        )
    }

    private fun buildFailedBackupNotification(title: String, text: String) {
        val stopPeriodicBackupsIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.STOP_PERIODIC_BACKUPS
        }
        val stopPeriodicBackupsPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                Random.nextInt(0, 999),
                stopPeriodicBackupsIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        val openBackupLogsIntent = Intent(context, LogViewerActivity::class.java)
        openBackupLogsIntent.putExtra("logfile", LoggingHelper.LOGFILES.BACKUP_LOGS.filename)
        val openBackupLogsPendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(openBackupLogsIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(Random.nextInt(0, 999), PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notification = NotificationCompat.Builder(context, FAILED_BACKUP_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(VISIBILITY_PUBLIC)
            .setColor(ContextCompat.getColor(context, R.color.primaryColor))
            .setSmallIcon(R.drawable.ic_database_export)
            .addAction(
                R.drawable.ic_database_export,
                context.resources.getString(R.string.disable_periodic_backups),
                stopPeriodicBackupsPendingIntent
            )
            .setLights(ContextCompat.getColor(context, R.color.primaryColor), 1000, 6000)
            .setContentIntent(openBackupLogsPendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(FAILED_BACKUP_NOTIFICATION_ID, notification)
        }
    }

    /*
    Watch alias
     */

    fun createAliasWatcherNotification(emailDifference: Int, id: String, email: String) {
        createChannel(
            ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.watch_alias),
            context.resources.getString(R.string.notification_channel_watch_alias_desc)
        )

        buildAliasWatcherNotification(
            context.resources.getString(R.string.notification_new_emails),
            context.resources.getString(R.string.notification_new_emails_desc, emailDifference, email),
            id
        )
    }

    // Every alias gets its own notification (See AliasWatcher)
    private fun buildAliasWatcherNotification(title: String, text: String, aliasId: String) {
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

        val notification = NotificationCompat.Builder(context, ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(visibility)
            .setColor(ContextCompat.getColor(context, R.color.primaryColor))
            .setSmallIcon(R.drawable.ic_watch_alias)
            .addAction(R.drawable.ic_watch_alias, context.resources.getString(R.string.stop_watching), stopWatchingPendingIntent)
            .setLights(ContextCompat.getColor(context, R.color.primaryColor), 1000, 6000)
            .setContentIntent(editAliasPendingIntent)
            .setAutoCancel(true)
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