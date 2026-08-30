package host.stjin.anonaddy.ui.domains.manage

import android.app.Application
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.network.NetworkResult

class ManageDomainViewModel(application: Application) : BaseViewModel(application) {

    private val domainRepository = ServiceLocator.domainRepository

    suspend fun getDomain(id: String): NetworkResult<Domains> {
        return domainRepository.getSpecificDomain(id)
    }

    suspend fun disableSharedWithFamilyDomain(domainId: String): NetworkResult<String> {
        return domainRepository.disableSharedWithFamilySpecificDomain(domainId)
    }

    suspend fun enableSharedWithFamilyDomain(domainId: String): NetworkResult<Domains> {
        return domainRepository.enableSharedWithFamilySpecificDomain(domainId)
    }

    suspend fun disableCatchAllDomain(domainId: String): NetworkResult<String> {
        return domainRepository.disableCatchAllSpecificDomain(domainId)
    }

    suspend fun enableCatchAllDomain(domainId: String): NetworkResult<Domains> {
        return domainRepository.enableCatchAllSpecificDomain(domainId)
    }

    suspend fun deactivateDomain(domainId: String): NetworkResult<String> {
        return domainRepository.deactivateSpecificDomain(domainId)
    }

    suspend fun activateDomain(domainId: String): NetworkResult<Domains> {
        return domainRepository.activateSpecificDomain(domainId)
    }

    suspend fun deleteDomain(domainId: String): NetworkResult<String> {
        return domainRepository.deleteDomain(domainId)
    }

    suspend fun updateDescriptionDomain(domainId: String, description: String?): NetworkResult<Domains> {
        return domainRepository.updateDescriptionSpecificDomain(domainId, description)
    }

    suspend fun updateFromNameDomain(domainId: String, fromName: String?): NetworkResult<Domains> {
        return domainRepository.updateFromNameSpecificDomain(domainId, fromName)
    }

    suspend fun updateDefaultRecipientDomain(domainId: String, recipientId: String?): NetworkResult<Domains> {
        return domainRepository.updateDefaultRecipientForSpecificDomain(domainId, recipientId)
    }

    private val recipientRepository = ServiceLocator.recipientRepository

    suspend fun getVerifiedRecipients() = recipientRepository.getRecipients(verifiedOnly = true)

    suspend fun updateAutoCreateRegexDomain(domainId: String, autoCreateRegex: String?): NetworkResult<Domains> {
        return domainRepository.updateAutoCreateRegexSpecificDomain(domainId, autoCreateRegex)
    }
}
