package host.stjin.anonaddy_shared.repositories

import android.content.Context
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

class RulesRepository(
    context: Context,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BaseNetworkClient(context, dispatchers) {

typealias RuleRepository = RulesRepository

    suspend fun getAllRules(): NetworkResult<PaginatedResponse<Rules>> {
        waitForInit()

        val response = executeGet(API_URL_RULES)

        return handleResponse(response, "getAllRules") { gson.fromJson(it) }
    }

    suspend fun getSpecificRule(ruleId: String): NetworkResult<Rules> {
        waitForInit()

        val response = executeGet("$API_URL_RULES/$ruleId")

        return handleResponse(response, "getSpecificRule") { gson.fromJson(it, SingleRule::class.java).data }
    }

    suspend fun deleteRule(ruleId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_RULES/$ruleId")

        return handleStatusResponse(response, "deleteRule", expectedCode = 204)
    }

    suspend fun createRule(rule: Rules): NetworkResult<Rules> {
        waitForInit()

        // Ensure actions with setAliasDescription have an empty string value rather than null so it serializes properly
        rule.actions.forEachIndexed { index, action ->
            if (action.type == "setAliasDescription" && action.value == null) {
                rule.actions[index] = action.copy(value = "")
            }
        }

        val ruleJson = gson.toJson(rule)
        val response = executePost(API_URL_RULES, ruleJson)

        return handleResponse(response, "createRule") { gson.fromJson(it, SingleRule::class.java).data }
    }

    suspend fun updateRule(ruleId: String, rule: Rules): NetworkResult<String> {
        waitForInit()

        // Ensure actions with setAliasDescription have an empty string value rather than null so it serializes properly
        rule.actions.forEachIndexed { index, action ->
            if (action.type == "setAliasDescription" && action.value == null) {
                rule.actions[index] = action.copy(value = "")
            }
        }

        val ruleJson = gson.toJson(rule)
        val response = executePatch("$API_URL_RULES/$ruleId", ruleJson)

        return handleStatusResponse(response, "updateRule", expectedCode = 200)
    }

    suspend fun reorderRules(rulesArray: List<Rules>): NetworkResult<String> {
        waitForInit()

        val array = JSONArray(rulesArray.map { it.id })
        val obj = JSONObject().put("ids", array)
        val response = executePost(API_URL_REORDER_RULES, obj.toString())

        return handleStatusResponse(response, "reorderRules", expectedCode = 200)
    }

    suspend fun activateSpecificRule(ruleId: String): NetworkResult<Rules> {
        waitForInit()

        val response = executePost(API_URL_ACTIVE_RULES, JSONObject().put("id", ruleId).toString())

        return handleResponse(response, "activateSpecificRule") { gson.fromJson(it, SingleRule::class.java).data }
    }

    suspend fun deactivateSpecificRule(ruleId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_ACTIVE_RULES/$ruleId")

        return handleStatusResponse(response, "deactivateSpecificRule", expectedCode = 204)
    }
}
