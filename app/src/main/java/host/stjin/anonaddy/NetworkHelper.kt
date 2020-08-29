package host.stjin.anonaddy

import android.content.Context
import android.os.Handler
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.google.gson.Gson
import host.stjin.anonaddy.AnonAddy.API_URL_ACCOUNT_DETAILS
import host.stjin.anonaddy.AnonAddy.API_URL_ACTIVE_ALIAS
import host.stjin.anonaddy.AnonAddy.API_URL_ACTIVE_DOMAINS
import host.stjin.anonaddy.AnonAddy.API_URL_ACTIVE_USERNAMES
import host.stjin.anonaddy.AnonAddy.API_URL_ALIAS
import host.stjin.anonaddy.AnonAddy.API_URL_ALIAS_RECIPIENTS
import host.stjin.anonaddy.AnonAddy.API_URL_DOMAINS
import host.stjin.anonaddy.AnonAddy.API_URL_DOMAIN_OPTIONS
import host.stjin.anonaddy.AnonAddy.API_URL_ENCRYPTED_RECIPIENTS
import host.stjin.anonaddy.AnonAddy.API_URL_RECIPIENTS
import host.stjin.anonaddy.AnonAddy.API_URL_RECIPIENT_KEYS
import host.stjin.anonaddy.AnonAddy.API_URL_RECIPIENT_RESEND
import host.stjin.anonaddy.AnonAddy.API_URL_USERNAMES
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

    init {
        // Obtain API key from the encrypted preferences
        val settingsManager = SettingsManager(true, context)
        API_KEY = settingsManager.getSettingsString(SettingsManager.PREFS.API_KEY)
    }

    suspend fun verifyApiKey(apiKey: String, callback: (String?) -> Unit) {
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
     * GET USER RESOURCE
     */

    suspend fun getUserResource(
        callback: (UserResource?, String?) -> Unit
    ) {
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
        domain: String,
        description: String,
        format: String,
        callback: (String?) -> Unit
    ) {

        val json = JSONObject()
        json.put("domain", domain)
        json.put("description", description)
        json.put("format", format)


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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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


    fun getAliasesWidget(): ArrayList<Aliases>? {
        var aliasWidgetList: ArrayList<Aliases>? = null
        Fuel.get(API_URL_ALIAS)
            .appendHeader(
                "Authorization" to "Bearer $API_KEY",
                "Content-Type" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json"
            ).responseString { _, response, result ->
                when (response.statusCode) {
                    200 -> {
                        val data = result.get()
                        val gson = Gson()
                        val anonAddyData = gson.fromJson(data, AliasesArray::class.java)

                        val aliasList = ArrayList<Aliases>()

                        for (alias in anonAddyData.data) {
                            if (alias.active) {
                                aliasList.add(alias)
                            }
                        }
                        aliasWidgetList = aliasList
                    }
                    401 -> {
                        Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                        Handler().postDelayed({
                            // Unauthenticated, clear settings
                            SettingsManager(true, context).clearSettingsAndCloseApp()
                        }, 5000)
                        aliasWidgetList = null
                    }
                    else -> {
                        val ex = result.component2()?.message
                        println(ex)
                        loggingHelper.addLog(ex.toString(), "getAliasesWidget")
                        aliasWidgetList = null
                    }
                }
            }


        // Wait 10 seconds
        //TODO fix this gross behaviour
        try {
            Thread.sleep(10000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return aliasWidgetList
    }

    suspend fun getAliases(
        callback: (ArrayList<Aliases>?) -> Unit,
        activeOnly: Boolean,
        includeDeleted: Boolean
    ) {
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
        address: String,
        callback: (String?) -> Unit
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
        recipientId: String,
        callback: (String?) -> Unit
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
        id: String,
        callback: (String?) -> Unit
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
        domain: String,
        callback: (String?, String?) -> Unit
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
        id: String,
        callback: (String?) -> Unit
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
        username: String,
        callback: (String?, String?) -> Unit
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
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
     * ANONADDY SETTINGS
     */

    /*
    Anonaddy settings cannot be changed by API
     */

}