package host.stjin.anonaddy.ui

import android.app.Application
import androidx.lifecycle.viewModelScope
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.Updater
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
}
