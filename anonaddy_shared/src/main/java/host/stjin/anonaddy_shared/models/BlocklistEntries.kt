package host.stjin.anonaddy_shared.models

data class BlocklistEntriesArray(
    val `data`: List<BlocklistEntries>
)

data class SingleBlocklistEntry(
    val `data`: BlocklistEntries
)

data class BlocklistEntries(
    val created_at: String,
    val id: String,
    val type: String,
    val updated_at: String,
    val user_id: String,
    val value: String
)

data class NewBlocklistEntry(
    val type: String,
    val value: String
)