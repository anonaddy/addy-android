package host.stjin.anonaddy.ui.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {
    protected val userRepository = ServiceLocator.userRepository

    private val _userResourceState = MutableStateFlow(
        (application as? AddyIoApp)?.userResourceOrNull?.let { UiState.Success(it) } ?: UiState.Loading
    )
    val userResourceState: StateFlow<UiState<UserResource>> = _userResourceState.asStateFlow()

    /**
     * Loads the user resource and updates the global cache in AddyIoApp.
     */
    fun loadUserResource(forceRefresh: Boolean = false): Job {
        return viewModelScope.launch {
            if (forceRefresh || _userResourceState.value !is UiState.Success) {
                when (val result = userRepository.getUserResource()) {
                    is NetworkResult.Success -> {
                        (getApplication() as? AddyIoApp)?.userResource = result.data
                        _userResourceState.value = UiState.Success(result.data)
                    }
                    is NetworkResult.Error -> {
                        _userResourceState.value = UiState.Error(result.error, result.statusCode)
                    }
                }
            }
        }
    }
    
    /**
     * Refreshes the user resource specifically for fragments that need the latest stats.
     */
    suspend fun refreshUserResource(): NetworkResult<UserResource> {
        return when (val result = userRepository.getUserResource()) {
            is NetworkResult.Success -> {
                (getApplication() as? AddyIoApp)?.userResource = result.data
                _userResourceState.value = UiState.Success(result.data)
                result
            }
            is NetworkResult.Error -> {
                // If it's a refresh, we might not want to override the current Success state if it fails?
                // But for consistency with existing logic:
                _userResourceState.value = UiState.Error(result.error, result.statusCode)
                result
            }
        }
    }
}
