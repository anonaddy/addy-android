package host.stjin.anonaddy_shared.models

data class WearOSSettings(
    var base_url: String?,
    var api_key: String?,
    val default_alias_domain: String?,
    val default_alias_format: String?,
)
