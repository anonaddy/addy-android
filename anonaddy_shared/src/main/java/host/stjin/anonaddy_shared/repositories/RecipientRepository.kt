package host.stjin.anonaddy_shared.repositories

import android.content.Context
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
        val response = executePost(API_URL_RECIPIENTS, json.toString())

        return handleResponse(response, "addRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun getRecipients(verifiedOnly: Boolean = false): NetworkResult<PaginatedResponse<Recipients>> {
        waitForInit()

        val parameters = ArrayList<Pair<String, Any>>()
        if (verifiedOnly) {
            parameters.add(Pair("filter[verified]", "true"))
        }

        val response = executeGet(API_URL_RECIPIENTS, parameters)

        return handleResponse(response, "getRecipients") { data ->
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

        val response = executeGet("$API_URL_RECIPIENTS/$recipientId")

        return handleResponse(response, "getSpecificRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun deleteRecipient(recipientId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_RECIPIENTS/$recipientId")

        return handleStatusResponse(response, "deleteRecipient", expectedCode = 204)
    }

    suspend fun activateRecipient(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val response = executePost(API_URL_ACTIVE_RECIPIENTS, JSONObject().put("id", recipientId).toString())

        return handleResponse(response, "activateRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun deactivateRecipient(recipientId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_ACTIVE_RECIPIENTS/$recipientId")

        return handleStatusResponse(response, "deactivateRecipient", expectedCode = 204)
    }

    suspend fun allowRecipientToReplySend(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val response = executePost(API_URL_ALLOWED_RECIPIENTS, JSONObject().put("id", recipientId).toString())

        return handleResponse(response, "allowRecipientToReplySend") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun disallowRecipientToReplySend(recipientId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_ALLOWED_RECIPIENTS/$recipientId")

        return handleStatusResponse(response, "disallowRecipientToReplySend", expectedCode = 204)
    }

    suspend fun enableEncryptionRecipient(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val response = executePost(API_URL_ENCRYPTED_RECIPIENTS, JSONObject().put("id", recipientId).toString())

        return handleResponse(response, "enableEncryptionRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun disableEncryptionRecipient(recipientId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_ENCRYPTED_RECIPIENTS/$recipientId")

        return handleStatusResponse(response, "disableEncryptionRecipient", expectedCode = 204)
    }

    suspend fun enablePgpInlineRecipient(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val response = executePost(API_URL_INLINE_ENCRYPTED_RECIPIENTS, JSONObject().put("id", recipientId).toString())

        return handleResponse(response, "enablePgpInlineRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun disablePgpInlineRecipient(recipientId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_INLINE_ENCRYPTED_RECIPIENTS/$recipientId")

        return handleStatusResponse(response, "disablePgpInlineRecipient", expectedCode = 204)
    }

    suspend fun enableRemovePgpKeysRecipients(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val response = executePost(API_URL_REMOVE_PGP_KEYS_RECIPIENTS, JSONObject().put("id", recipientId).toString())

        return handleResponse(response, "enableRemovePgpKeysRecipients") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun disableRemovePgpKeysRecipients(recipientId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_REMOVE_PGP_KEYS_RECIPIENTS/$recipientId")

        return handleStatusResponse(response, "disableRemovePgpKeysRecipients", expectedCode = 204)
    }

    suspend fun enableRemovePgpSignaturesRecipients(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val response = executePost(API_URL_REMOVE_PGP_SIGNATURES_RECIPIENTS, JSONObject().put("id", recipientId).toString())

        return handleResponse(response, "enableRemovePgpSignaturesRecipients") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun disableRemovePgpSignaturesRecipients(recipientId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_REMOVE_PGP_SIGNATURES_RECIPIENTS/$recipientId")

        return handleStatusResponse(response, "disableRemovePgpSignaturesRecipients", expectedCode = 204)
    }

    suspend fun enableProtectedHeadersRecipient(recipientId: String): NetworkResult<Recipients> {
        waitForInit()

        val response = executePost(API_URL_PROTECTED_HEADERS_RECIPIENTS, JSONObject().put("id", recipientId).toString())

        return handleResponse(response, "enableProtectedHeadersRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun disableProtectedHeadersRecipient(recipientId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_PROTECTED_HEADERS_RECIPIENTS/$recipientId")

        return handleStatusResponse(response, "disableProtectedHeadersRecipient", expectedCode = 204)
    }

    suspend fun addEncryptionKeyRecipient(recipientId: String, keyData: String): NetworkResult<Recipients> {
        waitForInit()

        val json = JSONObject().apply {
            put("key_data", keyData)
        }

        val response = executePatch("$API_URL_RECIPIENT_KEYS/$recipientId", json.toString())

        return handleResponse(response, "addEncryptionKeyRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun removeEncryptionKeyRecipient(recipientId: String): NetworkResult<String> {
        waitForInit()

        val response = executeDelete("$API_URL_RECIPIENT_KEYS/$recipientId")

        return handleStatusResponse(response, "removeEncryptionKeyRecipient", expectedCode = 204)
    }

    suspend fun updateDescriptionSpecificRecipient(recipientId: String, description: String?): NetworkResult<Recipients> {
        waitForInit()

        val json = JSONObject().put("description", description)
        val response = executePatch("$API_URL_RECIPIENTS/$recipientId", json.toString())

        return handleResponse(response, "updateDescriptionSpecificRecipient") { gson.fromJson(it, SingleRecipient::class.java).data }
    }

    suspend fun resendVerificationEmail(recipientId: String): NetworkResult<String> {
        waitForInit()

        val response = executePost(API_URL_RECIPIENT_RESEND, JSONObject().put("recipient_id", recipientId).toString())

        return handleStatusResponse(response, "resendVerificationEmail", expectedCode = 200)
    }
}
