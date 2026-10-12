package host.stjin.anonaddy_shared.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy_shared.models.Action
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.models.WearOSSettings
import host.stjin.anonaddy_shared.models.WebhookActionValue
import java.lang.reflect.Type

inline fun <reified T> Gson.fromJson(json: String): T {
    return fromJson(json, object : TypeToken<T>() {}.type)
}

class ActionAdapter : JsonDeserializer<Action>, JsonSerializer<Action> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Action {
        val obj = json.asJsonObject
        val type = obj.get("type")?.asString ?: ""
        val valueElement = obj.get("value")
        val value: Any? = when {
            valueElement == null || valueElement.isJsonNull -> null
            type == "webhook" -> {
                if (valueElement.isJsonObject) {
                    context.deserialize<WebhookActionValue>(valueElement, WebhookActionValue::class.java)
                } else if (valueElement.isJsonPrimitive && valueElement.asJsonPrimitive.isString) {
                    val str = valueElement.asString
                    try {
                        context.deserialize<WebhookActionValue>(JsonParser.parseString(str), WebhookActionValue::class.java)
                    } catch (e: Exception) {
                        WebhookActionValue(url = str, include_body = false, include_headers = false)
                    }
                } else null
            }
            valueElement.isJsonPrimitive && valueElement.asJsonPrimitive.isString -> valueElement.asString
            valueElement.isJsonPrimitive && valueElement.asJsonPrimitive.isBoolean -> valueElement.asBoolean
            valueElement.isJsonPrimitive && valueElement.asJsonPrimitive.isNumber -> valueElement.asNumber
            else -> context.deserialize(valueElement, Any::class.java)
        }
        return Action(type = type, value = value)
    }

    override fun serialize(src: Action, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()
        obj.addProperty("type", src.type)
        when (val v = src.value) {
            null -> {
                // omit value
            }
            is WebhookActionValue -> obj.add("value", context.serialize(v))
            is String -> obj.addProperty("value", v)
            is Boolean -> obj.addProperty("value", v)
            is Number -> obj.addProperty("value", v)
            else -> obj.add("value", context.serialize(v))
        }
        return obj
    }
}

object GsonTools {
    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Action::class.java, ActionAdapter())
        .create()

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