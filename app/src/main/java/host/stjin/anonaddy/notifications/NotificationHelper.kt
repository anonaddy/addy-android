package host.stjin.anonaddy.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.os.Build
import androidx.core.app.NotificationCompat.BigTextStyle
import androidx.core.app.NotificationCompat.Builder
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationCompat.VISIBILITY_PRIVATE
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_MAX
import androidx.core.app.NotificationManagerCompat.from
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.MessageEvent
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy.ui.accountnotifications.AccountNotificationsActivity
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.appsettings.update.AppSettingsUpdateActivity
import host.stjin.anonaddy.ui.appsettings.wearos.SetupWearOSBottomSheetActivity
import host.stjin.anonaddy.ui.domains.DomainSettingsActivity
import host.stjin.anonaddy.ui.faileddeliveries.FailedDeliveriesActivity
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlin.random.Random

class NotificationHelper(private val context: Context) {
    private val ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private val NEW_WEARABLE_PAIRING_REQUEST_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private val UPDATER_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private val FAILED_DELIVERIES_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private val ACCOUNT_NOTIFICATIONS_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private val API_TOKEN_EXPIRY_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private val DOMAIN_ERROR_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private val SUBSCRIPTION_EXPIRY_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private val FAILED_BACKUP_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID
    private val CERTIFICATE_EXPIRY_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID

    private var mNotificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val loggingHelper = LoggingHelper(context)

    companion object {
        /*
        ALIAS_WATCHER_NOTIFICATION_NOTIFICATION_ID is in the method combined (concat) with a Random() because its possible that multiple of these notifications can appear
        E.g. when 2 aliases have new emails.
        That means that these notification ID's have a prefix of 1 (so 1000 up to 1999)
         */
        const val ALIAS_WATCHER_NOTIFICATION_NOTIFICATION_ID = 1
        const val UPDATER_NOTIFICATION_ID = 2
        const val FAILED_DELIVERIES_NOTIFICATION_ID = 3
        const val FAILED_BACKUP_NOTIFICATION_ID = 4
        const val NEW_WEARABLE_PAIRING_REQUEST_NOTIFICATION_ID = 5
        const val API_KEY_EXPIRE_NOTIFICATION_ID = 6
        const val SUBSCRIPTION_EXPIRE_NOTIFICATION_ID = 7
        const val DOMAIN_ERROR_NOTIFICATION_ID = 8
        const val ACCOUNT_NOTIFICATIONS_NOTIFICATION_ID = 9
        const val CERTIFICATE_EXPIRE_NOTIFICATION_ID = 10

    }

    //region Wearable notifications
    // Is used in gplay version
    fun createSetupAppFirstNotification() {
        createChannel(
            NEW_WEARABLE_PAIRING_REQUEST_CHANNEL_ID,
            context.resources.getString(R.string.notification_channel_addyio_for_wearables),
            context.resources.getString(R.string.notification_channel_addyio_for_wearables_desc), IMPORTANCE_HIGH
        )

        buildSetupAppFirstNotification(
            context.resources.getString(R.string.notification_setup_app_first),
            context.resources.getString(R.string.notification_setup_app_first_desc)
        )
    }

    // Is used in gplay version
    fun createSetupWearableAppNotification(p0: MessageEvent) {
        createChannel(
            NEW_WEARABLE_PAIRING_REQUEST_CHANNEL_ID,
            context.resources.getString(R.string.notification_channel_addyio_for_wearables),
            context.resources.getString(R.string.notification_channel_addyio_for_wearables_desc), IMPORTANCE_MAX
        )

        buildSetupWearableAppNotification(
            p0,
            context.resources.getString(R.string.notification_setup_wearable_app),
            context.resources.getString(R.string.notification_setup_wearable_app_desc, String(p0.data))
        )
    }

