package host.stjin.anonaddy.models

import java.io.Serializable

data class Action(
    val type: String,
    val value: String
) : Serializable

data class Condition(
    val match: String,
    val type: String,
    val values: List<String>
) : Serializable

data class SingleRule(
    val `data`: Rules
)

data class RulesArray(
    val `data`: List<Rules>
)

data class Rules(
    val actions: ArrayList<Action>,
    val active: Boolean,
    val conditions: ArrayList<Condition>,
    val created_at: String,
    val id: String,
    var name: String,
    var `operator`: String,
    val order: Int,
    val updated_at: String,
    val user_id: String
)