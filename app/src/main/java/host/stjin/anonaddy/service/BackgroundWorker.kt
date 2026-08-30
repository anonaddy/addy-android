package host.stjin.anonaddy.service

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.security.KeyChain
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.Updater
import host.stjin.anonaddy.notifications.NotificationHelper
import host.stjin.anonaddy.widget.AliasWidget1Provider
import host.stjin.anonaddy.widget.AliasWidget2Provider
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.managers.SettingsManager.PREFS
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.GsonTools
import host.stjin.anonaddy_shared.utils.LoggingHelper
import org.ocpsoft.prettytime.PrettyTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

import host.stjin.anonaddy_shared.repositories.AliasRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


/*
This BackgroundWorker is used for obtaining data in the background, this data is then being used to "Watch" aliases and updating the data the widget uses.
 */

class BackgroundWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private fun updateWidgets() {
        // Update widget 1
        val updateWidget1Intent = Intent(applicationContext, AliasWidget1Provider::class.java)
        updateWidget1Intent.action = ACTION_APPWIDGET_UPDATE
        val ids1 = AppWidgetManager.getInstance(applicationContext)
            .getAppWidgetIds(ComponentName(applicationContext, AliasWidget1Provider::class.java))
        updateWidget1Intent.putExtra(EXTRA_APPWIDGET_IDS, ids1)
        applicationContext.sendBroadcast(updateWidget1Intent)


