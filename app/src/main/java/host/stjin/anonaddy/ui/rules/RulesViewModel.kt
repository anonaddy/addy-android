package host.stjin.anonaddy.ui.rules

import android.app.Application
import androidx.lifecycle.viewModelScope
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.models.Rules
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RulesScreenData(
    val rules: List<Rules>,
    val recipients: List<Recipients>
)

class RulesViewModel(application: Application) : BaseViewModel(application) {

    private val ruleRepository = ServiceLocator.ruleRepository
    private val recipientRepository = ServiceLocator.recipientRepository

    private val _rulesState = MutableStateFlow<UiState<RulesScreenData>>(UiState.Loading)
    val rulesState: StateFlow<UiState<RulesScreenData>> = _rulesState.asStateFlow()

    fun loadRules(forceRefresh: Boolean = false): Job {
        if (!forceRefresh && _rulesState.value is UiState.Success) {
            return Job().apply { complete() }
        }

        return viewModelScope.launch {
            _rulesState.value = UiState.Loading
            val rulesDeferred = async { ruleRepository.getAllRules() }
            val recipientsDeferred = async { recipientRepository.getRecipients() }

            val rulesResult = rulesDeferred.await()
            val recipientsResult = recipientsDeferred.await()

            if (rulesResult is NetworkResult.Success) {
                val recipientsList = if (recipientsResult is NetworkResult.Success) {
                    recipientsResult.data.data
                } else {
                    emptyList()
                }
                _rulesState.value = UiState.Success(
                    RulesScreenData(
                        rules = rulesResult.data.data,
                        recipients = recipientsList
                    )
                )
            } else if (rulesResult is NetworkResult.Error) {
                _rulesState.value = UiState.Error(rulesResult.error, rulesResult.statusCode)
            }
        }
    }

    suspend fun activateRule(ruleId: String): NetworkResult<Rules> {
        return ruleRepository.activateSpecificRule(ruleId)
    }

    suspend fun deactivateRule(ruleId: String): NetworkResult<String> {
        return ruleRepository.deactivateSpecificRule(ruleId)
    }

    suspend fun deleteRule(ruleId: String): NetworkResult<String> {
        return ruleRepository.deleteRule(ruleId)
    }

    suspend fun reorderRules(rulesList: List<Rules>): NetworkResult<String> {
        return ruleRepository.reorderRules(rulesList)
    }
}
