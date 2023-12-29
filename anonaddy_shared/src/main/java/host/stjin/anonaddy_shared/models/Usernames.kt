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
    val from_name: String?,
    var aliases: List<Aliases>?, // TODO turn back to val when below is done
    var aliases_count: Int?, // TODO NEEDS TO BE APPROVED BY ADDY
    val default_recipient: Recipients?,
    var active: Boolean,
    var catch_all: Boolean,
    var can_login: Boolean,
    val created_at: String,
    val updated_at: String
)