package host.stjin.anonaddy_shared.utils

import java.text.DateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    private val SERVER_ZONE_ID = ZoneId.of("GMT")
    private val SERVER_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("E d MMM", Locale.getDefault())

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
            val date = Date.from(localTimeZoneDate.toInstant())

            when (dateTimeFormat) {
                DatetimeFormat.DATE -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)
                DatetimeFormat.TIME -> DateFormat.getTimeInstance(DateFormat.MEDIUM).format(date)
                DatetimeFormat.DATETIME -> DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(date)
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