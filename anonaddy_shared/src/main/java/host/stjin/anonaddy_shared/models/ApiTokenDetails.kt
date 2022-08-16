package host.stjin.anonaddy_shared.models

data class ApiTokenDetails(
    val created_at: String,
    val expires_at: String?,
    val name: String
)