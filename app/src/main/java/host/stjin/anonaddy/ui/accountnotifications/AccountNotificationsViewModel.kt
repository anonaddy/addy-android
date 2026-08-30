package host.stjin.anonaddy.ui.accountnotifications

import android.app.Application
import androidx.lifecycle.viewModelScope
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy_shared.models.AccountNotifications
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountNotificationsViewModel(application: Application) : BaseViewModel(application) {

    private val appMaintenanceRepository = ServiceLocator.appMaintenanceRepository

    private val _notificationsState = MutableStateFlow<UiState<List<AccountNotifications>>>(UiState.Loading)
    val notificationsState: StateFlow<UiState<List<AccountNotifications>>> = _notificationsState.asStateFlow()

    fun loadNotifications(forceRefresh: Boolean = false): Job {
        if (!forceRefresh && _notificationsState.value is UiState.Success) {
            return Job().apply { complete() }
        }

        return viewModelScope.launch {
            _notificationsState.value = UiState.Loading
            when (val result = appMaintenanceRepository.getAllAccountNotifications()) {
                is NetworkResult.Success -> {
                    _notificationsState.value = UiState.Success(result.data.data)
                }
                is NetworkResult.Error -> {
                    _notificationsState.value = UiState.Error(result.error, result.statusCode)
                }
            }
        }
    }
}
