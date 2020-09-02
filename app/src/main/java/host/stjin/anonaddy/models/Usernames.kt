package host.stjin.anonaddy.models

data class UsernamesArray(
    val `data`: List<Usernames>
)

data class SingleUsername(
    val `data`: Usernames
)

data class Usernames(
    val active: Boolean,
    val aliases: List<Aliases>?,
    val created_at: String,
    val default_recipient: Recipients?,
    val description: String?,
    val id: String,
    val updated_at: String,
    val user_id: String,
    val username: String
)