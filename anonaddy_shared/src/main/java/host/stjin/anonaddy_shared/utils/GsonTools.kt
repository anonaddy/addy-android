package host.stjin.anonaddy_shared.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy_shared.models.*

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

    fun jsonToUserResourceObject(context: Context, json: String): UserResource? {
        val loggingHelper = LoggingHelper(context)

        return try {
            Gson().fromJson(
                json,
                object : TypeToken<UserResource?>() {}.type
            ) as UserResource

        } catch (e: Exception) {
            val ex = e.message
            println(ex)
            loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "jsonToUserResourceObject", null)
            null
        }
    }

    fun jsonToAliasSortFilterObject(context: Context, json: String): AliasSortFilter? {
        val loggingHelper = LoggingHelper(context)

        return try {
            Gson().fromJson(
                json,
                object : TypeToken<AliasSortFilter?>() {}.type
            ) as AliasSortFilter

        } catch (e: Exception) {
            val ex = e.message
            println(ex)
            loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "jsonToAliasSortFilterObject", null)
            null
        }
    }

    fun jsonToWearOSSettingsObject(context: Context, json: String): WearOSSettings? {
        val loggingHelper = LoggingHelper(context)

        return try {
            Gson().fromJson(
                json,
                object : TypeToken<WearOSSettings?>() {}.type
            ) as WearOSSettings

        } catch (e: Exception) {
            val ex = e.message
            println(ex)
            loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, ex.toString(), "jsonToWearOSSettingsObject", null)
            null
        }
    }
}