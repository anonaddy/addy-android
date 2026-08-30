package host.stjin.anonaddy_shared.repositories

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import host.stjin.anonaddy_shared.AddyIo.API_URL_ACTIVE_DOMAINS
import host.stjin.anonaddy_shared.AddyIo.API_URL_CATCH_ALL_DOMAINS
import host.stjin.anonaddy_shared.AddyIo.API_URL_DOMAINS
import host.stjin.anonaddy_shared.AddyIo.API_URL_DOMAIN_OPTIONS
import host.stjin.anonaddy_shared.AddyIo.API_URL_SHARED_WITH_FAMILY_DOMAINS
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

        val (_, response, result) = Fuel.get(API_URL_DOMAIN_OPTIONS)
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleResponse(response, result, "getDomainOptions") { gson.fromJson(it, DomainOptions::class.java) }
    }

    suspend fun getAllDomains(): NetworkResult<PaginatedResponse<Domains>> {
        waitForInit()

        val (_, response, result) = Fuel.get(API_URL_DOMAINS)
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleResponse(response, result, "getAllDomains") { gson.fromJson(it) }
    }

    suspend fun getSpecificDomain(domainId: String): NetworkResult<Domains> {
        waitForInit()

        val (_, response, result) = Fuel.get("$API_URL_DOMAINS/$domainId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleResponse(response, result, "getSpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun addDomain(domain: String): NetworkResult<Domains> {
        waitForInit()

        val json = JSONObject().put("domain", domain)
        val (_, response, result) = Fuel.post(API_URL_DOMAINS)
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "addDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun deleteDomain(domainId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_DOMAINS/$domainId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "deleteDomain", expectedCode = 204)
    }

    suspend fun activateSpecificDomain(domainId: String): NetworkResult<Domains> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_ACTIVE_DOMAINS)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", domainId).toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "activateSpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun deactivateSpecificDomain(domainId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_ACTIVE_DOMAINS/$domainId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "deactivateSpecificDomain", expectedCode = 204)
    }

    suspend fun enableCatchAllSpecificDomain(domainId: String): NetworkResult<Domains> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_CATCH_ALL_DOMAINS)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", domainId).toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "enableCatchAllSpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun disableCatchAllSpecificDomain(domainId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_CATCH_ALL_DOMAINS/$domainId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "disableCatchAllSpecificDomain", expectedCode = 204)
    }

    suspend fun enableSharedWithFamilySpecificDomain(domainId: String): NetworkResult<Domains> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_SHARED_WITH_FAMILY_DOMAINS)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", domainId).toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "enableSharedWithFamilySpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun disableSharedWithFamilySpecificDomain(domainId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_SHARED_WITH_FAMILY_DOMAINS/$domainId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "disableSharedWithFamilySpecificDomain", expectedCode = 204)
    }

    suspend fun updateDefaultRecipientForSpecificDomain(domainId: String, recipientId: String?): NetworkResult<Domains> {
        waitForInit()

        val json = JSONObject().put("default_recipient", recipientId)
        val (_, response, result) = Fuel.patch("$API_URL_DOMAINS/$domainId/default-recipient")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "updateDefaultRecipientForSpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun updateDescriptionSpecificDomain(domainId: String, description: String?): NetworkResult<Domains> {
        waitForInit()

        val json = JSONObject().put("description", description)
        val (_, response, result) = Fuel.patch("$API_URL_DOMAINS/$domainId")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "updateDescriptionSpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun updateAutoCreateRegexSpecificDomain(domainId: String, autoCreateRegex: String?): NetworkResult<Domains> {
        waitForInit()

        val json = JSONObject().put("auto_create_regex", autoCreateRegex)
        val (_, response, result) = Fuel.patch("$API_URL_DOMAINS/$domainId")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "updateAutoCreateRegexSpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }

    suspend fun updateFromNameSpecificDomain(domainId: String, fromName: String?): NetworkResult<Domains> {
        waitForInit()

        val json = JSONObject().put("from_name", fromName)
        val (_, response, result) = Fuel.patch("$API_URL_DOMAINS/$domainId")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "updateFromNameSpecificDomain") { gson.fromJson(it, SingleDomain::class.java).data }
    }
}
