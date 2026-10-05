package host.stjin.anonaddy_shared.models

@Suppress("PropertyName")
data class FailedDeliveries(
    val id: String,
    val user_id: String,
    val recipient_id: String?,
    val recipient_email: String?,
    val alias_id: String?,
    val alias_email: String?,
    val alias_description: String? = null,
    val bounce_type: String,
    val remote_mta: String,
    val sender: String?,
    val destination: String?,
    val ir_dedupe_key: String?,
    val type: String,
    val email_type: String,
    val email_type_text: String,
    val status: String,
    val code: String,
    val is_stored: Boolean,
    val quarantined: Boolean,
    val resent: Boolean,
    val attempted_at: String,
    val created_at: String,
    val updated_at: String
)