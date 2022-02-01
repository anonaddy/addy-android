package host.stjin.anonaddy_shared.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy_shared.models.WearOSSettings

object GsonTools {
    fun jsonToWearOSSettingsObject(context: Context, json: String): WearOSSettings? {
        //TODO FIX? maybe log to watch itself??
        //val loggingHelper = LoggingHelper(context)

        return try {
            Gson().fromJson(
                json,
                object : TypeToken<WearOSSettings?>() {}.type
            ) as WearOSSettings

        } catch (e: Exception) {
            val ex = e.message
            println(ex)
            //loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "jsonToWearOSSettingsObject", null)
            null
        }
    }
}

