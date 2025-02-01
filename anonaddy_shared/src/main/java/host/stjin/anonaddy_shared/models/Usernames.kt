package host.stjin.anonaddy_shared.models

data class UsernamesArray(
    val `data`: List<Usernames>
)

data class SingleUsername(
    val `data`: Usernames
)

@Suppress("PropertyName", "PropertyName")
data class Usernames(
    val id: String,
    val user_id: String,
    val username: String,
    val description: String?,
    val from_name: String?,
    var aliases_count: Int?,
    val default_recipient: Recipients?,
    var active: Boolean,
    var catch_all: Boolean,
    var auto_create_regex: String?,
    var can_login: Boolean,
    val created_at: String,
    val updated_at: String
)