package host.stjin.anonaddy_shared.models

data class BlocklistEntriesArray(
    val `data`: List<BlocklistEntries>
)

data class SingleBlocklistEntry(
    val `data`: BlocklistEntries
)

data class BlocklistEntries(
    val id: String,
    val user_id: String,
    val value: String,
    val type: String,
    val blocked: Int?,
    val last_blocked: String?,
    val created_at: String,
    val updated_at: String
)

data class NewBlocklistEntry(
    val type: String,
    val value: String
)