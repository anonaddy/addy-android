package host.stjin.anonaddy_shared.utils

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date

object DateTimeUtils {

    private val SERVER_ZONE_ID = ZoneId.of("GMT")
    private val SERVER_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    private val SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("E d MMM")

    enum class DatetimeFormat {
        DATE,
        TIME,
        DATETIME,
        SHORT_DATE,
    }

    // This method takes the string as its stored in addy.io's database, and turns it into local format
    fun convertStringToLocalTimeZoneString(string: String?, dateTimeFormat: DatetimeFormat = DatetimeFormat.DATETIME): String? {
        if (string.isNullOrEmpty()) {
            return ""
        }
        return try {
            val ldt = turnStringIntoLocalDateTime(string) ?: return "$string (GMT)"
            val zonedDateTime: ZonedDateTime = ldt.atZone(SERVER_ZONE_ID)
            val defaultZoneId = ZoneId.systemDefault()

            val localTimeZoneDate: ZonedDateTime = zonedDateTime.withZoneSameInstant(defaultZoneId)

            when (dateTimeFormat) {
                DatetimeFormat.DATE -> localTimeZoneDate.format(DATE_FORMATTER)
                DatetimeFormat.TIME -> localTimeZoneDate.format(TIME_FORMATTER)
                DatetimeFormat.DATETIME -> localTimeZoneDate.format(DATE_TIME_FORMATTER)
                DatetimeFormat.SHORT_DATE -> localTimeZoneDate.format(SHORT_DATE_FORMATTER)
            }
        } catch (e: Exception) {
            "$string (GMT)"
        }
    }

    fun convertStringToLocalTimeZoneDate(string: String?): LocalDateTime? {
        if (string.isNullOrEmpty()) return null
        return try {
            val ldt = turnStringIntoLocalDateTime(string) ?: return null
            val zonedDateTime: ZonedDateTime = ldt.atZone(SERVER_ZONE_ID)
            val defaultZoneId = ZoneId.systemDefault()

            zonedDateTime.withZoneSameInstant(defaultZoneId).toLocalDateTime()
        } catch (e: Exception) {
            null
        }
    }

    fun convertDateToLocalTimeZoneDate(date: Date): LocalDateTime? {
        return try {
            date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        } catch (e: Exception) {
            null
        }
    }


    // This method takes the string as its stored in addy.io's database, and turns it into a datetime object
    private fun turnStringIntoLocalDateTime(string: String?): LocalDateTime? {
        if (string.isNullOrEmpty()) return null
        return try {
            LocalDateTime.parse(string, SERVER_DATE_TIME_FORMATTER)
        } catch (e: Exception) {
            null
        }
    }
}