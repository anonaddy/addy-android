package host.stjin.anonaddy.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy.models.Aliases
import host.stjin.anonaddy.models.LOGIMPORTANCE

object GsonTools {
    fun jsonToAliasObject(context: Context, json: String): ArrayList<Aliases>? {
        val loggingHelper = LoggingHelper(context)

        return try {
            Gson().fromJson(
                json,
                object : TypeToken<ArrayList<Aliases?>?>() {}.type
            ) as ArrayList<Aliases>

        } catch (e: Exception) {
            val ex = e.message
            println(ex)
            loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "jsonToAliasObject", null)
            null
        }
    }
}