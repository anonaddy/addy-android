package host.stjin.anonaddy.ui.aliases

import android.app.Application
import androidx.lifecycle.viewModelScope
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.appendPage
import host.stjin.anonaddy_shared.models.nextPage
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AliasesViewModel(application: Application) : BaseViewModel(application) {

    private val aliasRepository = ServiceLocator.aliasRepository
    private val aliasWatcher = ServiceLocator.aliasWatcher

    private val _aliasesState = MutableStateFlow<UiState<PaginatedResponse<Aliases>>>(UiState.Loading)
    val aliasesState: StateFlow<UiState<PaginatedResponse<Aliases>>> = _aliasesState.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentPaginatedData: PaginatedResponse<Aliases>? = null

    val currentData: PaginatedResponse<Aliases>?
        get() = currentPaginatedData

    fun loadAliases(
        aliasSortFilter: AliasSortFilter,
        forceRefresh: Boolean = false,
        isLoadMore: Boolean = false
    ): Job {
        if (!forceRefresh && !isLoadMore && _aliasesState.value is UiState.Success) {
            return Job().apply { complete() }
        }

        return viewModelScope.launch {
            if (isLoadMore) {
                _isLoadingMore.value = true
            } else if (forceRefresh || currentPaginatedData == null) {
                currentPaginatedData = null
                _aliasesState.value = UiState.Loading
            }

            try {
                if (aliasSortFilter.onlyWatchedAliases) {
                    val aliasesToWatch = aliasWatcher.getAliasesToWatch().toList()
                    if (aliasesToWatch.isNotEmpty()) {
                        when (val result = aliasRepository.bulkGetAlias(aliasesToWatch)) {
                            is NetworkResult.Success -> {
                                val paginatedResponse = PaginatedResponse(result.data.data, links = null, meta = null)
                                currentPaginatedData = paginatedResponse
                                _aliasesState.value = UiState.Success(paginatedResponse)
                            }
                            is NetworkResult.Error -> {
                                _aliasesState.value = UiState.Error(result.error, result.statusCode)
                            }
                        }
                    } else {
                        val emptyResponse = PaginatedResponse<Aliases>(arrayListOf(), links = null, meta = null)
                        currentPaginatedData = emptyResponse
                        _aliasesState.value = UiState.Success(emptyResponse)
                    }
                } else {
                    val nextPage = currentPaginatedData.nextPage(isLoadMore)
                    when (val result = aliasRepository.getAliases(
                        aliasSortFilter = aliasSortFilter,
                        page = nextPage,
                        size = 25
                    )) {
                        is NetworkResult.Success -> {
                            val responseData = result.data
                            val updatedData = if (isLoadMore) currentPaginatedData.appendPage(responseData) else responseData
                            currentPaginatedData = updatedData
                            _aliasesState.value = UiState.Success(updatedData)
                        }
                        is NetworkResult.Error -> {
                            _aliasesState.value = UiState.Error(result.error, result.statusCode)
                        }
                    }
                }
            } finally {
                if (isLoadMore) {
                    _isLoadingMore.value = false
                }
            }
        }
    }

    private val domainRepository = ServiceLocator.domainRepository
    private val recipientRepository = ServiceLocator.recipientRepository
    private val labelRepository = ServiceLocator.labelRepository

    suspend fun getDomainOptions() = domainRepository.getDomainOptions()

    suspend fun getVerifiedRecipients() = recipientRepository.getRecipients(verifiedOnly = true)

    suspend fun getAllLabels() = labelRepository.getAllLabels()

    suspend fun addAlias(
        domain: String,
        description: String,
        format: String,
        aliasLocalPart: String,
        recipients: ArrayList<String>,
        labels: ArrayList<String>
    ): NetworkResult<Aliases> {
        return aliasRepository.addAlias(domain, description, format, aliasLocalPart, recipients, labels)
    }

    fun getAliasesToWatch() = ServiceLocator.aliasWatcher.getAliasesToWatch()
}
