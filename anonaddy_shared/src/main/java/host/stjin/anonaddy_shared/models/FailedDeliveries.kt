package host.stjin.anonaddy_shared.models


data class FailedDeliveriesArray(
    val `data`: List<FailedDeliveries>
)

data class SingleFailedDelivery(
    val `data`: FailedDeliveries
)

data class FailedDeliveries(
    val alias_id: String,
    val alias_email: String?,
    val attempted_at: String,
    val bounce_type: String,
    val code: String,
    val created_at: String,
    val email_type: String,
    val id: String,
    val recipient_id: String?,
    val recipient_email: String?,
    val remote_mta: String,
    val sender: String,
    val status: String,
    val updated_at: String,
    val user_id: String
)