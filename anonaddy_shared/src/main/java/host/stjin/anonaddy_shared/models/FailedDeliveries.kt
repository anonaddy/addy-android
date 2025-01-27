package host.stjin.anonaddy_shared.models


data class FailedDeliveriesArray(
    val `data`: List<FailedDeliveries>
)

data class SingleFailedDelivery(
    val `data`: FailedDeliveries
)

data class FailedDeliveries(
    val id: String,
    val user_id: String,
    val recipient_id: String?,
    val recipient_email: String?,
    val alias_id: String?,
    val alias_email: String?,
    val bounce_type: String,
    val remote_mta: String,
    val sender: String?,
    val email_type: String,
    val status: String,
    val code: String,
    val is_stored: Boolean,
    val attempted_at: String,
    val created_at: String,
    val updated_at: String
)