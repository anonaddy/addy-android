package host.stjin.anonaddy_shared.models

data class UsernamesArray(
    val `data`: List<Usernames>
)

data class SingleUsername(
    val `data`: Usernames
)

data class Usernames(
    val id: String,
    val user_id: String,
    val username: String,
    val description: String?,
    val aliases: List<Aliases>?,
    val default_recipient: Recipients?,
    var active: Boolean,
    var catch_all: Boolean,
    val created_at: String,
    val updated_at: String
)