    private fun buildSetupWearableAppNotification(p0: MessageEvent, title: String, text: String) {
        val disableWearOSQuickSetupIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.DISABLE_WEAROS_QUICK_SETUP
        }
        val disableWearOSQuickSetupPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                Random.nextInt(0, 999),
                disableWearOSQuickSetupIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )


        val mainIntent = Intent(context, SetupWearOSBottomSheetActivity::class.java)
        mainIntent.putExtra("nodeId", p0.sourceNodeId)
        mainIntent.putExtra("nodeDisplayName", String(p0.data))
        mainIntent.flags = FLAG_ACTIVITY_NO_ANIMATION

        val openSetupWearableBottomSheetActivity: PendingIntent =
            PendingIntent.getActivity(context, Random.nextInt(0, 999), mainIntent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = Builder(context, NEW_WEARABLE_PAIRING_REQUEST_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                BigTextStyle()
                    .bigText(text)
            )
            .setVisibility(VISIBILITY_PUBLIC)
            // Notifications should always have a static color to identify the app
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setSmallIcon(R.drawable.ic_device_watch)
            .addAction(
                R.drawable.ic_device_watch,
                context.resources.getString(R.string.disable_quick_setup),
                disableWearOSQuickSetupPendingIntent
            )
            // Notifications should always have a static color to identify the app
            .setLights(ContextCompat.getColor(context, R.color.md_theme_primary), 1000, 6000)
            .setContentIntent(openSetupWearableBottomSheetActivity)
            // Only cancel this when the setup either failed or is completed
            .setAutoCancel(false)
            .build()

        with(from(context)) {
            // notificationId is a unique int for each notification that you must define
            try {
                notify(NEW_WEARABLE_PAIRING_REQUEST_NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                val ex = e.message
                // User did not gave app POST_NOTIFICATION permissions
                loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "buildSetupWearableAppNotification", null)
            }
        }
    }

    private fun buildSetupAppFirstNotification(title: String, text: String) {
        val disableWearOSQuickSetupIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.DISABLE_WEAROS_QUICK_SETUP
        }
        val disableWearOSQuickSetupPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                Random.nextInt(0, 999),
                disableWearOSQuickSetupIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )


        val mainIntent = Intent(context, SplashActivity::class.java)
        val openSetupWearableBottomSheetActivity: PendingIntent = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(mainIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(Random.nextInt(0, 999), PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notification = Builder(context, NEW_WEARABLE_PAIRING_REQUEST_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                BigTextStyle()
                    .bigText(text)
            )
            .setVisibility(VISIBILITY_PUBLIC)
            // Notifications should always have a static color to identify the app
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setSmallIcon(R.drawable.ic_device_watch)
            .addAction(
                R.drawable.ic_device_watch,
                context.resources.getString(R.string.disable_quick_setup),
                disableWearOSQuickSetupPendingIntent
            )
            // Notifications should always have a static color to identify the app
            .setLights(ContextCompat.getColor(context, R.color.md_theme_primary), 1000, 6000)
            .setContentIntent(openSetupWearableBottomSheetActivity)
            .setAutoCancel(true)
            .build()

        with(from(context)) {
            // notificationId is a unique int for each notification that you must define
            try {
                notify(NEW_WEARABLE_PAIRING_REQUEST_NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                val ex = e.message
                // User did not gave app POST_NOTIFICATION permissions
                loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "buildSetupAppFirstNotification", null)
            }
        }
    }
    //endregion

    /*
    Updates
     */
    fun createUpdateNotification(version: String) {
        createChannel(
            UPDATER_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.notification_channel_update),
            context.resources.getString(R.string.notification_channel_update_desc), IMPORTANCE_DEFAULT
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

        val notification = Builder(context, UPDATER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                BigTextStyle()
                    .bigText(text)
            )
            .setPriority(PRIORITY_LOW)
            .setVisibility(VISIBILITY_PUBLIC)
            // Notifications should always have a static color to identify the app
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setSmallIcon(R.drawable.ic_cloud_download)
            .addAction(R.drawable.ic_cloud_download, context.resources.getString(R.string.stop_checking), stopCheckingUpdatePendingIntent)
            // Notifications should always have a static color to identify the app
            .setLights(ContextCompat.getColor(context, R.color.md_theme_primary), 1000, 6000)
            .setContentIntent(downloadUpdatePendingIntent)
            .setAutoCancel(true)
            .build()

        with(from(context)) {
            // notificationId is a unique int for each notification that you must define
            try {
                notify(UPDATER_NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                val ex = e.message
                // User did not gave app POST_NOTIFICATION permissions
                loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "buildUpdateNotification", null)
            }
        }
    }


    /*
    API token expiry
     */
    fun createApiTokenExpiryNotification(daysLeft: String) {
        createChannel(
            API_TOKEN_EXPIRY_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.notification_channel_api_token_expiry),
            context.resources.getString(R.string.notification_channel_api_token_expiry_desc), IMPORTANCE_DEFAULT
        )

        buildApiTokenExpiryNotification(
            context.resources.getString(R.string.notification_api_token_about_to_expire),
            context.resources.getString(R.string.notification_api_token_about_to_expire_desc, daysLeft)
        )
    }

    private fun buildApiTokenExpiryNotification(title: String, text: String) {
        val stopCheckingApiExpiryIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.STOP_API_EXPIRY_CHECK
        }
        val stopCheckingApiExpiryPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                Random.nextInt(0, 999),
                stopCheckingApiExpiryIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        // Mainactivity will check and auto remind user on launch
        val openApiExpiryIntent = Intent(context, MainActivity::class.java)
        val openApiExpiryPendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(openApiExpiryIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(Random.nextInt(0, 999), PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notification = Builder(context, API_TOKEN_EXPIRY_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                BigTextStyle()
                    .bigText(text)
            )
            .setPriority(PRIORITY_DEFAULT)
            .setVisibility(VISIBILITY_PUBLIC)
            // Notifications should always have a static color to identify the app
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setSmallIcon(R.drawable.ic_letters_case)
            .addAction(R.drawable.ic_letters_case, context.resources.getString(R.string.disable_notifications), stopCheckingApiExpiryPendingIntent)
            // Notifications should always have a static color to identify the app
            .setLights(ContextCompat.getColor(context, R.color.md_theme_primary), 1000, 6000)
            .setContentIntent(openApiExpiryPendingIntent)
            .setAutoCancel(true)
            .build()

        with(from(context)) {
            // notificationId is a unique int for each notification that you must define
            try {
                notify(API_KEY_EXPIRE_NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                val ex = e.message
                // User did not gave app POST_NOTIFICATION permissions
                loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "buildApiTokenExpiryNotification", null)
            }
        }
    }


    /*
    Certificate expiry
     */
    fun createCertificateExpiryNotification(daysLeft: String) {
        createChannel(
            CERTIFICATE_EXPIRY_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.notification_channel_certificate_expiry),
            context.resources.getString(R.string.notification_channel_certificate_expiry_desc), IMPORTANCE_DEFAULT
        )

        buildCertificateExpiryNotification(
            context.resources.getString(R.string.notification_certificate_about_to_expire),
            context.resources.getString(R.string.notification_certificate_about_to_expire_desc, daysLeft)
        )
    }

    private fun buildCertificateExpiryNotification(title: String, text: String) {
        val stopCheckingApiExpiryIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.STOP_CERTIFICATE_CHECK
        }
        val stopCheckingApiExpiryPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                Random.nextInt(0, 999),
                stopCheckingApiExpiryIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        // Mainactivity will check and auto remind user on launch
        val openCertificateExpiryIntent = Intent(context, MainActivity::class.java)
        val openCertificateExpiryPendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(openCertificateExpiryIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(Random.nextInt(0, 999), PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notification = Builder(context, CERTIFICATE_EXPIRY_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                BigTextStyle()
                    .bigText(text)
            )
            .setPriority(PRIORITY_DEFAULT)
            .setVisibility(VISIBILITY_PUBLIC)
            // Notifications should always have a static color to identify the app
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setSmallIcon(R.drawable.ic_certificate)
            .addAction(R.drawable.ic_certificate, context.resources.getString(R.string.disable_notifications), stopCheckingApiExpiryPendingIntent)
            // Notifications should always have a static color to identify the app
            .setLights(ContextCompat.getColor(context, R.color.md_theme_primary), 1000, 6000)
            .setContentIntent(openCertificateExpiryPendingIntent)
            .setAutoCancel(true)
            .build()

        with(from(context)) {
            // notificationId is a unique int for each notification that you must define
            try {
                notify(CERTIFICATE_EXPIRE_NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                val ex = e.message
                // User did not gave app POST_NOTIFICATION permissions
                loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "buildApiTokenExpiryNotification", null)
            }
        }
    }


    /*
    Subscription expiry
    */
    fun createSubscriptionExpiryNotification(daysLeft: String) {
        createChannel(
            SUBSCRIPTION_EXPIRY_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.notification_channel_subscription_expiry),
            context.resources.getString(R.string.notification_channel_subscription_expiry_desc), IMPORTANCE_DEFAULT
        )

        buildSubscriptionExpiryNotification(
            context.resources.getString(R.string.notification_subscription_about_to_expire),
            context.resources.getString(R.string.notification_subscription_about_to_expire_desc, daysLeft)
        )
    }

    private fun buildSubscriptionExpiryNotification(title: String, text: String) {
        val stopCheckingApiExpiryIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.STOP_SUBSCRIPTION_EXPIRY_CHECK
        }
        val stopCheckingSubscriptionExpiryPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                Random.nextInt(0, 999),
                stopCheckingApiExpiryIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        // Mainactivity will check and auto remind user on launch
        val openSubscriptionExpiryIntent = Intent(context, MainActivity::class.java)
        val openSubscriptionExpiryPendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(openSubscriptionExpiryIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(Random.nextInt(0, 999), PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notification = Builder(context, SUBSCRIPTION_EXPIRY_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                BigTextStyle()
                    .bigText(text)
            )
            .setPriority(PRIORITY_DEFAULT)
            .setVisibility(VISIBILITY_PUBLIC)
            // Notifications should always have a static color to identify the app
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setSmallIcon(R.drawable.ic_credit_card)
            .addAction(
                R.drawable.ic_credit_card,
                context.resources.getString(R.string.disable_notifications),
                stopCheckingSubscriptionExpiryPendingIntent
            )
            // Notifications should always have a static color to identify the app
            .setLights(ContextCompat.getColor(context, R.color.md_theme_primary), 1000, 6000)
            .setContentIntent(openSubscriptionExpiryPendingIntent)
            .setAutoCancel(true)
            .build()

        with(from(context)) {
            // notificationId is a unique int for each notification that you must define
            try {
                notify(SUBSCRIPTION_EXPIRE_NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                val ex = e.message
                // User did not gave app POST_NOTIFICATION permissions
                loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "buildSubscriptionExpiryNotification", null)
            }
        }
    }


    /*
    Failed deliveries
     */
    fun createFailedDeliveryNotification(difference: Int) {
        createChannel(
            FAILED_DELIVERIES_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.notification_channel_failed_deliveries),
            context.resources.getString(R.string.notification_channel_failed_deliveries_desc), IMPORTANCE_DEFAULT
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

        val notification = Builder(context, FAILED_DELIVERIES_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                BigTextStyle()
                    .bigText(text)
            )
            .setPriority(PRIORITY_DEFAULT)
            .setVisibility(VISIBILITY_PUBLIC)
            // Notifications should always have a static color to identify the app
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setSmallIcon(R.drawable.ic_mail_error)
            .addAction(R.drawable.ic_mail_error, context.resources.getString(R.string.stop_checking), stopCheckingFailedDeliveryPendingIntent)
            // Notifications should always have a static color to identify the app
            .setLights(ContextCompat.getColor(context, R.color.md_theme_primary), 1000, 6000)
            .setContentIntent(openFailedDeliveriesPendingIntent)
            .setAutoCancel(true)
            .build()

        with(from(context)) {
            // notificationId is a unique int for each notification that you must define
            try {
                notify(FAILED_DELIVERIES_NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                val ex = e.message
                // User did not gave app POST_NOTIFICATION permissions
                loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "buildFailedDeliveryNotification", null)
            }
        }
    }

    /*
    Account notifications
     */
    fun createAccountNotificationsNotification(difference: Int) {
        createChannel(
            ACCOUNT_NOTIFICATIONS_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.notification_channel_account_notifications),
            context.resources.getString(R.string.notification_channel_account_notifications_desc), IMPORTANCE_DEFAULT
        )

        buildAccountNotificationsNotification(
            context.resources.getString(R.string.notification_new_account_notifications),
            context.resources.getString(R.string.notification_new_account_notifications_desc, difference.toString())
        )
    }

    private fun buildAccountNotificationsNotification(title: String, text: String) {
        val stopCheckingAccountNotificationsIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.STOP_ACCOUNT_NOTIFICATIONS_CHECK
        }
        val stopCheckingAccountNotificationsPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                Random.nextInt(0, 999),
                stopCheckingAccountNotificationsIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        val openAccountNotificationsIntent = Intent(context, AccountNotificationsActivity::class.java)
        val openAccountNotificationsPendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(openAccountNotificationsIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(Random.nextInt(0, 999), PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notification = Builder(context, ACCOUNT_NOTIFICATIONS_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                BigTextStyle()
                    .bigText(text)
            )
            .setPriority(PRIORITY_DEFAULT)
            .setVisibility(VISIBILITY_PUBLIC)
            // Notifications should always have a static color to identify the app
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setSmallIcon(R.drawable.ic_bell)
            .addAction(R.drawable.ic_bell, context.resources.getString(R.string.stop_checking), stopCheckingAccountNotificationsPendingIntent)
            // Notifications should always have a static color to identify the app
            .setLights(ContextCompat.getColor(context, R.color.md_theme_primary), 1000, 6000)
            .setContentIntent(openAccountNotificationsPendingIntent)
            .setAutoCancel(true)
            .build()

        with(from(context)) {
            // notificationId is a unique int for each notification that you must define
            try {
                notify(ACCOUNT_NOTIFICATIONS_NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                val ex = e.message
                // User did not gave app POST_NOTIFICATION permissions
                loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "buildAccountNotificationsNotification", null)
            }
        }
    }

    /*
    Failed backup
    */
    fun createFailedBackupNotification() {
        createChannel(
            FAILED_BACKUP_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.notification_channel_backup),
            context.resources.getString(R.string.notification_channel_backup_desc), IMPORTANCE_DEFAULT
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

        val notification = Builder(context, FAILED_BACKUP_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                BigTextStyle()
                    .bigText(text)
            )
            .setPriority(PRIORITY_DEFAULT)
            .setVisibility(VISIBILITY_PUBLIC)
            // Notifications should always have a static color to identify the app
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setSmallIcon(R.drawable.ic_database_export)
            .addAction(
                R.drawable.ic_database_export,
                context.resources.getString(R.string.disable_periodic_backups),
                stopPeriodicBackupsPendingIntent
            )
            // Notifications should always have a static color to identify the app
            .setLights(ContextCompat.getColor(context, R.color.md_theme_primary), 1000, 6000)
            .setContentIntent(openBackupLogsPendingIntent)
            .setAutoCancel(true)
            .build()

        with(from(context)) {
            // notificationId is a unique int for each notification that you must define
            try {
                notify(FAILED_BACKUP_NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                val ex = e.message
                // User did not gave app POST_NOTIFICATION permissions
                loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "buildFailedBackupNotification", null)
            }
        }
    }

    /*
    Watch alias
     */

    fun createAliasWatcherNotification(emailDifference: Int, id: String, email: String) {
        createChannel(
            ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.watch_alias),
            context.resources.getString(R.string.notification_channel_watch_alias_desc), IMPORTANCE_DEFAULT
        )

        val encryptedSettingsManager = SettingsManager(true, context)
        if (encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.PRIVACY_MODE)) {
            // If privacy mode, hide email address
            buildAliasWatcherNotification(
                context.resources.getString(R.string.notification_new_emails),
                context.resources.getString(
                    R.string.notification_new_emails_desc,
                    emailDifference,
                    context.resources.getString(R.string.one_of_your_aliases)
                ),
                id
            )
        } else {
            buildAliasWatcherNotification(
                context.resources.getString(R.string.notification_new_emails),
                context.resources.getString(R.string.notification_new_emails_desc, emailDifference, email),
                id
            )
        }

    }

    fun createAliasWatcherAliasDoesNotExistAnymoreNotification(email: String) {
        createChannel(
            ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.watch_alias),
            context.resources.getString(R.string.notification_channel_watch_alias_desc), IMPORTANCE_DEFAULT
        )

        val encryptedSettingsManager = SettingsManager(true, context)
        if (encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.PRIVACY_MODE)) {
            // If privacy mode, hide email address
            buildAliasWatcherAliasDoesNotExistAnymoreNotification(
                context.resources.getString(R.string.notification_alias_watches_alias_does_not_exist_anymore),
                context.resources.getString(
                    R.string.notification_alias_watches_alias_does_not_exist_anymore_desc,
                    context.resources.getString(R.string.one_of_your_aliases)
                )
            )
        } else {
            buildAliasWatcherAliasDoesNotExistAnymoreNotification(
                context.resources.getString(R.string.notification_alias_watches_alias_does_not_exist_anymore),
                context.resources.getString(R.string.notification_alias_watches_alias_does_not_exist_anymore_desc, email)
            )
        }

    }


    // Every alias gets its own notification (See AliasWatcher)
    private fun buildAliasWatcherNotification(title: String, text: String, aliasId: String) {
        // Decide notification visibility based on if biometrics is enabled
        val visibility =
            if (SettingsManager(true, context).getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED)) VISIBILITY_PRIVATE else VISIBILITY_PUBLIC

        // Decide notificationID here, and send it to the actionReceiver so the correct notification can be cancelled
        // notificationID gets concat here with prefix of ALIAS_WATCHER_NOTIFICATION_NOTIFICATION_ID
        // So 1 + 443 = 1443
        val notificationID = Integer.valueOf(ALIAS_WATCHER_NOTIFICATION_NOTIFICATION_ID.toString() + Random.nextInt(0, 999).toString())

        val stopWatchingIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.STOP_WATCHING
            putExtra("extra", aliasId)
            putExtra("notificationID", notificationID)
        }
        val stopWatchingPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                Random.nextInt(0, 999),
                stopWatchingIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        val disableAliasIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.DISABLE_ALIAS
            putExtra("extra", aliasId)
            putExtra("notificationID", notificationID)
        }
        val disableAliasPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                Random.nextInt(0, 999),
                disableAliasIntent,
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

        val notification = Builder(context, ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                BigTextStyle()
                    .bigText(text)
            )
            .setPriority(PRIORITY_HIGH)
            .setVisibility(visibility)
            // Notifications should always have a static color to identify the app
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setSmallIcon(R.drawable.ic_watch_alias)
            .addAction(R.drawable.ic_watch_alias, context.resources.getString(R.string.stop_watching), stopWatchingPendingIntent)
            .addAction(R.drawable.ic_watch_alias, context.resources.getString(R.string.disable_alias), disableAliasPendingIntent)
            // Notifications should always have a static color to identify the app
            .setLights(ContextCompat.getColor(context, R.color.md_theme_primary), 1000, 6000)
            .setContentIntent(editAliasPendingIntent)
            .setAutoCancel(true)
            .build()
        with(from(context)) {
            // notificationId is a unique int for each notification that you must define
            try {
                notify(notificationID, notification)
            } catch (e: SecurityException) {
                val ex = e.message
                // User did not gave app POST_NOTIFICATION permissions
                loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "buildAliasWatcherNotification", null)
            }
        }
    }

    private fun buildAliasWatcherAliasDoesNotExistAnymoreNotification(title: String, text: String) {
        // Decide notification visibility based on if biometrics is enabled
        val visibility =
            if (SettingsManager(true, context).getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED)) VISIBILITY_PRIVATE else VISIBILITY_PUBLIC

        // Decide notificationID here, and send it to the actionReceiver so the correct notification can be cancelled
        // notificationID gets concat here with prefix of ALIAS_WATCHER_NOTIFICATION_NOTIFICATION_ID
        // So 1 + 443 = 1443
        val notificationID = Integer.valueOf(ALIAS_WATCHER_NOTIFICATION_NOTIFICATION_ID.toString() + Random.nextInt(0, 999).toString())

        val notification = Builder(context, ALIAS_WATCHER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                BigTextStyle()
                    .bigText(text)
            )
            .setPriority(PRIORITY_HIGH)
            .setVisibility(visibility)
            // Notifications should always have a static color to identify the app
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setSmallIcon(R.drawable.ic_watch_alias)
            // Notifications should always have a static color to identify the app
            .setLights(ContextCompat.getColor(context, R.color.md_theme_primary), 1000, 6000)
            .setAutoCancel(true)
            .build()
        with(from(context)) {
            // notificationId is a unique int for each notification that you must define
            try {
                notify(notificationID, notification)
            } catch (e: SecurityException) {
                val ex = e.message
                // User did not gave app POST_NOTIFICATION permissions
                loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "buildAliasWatcherAliasDoesNotExistAnymoreNotification", null)
            }
        }
    }

    /*
    Domain errors
    */
    fun createDomainErrorNotification(count: Int) {
        createChannel(
            DOMAIN_ERROR_NOTIFICATION_CHANNEL_ID,
            context.resources.getString(R.string.notification_channel_domain_error),
            context.resources.getString(R.string.notification_channel_domain_error_desc), IMPORTANCE_DEFAULT
        )

        buildDomainErrorNotification(
            context.resources.getString(R.string.notification_domain_error),
            context.resources.getString(R.string.notification_domain_error_desc, count.toString())
        )
    }

    private fun buildDomainErrorNotification(title: String, text: String) {
        val stopCheckingDomaineErrorIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.NOTIFICATIONACTIONS.STOP_DOMAIN_ERROR_CHECK
        }
        val stopCheckingDomainErrorsPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                Random.nextInt(0, 999),
                stopCheckingDomaineErrorIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        val openDomainSettingsIntent = Intent(context, DomainSettingsActivity::class.java)
        val openDomainSettingsPendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(openDomainSettingsIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(Random.nextInt(0, 999), PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notification = Builder(context, DOMAIN_ERROR_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                BigTextStyle()
                    .bigText(text)
            )
            .setPriority(PRIORITY_DEFAULT)
            .setVisibility(VISIBILITY_PUBLIC)
            // Notifications should always have a static color to identify the app
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setSmallIcon(R.drawable.ic_dns_alert)
            .addAction(R.drawable.ic_dns_alert, context.resources.getString(R.string.disable_notifications), stopCheckingDomainErrorsPendingIntent)
            // Notifications should always have a static color to identify the app
            .setLights(ContextCompat.getColor(context, R.color.md_theme_primary), 1000, 6000)
            .setContentIntent(openDomainSettingsPendingIntent)
            .setAutoCancel(true)
            .build()

        with(from(context)) {
            // notificationId is a unique int for each notification that you must define
            try {
                notify(DOMAIN_ERROR_NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                val ex = e.message
                // User did not gave app POST_NOTIFICATION permissions
                loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "buildDomainErrorNotification", null)
            }
        }
    }


    private fun createChannel(channelId: String, title: String, description: String, importance: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                title,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = description
            channel.importance = importance
            channel.enableLights(true)
            // Notifications should always have a static color to identify the app
            channel.lightColor = ContextCompat.getColor(context, R.color.md_theme_primary)
            mNotificationManager.createNotificationChannel(channel)
        }
    }

}