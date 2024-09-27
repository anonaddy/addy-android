@file:Suppress("unused")

package host.stjin.anonaddy_shared

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.einmalfel.earl.EarlParser
import com.einmalfel.earl.Feed
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.google.gson.Gson
import host.stjin.anonaddy_shared.AddyIo.API_BASE_URL
import host.stjin.anonaddy_shared.AddyIo.API_URL_ACCOUNT_DETAILS
import host.stjin.anonaddy_shared.AddyIo.API_URL_ACCOUNT_NOTIFICATIONS
import host.stjin.anonaddy_shared.AddyIo.API_URL_ACTIVE_ALIAS
import host.stjin.anonaddy_shared.AddyIo.API_URL_ACTIVE_DOMAINS
import host.stjin.anonaddy_shared.AddyIo.API_URL_ACTIVE_RULES
import host.stjin.anonaddy_shared.AddyIo.API_URL_ACTIVE_USERNAMES
import host.stjin.anonaddy_shared.AddyIo.API_URL_ALIAS
import host.stjin.anonaddy_shared.AddyIo.API_URL_ALIAS_RECIPIENTS
import host.stjin.anonaddy_shared.AddyIo.API_URL_ALLOWED_RECIPIENTS
import host.stjin.anonaddy_shared.AddyIo.API_URL_API_TOKEN_DETAILS
import host.stjin.anonaddy_shared.AddyIo.API_URL_APP_VERSION
import host.stjin.anonaddy_shared.AddyIo.API_URL_CAN_LOGIN_USERNAMES
import host.stjin.anonaddy_shared.AddyIo.API_URL_CATCH_ALL_DOMAINS
import host.stjin.anonaddy_shared.AddyIo.API_URL_CATCH_ALL_USERNAMES
import host.stjin.anonaddy_shared.AddyIo.API_URL_CHART_DATA
import host.stjin.anonaddy_shared.AddyIo.API_URL_DELETE_ACCOUNT
import host.stjin.anonaddy_shared.AddyIo.API_URL_DOMAINS
import host.stjin.anonaddy_shared.AddyIo.API_URL_DOMAIN_OPTIONS
import host.stjin.anonaddy_shared.AddyIo.API_URL_ENCRYPTED_RECIPIENTS
import host.stjin.anonaddy_shared.AddyIo.API_URL_FAILED_DELIVERIES
import host.stjin.anonaddy_shared.AddyIo.API_URL_INLINE_ENCRYPTED_RECIPIENTS
import host.stjin.anonaddy_shared.AddyIo.API_URL_LOGIN_VERIFY
import host.stjin.anonaddy_shared.AddyIo.API_URL_LOGOUT
import host.stjin.anonaddy_shared.AddyIo.API_URL_NOTIFY_SUBSCRIPTION
import host.stjin.anonaddy_shared.AddyIo.API_URL_PROTECTED_HEADERS_RECIPIENTS
import host.stjin.anonaddy_shared.AddyIo.API_URL_RECIPIENTS
import host.stjin.anonaddy_shared.AddyIo.API_URL_RECIPIENT_KEYS
import host.stjin.anonaddy_shared.AddyIo.API_URL_RECIPIENT_RESEND
import host.stjin.anonaddy_shared.AddyIo.API_URL_REGISTER
import host.stjin.anonaddy_shared.AddyIo.API_URL_REORDER_RULES
import host.stjin.anonaddy_shared.AddyIo.API_URL_RULES
import host.stjin.anonaddy_shared.AddyIo.API_URL_USERNAMES
import host.stjin.anonaddy_shared.AddyIo.GITHUB_TAGS_RSS_FEED
import host.stjin.anonaddy_shared.AddyIo.lazyMgr
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.AccountNotifications
import host.stjin.anonaddy_shared.models.AccountNotificationsArray
import host.stjin.anonaddy_shared.models.AddyChartData
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.AliasesArray
import host.stjin.anonaddy_shared.models.ApiTokenDetails
import host.stjin.anonaddy_shared.models.BulkActionResponse
import host.stjin.anonaddy_shared.models.BulkAliasesArray
import host.stjin.anonaddy_shared.models.DomainOptions
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.models.DomainsArray
import host.stjin.anonaddy_shared.models.Error
import host.stjin.anonaddy_shared.models.ErrorHelper
import host.stjin.anonaddy_shared.models.FailedDeliveries
import host.stjin.anonaddy_shared.models.FailedDeliveriesArray
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.Login
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.models.RecipientsArray
import host.stjin.anonaddy_shared.models.Rules
import host.stjin.anonaddy_shared.models.RulesArray
import host.stjin.anonaddy_shared.models.SingleAlias
import host.stjin.anonaddy_shared.models.SingleDomain
import host.stjin.anonaddy_shared.models.SingleFailedDelivery
import host.stjin.anonaddy_shared.models.SingleRecipient
import host.stjin.anonaddy_shared.models.SingleRule
import host.stjin.anonaddy_shared.models.SingleUserResource
import host.stjin.anonaddy_shared.models.SingleUsername
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.models.Usernames
import host.stjin.anonaddy_shared.models.UsernamesArray
import host.stjin.anonaddy_shared.models.Version
import host.stjin.anonaddy_shared.utils.LoggingHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream


