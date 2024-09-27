package host.stjin.anonaddy_shared.models

// Login data class representing the successful login response
data class Login(
    val api_key: String,
    val name: String,
    val created_at: String,
    val expires_at: String?
)

// LoginMfaRequired data class for when MFA is required
data class LoginMfaRequired(
    val message: String,
    val mfa_key: String,
    val csrf_token: String
)