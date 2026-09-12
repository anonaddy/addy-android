package host.stjin.anonaddy.ui

import android.app.Application
import android.security.KeyChain
import androidx.lifecycle.viewModelScope
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.Updater
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MainViewModel(application: Application) : BaseViewModel(application) {

    private val settingsManager = ServiceLocator.settingsManager
    private val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
    private val appMaintenanceRepository = ServiceLocator.appMaintenanceRepository
    private val failedDeliveriesRepository = ServiceLocator.failedDeliveriesRepository

    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable.asStateFlow()

    fun checkForUpdates() {
        viewModelScope.launch {
            if (settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_UPDATES)) {
                val updateInfo = Updater.isUpdateAvailable()
                _updateAvailable.value = updateInfo.isServerNewer
            }
        }
    }

    suspend fun getFailedDeliveriesCount(): Int {
        val result = failedDeliveriesRepository.getAllFailedDeliveries()
        val previousFailedDeliveryId = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_LATEST_ID)

        var newDeliveriesCount = 0
        if (result is NetworkResult.Success && result.data.data.isNotEmpty()) {
            val currentFailedDeliveryId = result.data.data.firstOrNull()?.id
            if (!currentFailedDeliveryId.isNullOrEmpty()) {
                if (previousFailedDeliveryId == null) {
                    newDeliveriesCount = result.data.meta?.total ?: result.data.data.size
                } else if (currentFailedDeliveryId != previousFailedDeliveryId) {
                    for (delivery in result.data.data) {
                        if (delivery.id == previousFailedDeliveryId) break
                        newDeliveriesCount++
                    }
                    if (newDeliveriesCount <= 0) newDeliveriesCount = 1
                }
            }
        }
        return newDeliveriesCount
    }

    suspend fun getNewAccountNotificationsCount(): Int {
        val result = appMaintenanceRepository.getAllAccountNotifications()
        val currentAccountNotifications = encryptedSettingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT)
        val listSize = (result as? NetworkResult.Success)?.data?.data?.size ?: 0
        return if (listSize > currentAccountNotifications) listSize - currentAccountNotifications else 0
    }

    suspend fun getApiTokenExpiryDateIfNear(): LocalDateTime? {
        val result = userRepository.getApiTokenDetails()
        if (result is NetworkResult.Success && result.data.expires_at != null) {
            val expiryDate = DateTimeUtils.convertStringToLocalTimeZoneDate(result.data.expires_at)
            val deadLineDate = expiryDate?.minusDays(5)
            if (deadLineDate != null && LocalDateTime.now().isAfter(deadLineDate)) {
                return expiryDate
            }
        }
        return null
    }

    suspend fun getSubscriptionExpiryDateIfNear(): LocalDateTime? {
        if (!AddyIo.isUsingHostedInstance) return null
        val result = userRepository.getUserResource()
        if (result is NetworkResult.Success && result.data.subscription_ends_at != null) {
            val expiryDate = DateTimeUtils.convertStringToLocalTimeZoneDate(result.data.subscription_ends_at)
            val deadLineDate = expiryDate?.minusDays(7)
            if (deadLineDate != null && LocalDateTime.now().isAfter(deadLineDate)) {
                return expiryDate
            }
        }
        return null
    }

    fun getCertificateExpiryDateIfNear(alias: String): LocalDateTime? {
        val chain = KeyChain.getCertificateChain(getApplication(), alias)
        val expiryDateOfChain = chain?.firstOrNull()?.notAfter
        if (expiryDateOfChain != null) {
            val expiryDate = DateTimeUtils.convertDateToLocalTimeZoneDate(expiryDateOfChain)
            val deadLineDate = expiryDate?.minusDays(5)
            if (deadLineDate != null && LocalDateTime.now().isAfter(deadLineDate)) {
                return expiryDate
            }
        }
        return null
    }
}