class NetworkHelper(private val context: Context) {

    /*https://app.addy.io/docs/#errors
    400	Bad Request -- Your request sucks
    401	Unauthenticated -- Your API key is wrong
    403	Forbidden -- You do not have permission to access the requested resource
    404	Not Found -- The specified resource could not be found
    405	Method Not Allowed -- You tried to access an endpoint with an invalid method
    422	Validation Error -- The given data was invalid
    429	Too Many Requests -- You're sending too many requests or have reached your limit for new aliases
    500	Internal Server Error -- We had a problem with our server. Try again later
    503	Service Unavailable -- We're temporarily offline for maintenance. Please try again later*/

    private val loggingHelper = LoggingHelper(context)
    val encryptedSettingsManager = SettingsManager(true, context)

    init {
        // Obtain API key from the encrypted preferences
        API_BASE_URL = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.BASE_URL) ?: API_BASE_URL
    }

    private fun getHeaders(apiKey: String? = null): Array<Pair<String, Any>> {
        val apiKeyToSend = apiKey ?: encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.API_KEY)
        return arrayOf(
            "Authorization" to "Bearer $apiKeyToSend",
            "Content-Type" to "application/json",
            "X-Requested-With" to "XMLHttpRequest",
            "Accept" to "application/json",
            "User-Agent" to getUserAgent()
        )
    }


    private fun getUserAgent(): String {
        // User-Agent: <product> / <product-version> <comment>
        // <product> / <product-version> <comment>

        val userAgent =
            "${(context.applicationContext as AddyIoApp).userAgent.userAgentApplicationID} (${(context.applicationContext as AddyIoApp).userAgent.userAgentApplicationBuildType}) / ${(context.applicationContext as AddyIoApp).userAgent.userAgentVersion} (${(context.applicationContext as AddyIoApp).userAgent.userAgentVersionCode})"

        if (BuildConfig.DEBUG) {
            println("User-Agent: $userAgent")
        }

        return userAgent
    }


    private fun getFuelResponse(response: Response): ByteArray? {
        return try {
            response.data
        } catch (e: Exception) {
            null
        }
    }


    // Separate method, with a try/catch because you can't toast on a Non-UI thread. And the widgets might call methods and there *is* a chance
    // these calls return a 404
    private fun invalidApiKey() {
        try {
            Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            val ex = e.message
            Log.e("AFA", ex.toString())
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "downloadBody",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun registration(
        callback: (String?) -> Unit,
        username: String,
        email: String,
        password: String,
        apiExpiration: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        json.put("username", username)
        json.put("email", email)
        json.put("password", password)
        json.put("device_name", "addy.io for Android")
        json.put("expiration", if (apiExpiration == "never") null else apiExpiration)


        val (_, response, result) = Fuel.post(API_URL_REGISTER)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback("204")
            }

            422 -> {
                val data = response.data.toString(Charsets.UTF_8)
                val gson = Gson()
                val addyIoData = gson.fromJson(data, Error::class.java)
                callback(addyIoData.message)
            }

            else -> {
                val ex = result.component2()?.message
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "registration",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun verifyRegistration(
        callback: (String?, String?) -> Unit,
        query: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.post("${API_URL_LOGIN_VERIFY}?${query}")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, Login::class.java)
                callback(addyIoData.api_key, null)
            }

            422, 404, 403 -> {
                val data = response.data.toString(Charsets.UTF_8)
                val gson = Gson()
                val addyIoData = gson.fromJson(data, Error::class.java)
                callback(null, addyIoData.message)
            }

            else -> {
                val ex = result.component2()?.message
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "verifyRegistration",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun deleteAccount(
        callback: (String?) -> Unit,
        password: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        json.put("password", password)


        val (_, response, result) = Fuel.post(API_URL_DELETE_ACCOUNT)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            204 -> {
                callback(response.statusCode.toString())
            }

            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }

            422 -> {
                callback(response.statusCode.toString())
            }

            else -> {
                val ex = result.component2()?.message
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deleteAccount",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun logout(callback: (String?) -> Unit) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }


        val (_, response, result) = Fuel.post(API_URL_LOGOUT)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "logout",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "verifyApiKey",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    /**
     * GET VERSION
     */

    suspend fun getAddyIoInstanceVersion(
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
                val addyIoData = gson.fromJson(data, Version::class.java)
                callback(addyIoData, null)
            }

            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null, null)
            }
            // Not found, aka the addy.io version is <0.6.0 (this endpoint was introduced in 0.6.0)
            // Send an empty version as callback to let the checks run in SplashActivity
            404 -> {
                callback(Version(0, 0, 0, ""), null)
            }

            else -> {
                val ex = result.component2()?.message
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getAddyIoInstanceVersion",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
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
                val addyIoData = gson.fromJson(data, SingleUserResource::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getUserResource",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
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
                val addyIoData = gson.fromJson(data, DomainOptions::class.java)
                callback(addyIoData, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getDomainOptions",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
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

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "addAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
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
        recipient: String? = null,
        domain: String? = null,
        username: String? = null,
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }


        /*
        Parameters
        https://app.addy.io/docs/#get-all-aliases
         */
        val parameters: ArrayList<Pair<String, String>> = arrayListOf()


        if (aliasSortFilter.onlyActiveAliases) {
            parameters.add("filter[active]=" to "true")
        } else if (aliasSortFilter.onlyInactiveAliases) {
            parameters.add("filter[active]=" to "false")
            parameters.add("filter[deleted]=" to "with")
        } else if (aliasSortFilter.onlyDeletedAliases) {
            parameters.add("filter[deleted]=" to "only")
        } else {
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

        if (!recipient.isNullOrEmpty()) {
            parameters.add("recipient" to recipient)
        }
        if (!domain.isNullOrEmpty()) {
            parameters.add("domain" to domain)
        }
        if (!username.isNullOrEmpty()) {
            parameters.add("username" to username)
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
                val addyIoData = gson.fromJson(data, AliasesArray::class.java)
                callback(addyIoData, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")

                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getAliases",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun getChartData(
        callback: (AddyChartData?, String?) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get(API_URL_CHART_DATA)
                .appendHeader(
                    *getHeaders()
                )
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, AddyChartData::class.java)
                callback(addyIoData, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getChartData",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
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
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getSpecificAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun updateDescriptionSpecificAlias(
        callback: (Aliases?, String?) -> Unit,
        aliasId: String,
        description: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "updateDescriptionSpecificAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun updateFromNameSpecificAlias(
        callback: (Aliases?, String?) -> Unit,
        aliasId: String,
        fromName: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        json.put("from_name", fromName)


        val (_, response, result) =
            Fuel.patch("${API_URL_ALIAS}/$aliasId")
                .appendHeader(
                    *getHeaders()
                )
                .body(json.toString())
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "updateFromNameSpecificAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun updateRecipientsSpecificAlias(
        callback: (Aliases?, String?) -> Unit,
        aliasId: String,
        recipients: ArrayList<String>
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "updateRecipientsSpecificAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun bulkGetAlias(
        callback: (BulkAliasesArray?, String?) -> Unit,
        aliases: List<String>
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        val array = JSONArray()

        for (id in aliases) {
            array.put(id)
        }

        json.put("ids", array)

        val (_, response, result) = Fuel.post("${API_URL_ALIAS}/get/bulk")
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, BulkAliasesArray::class.java)
                callback(addyIoData, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "bulkGetAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun deactivateSpecificAlias(
        callback: (String?) -> Unit?,
        aliasId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deactivateSpecificAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun bulkDeactivateAlias(
        callback: (BulkActionResponse?, String?) -> Unit,
        aliases: List<Aliases>
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        val array = JSONArray()

        for (alias in aliases) {
            array.put(alias.id)
        }

        json.put("ids", array)

        val (_, response, result) = Fuel.post("${API_URL_ALIAS}/deactivate/bulk")
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, BulkActionResponse::class.java)
                callback(addyIoData, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "bulkDeactivateAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun activateSpecificAlias(
        callback: (Aliases?, String?) -> Unit,
        aliasId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "activateSpecificAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun bulkActivateAlias(
        callback: (BulkActionResponse?, String?) -> Unit,
        aliases: List<Aliases>
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        val array = JSONArray()

        for (alias in aliases) {
            array.put(alias.id)
        }

        json.put("ids", array)

        val (_, response, result) = Fuel.post("${API_URL_ALIAS}/activate/bulk")
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, BulkActionResponse::class.java)
                callback(addyIoData, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "bulkActivateAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun deleteAlias(
        callback: (String?) -> Unit,
        aliasId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deleteAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun bulkDeleteAlias(
        callback: (BulkActionResponse?, String?) -> Unit,
        aliases: List<Aliases>
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        val array = JSONArray()

        for (alias in aliases) {
            array.put(alias.id)
        }

        json.put("ids", array)

        val (_, response, result) = Fuel.post("${API_URL_ALIAS}/delete/bulk")
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, BulkActionResponse::class.java)
                callback(addyIoData, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "bulkDeleteAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun forgetAlias(
        callback: (String?) -> Unit,
        aliasId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "forgetAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun bulkForgetAlias(
        callback: (BulkActionResponse?, String?) -> Unit,
        aliases: List<Aliases>
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        val array = JSONArray()

        for (alias in aliases) {
            array.put(alias.id)
        }

        json.put("ids", array)

        val (_, response, result) = Fuel.post("${API_URL_ALIAS}/forget/bulk")
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, BulkActionResponse::class.java)
                callback(addyIoData, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "bulkForgetAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun restoreAlias(
        callback: (Aliases?, String?) -> Unit,
        aliasId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.patch("${API_URL_ALIAS}/$aliasId/restore")
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleAlias::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "restoreAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun bulkRestoreAlias(
        callback: (BulkActionResponse?, String?) -> Unit,
        aliases: List<Aliases>
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        val array = JSONArray()

        for (alias in aliases) {
            array.put(alias.id)
        }

        json.put("ids", array)

        val (_, response, result) = Fuel.post("${API_URL_ALIAS}/restore/bulk")
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, BulkActionResponse::class.java)
                callback(addyIoData, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "bulkRestoreAlias",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
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

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val addyIoData = gson.fromJson(data, SingleRecipient::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "addRecipient",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
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
                val addyIoData = gson.fromJson(data, RecipientsArray::class.java)

                val recipientList = ArrayList<Recipients>()

                if (verifiedOnly) {
                    for (recipient in addyIoData.data) {
                        if (recipient.email_verified_at != null) {
                            recipientList.add(recipient)
                        }
                    }
                } else {
                    recipientList.addAll(addyIoData.data)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getRecipients",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun deleteRecipient(
        callback: (String?) -> Unit,
        recipientId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deleteRecipient",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun allowRecipientToReplySend(
        callback: (Recipients?, String?) -> Unit,
        recipientId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        json.put("id", recipientId)

        val (_, response, result) = Fuel.post(API_URL_ALLOWED_RECIPIENTS)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleRecipient::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "enableEncryptionRecipient",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun disallowRecipientToReplySend(
        callback: (String?) -> Unit,
        recipientId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.delete("${API_URL_ALLOWED_RECIPIENTS}/$recipientId")
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "disallowRecipientToReplySend",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun disableEncryptionRecipient(
        callback: (String?) -> Unit?,
        recipientId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "disableEncryptionRecipient",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun enableEncryptionRecipient(
        callback: (Recipients?, String?) -> Unit,
        recipientId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleRecipient::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "enableEncryptionRecipient",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun disablePgpInlineRecipient(
        callback: (String?) -> Unit?,
        recipientId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.delete("${API_URL_INLINE_ENCRYPTED_RECIPIENTS}/$recipientId")
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "disablePgpInlineRecipient",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun enablePgpInlineRecipient(
        callback: (Recipients?, String?) -> Unit,
        recipientId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        json.put("id", recipientId)

        val (_, response, result) = Fuel.post(API_URL_INLINE_ENCRYPTED_RECIPIENTS)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleRecipient::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "enablePgpInlineRecipient",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun disableProtectedHeadersRecipient(
        callback: (String?) -> Unit?,
        recipientId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.delete("${API_URL_PROTECTED_HEADERS_RECIPIENTS}/$recipientId")
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "disableProtectedHeadersRecipient",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun enableProtectedHeadersRecipient(
        callback: (Recipients?, String?) -> Unit,
        recipientId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        json.put("id", recipientId)

        val (_, response, result) = Fuel.post(API_URL_PROTECTED_HEADERS_RECIPIENTS)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleRecipient::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "enableProtectedHeadersRecipient",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun removeEncryptionKeyRecipient(
        callback: (String?) -> Unit,
        recipientId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "removeEncryptionKeyRecipient",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun addEncryptionKeyRecipient(
        callback: (Recipients?, String?) -> Unit,
        recipientId: String,
        keyData: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleRecipient::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "addEncryptionKeyRecipient",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun getSpecificRecipient(
        callback: (Recipients?, String?) -> Unit,
        recipientId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val addyIoData = gson.fromJson(data, SingleRecipient::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getSpecificRecipient",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun resendVerificationEmail(
        callback: (String?) -> Unit,
        recipientId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "resendVerificationEmail",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
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
                val addyIoData = gson.fromJson(data, DomainsArray::class.java)
                val domainList = ArrayList<Domains>()
                domainList.addAll(addyIoData.data)

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getDomains",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun deleteDomain(
        callback: (String?) -> Unit,
        id: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deleteDomain",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun addDomain(
        callback: (Domains?, String?, String?) -> Unit,
        domain: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val addyIoData = gson.fromJson(data, SingleDomain::class.java)
                callback(addyIoData.data, "201", null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "addDomain",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null, null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
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
                val addyIoData = gson.fromJson(data, SingleDomain::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun updateDefaultRecipientForSpecificDomain(
        callback: (Domains?, String?) -> Unit,
        domainId: String,
        recipientId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleDomain::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int, ex.toString(), "updateDefaultRecipientForSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        response.data
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun deactivateSpecificDomain(
        callback: (String?) -> Unit?,
        domainId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deactivateSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun activateSpecificDomain(
        callback: (Domains?, String?) -> Unit,
        domainId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleDomain::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "activateSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun disableCatchAllSpecificDomain(
        callback: (String?) -> Unit?,
        domainId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "disableCatchAllSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun enableCatchAllSpecificDomain(
        callback: (Domains?, String?) -> Unit,
        domainId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleDomain::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "enableCatchAllSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun updateDescriptionSpecificDomain(
        callback: (Domains?, String?) -> Unit,
        domainId: String,
        description: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleDomain::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int, ex.toString(), "updateDescriptionSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        response.data
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun updateAutoCreateRegexSpecificDomain(
        callback: (Domains?, String?) -> Unit,
        domainId: String,
        autoCreateRegex: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        json.put("auto_create_regex", autoCreateRegex)


        val (_, response, result) =
            Fuel.patch("${API_URL_DOMAINS}/$domainId")
                .appendHeader(
                    *getHeaders()
                )
                .body(json.toString())
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleDomain::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int, ex.toString(), "updateAutoCreateRegexSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        response.data
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun updateFromNameSpecificDomain(
        callback: (Domains?, String?) -> Unit,
        domainId: String,
        fromName: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        json.put("from_name", fromName)


        val (_, response, result) =
            Fuel.patch("${API_URL_DOMAINS}/$domainId")
                .appendHeader(
                    *getHeaders()
                )
                .body(json.toString())
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleDomain::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int, ex.toString(), "updateFromNameSpecificDomain",
                    ErrorHelper.getErrorMessage(
                        response.data
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
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
                val addyIoData = gson.fromJson(data, UsernamesArray::class.java)

                val usernamesList = ArrayList<Usernames>()
                usernamesList.addAll(addyIoData.data)

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getUsernames",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun deleteUsername(
        callback: (String?) -> Unit,
        id: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deleteUsername",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun addUsername(
        callback: (Usernames?, String?) -> Unit,
        username: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val addyIoData = gson.fromJson(data, SingleUsername::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "addUsername",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
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
                val addyIoData = gson.fromJson(data, SingleUsername::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun updateAutoCreateRegexSpecificUsername(
        callback: (Usernames?, String?) -> Unit,
        usernameId: String,
        autoCreateRegex: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        json.put("auto_create_regex", autoCreateRegex)


        val (_, response, result) =
            Fuel.patch("${API_URL_USERNAMES}/$usernameId")
                .appendHeader(
                    *getHeaders()
                )
                .body(json.toString())
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleUsername::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int, ex.toString(), "updateAutoCreateRegexSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        response.data
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun updateDefaultRecipientForSpecificUsername(
        callback: (Usernames?, String?) -> Unit,
        userNameId: String,
        recipientId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleUsername::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int, ex.toString(), "updateDefaultRecipientForSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        response.data
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun deactivateSpecificUsername(
        callback: (String?) -> Unit?,
        usernameId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deactivateSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun activateSpecificUsername(
        callback: (Usernames?, String?) -> Unit,
        usernameId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleUsername::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "activateSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun updateDescriptionSpecificUsername(
        callback: (Usernames?, String?) -> Unit,
        usernameId: String,
        description: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleUsername::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int, ex.toString(), "updateDescriptionSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        response.data
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun updateFromNameSpecificUsername(
        callback: (Usernames?, String?) -> Unit,
        usernameId: String,
        fromName: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        json.put("from_name", fromName)


        val (_, response, result) =
            Fuel.patch("${API_URL_USERNAMES}/$usernameId")
                .appendHeader(
                    *getHeaders()
                )
                .body(json.toString())
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleUsername::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int, ex.toString(), "updateFromNameSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        response.data
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun disableCatchAllSpecificUsername(
        callback: (String?) -> Unit?,
        usernameId: String
    ) {
        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }


        val (_, response, result) = Fuel.delete("${API_URL_CATCH_ALL_USERNAMES}/$usernameId")
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "disableCatchAllSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun enableCatchAllSpecificUsername(
        callback: (Usernames?, String?) -> Unit,
        usernameId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        json.put("id", usernameId)

        val (_, response, result) = Fuel.post(API_URL_CATCH_ALL_USERNAMES)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleUsername::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "enableCatchAllSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun disableCanLoginSpecificUsername(
        callback: (String?) -> Unit?,
        usernameId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.delete("${API_URL_CAN_LOGIN_USERNAMES}/$usernameId")
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "disableCanLoginSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun enableCanLoginSpecificUsername(
        callback: (Usernames?, String?) -> Unit,
        usernameId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        json.put("id", usernameId)

        val (_, response, result) = Fuel.post(API_URL_CAN_LOGIN_USERNAMES)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleUsername::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "enableCanLoginSpecificUsername",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    /**
     * RULES
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
                val addyIoData = gson.fromJson(data, RulesArray::class.java)

                val domainList = ArrayList<Rules>()
                domainList.addAll(addyIoData.data)
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
            // Not found, aka the addy.io version is <0.6.0 (this endpoint was introduced in 0.6.0)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getAllRules",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
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
                val addyIoData = gson.fromJson(data, SingleRule::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getSpecificRule",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun deleteRule(
        callback: (String?) -> Unit,
        id: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deleteRule",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun createRule(
        callback: (Rules?, String?) -> Unit,
        rule: Rules
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val ruleJson = Gson().toJson(rule)
        val (_, response, result) = Fuel.post(API_URL_RULES)
            .appendHeader(
                *getHeaders()
            )
            .body(ruleJson)
            .awaitStringResponseResult()

        when (response.statusCode) {
            201 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleRule::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "createRule",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "reorderRules",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
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

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "updateRule",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun deactivateSpecificRule(
        callback: (String?) -> Unit?,
        ruleId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deactivateSpecificRule",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun activateSpecificRule(
        callback: (Rules?, String?) -> Unit,
        ruleId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleRule::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "activateSpecificRule",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    /**
     * ADDY.IO SETTINGS
     */

    /*
    addy.io settings cannot be changed by API
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

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                    encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_MOST_ACTIVE_ALIASES_DATA, data)

                    // Stored data, let the BackgroundWorker know the task succeeded
                    callback(true)
                }
            },
            aliasSortFilter = AliasSortFilter(
                onlyActiveAliases = true,
                onlyDeletedAliases = false,
                onlyInactiveAliases = false,
                onlyWatchedAliases = false,
                sort = "emails_forwarded",
                sortDesc = true,
                filter = null
            ),
            size = amountOfAliasesToCache,
        )
    }

    suspend fun cacheLastUpdatedAliasesData(
        callback: (Boolean) -> Unit,
        amountOfAliasesToCache: Int? = 15
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                    encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_LAST_UPDATED_ALIASES_DATA, data)

                    // Stored data, let the BackgroundWorker know the task succeeded
                    callback(true)
                }
            },
            aliasSortFilter = AliasSortFilter(
                onlyActiveAliases = true,
                onlyDeletedAliases = false,
                onlyInactiveAliases = false,
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

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        getUserResource { userResource: UserResource?, _: String? ->
            if (userResource == null) {
                // Result is null, callback false to let the BackgroundWorker know the task failed.
                callback(false)
                return@getUserResource
            } else {
                // Turn the list into a json object
                val data = Gson().toJson(userResource)

                // Store a copy of the just received data locally
                encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_USER_RESOURCE, data)

                // Stored data, let the BackgroundWorker know the task succeeded
                callback(true)
            }
        }
    }

    suspend fun cacheFailedDeliveryCountForWidgetAndBackgroundService(
        callback: (Boolean) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        getAllFailedDeliveries { result, _ ->
            if (result == null) {
                // Result is null, callback false to let the BackgroundWorker know the task failed.
                callback(false)
                return@getAllFailedDeliveries
            } else {
                // First move the current count to the previous count (for comparison)
                encryptedSettingsManager.putSettingsInt(
                    SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT_PREVIOUS,
                    encryptedSettingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT)
                )
                // Now store the current count
                encryptedSettingsManager.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT, result.size)

                // Stored data, let the BackgroundWorker know the task succeeded
                callback(true)
            }
        }
    }


    /**
     * FAILED DELIVERIES
     */

    suspend fun getAllFailedDeliveries(
        callback: (ArrayList<FailedDeliveries>?, String?) -> Unit
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
                val addyIoData = gson.fromJson(data, FailedDeliveriesArray::class.java)

                val failedDeliveriesList = ArrayList<FailedDeliveries>()
                failedDeliveriesList.addAll(addyIoData.data)
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
            // Not found, aka the addy.io version is <0.8.1 (this endpoint was introduced in 0.8.1)
            // OR
            // Not found, aka the failed deliveries API is not enabled. (Not part of the user's subscription)
            // =
            // Show a toast (if enabled) letting the user know this feature is only available if the failed deliveries API is enabled
            404 -> {
                callback(null, "404")
            }

            else -> {
                val ex = result.component2()?.message
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getAllFailedDeliveries",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun getSpecificFailedDelivery(
        callback: (FailedDeliveries?, String?) -> Unit,
        id: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get("${API_URL_FAILED_DELIVERIES}/$id")
                .appendHeader(
                    *getHeaders()
                )
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleFailedDelivery::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getSpecificFailedDelivery",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    suspend fun deleteFailedDelivery(
        callback: (String?) -> Unit,
        id: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "deleteFailedDelivery",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    /**
     * UPDATE
     */
    suspend fun getGithubTags(
        callback: (Feed?, String?) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get(GITHUB_TAGS_RSS_FEED)
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val inputStream: InputStream = result.get().byteInputStream()
                val feed = EarlParser.parse(inputStream, 0)
                callback(feed, null)
            }

            else -> {
                val ex = result.component2()?.message
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")

                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getGithubTags",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    /**
     * API TOKEN DETAILS
     */
    suspend fun getApiTokenDetails(
        callback: (ApiTokenDetails?, String?) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get(API_URL_API_TOKEN_DETAILS)
                .appendHeader(
                    *getHeaders()
                )
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, ApiTokenDetails::class.java)
                callback(addyIoData, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")

                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getApiTokenDetails",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }

    /**
     * ACCOUNT NOTIFICATIONS
     */

    suspend fun cacheAccountNotificationsCountForWidgetAndBackgroundService(
        callback: (Boolean) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        getAllAccountNotifications { result, _ ->
            if (result == null) {
                // Result is null, callback false to let the BackgroundWorker know the task failed.
                callback(false)
                return@getAllAccountNotifications
            } else {
                // First move the current count to the previous count (for comparison)
                encryptedSettingsManager.putSettingsInt(
                    SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT_PREVIOUS,
                    encryptedSettingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT)
                )
                // Now store the current count
                encryptedSettingsManager.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT, result.size)

                // Stored data, let the BackgroundWorker know the task succeeded
                callback(true)
            }
        }
    }

    suspend fun getAllAccountNotifications(
        callback: (ArrayList<AccountNotifications>?, String?) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.get(API_URL_ACCOUNT_NOTIFICATIONS)
            .appendHeader(
                *getHeaders()
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, AccountNotificationsArray::class.java)
                val accountNotificationList = ArrayList<AccountNotifications>()
                accountNotificationList.addAll(addyIoData.data)

                callback(accountNotificationList, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "getAllAccountNotifications",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }


    suspend fun notifyServerForSubscriptionChange(
        callback: (UserResource?, String?) -> Unit,
        purchaseToken: String,
        subscriptionId: String,
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val json = JSONObject()
        json.put("purchaseToken", purchaseToken)
        json.put("subscriptionId", subscriptionId)


        val (_, response, result) = Fuel.post(API_URL_NOTIFY_SUBSCRIPTION)
            .appendHeader(
                *getHeaders()
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            201 -> {
                val data = result.get()
                val gson = Gson()
                val addyIoData = gson.fromJson(data, SingleUserResource::class.java)
                callback(addyIoData.data, null)
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
                val fuelResponse = getFuelResponse(response) ?: ex.toString().toByteArray()
                Log.e("AFA", "${response.statusCode} - $ex")
                loggingHelper.addLog(
                    LOGIMPORTANCE.CRITICAL.int,
                    ex.toString(),
                    "notifyServerForSubscriptionChange",
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
                callback(
                    null,
                    ErrorHelper.getErrorMessage(
                        fuelResponse
                    )
                )
            }
        }
    }
}