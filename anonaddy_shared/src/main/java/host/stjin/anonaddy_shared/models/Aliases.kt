package host.stjin.anonaddy_shared.models

data class AliasesArray(
    var `data`: ArrayList<Aliases>,
    var links: Links,
    var meta: Meta
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
    val extension: String,
    val id: String,
    val local_part: String,
    val recipients: List<Recipients>?,
    val updated_at: String,
    val user_id: String
)

data class Meta(
    val current_page: Int,
    val from: Int,
    val last_page: Int,
    val links: List<Link>,
    val path: String,
    val per_page: Int,
    val to: Int,
    val total: Int
)

data class Link(
    val active: Boolean,
    val label: String,
    val url: Any
)

data class Links(
    val first: String,
    val last: String,
    val next: Any,
    val prev: Any
)