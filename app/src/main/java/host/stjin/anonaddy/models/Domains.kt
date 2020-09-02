package host.stjin.anonaddy.models

data class DomainsArray(
    val `data`: List<Domains>
)

data class SingleDomain(
    val `data`: Domains
)

data class Domains(
    val active: Boolean,
    val aliases: List<Aliases>?,
    val created_at: String,
    val default_recipient: Recipients?,
    val description: String?,
    val domain: String,
    val domain_sending_verified_at: String?,
    val domain_verified_at: String?,
    val id: String,
    val updated_at: String,
    val user_id: String
)

data class DomainOptions(
    val `data`: List<String>,
    val defaultAliasDomain: String?,
    val defaultAliasFormat: String?
)