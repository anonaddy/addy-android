package host.stjin.anonaddy

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.google.gson.Gson
import host.stjin.anonaddy.AnonAddy.API_BASE_URL
import host.stjin.anonaddy.AnonAddy.API_URL_ACCOUNT_DETAILS
import host.stjin.anonaddy.AnonAddy.API_URL_ACTIVE_ALIAS
import host.stjin.anonaddy.AnonAddy.API_URL_ACTIVE_DOMAINS
import host.stjin.anonaddy.AnonAddy.API_URL_ACTIVE_RULES
import host.stjin.anonaddy.AnonAddy.API_URL_ACTIVE_USERNAMES
import host.stjin.anonaddy.AnonAddy.API_URL_ALIAS
import host.stjin.anonaddy.AnonAddy.API_URL_ALIAS_RECIPIENTS
import host.stjin.anonaddy.AnonAddy.API_URL_APP_VERSION
import host.stjin.anonaddy.AnonAddy.API_URL_CATCH_ALL_DOMAINS
import host.stjin.anonaddy.AnonAddy.API_URL_DOMAINS
import host.stjin.anonaddy.AnonAddy.API_URL_DOMAIN_OPTIONS
import host.stjin.anonaddy.AnonAddy.API_URL_ENCRYPTED_RECIPIENTS
import host.stjin.anonaddy.AnonAddy.API_URL_RECIPIENTS
import host.stjin.anonaddy.AnonAddy.API_URL_RECIPIENT_KEYS
import host.stjin.anonaddy.AnonAddy.API_URL_RECIPIENT_RESEND
import host.stjin.anonaddy.AnonAddy.API_URL_REORDER_RULES
import host.stjin.anonaddy.AnonAddy.API_URL_RULES
import host.stjin.anonaddy.AnonAddy.API_URL_USERNAMES
import host.stjin.anonaddy.AnonAddy.lazyMgr
import host.stjin.anonaddy.models.*
import host.stjin.anonaddy.utils.LoggingHelper
import org.apache.commons.lang3.StringUtils
import org.json.JSONArray
import org.json.JSONObject


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
    503	Service Unavailable -- We're temporarially offline for maintanance. Please try again later*/

    private var API_KEY: String? = null
    private val loggingHelper = LoggingHelper(context)
    val settingsManager = SettingsManager(true, context)

    init {
        // Obtain API key from the encrypted preferences
        API_KEY = settingsManager.getSettingsString(SettingsManager.PREFS.API_KEY)
        API_BASE_URL = settingsManager.getSettingsString(SettingsManager.PREFS.BASE_URL) ?: API_BASE_URL
    }


    // Separate method, with a try/catch because you can't toast on a Non-UI thread. And the widgets might call methods and there *is* a chance
    // these calls return a 404
    private fun invalidApiKey() {
        try {
            Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            val ex = e.message
            println(ex)
            loggingHelper.addLog(ex.toString(), "invalidApiKey")
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
        val (_, response, result) = Fuel.get(API_URL_DOMAIN_OPTIONS)
            .appendHeader(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "verifyApiKey")
                callback(ex.toString())
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
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "getAnonAddyInstanceVersion")
                callback(null, ex.toString())
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
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "getUserResource")
                callback(null, ex.toString())
            }
        }
    }

    suspend fun getDomainOptions(
        callback: (DomainOptions?) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.get(API_URL_DOMAIN_OPTIONS)
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
            )
            .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, DomainOptions::class.java)
                callback(anonAddyData)
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
                loggingHelper.addLog(ex.toString(), "getDomainOptions")
                callback(null)
            }
        }
    }


    /**
     * ALIASES
     */


    suspend fun addAlias(
        callback: (String?) -> Unit,
        domain: String,
        description: String,
        format: String,
        local_part: String
    ) {
        val json = JSONObject()
        json.put("domain", domain)
        json.put("description", description)
        json.put("format", format)
        json.put("local_part", local_part)


        val (_, response, result) = Fuel.post(API_URL_ALIAS)
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
            )
            .body(json.toString())
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
                loggingHelper.addLog(ex.toString(), "addAlias")
                callback(ex.toString())
            }
        }
    }


    suspend fun getAliases(
        callback: (ArrayList<Aliases>?) -> Unit,
        activeOnly: Boolean,
        includeDeleted: Boolean
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }


        val (_, response, result) = if (includeDeleted) {
            Fuel.get(API_URL_ALIAS, listOf("deleted" to "with"))
                .appendHeader(
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
                )
                .awaitStringResponseResult()
        } else {
            Fuel.get(API_URL_ALIAS)
                .appendHeader(
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
                )
                .awaitStringResponseResult()
        }


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, AliasesArray::class.java)
                val aliasList = ArrayList<Aliases>()

                if (activeOnly) {
                    for (alias in anonAddyData.data) {
                        if (alias.active) {
                            aliasList.add(alias)
                        }
                    }
                } else {
                    aliasList.addAll(anonAddyData.data)
                }
                callback(aliasList)
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
                loggingHelper.addLog(ex.toString(), "getAliases")
                callback(null)
            }
        }
    }

    suspend fun getSpecificAlias(
        callback: (Aliases?) -> Unit,
        aliasId: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get("${API_URL_ALIAS}/$aliasId")
                .appendHeader(
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
                )
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleAlias::class.java)
                callback(anonAddyData.data)
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
                loggingHelper.addLog(ex.toString(), "getSpecificAlias")
                callback(null)
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
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "updateDescriptionSpecificAlias")
                callback(ex.toString())
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
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "updateRecipientsSpecificAlias")
                callback(ex.toString())
            }
        }
    }


    suspend fun deactivateSpecificAlias(
        callback: (String?) -> Unit?,
        aliasId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_ACTIVE_ALIAS}/$aliasId")
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "deactivateSpecificAlias")
                callback(ex.toString())
            }
        }
    }


    suspend fun activateSpecificAlias(
        callback: (String?) -> Unit,
        aliasId: String
    ) {
        val json = JSONObject()
        json.put("id", aliasId)

        val (_, response, result) = Fuel.post(API_URL_ACTIVE_ALIAS)
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "activateSpecificAlias")
                callback(ex.toString())
            }
        }
    }


    suspend fun deleteAlias(
        callback: (String?) -> Unit,
        aliasId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_ALIAS}/$aliasId")
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "deleteAlias")
                callback(ex.toString())
            }
        }
    }

    suspend fun forgetAlias(
        callback: (String?) -> Unit,
        aliasId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_ALIAS}/$aliasId/forget")
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "forgetAlias")
                callback(ex.toString())
            }
        }
    }

    suspend fun restoreAlias(
        callback: (String?) -> Unit,
        aliasId: String
    ) {
        val (_, response, result) = Fuel.patch("${API_URL_ALIAS}/$aliasId/restore")
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "restoreAlias")
                callback(ex.toString())
            }
        }
    }


    /**
     * RECIPIENTS
     */

    suspend fun addRecipient(
        callback: (String?) -> Unit,
        address: String
    ) {
        val json = JSONObject()
        json.put("email", address)

        val (_, response, result) = Fuel.post(API_URL_RECIPIENTS)
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
            )
            .body(json.toString())
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
                loggingHelper.addLog(ex.toString(), "addRecipient")
                callback(ex.toString())
            }
        }
    }

    suspend fun getRecipients(
        callback: (ArrayList<Recipients>?) -> Unit,
        verifiedOnly: Boolean
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.get(API_URL_RECIPIENTS)
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                callback(recipientList)
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
                loggingHelper.addLog(ex.toString(), "getRecipients")
                callback(null)
            }
        }
    }

    suspend fun deleteRecipient(
        callback: (String?) -> Unit,
        recipientId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_RECIPIENTS}/$recipientId")
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "deleteRecipient")
                callback(ex.toString())
            }
        }
    }


    suspend fun disableEncryptionRecipient(
        callback: (String?) -> Unit?,
        recipientId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_ENCRYPTED_RECIPIENTS}/$recipientId")
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "disableEncryptionRecipient")
                callback(ex.toString())
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
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "enableEncryptionRecipient")
                callback(ex.toString())
            }
        }
    }


    suspend fun removeEncryptionKeyRecipient(
        callback: (String?) -> Unit,
        recipientId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_RECIPIENT_KEYS}/$recipientId")
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "removeEncryptionKeyRecipient")
                callback(ex.toString())
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
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "addEncryptionKeyRecipient")
                callback(ex.toString())
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
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "getSpecificRecipient")
                callback(null, ex.toString())
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
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "resendVerificationEmail")
                callback(ex.toString())
            }
        }
    }


    /**
     * DOMAINS
     */

    suspend fun getAllDomains(
        callback: (ArrayList<Domains>?) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.get(API_URL_DOMAINS)
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, DomainsArray::class.java)
                val domainList = ArrayList<Domains>()
                domainList.addAll(anonAddyData.data)
                callback(domainList)
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
                loggingHelper.addLog(ex.toString(), "getDomains")
                callback(null)
            }
        }
    }

    suspend fun deleteDomain(
        callback: (String?) -> Unit,
        id: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_DOMAINS}/$id")
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "deleteDomain")
                callback(ex.toString())
            }
        }
    }

    suspend fun addDomain(
        callback: (String?, String?) -> Unit,
        domain: String
    ) {
        val json = JSONObject()
        json.put("domain", domain)

        val (_, response, result) = Fuel.post(API_URL_DOMAINS)
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            201 -> {
                callback("201", null)
            }

            // 404 means that the setup is not completed
            404 -> {
                //TODO Fix getting body the normal way
                val body = StringUtils.substringBetween(response.toString(), "Body : ", "\n")
                callback("404", body)
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
                loggingHelper.addLog(ex.toString(), "addDomain")
                callback(ex.toString(), null)
            }
        }
    }

    suspend fun getSpecificDomain(
        callback: (Domains?) -> Unit,
        id: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get("${API_URL_DOMAINS}/$id")
                .appendHeader(
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
                )
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleDomain::class.java)
                callback(anonAddyData.data)
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
                loggingHelper.addLog(ex.toString(), "getSpecificDomain")
                callback(null)
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
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "updateDefaultRecipientForSpecificDomain")
                callback(ex.toString())
            }
        }
    }

    suspend fun deactivateSpecificDomain(
        callback: (String?) -> Unit?,
        domainId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_ACTIVE_DOMAINS}/$domainId")
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "deactivateSpecificDomain")
                callback(ex.toString())
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
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "activateSpecificDomain")
                callback(ex.toString())
            }
        }
    }

    suspend fun disableCatchAllSpecificDomain(
        callback: (String?) -> Unit?,
        domainId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_CATCH_ALL_DOMAINS}/$domainId")
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "disableCatchAllSpecificDomain")
                callback(ex.toString())
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
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "enableCatchAllSpecificDomain")
                callback(ex.toString())
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
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "updateDescriptionSpecificDomain")
                callback(ex.toString())
            }
        }
    }


    /**
     * USERNAMES
     */

    suspend fun getAllUsernames(
        callback: (ArrayList<Usernames>?) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.get(API_URL_USERNAMES)
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, UsernamesArray::class.java)

                val usernamesList = ArrayList<Usernames>()
                usernamesList.addAll(anonAddyData.data)
                callback(usernamesList)
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
                loggingHelper.addLog(ex.toString(), "getUsernames")
                callback(null)
            }
        }
    }

    suspend fun deleteUsername(
        callback: (String?) -> Unit,
        id: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_USERNAMES}/$id")
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "deleteUsername")
                callback(ex.toString())
            }
        }
    }

    suspend fun addUsername(
        callback: (String?, String?) -> Unit,
        username: String
    ) {
        val json = JSONObject()
        json.put("username", username)

        val (_, response, result) = Fuel.post(API_URL_USERNAMES)
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
            )
            .body(json.toString())
            .awaitStringResponseResult()

        when (response.statusCode) {
            201 -> {
                callback("201", null)
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
                loggingHelper.addLog(ex.toString(), "addUsername")
                callback(ex.toString(), null)
            }
        }
    }

    suspend fun getSpecificUsername(
        callback: (Usernames?) -> Unit,
        id: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get("${API_URL_USERNAMES}/$id")
                .appendHeader(
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
                )
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleUsername::class.java)
                callback(anonAddyData.data)
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
                loggingHelper.addLog(ex.toString(), "getSpecificUsername")
                callback(null)
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
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "updateDefaultRecipientForSpecificUsername")
                callback(ex.toString())
            }
        }
    }

    suspend fun deactivateSpecificUsername(
        callback: (String?) -> Unit?,
        usernameId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_ACTIVE_USERNAMES}/$usernameId")
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "deactivateSpecificUsername")
                callback(ex.toString())
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
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "activateSpecificUsername")
                callback(ex.toString())
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
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "updateDescriptionSpecificUsername")
                callback(ex.toString())
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
        callback: (ArrayList<Rules>?) -> Unit
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) = Fuel.get(API_URL_RULES)
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
            )
            .awaitStringResponseResult()

        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, RulesArray::class.java)

                val domainList = ArrayList<Rules>()
                domainList.addAll(anonAddyData.data)
                callback(domainList)
            }
            401 -> {
                invalidApiKey()
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettingsAndCloseApp()
                }, 5000)
                callback(null)
            }
            // Not found, aka the AnonAddy version is <0.6.0 (this endpoint was introduced in 0.6.0)
            // OR
            // Not found, aka the rules API (which is in beta as of 0.6.0) is not enabled.
            // =
            // Show a toast letting the user know this feature is only available if the rules API is enabled
            404 -> {
                Toast.makeText(context, context.resources.getString(R.string.rules_unavailable_404), Toast.LENGTH_LONG).show()
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(ex.toString(), "getAllRules")
                callback(null)
            }
        }
    }

    suspend fun getSpecificRule(
        callback: (Rules?) -> Unit,
        id: String
    ) {

        if (BuildConfig.DEBUG) {
            println("${object {}.javaClass.enclosingMethod?.name} called from ${Thread.currentThread().stackTrace[3].className};${Thread.currentThread().stackTrace[3].methodName}")
        }

        val (_, response, result) =
            Fuel.get("${API_URL_RULES}/$id")
                .appendHeader(
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
                )
                .awaitStringResponseResult()


        when (response.statusCode) {
            200 -> {
                val data = result.get()
                val gson = Gson()
                val anonAddyData = gson.fromJson(data, SingleRule::class.java)
                callback(anonAddyData.data)
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
                loggingHelper.addLog(ex.toString(), "getSpecificRule")
                callback(null)
            }
        }
    }


    suspend fun deleteRule(
        callback: (String?) -> Unit,
        id: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_RULES}/$id")
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "deleteRule")
                callback(ex.toString())
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
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "createRule")
                callback(ex.toString())
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
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "reorderRules")
                callback(ex.toString())
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
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "updateRule")
                callback(ex.toString())
            }
        }
    }

    suspend fun deactivateSpecificRule(
        callback: (String?) -> Unit?,
        ruleId: String
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_ACTIVE_RULES}/$ruleId")
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "deactivateSpecificRule")
                callback(ex.toString())
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
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
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
                loggingHelper.addLog(ex.toString(), "activateSpecificRule")
                callback(ex.toString())
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
     * WIDGET
     */
    // The widgets require the following data:
    // Widget 1: Aliases
    // Widget 2: Domain options

    suspend fun cacheAliasDataForWidget(
        callback: (Boolean) -> Unit
    ) {
        // Widget 1
        getAliases({ list ->
            if (list == null) {
                // Result is null, callback false to let the BackgroundWorker know the task failed.
                callback(false)
                return@getAliases
            } else {
                // Turn the list into a json object
                val data = Gson().toJson(list)

                // Get and turn the current list (before this call) into a string
                val currentList = settingsManager.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DATA_ALIASES)

                // If the list is not null, move the current list (before this call) to the PREV position for AliasWatcher to compare
                // List could be null if this would be the first time the service is running
                currentList?.let { settingsManager.putSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DATA_ALIASES_PREVIOUS, it) }

                // Store a copy of the just received data locally
                settingsManager.putSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DATA_ALIASES, data)

                // Stored data, let the BackgroundWorker know the task succeeded
                callback(true)
            }
        }, activeOnly = false, includeDeleted = true)
    }

    suspend fun cacheDomainsDataForWidget(
        callback: (Boolean) -> Unit
    ) {
        // Widget 2
        getDomainOptions { result ->
            if (result == null) {
                // Result is null, callback false to let the BackgroundWorker know the task failed.
                callback(false)
                return@getDomainOptions
            } else {
                // Turn the list into a json object
                val data = Gson().toJson(result)
                // Store a copy of this data locally
                settingsManager.putSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DATA_DOMAIN_OPTIONS, data)

                // Stored data, let the BackgroundWorker know the task succeeded
                callback(true)
            }
        }
    }
}