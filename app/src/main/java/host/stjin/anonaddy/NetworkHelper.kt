package host.stjin.anonaddy

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.google.gson.Gson
import host.stjin.anonaddy.AnonAddy.API_URL_ALL_ALIAS
import host.stjin.anonaddy.AnonAddy.API_URL_ALL_RECIPIENTS
import host.stjin.anonaddy.AnonAddy.API_URL_DOMAIN_OPTIONS
import host.stjin.anonaddy.models.Aliases
import host.stjin.anonaddy.models.AliasesArray
import host.stjin.anonaddy.models.Recipients
import host.stjin.anonaddy.models.RecipientsArray


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

    init {
        // Obtain API key from the encrypted preferences
        val settingsManager = SettingsManager(true, context)
        API_KEY = settingsManager.getSettingsString("API_KEY")
    }

    suspend fun verifyApiKey(apiKey: String, callback: (String?) -> Unit) {
        val (request, response, result) = Fuel.get(API_URL_DOMAIN_OPTIONS)
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
            else -> {
                val ex = result.component2()?.message
                println(ex)
                callback(ex.toString())
            }
        }
    }

    suspend fun getRecipientCount(callback: (Int?) -> Unit) {
        //TODO check responsecode
        val (request, response, result) = Fuel.get(API_URL_ALL_RECIPIENTS)
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
                // Unauthenticated, clear settings
                SettingsManager(true, context).clearSettings()

                val ex = result.component2()?.message
                //TODO log this
                println(ex)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                //TODO log this
                println(ex)
                callback(null)
            }
        }
    }

    suspend fun getAliasesCount(
        callback: (Int?) -> Unit,
        activeOnly: Boolean = true
    ) {
        //TODO check responsecode
        val (request, response, result) = Fuel.get(API_URL_ALL_ALIAS)
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
                // Unauthenticated, clear settings
                SettingsManager(true, context).clearSettings()

                val ex = result.component2()?.message
                //TODO log this
                println(ex)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                //TODO log this
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
        val (request, response, result) = if (includeDeleted) {
            Fuel.get(API_URL_ALL_ALIAS, listOf("deleted" to "with"))
                .appendHeader(
                    "Authorization" to "Bearer $API_KEY",
                    "Content-Type" to "application/json",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Accept" to "application/json"
                )
                .awaitStringResponseResult()
        } else {
            Fuel.get(API_URL_ALL_ALIAS)
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
                // Unauthenticated, clear settings
                SettingsManager(true, context).clearSettings()

                val ex = result.component2()?.message
                //TODO log this
                println(ex)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                //TODO log this
                println(ex)
                callback(null)
            }
        }
    }

    suspend fun getRecipients(
        callback: (ArrayList<Recipients>?) -> Unit,
        verifiedOnly: Boolean
    ) {
        val (request, response, result) = Fuel.get(API_URL_ALL_RECIPIENTS)
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
                // Unauthenticated, clear settings
                SettingsManager(true, context).clearSettings()

                val ex = result.component2()?.message
                //TODO log this
                println(ex)
                callback(null)
            }
            else -> {
                val ex = result.component2()?.message
                //TODO log this
                println(ex)
                callback(null)
            }
        }
    }
}