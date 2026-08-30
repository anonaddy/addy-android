package host.stjin.anonaddy_shared.repositories

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import host.stjin.anonaddy_shared.AddyIo.API_URL_ACTIVE_RULES
import host.stjin.anonaddy_shared.AddyIo.API_URL_REORDER_RULES
import host.stjin.anonaddy_shared.AddyIo.API_URL_RULES
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.Rules
import host.stjin.anonaddy_shared.models.SingleRule
import host.stjin.anonaddy_shared.network.BaseNetworkClient
import host.stjin.anonaddy_shared.network.NetworkResult
import org.json.JSONArray
import org.json.JSONObject
import host.stjin.anonaddy_shared.utils.DefaultDispatcherProvider
import host.stjin.anonaddy_shared.utils.DispatcherProvider
import host.stjin.anonaddy_shared.utils.fromJson

class RuleRepository(
    context: Context,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BaseNetworkClient(context, dispatchers) {

    suspend fun getAllRules(page: Int? = null, size: Int? = 20): NetworkResult<PaginatedResponse<Rules>> {
        waitForInit()

        val parameters = arrayListOf<Pair<String, String>>()
        if (size != null) parameters.add("page[size]" to size.toString())
        if (page != null) parameters.add("page[number]" to page.toString())

        val (_, response, result) = Fuel.get(API_URL_RULES, parameters)
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleResponse(response, result, "getAllRules") { gson.fromJson(it) }
    }

    suspend fun getSpecificRule(ruleId: String): NetworkResult<Rules> {
        waitForInit()

        val (_, response, result) = Fuel.get("$API_URL_RULES/$ruleId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleResponse(response, result, "getSpecificRule") { gson.fromJson(it, SingleRule::class.java).data }
    }

    suspend fun deleteRule(ruleId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_RULES/$ruleId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "deleteRule", expectedCode = 204)
    }

    suspend fun createRule(rule: Rules): NetworkResult<Rules> {
        waitForInit()

        val ruleJson = gson.toJson(rule)
        val (_, response, result) = Fuel.post(API_URL_RULES)
            .appendHeader(*getHeaders())
            .body(ruleJson)
            .awaitStringResponseResult()

        return handleResponse(response, result, "createRule") { gson.fromJson(it, SingleRule::class.java).data }
    }

    suspend fun updateRule(ruleId: String, rule: Rules): NetworkResult<String> {
        waitForInit()

        val ruleJson = gson.toJson(rule)
        val (_, response, result) = Fuel.patch("$API_URL_RULES/$ruleId")
            .appendHeader(*getHeaders())
            .body(ruleJson)
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "updateRule", expectedCode = 200)
    }

    suspend fun reorderRules(rulesArray: List<Rules>): NetworkResult<String> {
        waitForInit()

        val array = JSONArray(rulesArray.map { it.id })
        val obj = JSONObject().put("ids", array)
        val (_, response, result) = Fuel.post(API_URL_REORDER_RULES)
            .appendHeader(*getHeaders())
            .body(obj.toString())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "reorderRules", expectedCode = 200)
    }

    suspend fun activateSpecificRule(ruleId: String): NetworkResult<Rules> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_ACTIVE_RULES)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", ruleId).toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "activateSpecificRule") { gson.fromJson(it, SingleRule::class.java).data }
    }

    suspend fun deactivateSpecificRule(ruleId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_ACTIVE_RULES/$ruleId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "deactivateSpecificRule", expectedCode = 204)
    }
}
