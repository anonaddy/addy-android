package host.stjin.anonaddy_shared.models

import com.google.gson.Gson

data class Error(
    val message: String
)

object ErrorHelper {

    //Try to extract message from error. if fails return full json
    fun getErrorMessage(byteArray: ByteArray): String {
        return try {
            val gson = Gson()
            val addyIoData = gson.fromJson(String(byteArray), Error::class.java)
            addyIoData.message
        } catch (e: Exception) {
            String(byteArray)
        }

    }
}