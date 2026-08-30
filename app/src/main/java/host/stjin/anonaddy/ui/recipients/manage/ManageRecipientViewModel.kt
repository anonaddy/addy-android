package host.stjin.anonaddy.ui.recipients.manage

import android.app.Application
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.network.NetworkResult

class ManageRecipientViewModel(application: Application) : BaseViewModel(application) {

    private val recipientRepository = ServiceLocator.recipientRepository

    suspend fun getRecipient(id: String): NetworkResult<Recipients> {
        return recipientRepository.getSpecificRecipient(id)
    }

    suspend fun disallowRecipientToReplySend(recipientId: String): NetworkResult<String> {
        return recipientRepository.disallowRecipientToReplySend(recipientId)
    }

    suspend fun allowRecipientToReplySend(recipientId: String): NetworkResult<Recipients> {
        return recipientRepository.allowRecipientToReplySend(recipientId)
    }

    suspend fun disableEncryptionRecipient(recipientId: String): NetworkResult<String> {
        return recipientRepository.disableEncryptionRecipient(recipientId)
    }

    suspend fun enableEncryptionRecipient(recipientId: String): NetworkResult<Recipients> {
        return recipientRepository.enableEncryptionRecipient(recipientId)
    }

    suspend fun disablePgpInlineRecipient(recipientId: String): NetworkResult<String> {
        return recipientRepository.disablePgpInlineRecipient(recipientId)
    }

    suspend fun enablePgpInlineRecipient(recipientId: String): NetworkResult<Recipients> {
        return recipientRepository.enablePgpInlineRecipient(recipientId)
    }

    suspend fun disableRemovePgpKeysRecipients(recipientId: String): NetworkResult<String> {
        return recipientRepository.disableRemovePgpKeysRecipients(recipientId)
    }

    suspend fun enableRemovePgpKeysRecipients(recipientId: String): NetworkResult<Recipients> {
        return recipientRepository.enableRemovePgpKeysRecipients(recipientId)
    }

    suspend fun disableRemovePgpSignaturesRecipients(recipientId: String): NetworkResult<String> {
        return recipientRepository.disableRemovePgpSignaturesRecipients(recipientId)
    }

    suspend fun enableRemovePgpSignaturesRecipients(recipientId: String): NetworkResult<Recipients> {
        return recipientRepository.enableRemovePgpSignaturesRecipients(recipientId)
    }

    suspend fun disableProtectedHeadersRecipient(recipientId: String): NetworkResult<String> {
        return recipientRepository.disableProtectedHeadersRecipient(recipientId)
    }

    suspend fun enableProtectedHeadersRecipient(recipientId: String): NetworkResult<Recipients> {
        return recipientRepository.enableProtectedHeadersRecipient(recipientId)
    }

    suspend fun deleteRecipient(recipientId: String): NetworkResult<String> {
        return recipientRepository.deleteRecipient(recipientId)
    }

    suspend fun removeEncryptionKeyRecipient(recipientId: String): NetworkResult<String> {
        return recipientRepository.removeEncryptionKeyRecipient(recipientId)
    }

    suspend fun deactivateRecipient(recipientId: String): NetworkResult<String> {
        return recipientRepository.deactivateRecipient(recipientId)
    }

    suspend fun activateRecipient(recipientId: String): NetworkResult<Recipients> {
        return recipientRepository.activateRecipient(recipientId)
    }

    suspend fun resendVerificationEmail(recipientId: String): NetworkResult<String> {
        return recipientRepository.resendVerificationEmail(recipientId)
    }

    suspend fun addEncryptionKeyRecipient(recipientId: String, keyData: String): NetworkResult<Recipients> {
        return recipientRepository.addEncryptionKeyRecipient(recipientId, keyData)
    }

    suspend fun updateDescriptionRecipient(recipientId: String, description: String?): NetworkResult<Recipients> {
        return recipientRepository.updateDescriptionSpecificRecipient(recipientId, description)
    }
}
