package host.stjin.anonaddy

import org.ocpsoft.prettytime.PrettyTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object DateTimeUtils {
    fun turnStringIntoLocalPrettyString(string: String): String? {
        return try {
            val ldt =
                LocalDateTime.parse(string, DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss"))
            var date = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())
            val singaporeZoneId = ZoneId.of("GMT")
            val asiaZonedDateTime: ZonedDateTime = ldt.atZone(singaporeZoneId)
            val newYokZoneId = ZoneId.systemDefault()

            val nyDateTime: ZonedDateTime = asiaZonedDateTime.withZoneSameInstant(newYokZoneId)
            date = Date.from(nyDateTime.toInstant())

            val prettyTime = PrettyTime()
            prettyTime.format(date)
        } catch (e: Exception) {
            "$string (GMT)"
        }
    }
}