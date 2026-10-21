package host.stjin.anonaddy_shared.models

data class ServiceStatus(
    val title: String? = null,
    val timezone: String? = null,
    val pinnedUpdate: StatusUpdate? = null,
    val monitors: Map<String, List<StatusMonitor>>? = null,
    val updatesPerDay: Map<String, List<StatusUpdate>>? = null,
    val summarizedStatus: String? = null
) {
    val isOperational: Boolean
        get() = summarizedStatus.equals("up", ignoreCase = true)

    val latestUpdate: StatusUpdate?
        get() {
            if (pinnedUpdate != null) return pinnedUpdate
            return updatesPerDay?.values?.flatten()?.maxByOrNull { it.time ?: 0L }
        }

    val webApplicationStatus: String?
        get() {
            monitors?.values?.forEach { list ->
                list.forEach { monitor ->
                    if (monitor.label?.contains("web app", ignoreCase = true) == true ||
                        monitor.label?.contains("application", ignoreCase = true) == true ||
                        monitor.url?.contains("app.addy.io", ignoreCase = true) == true
                    ) {
                        return monitor.status
                    }
                }
            }
            return null
        }
}

data class StatusUpdate(
    val id: Int? = null,
    val title: String? = null,
    val text: String? = null,
    val pinned: Boolean? = null,
    val severity: String? = null,
    val time: Long? = null
)

data class StatusMonitor(
    val label: String? = null,
    val url: String? = null,
    val status: String? = null
)
