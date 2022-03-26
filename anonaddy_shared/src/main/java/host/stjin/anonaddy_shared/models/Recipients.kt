package host.stjin.anonaddy_shared.models

data class RecipientsArray(
    val `data`: List<Recipients>
)

data class SingleRecipient(
    val `data`: Recipients
)

data class Recipients(
    val aliases: List<Aliases>?,
    val created_at: String,
    val email: String,
    val email_verified_at: String?,
    val fingerprint: String?,
    val id: String,
    val should_encrypt: Boolean,
    val can_reply_send: Boolean,
    val updated_at: String,
    val user_id: String
)