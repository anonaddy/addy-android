package host.stjin.anonaddy_shared.repositories

import android.content.Context
import host.stjin.anonaddy_shared.AddyIo.API_URL_ACTIVE_DOMAINS
import host.stjin.anonaddy_shared.AddyIo.API_URL_CATCH_ALL_DOMAINS
import host.stjin.anonaddy_shared.AddyIo.API_URL_DOMAINS
import host.stjin.anonaddy_shared.AddyIo.API_URL_DOMAIN_OPTIONS
import host.stjin.anonaddy_shared.AddyIo.API_URL_SHARED_WITH_FAMILY_DOMAINS
import host.stjin.anonaddy_shared.models.CheckDomainSendingResponse
import host.stjin.anonaddy_shared.models.DomainOptions
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.SingleDomain
import host.stjin.anonaddy_shared.network.BaseNetworkClient
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.DefaultDispatcherProvider
import host.stjin.anonaddy_shared.utils.DispatcherProvider
import host.stjin.anonaddy_shared.utils.fromJson
import org.json.JSONObject

class DomainRepository(
    context: Context,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BaseNetworkClient(context, dispatchers) {

    suspend fun getDomainOptions(): NetworkResult<DomainOptions> {
        waitForInit()

        val response = executeGet(API_URL_DOMAIN_OPTIONS)

        return handleResponse(response, "getDomainOptions") { gson.fromJson(it, DomainOptions::class.java) }
    }

    suspend fun getAllDomains(): NetworkResult<PaginatedResponse<Domains>> {
        waitForInit()

        val response = executeGet(API_URL_DOMAINS)

        return handleResponse(response, "getAllDomains") { gson.fromJson(it) }
    }

    suspend fun getSpecificDomain(domainId: String): NetworkResult<Domains> {
        waitForInit()

        val response = executeGet("$API_URL_DOMAINS/$domainId")

        return handleResponse(response, "getSpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun addDomain(domain: String): NetworkResult<Domains> {
        waitForInit()

        val json = JSONObject().put("domain", domain)
        val response = executePost(API_URL_DOMAINS, json.toString())

        return handleResponse(response, "addDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun deleteDomain(domainId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_DOMAINS/$domainId")

        return handleStatusResponse(response, "deleteDomain", expectedCode = 204)
    }

    suspend fun activateSpecificDomain(domainId: String): NetworkResult<Domains> {
        waitForInit()

        val response = executePost(API_URL_ACTIVE_DOMAINS, JSONObject().put("id", domainId).toString())

        return handleResponse(response, "activateSpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun deactivateSpecificDomain(domainId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_ACTIVE_DOMAINS/$domainId")

        return handleStatusResponse(response, "deactivateSpecificDomain", expectedCode = 204)
    }

    suspend fun enableCatchAllSpecificDomain(domainId: String): NetworkResult<Domains> {
        waitForInit()

        val response = executePost(API_URL_CATCH_ALL_DOMAINS, JSONObject().put("id", domainId).toString())

        return handleResponse(response, "enableCatchAllSpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun disableCatchAllSpecificDomain(domainId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_CATCH_ALL_DOMAINS/$domainId")

        return handleStatusResponse(response, "disableCatchAllSpecificDomain", expectedCode = 204)
    }

    suspend fun enableSharedWithFamilySpecificDomain(domainId: String): NetworkResult<Domains> {
        waitForInit()

        val response = executePost(API_URL_SHARED_WITH_FAMILY_DOMAINS, JSONObject().put("id", domainId).toString())

        return handleResponse(response, "enableSharedWithFamilySpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun disableSharedWithFamilySpecificDomain(domainId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_SHARED_WITH_FAMILY_DOMAINS/$domainId")

        return handleStatusResponse(response, "disableSharedWithFamilySpecificDomain", expectedCode = 204)
    }

    suspend fun updateDefaultRecipientForSpecificDomain(domainId: String, recipientId: String?): NetworkResult<Domains> {
        waitForInit()

        val json = JSONObject().put("default_recipient", recipientId)
        val response = executePatch("$API_URL_DOMAINS/$domainId/default-recipient", json.toString())

        return handleResponse(response, "updateDefaultRecipientForSpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun updateDescriptionSpecificDomain(domainId: String, description: String?): NetworkResult<Domains> {
        waitForInit()

        val json = JSONObject().put("description", description)
        val response = executePatch("$API_URL_DOMAINS/$domainId", json.toString())

        return handleResponse(response, "updateDescriptionSpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun updateAutoCreateRegexSpecificDomain(domainId: String, autoCreateRegex: String?): NetworkResult<Domains> {
        waitForInit()

        val json = JSONObject().put("auto_create_regex", autoCreateRegex)
        val response = executePatch("$API_URL_DOMAINS/$domainId", json.toString())

        return handleResponse(response, "updateAutoCreateRegexSpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun updateFromNameSpecificDomain(domainId: String, fromName: String?): NetworkResult<Domains> {
        waitForInit()

        val json = JSONObject().put("from_name", fromName)
        val response = executePatch("$API_URL_DOMAINS/$domainId", json.toString())

        return handleResponse(response, "updateFromNameSpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun checkDomainSending(domainId: String): NetworkResult<CheckDomainSendingResponse> {
        waitForInit()

        val response = executePost("$API_URL_DOMAINS/$domainId/check-sending")

        return handleResponse(response, "checkDomainSending") { gson.fromJson(it, CheckDomainSendingResponse::class.java) }
    }
}
