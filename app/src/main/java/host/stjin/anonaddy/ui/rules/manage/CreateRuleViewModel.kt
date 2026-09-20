package host.stjin.anonaddy.ui.rules.manage

import android.app.Application
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.models.Rules
import host.stjin.anonaddy_shared.network.NetworkResult

class CreateRuleViewModel(application: Application) : BaseViewModel(application) {

    private val rulesRepository = ServiceLocator.rulesRepository
    private val recipientRepository = ServiceLocator.recipientRepository

    suspend fun getSpecificRule(ruleId: String): NetworkResult<Rules> {
        return rulesRepository.getSpecificRule(ruleId)
    }

    suspend fun createRule(rule: Rules): NetworkResult<Rules> {
        return rulesRepository.createRule(rule)
    }

    suspend fun updateRule(ruleId: String, rule: Rules): NetworkResult<String> {
        return rulesRepository.updateRule(ruleId, rule)
    }

    suspend fun getAllRecipients(): NetworkResult<PaginatedResponse<Recipients>> {
        return recipientRepository.getRecipients(verifiedOnly = false)
    }
}
