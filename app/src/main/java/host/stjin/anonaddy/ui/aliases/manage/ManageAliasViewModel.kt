package host.stjin.anonaddy.ui.aliases.manage

import android.app.Application
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.network.NetworkResult

class ManageAliasViewModel(application: Application) : BaseViewModel(application) {

    private val aliasRepository = ServiceLocator.aliasRepository

    suspend fun getAlias(id: String): NetworkResult<Aliases> {
        return aliasRepository.getSpecificAlias(id)
    }

    suspend fun deactivateAlias(aliasId: String): NetworkResult<String> {
        return aliasRepository.deactivateSpecificAlias(aliasId)
    }

    suspend fun activateAlias(aliasId: String): NetworkResult<Aliases> {
        return aliasRepository.activateSpecificAlias(aliasId)
    }

    suspend fun unpinAlias(aliasId: String): NetworkResult<String> {
        return aliasRepository.unpinSpecificAlias(aliasId)
    }

    suspend fun pinAlias(aliasId: String): NetworkResult<Aliases> {
        return aliasRepository.pinSpecificAlias(aliasId)
    }

    suspend fun deactivateAttachedRecipientsOnly(aliasId: String): NetworkResult<String> {
        return aliasRepository.deactivateAttachedRecipientsOnly(aliasId)
    }

    suspend fun activateAttachedRecipientsOnly(aliasId: String): NetworkResult<Aliases> {
        return aliasRepository.activateAttachedRecipientsOnly(aliasId)
    }

    suspend fun deleteAlias(aliasId: String): NetworkResult<String> {
        return aliasRepository.deleteAlias(aliasId)
    }

    suspend fun forgetAlias(aliasId: String): NetworkResult<String> {
        return aliasRepository.forgetAlias(aliasId)
    }

    suspend fun restoreAlias(aliasId: String): NetworkResult<Aliases> {
        return aliasRepository.restoreAlias(aliasId)
    }

    suspend fun updateDescriptionAlias(aliasId: String, description: String?): NetworkResult<Aliases> {
        return aliasRepository.updateDescriptionSpecificAlias(aliasId, description)
    }

    suspend fun updateFromNameAlias(aliasId: String, fromName: String?): NetworkResult<Aliases> {
        return aliasRepository.updateFromNameSpecificAlias(aliasId, fromName)
    }

    private val recipientRepository = ServiceLocator.recipientRepository
    private val labelRepository = ServiceLocator.labelRepository

    suspend fun getVerifiedRecipients() = recipientRepository.getRecipients(verifiedOnly = true)

    suspend fun getAllLabels() = labelRepository.getAllLabels()

    suspend fun bulkUpdateAliasesLabels(aliasIds: List<String>, labelIds: List<String>) =
        aliasRepository.bulkUpdateAliasesLabels(aliasIds, labelIds)

    suspend fun bulkUpdateAliasesRecipients(aliasIds: List<String>, recipientIds: List<String>) =
        aliasRepository.bulkUpdateAliasesRecipients(aliasIds, recipientIds)

    suspend fun bulkDeactivateAlias(aliasIds: List<String>) =
        aliasRepository.bulkDeactivateAlias(aliasIds)

    suspend fun bulkActivateAlias(aliasIds: List<String>) =
        aliasRepository.bulkActivateAlias(aliasIds)

    suspend fun bulkUnpinAlias(aliasIds: List<String>) =
        aliasRepository.bulkUnpinAlias(aliasIds)

    suspend fun bulkPinAlias(aliasIds: List<String>) =
        aliasRepository.bulkPinAlias(aliasIds)

    suspend fun bulkDeleteAlias(aliasIds: List<String>) =
        aliasRepository.bulkDeleteAlias(aliasIds)

    suspend fun bulkForgetAlias(aliasIds: List<String>) =
        aliasRepository.bulkForgetAlias(aliasIds)

    suspend fun bulkRestoreAlias(aliasIds: List<String>) =
        aliasRepository.bulkRestoreAlias(aliasIds)

    suspend fun updateRecipientsAlias(aliasId: String, recipientIds: List<String>): NetworkResult<Aliases> {
        return aliasRepository.updateRecipientsSpecificAlias(aliasId, recipientIds)
    }
}
