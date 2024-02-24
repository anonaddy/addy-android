package host.stjin.anonaddy_shared.models

data class RecipientsArray(
    val `data`: List<Recipients>
)

data class SingleRecipient(
    val `data`: Recipients
)

data class Recipients(
    val id: String,
    val user_id: String,
    val email: String,
    var can_reply_send: Boolean,
    var should_encrypt: Boolean,
    var inline_encryption: Boolean,
    var protected_headers: Boolean,
    var fingerprint: String?,
    val email_verified_at: String?,
    var aliases_count: Int?,
    val created_at: String,
    val updated_at: String
)