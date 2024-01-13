package host.stjin.anonaddy_shared.models

data class AliasSortFilter(
    var onlyActiveAliases: Boolean,
    var onlyDeletedAliases: Boolean,
    var onlyInactiveAliases: Boolean,
    var onlyWatchedAliases: Boolean,
    var sort: String?,
    var sortDesc: Boolean,
    var filter: String?
)