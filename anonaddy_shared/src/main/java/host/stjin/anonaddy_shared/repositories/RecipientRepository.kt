package host.stjin.anonaddy_shared.repositories

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import host.stjin.anonaddy_shared.AddyIo.API_URL_ACTIVE_RECIPIENTS
import host.stjin.anonaddy_shared.AddyIo.API_URL_ALLOWED_RECIPIENTS
import host.stjin.anonaddy_shared.AddyIo.API_URL_ENCRYPTED_RECIPIENTS
import host.stjin.anonaddy_shared.AddyIo.API_URL_INLINE_ENCRYPTED_RECIPIENTS
import host.stjin.anonaddy_shared.AddyIo.API_URL_PROTECTED_HEADERS_RECIPIENTS
import host.stjin.anonaddy_shared.AddyIo.API_URL_RECIPIENTS
import host.stjin.anonaddy_shared.AddyIo.API_URL_RECIPIENT_KEYS
import host.stjin.anonaddy_shared.AddyIo.API_URL_RECIPIENT_RESEND
import host.stjin.anonaddy_shared.AddyIo.API_URL_REMOVE_PGP_KEYS_RECIPIENTS
import host.stjin.anonaddy_shared.AddyIo.API_URL_REMOVE_PGP_SIGNATURES_RECIPIENTS
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.models.SingleRecipient
import host.stjin.anonaddy_shared.network.BaseNetworkClient
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.DefaultDispatcherProvider
import host.stjin.anonaddy_shared.utils.DispatcherProvider
import host.stjin.anonaddy_shared.utils.fromJson
import org.json.JSONObject

class RecipientRepository(
    context: Context,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BaseNetworkClient(context, dispatchers) {

    suspend fun addRecipient(email: String): NetworkResult<Recipients> {
        waitForInit()

        val json = JSONObject().put("email", email)
        val (_, response, result) = Fuel.post(API_URL_RECIPIENTS)
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "addRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun getRecipients(page: Int? = null, size: Int? = 20, verifiedOnly: Boolean = false): NetworkResult<PaginatedResponse<Recipients>> {
        waitForInit()

        val parameters = arrayListOf<Pair<String, String>>()
        if (size != null) parameters.add("page[size]" to size.toString())
        if (page != null) parameters.add("page[number]" to page.toString())

        val (_, response, result) = Fuel.get(API_URL_RECIPIENTS, parameters)
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleResponse(response, result, "getRecipients") { data ->
            val addyIoData: PaginatedResponse<Recipients> = gson.fromJson(data)
            if (verifiedOnly) {
                PaginatedResponse(
                    data = ArrayList(addyIoData.data.filter { it.email_verified_at != null })
                )
            } else {
                addyIoData
            }
        }
    }

    suspend fun getSpecificRecipient(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val (_, response, result) = Fuel.get("$API_URL_RECIPIENTS/$recipientId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleResponse(response, result, "getSpecificRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun deleteRecipient(recipientId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_RECIPIENTS/$recipientId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "deleteRecipient", expectedCode = 204)
    }

    suspend fun activateRecipient(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_ACTIVE_RECIPIENTS)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", recipientId).toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "activateRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun deactivateRecipient(recipientId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_ACTIVE_RECIPIENTS/$recipientId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "deactivateRecipient", expectedCode = 204)
    }

    suspend fun allowRecipientToReplySend(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_ALLOWED_RECIPIENTS)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", recipientId).toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "allowRecipientToReplySend") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun disallowRecipientToReplySend(recipientId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_ALLOWED_RECIPIENTS/$recipientId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "disallowRecipientToReplySend", expectedCode = 204)
    }

    suspend fun enableEncryptionRecipient(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_ENCRYPTED_RECIPIENTS)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", recipientId).toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "enableEncryptionRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun disableEncryptionRecipient(recipientId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_ENCRYPTED_RECIPIENTS/$recipientId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "disableEncryptionRecipient", expectedCode = 204)
    }

    suspend fun enablePgpInlineRecipient(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_INLINE_ENCRYPTED_RECIPIENTS)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", recipientId).toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "enablePgpInlineRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun disablePgpInlineRecipient(recipientId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_INLINE_ENCRYPTED_RECIPIENTS/$recipientId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "disablePgpInlineRecipient", expectedCode = 204)
    }

    suspend fun enableRemovePgpKeysRecipients(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_REMOVE_PGP_KEYS_RECIPIENTS)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", recipientId).toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "enableRemovePgpKeysRecipients") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun disableRemovePgpKeysRecipients(recipientId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_REMOVE_PGP_KEYS_RECIPIENTS/$recipientId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "disableRemovePgpKeysRecipients", expectedCode = 204)
    }

    suspend fun enableRemovePgpSignaturesRecipients(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_REMOVE_PGP_SIGNATURES_RECIPIENTS)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", recipientId).toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "enableRemovePgpSignaturesRecipients") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun disableRemovePgpSignaturesRecipients(recipientId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_REMOVE_PGP_SIGNATURES_RECIPIENTS/$recipientId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "disableRemovePgpSignaturesRecipients", expectedCode = 204)
    }

    suspend fun enableProtectedHeadersRecipient(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_PROTECTED_HEADERS_RECIPIENTS)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("id", recipientId).toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "enableProtectedHeadersRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun disableProtectedHeadersRecipient(recipientId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_PROTECTED_HEADERS_RECIPIENTS/$recipientId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "disableProtectedHeadersRecipient", expectedCode = 204)
    }

    suspend fun addEncryptionKeyRecipient(recipientId: String, keyData: String): NetworkResult<Recipients> {
        waitForInit()

        val json = JSONObject().apply {
            put("key_data", keyData)
        }

        val (_, response, result) = Fuel.patch("$API_URL_RECIPIENT_KEYS/$recipientId")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "addEncryptionKeyRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun removeEncryptionKeyRecipient(recipientId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.delete("$API_URL_RECIPIENT_KEYS/$recipientId")
            .appendHeader(*getHeaders())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "removeEncryptionKeyRecipient", expectedCode = 204)
    }

    suspend fun updateDescriptionSpecificRecipient(recipientId: String, description: String?): NetworkResult<Recipients> {
        waitForInit()

        val json = JSONObject().put("description", description)
        val (_, response, result) = Fuel.patch("$API_URL_RECIPIENTS/$recipientId")
            .appendHeader(*getHeaders())
            .body(json.toString())
            .awaitStringResponseResult()

        return handleResponse(response, result, "updateDescriptionSpecificRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun resendVerificationEmail(recipientId: String): NetworkResult<String> {
        waitForInit()

        val (_, response, result) = Fuel.post(API_URL_RECIPIENT_RESEND)
            .appendHeader(*getHeaders())
            .body(JSONObject().put("recipient_id", recipientId).toString())
            .awaitStringResponseResult()

        return handleStatusResponse(response, result, "resendVerificationEmail", expectedCode = 200)
    }
}
