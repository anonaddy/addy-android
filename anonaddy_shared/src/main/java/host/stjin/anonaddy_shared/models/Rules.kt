package host.stjin.anonaddy_shared.models

import java.io.Serializable

data class Action(
    val type: String,
    val value: String
) : Serializable

data class Condition(
    val type: String,
    val match: String,
    val values: List<String>
) : Serializable

data class SingleRule(
    val `data`: Rules
)

data class RulesArray(
    val `data`: List<Rules>
)

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
    val last_applied: String,
    val created_at: String,
    val updated_at: String
)


