package host.stjin.anonaddy.ui.aliases

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.CacheHelper
import kotlinx.coroutines.launch

import host.stjin.anonaddy.ui.base.BaseViewModel

class AliasesViewModel(application: Application) : BaseViewModel(application) {

    var aliasesList by mutableStateOf<List<Aliases>>(listOf())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val aliasRepository = ServiceLocator.aliasRepository

    init {
        refreshAliasesFromCache()
    }

    fun refreshAliasesFromCache() {
        val cached = CacheHelper.getBackgroundServiceCacheLastUpdatedAliasesData(getApplication())
        if (cached != null) {
            aliasesList = cached.toList()
        }
    }

    fun downloadAliases(baseError: String, localNetworkPermissionRationale: String, isLocalAddress: Boolean, hasLocalNetworkPermission: Boolean) {
        if (aliasesList.isEmpty()) {
            isLoading = true
        }
        viewModelScope.launch {
            launch { ServiceLocator.userRepository.cacheUserResourceForWidget() }
            val result = aliasRepository.cacheLastUpdatedAliasesData()
            isLoading = false
            if (result is NetworkResult.Success && result.data) {
                errorMessage = null
                val updated = CacheHelper.getBackgroundServiceCacheLastUpdatedAliasesData(getApplication())
                if (updated != null) {
                    aliasesList = updated.toList()
                }
            } else {
                if (aliasesList.isEmpty()) {
                    errorMessage = if (isLocalAddress && !hasLocalNetworkPermission) {
                        baseError + "\n\n" + localNetworkPermissionRationale
                    } else {
                        baseError
                    }
                }
            }
        }
    }
}
