package host.stjin.anonaddy_shared.models

data class AccountNotificationsArray(
    val `data`: List<AccountNotifications>
)

data class AccountNotifications(
    val category: String,
    val created_at: String,
    val id: String,
    val link: String,
    val link_text: String,
    val text: String,
    val title: String
)