        // Update widget 2
        val updateWidget2Intent = Intent(applicationContext, AliasWidget2Provider::class.java)
        updateWidget2Intent.action = ACTION_APPWIDGET_UPDATE
        val ids2 = AppWidgetManager.getInstance(applicationContext)
            .getAppWidgetIds(ComponentName(applicationContext, AliasWidget2Provider::class.java))
        updateWidget2Intent.putExtra(EXTRA_APPWIDGET_IDS, ids2)
        applicationContext.sendBroadcast(updateWidget2Intent)
    }

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val backgroundWorkerHelper = BackgroundWorkerHelper(appContext)
        val settingsManager = ServiceLocator.settingsManager
        val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager

        // True if there are aliases to be watched, widgets to be updated or checked for updates
        if (backgroundWorkerHelper.isThereWorkTodo()) {
            val userRepository = ServiceLocator.userRepository
            val aliasRepository = ServiceLocator.aliasRepository
            val domainRepository = ServiceLocator.domainRepository
            val failedDeliveriesRepository = ServiceLocator.failedDeliveriesRepository
            val appMaintenanceRepository = ServiceLocator.appMaintenanceRepository

            val (
                userResourceNetworkCallResult,
                aliasNetworkCallResult,
                aliasWatcherNetworkCallResult,
                notifyApiExpiryNetworkCallResult,
                notifyCertificateExpiryResult,
                notifySubscriptionNetworkCallResult,
                failedDeliveriesNetworkCallResult,
                accountNotificationsNetworkCallResult
            ) = coroutineScope {
                val userResourceDeferred = async {
                    val userResourceResult = userRepository.cacheUserResourceForWidget()
                    userResourceResult is NetworkResult.Success && userResourceResult.data
                }

                val aliasCacheDeferred = async {
                    val aliasCacheResult = aliasRepository.cacheMostPopularAliasesDataForWidget()
                    aliasCacheResult is NetworkResult.Success && aliasCacheResult.data
                }

                val aliasWatcherDeferred = async {
                    aliasWatcherTask(appContext, aliasRepository, encryptedSettingsManager)
                }

                val updateCheckDeferred = async {
                    if (settingsManager.getSettingsBool(PREFS.NOTIFY_UPDATES)) {
                        val updateInfo = Updater.isUpdateAvailable()
                        if (updateInfo.isServerNewer) {
                            updateInfo.serverVersion?.let {
                                NotificationHelper(appContext).createUpdateNotification(it)
                            }
                        }
                    }
                }

                val apiTokenDeferred = async {
                    if (settingsManager.getSettingsBool(PREFS.NOTIFY_API_TOKEN_EXPIRY, true)) {
                        val tokenResult = userRepository.getApiTokenDetails()
                        if (tokenResult is NetworkResult.Success) {
                            val apiTokenDetails = tokenResult.data
                            if (apiTokenDetails.expires_at != null) {
                                val expiryDate = DateTimeUtils.convertStringToLocalTimeZoneDate(apiTokenDetails.expires_at)
                                val currentDateTime = LocalDateTime.now()
                                val deadLineDate = expiryDate?.minusDays(5)
                                if (deadLineDate != null && currentDateTime.isAfter(deadLineDate)) {
                                    val previousNotificationLeftDays =
                                        encryptedSettingsManager.getSettingsInt(PREFS.BACKGROUND_SERVICE_CACHE_API_KEY_EXPIRY_LEFT_COUNT)
                                    val currentLeftDays = ChronoUnit.DAYS.between(currentDateTime, deadLineDate).toInt()

                                    if (previousNotificationLeftDays != currentLeftDays) {
                                        encryptedSettingsManager.putSettingsInt(
                                            PREFS.BACKGROUND_SERVICE_CACHE_API_KEY_EXPIRY_LEFT_COUNT,
                                            currentLeftDays
                                        )
                                        val text = PrettyTime().format(expiryDate)
                                        NotificationHelper(appContext).createApiTokenExpiryNotification(text)
                                    }
                                }
                            }
                            true
                        } else {
                            false
                        }
                    } else {
                        true
                    }
                }

                val certificateDeferred = async {
                    if (settingsManager.getSettingsBool(PREFS.NOTIFY_CERTIFICATE_EXPIRY)) {
                        val alias = encryptedSettingsManager.getSettingsString(PREFS.CERTIFICATE_ALIAS)

                        if (alias != null) {
                            val chain = KeyChain.getCertificateChain(appContext, alias)
                            val expiryDateOfChain = chain?.firstOrNull()?.notAfter

                            if (expiryDateOfChain != null) {
                                val expiryDate = DateTimeUtils.convertDateToLocalTimeZoneDate(expiryDateOfChain)
                                val currentDateTime = LocalDateTime.now()
                                val deadLineDate = expiryDate?.minusDays(5)
                                if (deadLineDate != null && currentDateTime.isAfter(deadLineDate)) {
                                    val previousNotificationLeftDays =
                                        encryptedSettingsManager.getSettingsInt(PREFS.BACKGROUND_SERVICE_CACHE_CERTIFICATE_EXPIRY_LEFT_COUNT)
                                    val currentLeftDays = ChronoUnit.DAYS.between(currentDateTime, deadLineDate).toInt()

                                    if (previousNotificationLeftDays != currentLeftDays) {
                                        encryptedSettingsManager.putSettingsInt(
                                            PREFS.BACKGROUND_SERVICE_CACHE_CERTIFICATE_EXPIRY_LEFT_COUNT,
                                            currentLeftDays
                                        )
                                        val text = PrettyTime().format(expiryDate)
                                        NotificationHelper(appContext).createCertificateExpiryNotification(text)
                                    }
                                }
                            }
                        }
                        true
                    } else {
                        true
                    }
                }

                val domainErrorsDeferred = async {
                    if (settingsManager.getSettingsBool(PREFS.NOTIFY_DOMAIN_ERROR, false)) {
                        val domainsResult = domainRepository.getAllDomains()
                        if (domainsResult is NetworkResult.Success) {
                            val domains = domainsResult.data.data
                            val amountOfDomainsWithErrors = domains.count { it.domain_mx_validated_at == null }
                            if (amountOfDomainsWithErrors > 0) {
                                val previousNotificationLeftDays =
                                    encryptedSettingsManager.getSettingsInt(PREFS.BACKGROUND_SERVICE_CACHE_DOMAIN_ERROR_COUNT)

                                if (previousNotificationLeftDays != amountOfDomainsWithErrors) {
                                    encryptedSettingsManager.putSettingsInt(
                                        PREFS.BACKGROUND_SERVICE_CACHE_DOMAIN_ERROR_COUNT,
                                        amountOfDomainsWithErrors
                                    )
                                    NotificationHelper(appContext).createDomainErrorNotification(amountOfDomainsWithErrors)
                                }
                            }
                        }
                    }
                }

                val subscriptionDeferred = async {
                    if (settingsManager.getSettingsBool(PREFS.NOTIFY_SUBSCRIPTION_EXPIRY, false)) {
                        val userRes = userRepository.getUserResource()
                        if (userRes is NetworkResult.Success) {
                            val user = userRes.data
                            if (user.subscription_ends_at != null) {
                                val expiryDate = DateTimeUtils.convertStringToLocalTimeZoneDate(user.subscription_ends_at)
                                val currentDateTime = LocalDateTime.now()
                                val deadLineDate = expiryDate?.minusDays(7)
                                if (deadLineDate != null && currentDateTime.isAfter(deadLineDate)) {
                                    val previousNotificationLeftDays =
                                        encryptedSettingsManager.getSettingsInt(PREFS.BACKGROUND_SERVICE_CACHE_SUBSCRIPTION_EXPIRY_LEFT_COUNT)
                                    val currentLeftDays = ChronoUnit.DAYS.between(currentDateTime, deadLineDate).toInt()

                                    if (previousNotificationLeftDays != currentLeftDays) {
                                        encryptedSettingsManager.putSettingsInt(
                                            PREFS.BACKGROUND_SERVICE_CACHE_SUBSCRIPTION_EXPIRY_LEFT_COUNT,
                                            currentLeftDays
                                        )
                                        val text = PrettyTime().format(expiryDate)
                                        NotificationHelper(appContext).createSubscriptionExpiryNotification(text)
                                    }
                                }
                            }
                            true
                        } else {
                            false
                        }
                    } else {
                        true
                    }
                }

                val backupDeferred = async {
                    if (settingsManager.getSettingsBool(PREFS.PERIODIC_BACKUPS)) {
                        BackupHelper(appContext).let {
                            val date: LocalDate? =
                                it.getLatestBackupDate()?.let { it1 -> Instant.ofEpochMilli(it1).atZone(ZoneId.systemDefault()).toLocalDate() }
                            val today: LocalDate = LocalDate.now()
                            if (date?.isBefore(today.minusDays(1)) != false) {
                                if (it.createBackup()) {
                                    it.deleteBackupsOlderThanXDays(30)
                                } else {
                                    NotificationHelper(appContext).createFailedBackupNotification()
                                }
                            }
                        }
                    }
                }

                val failedDeliveriesDeferred = async {
                    if (settingsManager.getSettingsBool(PREFS.NOTIFY_FAILED_DELIVERIES)) {
                        val previousFailedDeliveryId =
                            encryptedSettingsManager.getSettingsString(PREFS.BACKGROUND_SERVICE_NOTIFIED_FAILED_DELIVERIES_LATEST_ID)

                        val deliveryResult = failedDeliveriesRepository.cacheFailedDeliveryCountForWidgetAndBackgroundService(previousFailedDeliveryId)
                        if (deliveryResult is NetworkResult.Success) {
                            val (newDeliveriesCount, currentFailedDeliveryId) = deliveryResult.data

                            if (currentFailedDeliveryId != null && previousFailedDeliveryId != null && currentFailedDeliveryId != previousFailedDeliveryId && currentFailedDeliveryId.isNotEmpty()) {
                                if (newDeliveriesCount > 0) {
                                    NotificationHelper(appContext).createFailedDeliveryNotification(newDeliveriesCount)
                                }
                            }

                            if (!currentFailedDeliveryId.isNullOrEmpty()) {
                                encryptedSettingsManager.putSettingsString(
                                    PREFS.BACKGROUND_SERVICE_NOTIFIED_FAILED_DELIVERIES_LATEST_ID,
                                    currentFailedDeliveryId
                                )
                            }
                            true
                        } else {
                            false
                        }
                    } else {
                        true
                    }
                }

                val accountNotificationsDeferred = async {
                    if (settingsManager.getSettingsBool(PREFS.NOTIFY_ACCOUNT_NOTIFICATIONS)) {
                        val notifCacheResult = appMaintenanceRepository.cacheAccountNotificationsCountForWidgetAndBackgroundService()
                        val success = notifCacheResult is NetworkResult.Success && notifCacheResult.data

                        val currentAccountNotifications =
                            encryptedSettingsManager.getSettingsInt(PREFS.BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT)
                        val previousAccountNotifications =
                            encryptedSettingsManager.getSettingsInt(PREFS.BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT_PREVIOUS)
                        if (currentAccountNotifications > previousAccountNotifications) {
                            NotificationHelper(appContext).createAccountNotificationsNotification(
                                currentAccountNotifications - previousAccountNotifications
                            )
                        }
                        success
                    } else {
                        true
                    }
                }

                updateCheckDeferred.await()
                domainErrorsDeferred.await()
                backupDeferred.await()

                WorkerTaskResults(
                    userResourceNetworkCallResult = userResourceDeferred.await(),
                    aliasNetworkCallResult = aliasCacheDeferred.await(),
                    aliasWatcherNetworkCallResult = aliasWatcherDeferred.await(),
                    notifyApiExpiryNetworkCallResult = apiTokenDeferred.await(),
                    notifyCertificateExpiryResult = certificateDeferred.await(),
                    notifySubscriptionNetworkCallResult = subscriptionDeferred.await(),
                    failedDeliveriesNetworkCallResult = failedDeliveriesDeferred.await(),
                    accountNotificationsNetworkCallResult = accountNotificationsDeferred.await()
                )
            }

            // If the aliasNetwork call was successful, perform the check
            if (aliasWatcherNetworkCallResult) {
                AliasWatcher(appContext).watchAliasesForDifferences()
            }

            if (BuildConfig.DEBUG) {
                LoggingHelper(appContext, LoggingHelper.LOGFILES.DEFAULT).addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    "userResourceNetworkCallResult=${userResourceNetworkCallResult}\n" +
                            "aliasNetworkCallResult=${aliasNetworkCallResult}\n" +
                            "aliasWatcherNetworkCallResult=${aliasWatcherNetworkCallResult}\n" +
                            "failedDeliveriesNetworkCallResult=${failedDeliveriesNetworkCallResult}\n" +
                            "notifyApiExpiryNetworkCallResult=${notifyApiExpiryNetworkCallResult}\n" +
                            "notifyCertificateExpiryResult=${notifyCertificateExpiryResult}\n" +
                            "notifySubscriptionNetworkCallResult=${notifySubscriptionNetworkCallResult}\n" +
                            "accountNotificationsNetworkCallResult=${accountNotificationsNetworkCallResult}\n",
                    "doWork()", null
                )
            }

            return if (userResourceNetworkCallResult &&
                aliasNetworkCallResult &&
                aliasWatcherNetworkCallResult &&
                failedDeliveriesNetworkCallResult &&
                notifyApiExpiryNetworkCallResult &&
                notifyCertificateExpiryResult &&
                notifySubscriptionNetworkCallResult &&
                accountNotificationsNetworkCallResult
            ) {
                updateWidgets()
                Result.success()
            } else {
                Result.failure()
            }
        } else {
            backgroundWorkerHelper.cancelScheduledBackgroundWorker()
            return Result.success()
        }
    }

    private suspend fun aliasWatcherTask(appContext: Context, aliasRepository: AliasRepository, settingsManager: SettingsManager): Boolean {
        val aliasWatcher = AliasWatcher(appContext)
        val aliasesToWatch = aliasWatcher.getAliasesToWatch().toList()

        if (aliasesToWatch.isNotEmpty()) {
            val result = aliasRepository.bulkGetAlias(aliasesToWatch)
            if (result is NetworkResult.Success) {
                val bulkAliases = result.data
                val aliasesJson = settingsManager.getSettingsString(PREFS.BACKGROUND_SERVICE_CACHE_WATCH_ALIAS_DATA)
                val aliasesList = aliasesJson?.let { GsonTools.jsonToAliasObject(appContext, it) }

                val currentList = settingsManager.getSettingsString(PREFS.BACKGROUND_SERVICE_CACHE_WATCH_ALIAS_DATA)
                currentList?.let {
                    settingsManager.putSettingsString(
                        PREFS.BACKGROUND_SERVICE_CACHE_WATCH_ALIAS_DATA_PREVIOUS,
                        it
                    )
                }

                for (id in aliasesToWatch) {
                    if (bulkAliases.data.none { it.id == id }) {
                        LoggingHelper(appContext, LoggingHelper.LOGFILES.DEFAULT).addLog(
                            LOGIMPORTANCE.WARNING.int,
                            appContext.resources.getString(
                                R.string.notification_alias_watches_alias_does_not_exist_anymore_desc,
                                aliasesList?.firstOrNull { it.id == id }?.email ?: id
                            ),
                            "aliasWatcherTask",
                            null
                        )

                        NotificationHelper(appContext).createAliasWatcherAliasDoesNotExistAnymoreNotification(
                            aliasesList?.firstOrNull { it.id == id }?.email ?: id
                        )

                        aliasWatcher.removeAliasToWatch(id)
                    }
                }

                val data = GsonTools.gson.toJson(bulkAliases.data)
                settingsManager.putSettingsString(PREFS.BACKGROUND_SERVICE_CACHE_WATCH_ALIAS_DATA, data)
            }
        }

        return true
    }

    private data class WorkerTaskResults(
        val userResourceNetworkCallResult: Boolean,
        val aliasNetworkCallResult: Boolean,
        val aliasWatcherNetworkCallResult: Boolean,
        val notifyApiExpiryNetworkCallResult: Boolean,
        val notifyCertificateExpiryResult: Boolean,
        val notifySubscriptionNetworkCallResult: Boolean,
        val failedDeliveriesNetworkCallResult: Boolean,
        val accountNotificationsNetworkCallResult: Boolean
    )
}

