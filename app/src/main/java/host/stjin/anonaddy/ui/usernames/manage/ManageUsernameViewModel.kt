package host.stjin.anonaddy.ui.usernames.manage

import android.app.Application
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.Usernames
import host.stjin.anonaddy_shared.network.NetworkResult

class ManageUsernameViewModel(application: Application) : BaseViewModel(application) {

    private val usernameRepository = ServiceLocator.usernameRepository

    suspend fun getUsername(id: String): NetworkResult<Usernames> {
        return usernameRepository.getSpecificUsername(id)
    }

    suspend fun disableCanLoginUsername(usernameId: String): NetworkResult<String> {
        return usernameRepository.disableCanLoginSpecificUsername(usernameId)
    }

    suspend fun enableCanLoginUsername(usernameId: String): NetworkResult<Usernames> {
        return usernameRepository.enableCanLoginSpecificUsername(usernameId)
    }

    suspend fun disableCatchAllUsername(usernameId: String): NetworkResult<String> {
        return usernameRepository.disableCatchAllSpecificUsername(usernameId)
    }

    suspend fun enableCatchAllUsername(usernameId: String): NetworkResult<Usernames> {
        return usernameRepository.enableCatchAllSpecificUsername(usernameId)
    }

    suspend fun deactivateUsername(usernameId: String): NetworkResult<String> {
        return usernameRepository.deactivateSpecificUsername(usernameId)
    }

    suspend fun activateUsername(usernameId: String): NetworkResult<Usernames> {
        return usernameRepository.activateSpecificUsername(usernameId)
    }

    suspend fun deleteUsername(usernameId: String): NetworkResult<String> {
        return usernameRepository.deleteUsername(usernameId)
    }

    suspend fun updateDescriptionUsername(usernameId: String, description: String?): NetworkResult<Usernames> {
        return usernameRepository.updateDescriptionSpecificUsername(usernameId, description)
    }

    suspend fun updateFromNameUsername(usernameId: String, fromName: String?): NetworkResult<Usernames> {
        return usernameRepository.updateFromNameSpecificUsername(usernameId, fromName)
    }

    suspend fun updateDefaultRecipientUsername(usernameId: String, recipientId: String?): NetworkResult<Usernames> {
        return usernameRepository.updateDefaultRecipientForSpecificUsername(usernameId, recipientId)
    }

    private val recipientRepository = ServiceLocator.recipientRepository

    suspend fun getVerifiedRecipients() = recipientRepository.getRecipients(verifiedOnly = true)

    suspend fun updateAutoCreateRegexUsername(usernameId: String, autoCreateRegex: String?): NetworkResult<Usernames> {
        return usernameRepository.updateAutoCreateRegexSpecificUsername(usernameId, autoCreateRegex)
    }
}
