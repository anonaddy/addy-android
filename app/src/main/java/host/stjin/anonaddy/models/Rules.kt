package host.stjin.anonaddy.models

data class Action(
    val type: String,
    val value: String
)

data class Condition(
    val match: String,
    val type: String,
    val values: List<String>
)

data class SingleRule(
    val `data`: Rules
)

data class RulesArray(
    val `data`: List<Rules>
)

data class Rules(
    val actions: List<Action>,
    val active: Boolean,
    var conditions: List<Condition>,
    val created_at: String,
    val id: String,
    var name: String,
    val `operator`: String,
    val order: Int,
    val updated_at: String,
    val user_id: String
)