package host.stjin.anonaddy_shared.models

data class DomainsArray(
    val `data`: List<Domains>
)

data class SingleDomain(
    val `data`: Domains
)

data class Domains(
    val id: String,
    val user_id: String,
    val domain: String,
    val description: String?,
    val from_name: String?,
    var aliases_count: Int?,
    val default_recipient: Recipients?,
    var active: Boolean,
    var catch_all: Boolean,
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