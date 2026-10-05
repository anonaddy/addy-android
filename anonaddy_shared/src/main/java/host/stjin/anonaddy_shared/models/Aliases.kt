package host.stjin.anonaddy_shared.models



data class SingleAlias(
    val `data`: Aliases
)


@Suppress("PropertyName")
data class Aliases(
    val id: String,
    val user_id: String,
    val aliasable_id: String?,
    val aliasable_type: String?,
    val local_part: String,
    val extension: String?,
    val domain: String,
    val email: String,
    var active: Boolean,
    var pinned: Boolean,
    val description: String?,
    val from_name: String?,
    var attached_recipients_only: Boolean,
    val emails_forwarded: Int,
    val emails_blocked: Int,
    val emails_replied: Int,
    val emails_sent: Int,
    val recipients: List<Recipients>?,
    val labels: List<Labels>?,
    val last_forwarded: String?,
    val last_blocked: String?,
    val last_replied: String?,
    val last_sent: String?,
    val created_at: String,
    val updated_at: String,
    var deleted_at: String?
)
