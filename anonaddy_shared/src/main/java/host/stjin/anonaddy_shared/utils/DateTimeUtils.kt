package host.stjin.anonaddy_shared.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

object DateTimeUtils {

    enum class DatetimeFormat {
        DATE,
        TIME,
        DATETIME,
        SHORT_DATE,
    }

    // This method takes the string as its stored in addy.io's database, and turns it into local format
    fun convertStringToLocalTimeZoneString(string: String?, dateTimeFormat: DatetimeFormat = DatetimeFormat.DATETIME): String? {
        if (string == null) {
            return ""
        } else {
            return try {
                val ldt = turnStringIntoLocalDateTime(string)
                val date: Date
                val serverZoneId = ZoneId.of("GMT")
                val zonedDateTime: ZonedDateTime = ldt!!.atZone(serverZoneId)
                val defaultZoneId = ZoneId.systemDefault()

                val localTimeZoneDate: ZonedDateTime = zonedDateTime.withZoneSameInstant(defaultZoneId)
                date = Date.from(localTimeZoneDate.toInstant())


                return when (dateTimeFormat) {
                    DatetimeFormat.DATE -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)
                    DatetimeFormat.TIME -> DateFormat.getTimeInstance(DateFormat.MEDIUM).format(date)
                    DatetimeFormat.DATETIME -> DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(date)
                    DatetimeFormat.SHORT_DATE -> SimpleDateFormat("E d MMM").format(date)
                }
            } catch (e: Exception) {
                "$string (GMT)"
            }
        }
    }

    fun convertStringToLocalTimeZoneDate(string: String?): LocalDateTime? {
        try {
            val ldt = turnStringIntoLocalDateTime(string)
            val serverZoneId = ZoneId.of("GMT")
            val zonedDateTime: ZonedDateTime = ldt!!.atZone(serverZoneId)
            val defaultZoneId = ZoneId.systemDefault()

            return zonedDateTime.withZoneSameInstant(defaultZoneId).toLocalDateTime()
        } catch (e: Exception) {
            return null
        }
    }

    // This method takes the string as its stored in addy.io's database, and turns it into a datetime object
    private fun turnStringIntoLocalDateTime(string: String?): LocalDateTime? {
        return LocalDateTime.parse(string, DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss"))
    }

}