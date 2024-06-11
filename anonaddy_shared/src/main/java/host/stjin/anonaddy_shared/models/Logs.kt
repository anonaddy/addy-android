package host.stjin.anonaddy_shared.models

data class Logs(

    /*
    importance
    0 = Critical (red)
    1 = Warning (yellow)
    2 = Info (green)
     */

    val importance: Int = LOGIMPORTANCE.INFO.int,
    val dateTime: String,
    val method: String?,
    val message: String?,
    val extra: String?,

    )

enum class LOGIMPORTANCE(val int: Int) {
    CRITICAL(0),
    WARNING(1),
    INFO(2)
}