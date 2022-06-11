package host.stjin.anonaddy_shared.models

enum class SUBSCRIPTIONS(val subscription: String) {
    FREE("free"),
    LITE("lite"),
    PRO("pro")
}

data class SingleUserResource(
    val `data`: UserResource
)

data class UserResourceExtended(
    var default_recipient_email: String
)

data class UserResource(
    val id: String,
    val username: String,
    val from_name: String?,
    val email_subject: String?,
    val banner_location: String,
    val bandwidth: Int,
    val username_count: Int,
    val username_limit: Int,
    val default_recipient_id: String,
    val default_alias_domain: String,
    val default_alias_format: String,
    val subscription: String?,
    val subscription_ends_at: String?,
    val bandwidth_limit: Int,
    val recipient_count: Int,
    val recipient_limit: Int,
    val active_domain_count: Int,
    val active_domain_limit: Int,
    val active_shared_domain_alias_count: Int,
    val active_shared_domain_alias_limit: Int,
    val total_emails_forwarded: Int,
    val total_emails_blocked: Int,
    val total_emails_replied: Int,
    val total_emails_sent: Int,
    val created_at: String,
    val updated_at: String
)
