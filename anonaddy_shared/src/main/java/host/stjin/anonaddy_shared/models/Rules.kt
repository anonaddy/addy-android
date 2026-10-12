package host.stjin.anonaddy_shared.models

import java.io.Serializable

data class WebhookActionValue(
    val url: String,
    val include_body: Boolean = false,
    val include_headers: Boolean = false
) : Serializable

data class Action(
    val type: String,
    val value: Any? = null
) : Serializable {
    fun getWebhookValue(): WebhookActionValue? {
        return when (val v = value) {
            is WebhookActionValue -> v
            is Map<*, *> -> {
                val url = v["url"] as? String ?: return null
                val includeBody = (v["include_body"] as? Boolean) ?: false
                val includeHeaders = (v["include_headers"] as? Boolean) ?: false
                WebhookActionValue(url, includeBody, includeHeaders)
            }
            else -> null
        }
    }

    val valueString: String?
        get() = when (val v = value) {
            is String -> v
            is WebhookActionValue -> v.url
            null -> null
            else -> v.toString()
        }
}


data class Condition(
    val type: String,
    val match: String? = null,
    val values: List<String>? = null
) : Serializable

data class SingleRule(
    val `data`: Rules
)


@Suppress("PropertyName")
data class Rules(
    val id: String,
    val user_id: String,
    var name: String,
    val order: Int,
    val conditions: ArrayList<Condition>,
    val actions: ArrayList<Action>,
    var `operator`: String,
    var forwards: Boolean,
    var replies: Boolean,
    var sends: Boolean,
    val active: Boolean,
    val applied: Int,
    val last_applied: String?,
    val created_at: String,
    val updated_at: String
)


