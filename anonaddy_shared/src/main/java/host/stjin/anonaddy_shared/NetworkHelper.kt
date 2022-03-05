package host.stjin.anonaddy_shared

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.einmalfel.earl.EarlParser
import com.einmalfel.earl.Feed
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.google.gson.Gson
import host.stjin.anonaddy_shared.AnonAddy.API_BASE_URL
import host.stjin.anonaddy_shared.AnonAddy.API_URL_ACCOUNT_DETAILS
import host.stjin.anonaddy_shared.AnonAddy.API_URL_ACTIVE_ALIAS
import host.stjin.anonaddy_shared.AnonAddy.API_URL_ACTIVE_DOMAINS
import host.stjin.anonaddy_shared.AnonAddy.API_URL_ACTIVE_RULES
import host.stjin.anonaddy_shared.AnonAddy.API_URL_ACTIVE_USERNAMES
import host.stjin.anonaddy_shared.AnonAddy.API_URL_ALIAS
import host.stjin.anonaddy_shared.AnonAddy.API_URL_ALIAS_RECIPIENTS
import host.stjin.anonaddy_shared.AnonAddy.API_URL_APP_VERSION
import host.stjin.anonaddy_shared.AnonAddy.API_URL_CATCH_ALL_DOMAINS
import host.stjin.anonaddy_shared.AnonAddy.API_URL_DOMAINS
import host.stjin.anonaddy_shared.AnonAddy.API_URL_DOMAIN_OPTIONS
import host.stjin.anonaddy_shared.AnonAddy.API_URL_ENCRYPTED_RECIPIENTS
import host.stjin.anonaddy_shared.AnonAddy.API_URL_FAILED_DELIVERIES
import host.stjin.anonaddy_shared.AnonAddy.API_URL_RECIPIENTS
import host.stjin.anonaddy_shared.AnonAddy.API_URL_RECIPIENT_KEYS
import host.stjin.anonaddy_shared.AnonAddy.API_URL_RECIPIENT_RESEND
import host.stjin.anonaddy_shared.AnonAddy.API_URL_REORDER_RULES
import host.stjin.anonaddy_shared.AnonAddy.API_URL_RULES
import host.stjin.anonaddy_shared.AnonAddy.API_URL_USERNAMES
import host.stjin.anonaddy_shared.AnonAddy.GITLAB_TAGS_RSS_FEED
import host.stjin.anonaddy_shared.AnonAddy.lazyMgr
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.*
import host.stjin.anonaddy_shared.utils.LoggingHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream


class NetworkHelper(private val context: Context) {

    /*https://app.anonaddy.com/docs/#errors
    400	Bad Request -- Your request sucks
    401	Unauthenticated -- Your API key is wrong
    403	Forbidden -- You do not have permission to access the requested resource
    404	Not Found -- The specified resource could not be found
    405	Method Not Allowed -- You tried to access an endpoint with an invalid method
    422	Validation Error -- The given data was invalid
    429	Too Many Requests -- You're sending too many requests or have reached your limit for new aliases
    500	Internal Server Error -- We had a problem with our server. Try again later
    503	Service Unavailable -- We're temporarily offline for maintenance. Please try again later*/

    private var API_KEY: String? = null
    private val loggingHelper = LoggingHelper(context)
    val settingsManager = SettingsManager(true, context)

    init {
        // Obtain API key from the encrypted preferences
        API_KEY = settingsManager.getSettingsString(SettingsManager.PREFS.API_KEY)
        API_BASE_URL = settingsManager.getSettingsString(SettingsManager.PREFS.BASE_URL) ?: API_BASE_URL
    }


    private fun getHeaders(apiKey: String? = null): Array<Pair<String, Any>> {
        val apiKeyToSend = apiKey ?: API_KEY
        return arrayOf(
            "Authorization" to "Bearer $apiKeyToSend",
            "Content-Type" to "application/json",
            "X-Requested-With" to "XMLHttpRequest",
            "Accept" to "application/json"
        )
    }


