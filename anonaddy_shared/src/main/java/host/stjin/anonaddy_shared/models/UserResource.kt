package host.stjin.anonaddy_shared.models

enum class SUBSCRIPTIONS(val subscription: String) {
    FREE("free"),
    LITE("lite"),
    PRO("pro")
}

data class SingleUserResource(
    val `data`: UserResource
)

@Suppress("PropertyName", "PropertyName")
data class UserResourceExtended(
    var default_recipient_email: String
)

@Suppress("PropertyName", "PropertyName")
data class UserResource(
    val id: String,
    val username: String,
    val disabled: Boolean?,
    val from_name: String?,
    val email_subject: String?,
    val banner_location: String,
    val bandwidth: Long,
    val username_count: Int,
    val username_limit: Int,
    val default_username_id: String,
    val default_recipient_id: String,
    val default_alias_domain: String,
    val default_alias_format: String,
    val subscription: String?, // Can be null on selfhosted
    val subscription_type: String?, // Can be null on selfhosted
    val subscription_ends_at: String?, // Can be null on selfhosted
    val bandwidth_limit: Long,
    val recipient_count: Int,
    val recipient_limit: Int?, // Can be null on selfhosted
    val active_domain_count: Int,
    val active_domain_limit: Int?, // Can be null on selfhosted
    val active_shared_domain_alias_count: Int,
    val active_shared_domain_alias_limit: Int?, // Can be null on selfhosted
    val active_rule_count: Int,
    val active_rule_limit: Int?, // Can be null on selfhosted
    val total_emails_forwarded: Int,
    val total_emails_blocked: Int,
    val total_emails_replied: Int,
    val total_emails_sent: Int,
    val total_aliases: Int,
    val total_active_aliases: Int,
    val total_inactive_aliases: Int,
    val total_deleted_aliases: Int,
    val created_at: String,
    val updated_at: String
) {
    val hasUserFreeSubscription: Boolean
        get() = subscription == SUBSCRIPTIONS.FREE.subscription
}
