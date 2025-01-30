package host.stjin.anonaddy_shared.models


@Suppress("PropertyName", "PropertyName")
// Login data class representing the successful login response
data class Login(
    val api_key: String,
    val name: String,
    val created_at: String,
    val expires_at: String?
)

@Suppress("PropertyName", "PropertyName")
// LoginMfaRequired data class for when MFA is required
data class LoginMfaRequired(
    val message: String,
    val mfa_key: String,
    val csrf_token: String,
    var cookie: Collection<String> // This is not part of the return body, this is just because we need to send the cookie manually in Fuel (https://github.com/kittinunf/fuel/issues/263)
)