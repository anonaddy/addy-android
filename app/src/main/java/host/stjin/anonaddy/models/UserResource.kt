package host.stjin.anonaddy.models

object User {
    lateinit var userResource: UserResource
}

data class SingleUserResource(
    val `data`: UserResource
)

data class UserResource(
    val active_domain_count: Int,
    val active_domain_limit: Int,
    val active_shared_domain_alias_count: Int,
    val active_shared_domain_alias_limit: Int,
    val bandwidth: Int,
    val bandwidth_limit: Int,
    val banner_location: String,
    val created_at: String,
    val default_alias_domain: String?,
    val default_alias_format: String?,
    val default_recipient_id: String,
    val email_subject: String?,
    val from_name: String?,
    val id: String,
    val recipient_count: Int,
    val recipient_limit: Int,
    val subscription: String,
    val updated_at: String,
    val username: String,
    val username_count: Int,
    val username_limit: Int
)