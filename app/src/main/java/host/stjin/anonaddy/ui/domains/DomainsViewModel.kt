package host.stjin.anonaddy.ui.domains

import android.app.Application
import androidx.lifecycle.viewModelScope
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DomainsViewModel(application: Application) : BaseViewModel(application) {

    private val domainRepository = ServiceLocator.domainRepository

    private val _domainsState = MutableStateFlow<UiState<List<Domains>>>(UiState.Loading)
    val domainsState: StateFlow<UiState<List<Domains>>> = _domainsState.asStateFlow()

    fun loadDomains(forceRefresh: Boolean = false): Job {
        if (!forceRefresh && _domainsState.value is UiState.Success) {
            return Job().apply { complete() }
        }

        return viewModelScope.launch {
            _domainsState.value = UiState.Loading
            when (val result = domainRepository.getAllDomains()) {
                is NetworkResult.Success -> {
                    _domainsState.value = UiState.Success(result.data.data)
                }
                is NetworkResult.Error -> {
                    _domainsState.value = UiState.Error(result.error, result.statusCode)
                }
            }
        }
    }

    suspend fun deleteDomain(domainId: String): NetworkResult<String> {
        return domainRepository.deleteDomain(domainId)
    }

    suspend fun addDomain(domain: String): NetworkResult<Domains> {
        return domainRepository.addDomain(domain)
    }
}
