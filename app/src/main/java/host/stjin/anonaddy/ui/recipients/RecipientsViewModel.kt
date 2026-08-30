package host.stjin.anonaddy.ui.recipients

import android.app.Application
import androidx.lifecycle.viewModelScope
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipientsViewModel(application: Application) : BaseViewModel(application) {

    private val recipientRepository = ServiceLocator.recipientRepository

    private val _recipientsState = MutableStateFlow<UiState<List<Recipients>>>(UiState.Loading)
    val recipientsState: StateFlow<UiState<List<Recipients>>> = _recipientsState.asStateFlow()

    fun loadRecipients(forceRefresh: Boolean = false, verifiedOnly: Boolean = false): Job {
        if (!forceRefresh && _recipientsState.value is UiState.Success) {
            return Job().apply { complete() }
        }

        return viewModelScope.launch {
            _recipientsState.value = UiState.Loading
            when (val result = recipientRepository.getRecipients(verifiedOnly = verifiedOnly)) {
                is NetworkResult.Success -> {
                    _recipientsState.value = UiState.Success(result.data.data)
                }
                is NetworkResult.Error -> {
                    _recipientsState.value = UiState.Error(result.error, result.statusCode)
                }
            }
        }
    }

    suspend fun deleteRecipient(recipientId: String): NetworkResult<String> {
        return recipientRepository.deleteRecipient(recipientId)
    }

    suspend fun resendVerificationEmail(recipientId: String): NetworkResult<String> {
        return recipientRepository.resendVerificationEmail(recipientId)
    }

    suspend fun addRecipient(email: String): NetworkResult<Recipients> {
        return recipientRepository.addRecipient(email)
    }
}