    // Separate method, with a try/catch because you can't toast on a Non-UI thread. And the widgets might call methods and there *is* a chance
    // these calls return a 404
    private fun invalidApiKey() {
        try {
            Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            val ex = e.message
            println(ex)
            loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "invalidApiKey", null)
        }
    }


    suspend fun downloadBody(url: String, callback: (String?, String?) -> Unit) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get(url)
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                callback(data, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "downloadBody",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun verifyApiKey(baseUrl: String, apiKey: String, callback: (String?) -> Unit) {
        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        // Reset all values as API_BASE_URL is being set
        lazyMgr.reset() // prop1, prop2, and prop3 all will do new lazy values on next access

        // Set base URL
        API_BASE_URL = baseUrl
        val (_, response, result) = Fuel.get(API_URL_ACCOUNT_DETAILS)
            .appendHeader(
                *getHeaders(apiKey)
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            // Do not check for a 401 since the UI will take care of it
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "verifyApiKey",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    /**
     * GET VERSION
     */

    suspend fun getAnonAddyInstanceVersion(
        callback: (Version?, String?) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get(API_URL_APP_VERSION)
                .appendHeader(
                    *getHeaders()
                )
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, Version::class.java)
                callback(anonAddyData, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            // Not found, aka the AnonAddy version is <0.6.0 (this endpoint was introduced in 0.6.0)
            // Send an empty version as callback to let the checks run in SplashActivity
            404 -> {
                callback(Version(0, 0, 0, ""), null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getAnonAddyInstanceVersion",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    /**
     * GET USER RESOURCE
     */

    suspend fun getUserResource(
        callback: (UserResource?, String?) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get(API_URL_ACCOUNT_DETAILS)
                .appendHeader(
                    *getHeaders()
                )
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleUserResource::class.java)
                callback(anonAddyData.data, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getUserResource",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun getDomainOptions(
        callback: (DomainOptions?, String?) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }


        val (_, response, result) = Fuel.get(API_URL_DOMAIN_OPTIONS)
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, DomainOptions::class.java)
                callback(anonAddyData, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getDomainOptions",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    /**
     * ALIASES
     */


    suspend fun addAlias(
        callback: (Aliases?, String?) -> Unit,
        domain: String,
        description: String,
        format: String,
        local_part: String,
        recipients: ArrayList<String>?
    ) {
        val array = JSONArray()

        if (recipients != null) {
            for (recipient in recipients) {
                array.put(recipient)
            }
        }

        val json = JSONObject()
        json.put("domain", domain)
        json.put("description", description)
        json.put("format", format)
        json.put("local_part", local_part)
        json.put("recipient_ids", array)


        val (_, response, result) = Fuel.post(API_URL_ALIAS)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            201 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleAlias::class.java)
                callback(anonAddyData.data, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "addAlias",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun getAliases(
        callback: (AliasesArray?, String?) -> Unit,
        aliasSortFilter: AliasSortFilter,
        page: Int? = null,
        size: Int? = 20,
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }


        /*
        Parameters
        https://app.anonaddy.com/docs/#get-all-aliases
         */
        val parameters: ArrayList<Pair<String, String>> = arrayListOf()


        if (aliasSortFilter.onlyActiveAliases) {
            parameters.add("filter[active]=" to "true")
        } else if (aliasSortFilter.onlyInactiveAliases) {
            parameters.add("filter[active]=" to "false")
        }

        if (aliasSortFilter.includeDeleted) {
            parameters.add("filter[deleted]=" to "with")
        }

        if (size != null) {
            parameters.add("page[size]" to size.toString())
        }

        if (!aliasSortFilter.filter.isNullOrEmpty()) {
            parameters.add("filter[search]" to aliasSortFilter.filter.toString())
        }

        if (page != null) {
            parameters.add("page[number]" to page.toString())
        }

        if (!aliasSortFilter.sort.isNullOrEmpty()) {

            val sortFilter: String = if (aliasSortFilter.sortDesc) {
                "-${aliasSortFilter.sort.toString()}"
            } else {
                aliasSortFilter.sort.toString()
            }

            parameters.add("sort" to sortFilter)
        }

        val (_, response, result) = Fuel.get(API_URL_ALIAS, parameters)
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, AliasesArray::class.java)
                callback(anonAddyData, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)

                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getAliases",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun getSpecificAlias(
        callback: (Aliases?, String?) -> Unit,
        aliasId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get("${API_URL_ALIAS}/$aliasId")
                .appendHeader(
                    *getHeaders()
                )
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleAlias::class.java)
                callback(anonAddyData.data, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getSpecificAlias",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun updateDescriptionSpecificAlias(
        callback: (String?) -> Unit,
        aliasId: String,
        description: String
    ) {
        val json = JSONObject()
        json.put("description", description)


        val (_, response, result) =
            Fuel.patch("${API_URL_ALIAS}/$aliasId")
                .appendHeader(
                    *getHeaders()
                )
                .body(json.toString())
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "updateDescriptionSpecificAlias",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun updateRecipientsSpecificAlias(
        callback: (String?) -> Unit,
        aliasId: String,
        recipients: ArrayList<String>
    ) {
        val json = JSONObject()
        val array = JSONArray()

        for (recipient in recipients) {
            array.put(recipient)
        }

        json.put("alias_id", aliasId)
        json.put("recipient_ids", array)



        val (_, response, result) =
            Fuel.post(API_URL_ALIAS_RECIPIENTS)
                .appendHeader(
                    *getHeaders()
                )
                .body(json.toString())
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "updateRecipientsSpecificAlias",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun deactivateSpecificAlias(
        callback: (String?) -> Unit?,
        aliasId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_ACTIVE_ALIAS}/$aliasId")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deactivateSpecificAlias",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun activateSpecificAlias(
        callback: (Aliases?, String?) -> Unit,
        aliasId: String
    ) {
        val json = JSONObject()
        json.put("id", aliasId)

        val (_, response, result) = Fuel.post(API_URL_ACTIVE_ALIAS)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleAlias::class.java)
                callback(anonAddyData.data, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "activateSpecificAlias",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun deleteAlias(
        callback: (String?) -> Unit,
        aliasId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_ALIAS}/$aliasId")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deleteAlias",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun forgetAlias(
        callback: (String?) -> Unit,
        aliasId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_ALIAS}/$aliasId/forget")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "forgetAlias",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun restoreAlias(
        callback: (String?) -> Unit,
        aliasId: String
    ) {
        val (_, response, result) = Fuel.patch("${API_URL_ALIAS}/$aliasId/restore")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "restoreAlias",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    /**
     * RECIPIENTS
     */

    suspend fun addRecipient(
        callback: (Recipients?, String?) -> Unit,
        address: String
    ) {
        val json = JSONObject()
        json.put("email", address)

        val (_, response, result) = Fuel.post(API_URL_RECIPIENTS)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            201 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleRecipient::class.java)
                callback(anonAddyData.data, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "addRecipient",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun getRecipients(
        callback: (ArrayList<Recipients>?, String?) -> Unit,
        verifiedOnly: Boolean
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.get(API_URL_RECIPIENTS)
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, RecipientsArray::class.java)

                val recipientList = ArrayList<Recipients>()

                if (verifiedOnly) {
                    for (recipient in anonAddyData.data) {
                        if (recipient.email_verified_at != null) {
                            recipientList.add(recipient)
                        }
                    }
                } else {
                    recipientList.addAll(anonAddyData.data)
                }
                callback(recipientList, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getRecipients",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun deleteRecipient(
        callback: (String?) -> Unit,
        recipientId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_RECIPIENTS}/$recipientId")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deleteRecipient",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun disableEncryptionRecipient(
        callback: (String?) -> Unit?,
        recipientId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_ENCRYPTED_RECIPIENTS}/$recipientId")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "disableEncryptionRecipient",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun enableEncryptionRecipient(
        callback: (String?) -> Unit,
        recipientId: String
    ) {

        val json = JSONObject()
        json.put("id", recipientId)

        val (_, response, result) = Fuel.post(API_URL_ENCRYPTED_RECIPIENTS)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "enableEncryptionRecipient",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun removeEncryptionKeyRecipient(
        callback: (String?) -> Unit,
        recipientId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_RECIPIENT_KEYS}/$recipientId")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "removeEncryptionKeyRecipient",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun addEncryptionKeyRecipient(
        callback: (String?) -> Unit,
        recipientId: String,
        keyData: String
    ) {
        val json = JSONObject()
        json.put("key_data", keyData)


        val (_, response, result) = Fuel.patch("${API_URL_RECIPIENT_KEYS}/$recipientId")
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "addEncryptionKeyRecipient",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun getSpecificRecipient(
        callback: (Recipients?, String?) -> Unit,
        recipientId: String
    ) {
        val (_, response, result) =
            Fuel.get("${API_URL_RECIPIENTS}/$recipientId")
                .appendHeader(
                    *getHeaders()
                )
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleRecipient::class.java)
                callback(anonAddyData.data, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getSpecificRecipient",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun resendVerificationEmail(
        callback: (String?) -> Unit,
        recipientId: String
    ) {
        val json = JSONObject()
        json.put("recipient_id", recipientId)

        val (_, response, result) = Fuel.post(API_URL_RECIPIENT_RESEND)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "resendVerificationEmail",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    /**
     * DOMAINS
     */

    suspend fun getAllDomains(
        callback: (ArrayList<Domains>?, String?) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.get(API_URL_DOMAINS)
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, DomainsArray::class.java)
                val domainList = ArrayList<Domains>()
                domainList.addAll(anonAddyData.data)
                callback(domainList, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getDomains",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun deleteDomain(
        callback: (String?) -> Unit,
        id: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_DOMAINS}/$id")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deleteDomain",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun addDomain(
        callback: (Domains?, String?, String?) -> Unit,
        domain: String
    ) {
        val json = JSONObject()
        json.put("domain", domain)

        val (_, response, result) = Fuel.post(API_URL_DOMAINS)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            201 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleDomain::class.java)
                callback(anonAddyData.data, "201", null)
            }
            // 404 means that the setup is not completed
            404 -> {
                callback(null, "404", String(response.body().toByteArray()))
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "addDomain",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null, null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun getSpecificDomain(
        callback: (Domains?, String?) -> Unit,
        id: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get("${API_URL_DOMAINS}/$id")
                .appendHeader(
                    *getHeaders()
                )
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleDomain::class.java)
                callback(anonAddyData.data, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun updateDefaultRecipientForSpecificDomain(
        callback: (String?) -> Unit,
        domainId: String,
        recipientId: String
    ) {
        val json = JSONObject()
        json.put("default_recipient", recipientId)


        val (_, response, result) =
            Fuel.patch("${API_URL_DOMAINS}/$domainId/default-recipient")
                .appendHeader(
                    *getHeaders()
                )
                .body(json.toString())
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int, ex.toString(), "updateDefaultRecipientForSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        response.data
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun deactivateSpecificDomain(
        callback: (String?) -> Unit?,
        domainId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_ACTIVE_DOMAINS}/$domainId")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deactivateSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun activateSpecificDomain(
        callback: (String?) -> Unit,
        domainId: String
    ) {
        val json = JSONObject()
        json.put("id", domainId)

        val (_, response, result) = Fuel.post(API_URL_ACTIVE_DOMAINS)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "activateSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun disableCatchAllSpecificDomain(
        callback: (String?) -> Unit?,
        domainId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_CATCH_ALL_DOMAINS}/$domainId")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "disableCatchAllSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun enableCatchAllSpecificDomain(
        callback: (String?) -> Unit,
        domainId: String
    ) {
        val json = JSONObject()
        json.put("id", domainId)

        val (_, response, result) = Fuel.post(API_URL_CATCH_ALL_DOMAINS)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "enableCatchAllSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun updateDescriptionSpecificDomain(
        callback: (String?) -> Unit,
        domainId: String,
        description: String
    ) {
        val json = JSONObject()
        json.put("description", description)


        val (_, response, result) =
            Fuel.patch("${API_URL_DOMAINS}/$domainId")
                .appendHeader(
                    *getHeaders()
                )
                .body(json.toString())
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int, ex.toString(), "updateDescriptionSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        response.data
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    /**
     * USERNAMES
     */

    suspend fun getAllUsernames(
        callback: (ArrayList<Usernames>?, String?) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.get(API_URL_USERNAMES)
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, UsernamesArray::class.java)

                val usernamesList = ArrayList<Usernames>()
                usernamesList.addAll(anonAddyData.data)
                callback(usernamesList, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getUsernames",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun deleteUsername(
        callback: (String?) -> Unit,
        id: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_USERNAMES}/$id")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deleteUsername",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun addUsername(
        callback: (Usernames?, String?) -> Unit,
        username: String
    ) {
        val json = JSONObject()
        json.put("username", username)

        val (_, response, result) = Fuel.post(API_URL_USERNAMES)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            201 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleUsername::class.java)
                callback(anonAddyData.data, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "addUsername",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun getSpecificUsername(
        callback: (Usernames?, String?) -> Unit,
        id: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get("${API_URL_USERNAMES}/$id")
                .appendHeader(
                    *getHeaders()
                )
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleUsername::class.java)
                callback(anonAddyData.data, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun updateDefaultRecipientForSpecificUsername(
        callback: (String?) -> Unit,
        userNameId: String,
        recipientId: String
    ) {
        val json = JSONObject()
        json.put("default_recipient", recipientId)


        val (_, response, result) =
            Fuel.patch("${API_URL_USERNAMES}/$userNameId/default-recipient")
                .appendHeader(
                    *getHeaders()
                )
                .body(json.toString())
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int, ex.toString(), "updateDefaultRecipientForSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        response.data
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun deactivateSpecificUsername(
        callback: (String?) -> Unit?,
        usernameId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_ACTIVE_USERNAMES}/$usernameId")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deactivateSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun activateSpecificUsername(
        callback: (String?) -> Unit,
        usernameId: String
    ) {
        val json = JSONObject()
        json.put("id", usernameId)

        val (_, response, result) = Fuel.post(API_URL_ACTIVE_USERNAMES)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "activateSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun updateDescriptionSpecificUsername(
        callback: (String?) -> Unit,
        usernameId: String,
        description: String
    ) {
        val json = JSONObject()
        json.put("description", description)


        val (_, response, result) =
            Fuel.patch("${API_URL_USERNAMES}/$usernameId")
                .appendHeader(
                    *getHeaders()
                )
                .body(json.toString())
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int, ex.toString(), "updateDescriptionSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        response.data
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    /**
     * RULES
     */


    /**
     * DOMAINS
     */

    suspend fun getAllRules(
        callback: (ArrayList<Rules>?, String?) -> Unit,
        show404Toast: Boolean = false
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.get(API_URL_RULES)
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, RulesArray::class.java)

                val domainList = ArrayList<Rules>()
                domainList.addAll(anonAddyData.data)
                callback(domainList, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            // Not found, aka the AnonAddy version is <0.6.0 (this endpoint was introduced in 0.6.0)
            // OR
            // Not found, aka the rules API (which is in beta as of 0.6.0) is not enabled. (Not part of the user's subscription)
            // =
            // Show a toast letting the user know this feature is only available if the rules API is enabled
            404 -> {
                if (show404Toast) {
                    Toast.makeText(context, context.resources.getString(R.string.rules_unavailable_404), Toast.LENGTH_LONG).show()
                }
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getAllRules",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun getSpecificRule(
        callback: (Rules?, String?) -> Unit,
        id: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get("${API_URL_RULES}/$id")
                .appendHeader(
                    *getHeaders()
                )
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleRule::class.java)
                callback(anonAddyData.data, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getSpecificRule",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun deleteRule(
        callback: (String?) -> Unit,
        id: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_RULES}/$id")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deleteRule",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun createRule(
        callback: (String?) -> Unit,
        rule: Rules
    ) {
        val ruleJson = Gson().toJson(rule)
        val (_, response, result) = Fuel.post(API_URL_RULES)
            .appendHeader(
                *getHeaders()
            )
            .body(ruleJson)
            .awaitStringResponseResult()

        when (response.statusCode) {
            201 -> {
                callback("201")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "createRule",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun reorderRules(
        callback: (String?) -> Unit,
        rulesArray: ArrayList<Rules>
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val array = JSONArray()
        // Sum up the ids
        for (rule in rulesArray) {
            array.put(rule.id)
        }
        val obj = JSONObject()
        obj.put("ids", array)
        val ruleJson = obj.toString()

        val (_, response, result) = Fuel.post(API_URL_REORDER_RULES)
            .appendHeader(
                *getHeaders()
            )
            .body(ruleJson)
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "reorderRules",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun updateRule(
        callback: (String?) -> Unit,
        ruleId: String,
        rule: Rules
    ) {
        val ruleJson = Gson().toJson(rule)
        val (_, response, result) = Fuel.patch("${API_URL_RULES}/$ruleId")
            .appendHeader(
                *getHeaders()
            )
            .body(ruleJson)
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "updateRule",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun deactivateSpecificRule(
        callback: (String?) -> Unit?,
        ruleId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_ACTIVE_RULES}/$ruleId")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deactivateSpecificRule",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    suspend fun activateSpecificRule(
        callback: (String?) -> Unit,
        ruleId: String
    ) {
        val json = JSONObject()
        json.put("id", ruleId)

        val (_, response, result) = Fuel.post(API_URL_ACTIVE_RULES)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                callback("200")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "activateSpecificRule",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    /**
     * ANONADDY SETTINGS
     */

    /*
    Anonaddy settings cannot be changed by API
     */


    /**
     * WIDGET AND WEAROS
     */
    // The widgets require the following data:
    // Widget 1: Aliases
    // Widget 2: Stats + Aliases

    // WearOS uses this data to show in the AliasActivity

    suspend fun cacheMostPopularAliasesDataForWidget(
        callback: (Boolean) -> Unit,
        amountOfAliasesToCache: Int? = 15
    ) {
        getAliases(
            { list, _ ->
                if (list == null) {
                    // Result is null, callback false to let the BackgroundWorker know the task failed.
                    callback(false)
                    return@getAliases
                } else {
                    // Turn the list into a json object
                    val data = Gson().toJson(list.data)

                    // Store a copy of the just received data locally
                    settingsManager.putSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_MOST_ACTIVE_ALIASES_DATA, data)

                    // Stored data, let the BackgroundWorker know the task succeeded
                    callback(true)
                }
            },
            aliasSortFilter = AliasSortFilter(
                onlyActiveAliases = true,
                onlyInactiveAliases = false,
                includeDeleted = false,
                onlyWatchedAliases = false,
                sort = "emails_forwarded",
                sortDesc = true,
                filter = null
            ),
            size = amountOfAliasesToCache,
        )
    }

    suspend fun cacheLastUpdatedAliasesDataForWidget(
        callback: (Boolean) -> Unit,
        amountOfAliasesToCache: Int? = 15
    ) {
        getAliases(
            { list, _ ->
                if (list == null) {
                    // Result is null, callback false to let the BackgroundWorker know the task failed.
                    callback(false)
                    return@getAliases
                } else {
                    // Turn the list into a json object
                    val data = Gson().toJson(list.data)

                    // Store a copy of the just received data locally
                    settingsManager.putSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_LAST_UPDATED_ALIASES_DATA, data)

                    // Stored data, let the BackgroundWorker know the task succeeded
                    callback(true)
                }
            },
            aliasSortFilter = AliasSortFilter(
                onlyActiveAliases = true,
                onlyInactiveAliases = false,
                includeDeleted = false,
                onlyWatchedAliases = false,
                sort = "updated_at",
                sortDesc = true,
                filter = null
            ),
            size = amountOfAliasesToCache,
        )
    }


    suspend fun cacheUserResourceForWidget(
        callback: (Boolean) -> Unit
    ) {
        getUserResource { userResource: UserResource?, _: String? ->
            if (userResource == null) {
                // Result is null, callback false to let the BackgroundWorker know the task failed.
                callback(false)
                return@getUserResource
            } else {
                // Turn the list into a json object
                val data = Gson().toJson(userResource)

                // Store a copy of the just received data locally
                settingsManager.putSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_USER_RESOURCE, data)

                // Stored data, let the BackgroundWorker know the task succeeded
                callback(true)
            }
        }
    }

    suspend fun cacheFailedDeliveryCountForWidgetAndBackgroundService(
        callback: (Boolean) -> Unit
    ) {
        getAllFailedDeliveries({ result, _ ->
            if (result == null) {
                // Result is null, callback false to let the BackgroundWorker know the task failed.
                callback(false)
                return@getAllFailedDeliveries
            } else {
                // First move the current count to the previous count (for comparison)
                settingsManager.putSettingsInt(
                    SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT_PREVIOUS,
                    settingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT)
                )
                // Now store the current count
                settingsManager.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT, result.size)

                // Stored data, let the BackgroundWorker know the task succeeded
                callback(true)
            }
            // Also take the not-verified recipients in account. As this value is being used to set the shimmerview
        })
    }


    /**
     * FAILED DELIVERIES
     */

    suspend fun getAllFailedDeliveries(
        callback: (ArrayList<FailedDeliveries>?, String?) -> Unit,
        show404Toast: Boolean = false
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.get(API_URL_FAILED_DELIVERIES)
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, FailedDeliveriesArray::class.java)

                val failedDeliveriesList = ArrayList<FailedDeliveries>()
                failedDeliveriesList.addAll(anonAddyData.data)
                callback(failedDeliveriesList, null)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            // Not found, aka the AnonAddy version is <0.8.1 (this endpoint was introduced in 0.8.1)
            // OR
            // Not found, aka the failed deliveries API is not enabled. (Not part of the user's subscription)
            // =
            // Show a toast (if enabled) letting the user know this feature is only available if the failed deliveries API is enabled
            404 -> {
                if (show404Toast) {
                    Toast.makeText(context, context.resources.getString(R.string.failed_deliveries_unavailable_404), Toast.LENGTH_LONG).show()
                }
                callback(null, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getAllFailedDeliveries",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }

    suspend fun deleteFailedDelivery(
        callback: (String?) -> Unit,
        id: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_FAILED_DELIVERIES}/$id")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deleteFailedDelivery",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }
    }


    /**
     * UPDATE
     */
    suspend fun getGitlabTags(
        callback: (Feed?, String?) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get(GITLAB_TAGS_RSS_FEED)
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val inputStream: InputStream = result.get().byteInputStream()
                val feed = EarlParser.parse(inputStream, 0)
                callback(feed, null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getGitlabTags",
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        if (response.data.isNotEmpty()) response.data else ex.toString().toByteArray()
                    )
                )
            }
        }


    }
}