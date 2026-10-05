package host.stjin.anonaddy.ui.usernames

import android.app.Application
import androidx.lifecycle.viewModelScope
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy_shared.models.Usernames
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UsernamesViewModel(application: Application) : BaseViewModel(application) {

    private val usernameRepository = ServiceLocator.usernameRepository

    private val _usernamesState = MutableStateFlow<UiState<List<Usernames>>>(UiState.Loading)
    val usernamesState: StateFlow<UiState<List<Usernames>>> = _usernamesState.asStateFlow()

    fun loadUsernames(forceRefresh: Boolean = false): Job {
        if (!forceRefresh && _usernamesState.value is UiState.Success) {
            return Job().apply { complete() }
        }

        return viewModelScope.launch {
            _usernamesState.value = UiState.Loading
            when (val result = usernameRepository.getAllUsernames()) {
                is NetworkResult.Success -> {
                    _usernamesState.value = UiState.Success(result.data.data)
                }
                is NetworkResult.Error -> {
                    _usernamesState.value = UiState.Error(result.error, result.statusCode)
                }
            }
        }
    }

    suspend fun deleteUsername(usernameId: String): NetworkResult<String> {
        return usernameRepository.deleteUsername(usernameId)
    }

    suspend fun addUsername(username: String): NetworkResult<Usernames> {
        return usernameRepository.addUsername(username)
    }
}
