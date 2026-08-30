package host.stjin.anonaddy.ui.blocklist

import android.app.Application
import androidx.lifecycle.viewModelScope
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy_shared.models.BlocklistEntries
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.appendPage
import host.stjin.anonaddy_shared.models.nextPage
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BlocklistViewModel(application: Application) : BaseViewModel(application) {

    private val blocklistRepository = ServiceLocator.blocklistRepository

    private val _blocklistState = MutableStateFlow<UiState<PaginatedResponse<BlocklistEntries>>>(UiState.Loading)
    val blocklistState: StateFlow<UiState<PaginatedResponse<BlocklistEntries>>> = _blocklistState.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentPaginatedData: PaginatedResponse<BlocklistEntries>? = null

    val currentData: PaginatedResponse<BlocklistEntries>?
        get() = currentPaginatedData

    fun loadBlocklist(
        filter: String? = null,
        search: String? = null,
        forceRefresh: Boolean = false,
        isLoadMore: Boolean = false
    ): Job {
        if (!forceRefresh && !isLoadMore && _blocklistState.value is UiState.Success && filter == null && search == null) {
            return Job().apply { complete() }
        }

        return viewModelScope.launch {
            if (isLoadMore) {
                _isLoadingMore.value = true
            } else if (forceRefresh || currentPaginatedData == null) {
                currentPaginatedData = null
                _blocklistState.value = UiState.Loading
            }

            try {
                val nextPage = currentPaginatedData.nextPage(isLoadMore)
                when (val result = blocklistRepository.getAllBlocklistEntries(page = nextPage, size = 25, filter = filter, search = search)) {
                    is NetworkResult.Success -> {
                        val responseData = result.data
                        val updatedData = if (isLoadMore) currentPaginatedData.appendPage(responseData) else responseData
                        currentPaginatedData = updatedData
                        _blocklistState.value = UiState.Success(updatedData)
                    }
                    is NetworkResult.Error -> {
                        _blocklistState.value = UiState.Error(result.error, result.statusCode)
                    }
                }
            } finally {
                if (isLoadMore) {
                    _isLoadingMore.value = false
                }
            }
        }
    }

    suspend fun deleteBlocklistEntry(blocklistId: String): NetworkResult<String> {
        return blocklistRepository.deleteBlocklistEntry(blocklistId)
    }

    suspend fun addBlocklistEntry(newBlocklistEntry: host.stjin.anonaddy_shared.models.NewBlocklistEntry): NetworkResult<BlocklistEntries> {
        return blocklistRepository.addBlocklistEntry(newBlocklistEntry)
    }
}
