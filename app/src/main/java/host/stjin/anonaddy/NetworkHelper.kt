package host.stjin.anonaddy

import android.content.Context
import android.os.Handler
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.google.gson.Gson
import host.stjin.anonaddy.AnonAddy.API_URL_ACTIVE_ALIAS
import host.stjin.anonaddy.AnonAddy.API_URL_ALIAS
import host.stjin.anonaddy.AnonAddy.API_URL_ALIAS_RECIPIENTS
import host.stjin.anonaddy.AnonAddy.API_URL_DOMAIN_OPTIONS
import host.stjin.anonaddy.AnonAddy.API_URL_ENCRYPTED_RECIPIENTS
import host.stjin.anonaddy.AnonAddy.API_URL_RECIPIENTS
import host.stjin.anonaddy.AnonAddy.API_URL_RECIPIENT_KEYS
import host.stjin.anonaddy.models.*
import host.stjin.anonaddy.utils.LoggingHelper
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
        API_KEY = settingsManager.getSettingsString("API_KEY")
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
                    SettingsManager(true, context).clearSettings()
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

    suspend fun addRecipient(
        address: String,
        callback: (String?) -> Unit
    ) {

        val json = JSONObject()
        json.put("email", address)

        val (request, response, result) = Fuel.post(API_URL_RECIPIENTS)
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
                    SettingsManager(true, context).clearSettings()
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

    suspend fun getRecipientCount(callback: (Int?) -> Unit) {
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
                callback(anonAddyData.data.size)
            }
            401 -> {
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettings()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(ex.toString(), "getRecipientCount")
                callback(null)
            }
        }
    }

    suspend fun getAliasesCount(
        callback: (Int?) -> Unit,
        activeOnly: Boolean = true
    ) {
        val (_, response, result) = Fuel.get(API_URL_ALIAS)
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
                val anonAddyData = gson.fromJson(data, AliasesArray::class.java)

                var activeAliasses = 0
                if (activeOnly) {
                    for (alias in anonAddyData.data) {
                        if (alias.active) {
                            activeAliasses++
                        }
                    }
                } else {
                    activeAliasses = anonAddyData.data.size
                }

                callback(activeAliasses)
            }
            401 -> {
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettings()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                loggingHelper.addLog(ex.toString(), "getAliasesCount")
                println(ex)
                callback(null)
            }
        }
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
                    SettingsManager(true, context).clearSettings()
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
        id: String
    ) {
        val (_, response, result) =
            Fuel.get("${API_URL_ALIAS}/$id")
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
                    SettingsManager(true, context).clearSettings()
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
                    SettingsManager(true, context).clearSettings()
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
                    SettingsManager(true, context).clearSettings()
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

    suspend fun getDomainOptions(
        callback: (Domains?) -> Unit
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
                val anonAddyData = gson.fromJson(data, Domains::class.java)
                callback(anonAddyData)
            }
            401 -> {
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettings()
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
                    SettingsManager(true, context).clearSettings()
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
                    SettingsManager(true, context).clearSettings()
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
                    SettingsManager(true, context).clearSettings()
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
                    SettingsManager(true, context).clearSettings()
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
                    SettingsManager(true, context).clearSettings()
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


    /*
    Manage Recipient
     */

    suspend fun deleteRecipient(
        id: String,
        callback: (String?) -> Unit
    ) {
        val (_, response, result) = Fuel.delete("${API_URL_RECIPIENTS}/$id")
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
                    SettingsManager(true, context).clearSettings()
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
                    SettingsManager(true, context).clearSettings()
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
                    SettingsManager(true, context).clearSettings()
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
                    SettingsManager(true, context).clearSettings()
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
        aliasId: String,
        keyData: String
    ) {

        val json = JSONObject()
        json.put("key_data", keyData)


        val (_, response, result) = Fuel.patch("${API_URL_RECIPIENT_KEYS}/$aliasId")
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
                    SettingsManager(true, context).clearSettings()
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
        callback: (Recipients?) -> Unit,
        aliasId: String
    ) {
        val (_, response, result) =
            Fuel.get("${API_URL_RECIPIENTS}/$aliasId")
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
                callback(anonAddyData.data)
            }
            401 -> {
                Toast.makeText(context, context.resources.getString(R.string.api_key_invalid), Toast.LENGTH_LONG).show()
                Handler().postDelayed({
                    // Unauthenticated, clear settings
                    SettingsManager(true, context).clearSettings()
                }, 5000)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                println(ex)
                loggingHelper.addLog(ex.toString(), "getSpecificRecipient")
                callback(null)
            }
        }
    }
}