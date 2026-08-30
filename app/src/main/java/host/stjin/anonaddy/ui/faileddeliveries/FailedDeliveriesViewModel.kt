package host.stjin.anonaddy.ui.faileddeliveries

import android.app.Application
import androidx.lifecycle.viewModelScope
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy_shared.models.FailedDeliveries
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.appendPage
import host.stjin.anonaddy_shared.models.nextPage
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FailedDeliveriesViewModel(application: Application) : BaseViewModel(application) {

    private val failedDeliveriesRepository = ServiceLocator.failedDeliveriesRepository

    private val _failedDeliveriesState = MutableStateFlow<UiState<PaginatedResponse<FailedDeliveries>>>(UiState.Loading)
    val failedDeliveriesState: StateFlow<UiState<PaginatedResponse<FailedDeliveries>>> = _failedDeliveriesState.asStateFlow()

    private var _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentPaginatedData: PaginatedResponse<FailedDeliveries>? = null

    val currentData: PaginatedResponse<FailedDeliveries>?
        get() = currentPaginatedData

    fun loadFailedDeliveries(
        filter: String? = null,
        forceRefresh: Boolean = false,
        isLoadMore: Boolean = false
    ): Job {
        if (!forceRefresh && !isLoadMore && _failedDeliveriesState.value is UiState.Success && filter == null) {
            return Job().apply { complete() }
        }

        return viewModelScope.launch {
            if (forceRefresh || !isLoadMore) {
                currentPaginatedData = null
                _failedDeliveriesState.value = UiState.Loading
            } else {
                _isLoadingMore.value = true
            }

            val nextPage = currentPaginatedData.nextPage(isLoadMore)
            when (val result = failedDeliveriesRepository.getAllFailedDeliveries(page = nextPage, size = 25, filter = filter)) {
                is NetworkResult.Success -> {
                    val responseData = result.data
                    val updatedData = if (isLoadMore) currentPaginatedData.appendPage(responseData) else responseData
                    currentPaginatedData = updatedData
                    _failedDeliveriesState.value = UiState.Success(updatedData)
                }
                is NetworkResult.Error -> {
                    _failedDeliveriesState.value = UiState.Error(result.error, result.statusCode)
                }
            }
            _isLoadingMore.value = false
        }
    }

    private val blocklistRepository = ServiceLocator.blocklistRepository

    suspend fun resendFailedDelivery(id: String) = failedDeliveriesRepository.resendFailedDelivery(id)

    suspend fun deleteFailedDelivery(id: String) = failedDeliveriesRepository.deleteFailedDelivery(id)

    suspend fun downloadSpecificFailedDelivery(id: String) = failedDeliveriesRepository.downloadSpecificFailedDelivery(id)

    suspend fun addBlocklistEntry(newBlocklistEntry: host.stjin.anonaddy_shared.models.NewBlocklistEntry) =
        blocklistRepository.addBlocklistEntry(newBlocklistEntry)
}
