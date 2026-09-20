package host.stjin.anonaddy_shared.models


@Suppress("PropertyName")
// Login data class representing the successful login response
data class Login(
    val api_key: String,
    val name: String,
    val created_at: String,
    val expires_at: String?
)

@Suppress("PropertyName")
// LoginMfaRequired data class for when MFA is required
data class LoginMfaRequired(
    val message: String,
    val mfa_key: String,
    var cookie: Collection<String> // Cookies to attach to the subsequent MFA request
)