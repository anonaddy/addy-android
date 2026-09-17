package host.stjin.anonaddy_shared.models

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

@JsonAdapter(BulkActionResponseDeserializer::class)
data class BulkActionResponse(
    val ids: List<String>,
    val message: String
)

class BulkActionResponseDeserializer : JsonDeserializer<BulkActionResponse> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BulkActionResponse {
        val jsonObject = json.asJsonObject
        val targetObj = if (jsonObject.has("data") && jsonObject.get("data").isJsonObject) {
            jsonObject.getAsJsonObject("data")
        } else {
            jsonObject
        }
        val message = if (targetObj.has("message") && !targetObj.get("message").isJsonNull) {
            targetObj.get("message").asString
        } else {
            ""
        }
        val ids = mutableListOf<String>()
        if (targetObj.has("ids") && targetObj.get("ids").isJsonArray) {
            targetObj.getAsJsonArray("ids").forEach { element ->
                if (!element.isJsonNull) {
                    ids.add(element.asString)
                }
            }
        }
        return BulkActionResponse(ids = ids, message = message)
    }
}