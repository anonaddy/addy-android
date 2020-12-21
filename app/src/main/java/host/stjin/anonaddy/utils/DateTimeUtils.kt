package host.stjin.anonaddy.utils

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object DateTimeUtils {

    // This method takes the string as its stored in Anonaddy's database, and turns it into local format
    fun turnStringIntoLocalString(string: String?): String? {
        if (string == null) {
            return ""
        } else {
            return try {
                val ldt =
                    LocalDateTime.parse(string, DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss"))
                val date: Date
                val serverZoneId = ZoneId.of("GMT")
                val asiaZonedDateTime: ZonedDateTime = ldt.atZone(serverZoneId)
                val defaultZoneId = ZoneId.systemDefault()

                val nyDateTime: ZonedDateTime = asiaZonedDateTime.withZoneSameInstant(defaultZoneId)
                date = Date.from(nyDateTime.toInstant())
                return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
            } catch (e: Exception) {
                "$string (GMT)"
            }
        }
    }
}