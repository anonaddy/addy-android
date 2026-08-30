package host.stjin.anonaddy_shared.models



data class SingleDomain(
    val `data`: Domains
)

@Suppress("PropertyName")
data class Domains(
    val id: String,
    val user_id: String,
    val domain: String,
    val description: String?,
    val from_name: String?,
    var aliases_count: Int?,
    var family_aliases_count: Int? = 0,
    val default_recipient: Recipients?,
    var active: Boolean,
    var catch_all: Boolean,
    var shared_with_family: Boolean = false,
    var auto_create_regex: String?,
    val domain_verified_at: String?,
    val domain_mx_validated_at: String?,
    val domain_sending_verified_at: String?,
    val created_at: String,
    val updated_at: String
)

data class DomainOptions(
    val `data`: List<String>,
    val defaultAliasDomain: String,
    val defaultAliasFormat: String,
    val sharedDomains: List<String>
)