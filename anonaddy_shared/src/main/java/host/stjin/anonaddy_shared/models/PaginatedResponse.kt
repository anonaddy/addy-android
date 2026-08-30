package host.stjin.anonaddy_shared.models

data class PaginatedResponse<T>(
    var data: ArrayList<T>,
    var links: Links? = null,
    var meta: Meta? = null
)

@Suppress("PropertyName")
data class Meta(
    val current_page: Int,
    val from: Int?,
    val last_page: Int,
    val links: List<Link>,
    val path: String,
    val per_page: Int,
    val to: Int?,
    val total: Int
)

data class Link(
    val url: String?,
    val label: String,
    val active: Boolean
)

data class Links(
    val first: String?,
    val last: String?,
    val prev: String?,
    val next: String?
)

fun <T> PaginatedResponse<T>?.appendPage(newPage: PaginatedResponse<T>): PaginatedResponse<T> {
    if (this == null) return newPage
    val combinedList = ArrayList(this.data).apply { addAll(newPage.data) }
    return PaginatedResponse(
        data = combinedList,
        links = newPage.links,
        meta = newPage.meta
    )
}

fun <T> PaginatedResponse<T>?.nextPage(isLoadMore: Boolean): Int {
    return if (isLoadMore) (this?.meta?.current_page ?: 0) + 1 else 1
}


