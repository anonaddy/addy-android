package host.stjin.anonaddy_shared.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.models.WearOSSettings

inline fun <reified T> Gson.fromJson(json: String): T {
    return fromJson(json, object : TypeToken<T>() {}.type)
}

object GsonTools {
    val gson = Gson()

    inline fun <reified T> fromJsonSafe(context: Context, json: String, methodName: String): T? {
        return try {
            val type = object : TypeToken<T>() {}.type
            gson.fromJson<T>(json, type)
        } catch (e: Exception) {
            val ex = e.message
            Log.e("GsonTools", ex.toString())
            LoggingHelper(context).addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), methodName, null)
            null
        }
    }

    fun jsonToAliasObject(context: Context, json: String): ArrayList<Aliases>? {
        return fromJsonSafe<ArrayList<Aliases>>(context, json, "jsonToAliasObject")
    }

    fun jsonToUserResourceObject(context: Context, json: String): UserResource? {
        return fromJsonSafe<UserResource>(context, json, "jsonToUserResourceObject")
    }

    fun jsonToAliasSortFilterObject(context: Context, json: String): AliasSortFilter? {
        return fromJsonSafe<AliasSortFilter>(context, json, "jsonToAliasSortFilterObject")
    }

    fun jsonToWearOSSettingsObject(context: Context, json: String): WearOSSettings? {
        return fromJsonSafe<WearOSSettings>(context, json, "jsonToWearOSSettingsObject")
    }
}