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
    val aliases: List<Aliases>?,
    val default_recipient: Recipients?,
    val active: Boolean,
    val catch_all: Boolean, // Introduced in v0.5.0
    val test: Boolean, // Introduced in v0.5.0
    val domain_verified_at: String?,
    val domain_sending_verified_at: String?,
    val created_at: String,
    val updated_at: String,
)

data class DomainOptions(
    val `data`: List<String>,
    val defaultAliasDomain: String?,
    val defaultAliasFormat: String?
)