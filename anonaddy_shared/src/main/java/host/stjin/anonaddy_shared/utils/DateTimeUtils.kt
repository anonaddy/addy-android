package host.stjin.anonaddy_shared.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object DateTimeUtils {

    enum class DATETIMEUTILS {
        DATE,
        TIME,
        DATETIME,
        SHORT_DATE,
    }

    // This method takes the string as its stored in Anonaddy's database, and turns it into local format
    fun turnStringIntoLocalString(string: String?, dateTimeFormat: DATETIMEUTILS = DATETIMEUTILS.DATETIME): String? {
        if (string == null) {
            return ""
        } else {
            return try {
                val ldt =
                    LocalDateTime.parse(string, DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss"))
                val date: Date
                val serverZoneId = ZoneId.of("GMT")
                val zonedDateTime: ZonedDateTime = ldt.atZone(serverZoneId)
                val defaultZoneId = ZoneId.systemDefault()

                val nyDateTime: ZonedDateTime = zonedDateTime.withZoneSameInstant(defaultZoneId)
                date = Date.from(nyDateTime.toInstant())


                return when (dateTimeFormat) {
                    DATETIMEUTILS.DATE -> DateFormat.getDateInstance(DateFormat.SHORT).format(date)
                    DATETIMEUTILS.TIME -> DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
                    DATETIMEUTILS.DATETIME -> DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date)
                    DATETIMEUTILS.SHORT_DATE -> SimpleDateFormat("E d MMM").format(date)
                }
            } catch (e: Exception) {
                "$string (GMT)"
            }
        }
    }

}