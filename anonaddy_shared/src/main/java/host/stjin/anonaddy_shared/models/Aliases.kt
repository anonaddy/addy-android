package host.stjin.anonaddy_shared.models

data class AliasesArray(
    var `data`: ArrayList<Aliases>,
    var links: Links?,
    var meta: Meta?
)

data class BulkAliasesArray(
    var `data`: ArrayList<Aliases>,
)

data class SingleAlias(
    val `data`: Aliases
)

data class Aliases(
    val id: String,
    val user_id: String,
    val aliasable_id: String,
    val aliasable_type: String,
    val local_part: String,
    val extension: String,
    val domain: String,
    val email: String,
    var active: Boolean,
    val description: String?,
    val from_name: String?,
    val emails_forwarded: Int,
    val emails_blocked: Int,
    val emails_replied: Int,
    val emails_sent: Int,
    val recipients: List<Recipients>?,
    val last_forwarded: String?,
    val last_blocked: String?,
    val last_replied: String?,
    val last_sent: String?,
    val created_at: String,
    val updated_at: String,
    var deleted_at: String?
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
    val url: String?,
    val label: String,
    val active: Boolean
)

data class Links(
    val first: String?,
    val last: String?,
    val prev: String?,
    val next: String?
)