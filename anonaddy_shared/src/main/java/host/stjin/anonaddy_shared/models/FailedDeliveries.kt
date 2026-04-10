package host.stjin.anonaddy_shared.models


data class FailedDeliveriesArray(
    var `data`: ArrayList<FailedDeliveries>,
    var links: Links?,
    var meta: Meta?
)

data class SingleFailedDelivery(
    val `data`: FailedDeliveries
)

@Suppress("PropertyName", "PropertyName")
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
    val destination: String?,
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