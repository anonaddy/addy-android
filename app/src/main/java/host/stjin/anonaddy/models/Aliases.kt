package host.stjin.anonaddy.models

data class AliasesArray(
    val `data`: List<Aliases>
)

data class SingleAlias(
    val `data`: Aliases
)

data class Aliases(
    val active: Boolean,
    val aliasable_id: String,
    val aliasable_type: String,
    val created_at: String,
    val deleted_at: String?,
    val description: String?,
    val domain: String,
    val email: String,
    val emails_blocked: Int,
    val emails_forwarded: Int,
    val emails_replied: Int,
    val emails_sent: Int,
    val extension: Any,
    val id: String,
    val local_part: String,
    val recipients: List<Recipients>?,
    val updated_at: String,
    val user_id: String
)