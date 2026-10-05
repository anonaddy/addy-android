package host.stjin.anonaddy_shared.models

import host.stjin.anonaddy_shared.utils.GsonTools

data class Error(
    val message: String
)

object ErrorHelper {

    //Try to extract message from error. if fails return full json
    fun getErrorMessage(byteArray: ByteArray): String {
        return try {
            val addyIoData = GsonTools.gson.fromJson(String(byteArray), Error::class.java)
            addyIoData.message
        } catch (e: Exception) {
            String(byteArray)
        }

    }
}