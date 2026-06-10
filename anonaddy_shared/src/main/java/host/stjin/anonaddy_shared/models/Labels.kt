package host.stjin.anonaddy_shared.models

import java.io.Serializable

data class SingleLabel(
    val `data`: Labels
)

data class LabelsArray(
    val `data`: List<Labels>
)

data class Labels(
    val id: String,
    val user_id: String,
    var name: String,
    var colour: String,
    val aliases_count: Int?,
    val created_at: String,
    val updated_at: String
) : Serializable
