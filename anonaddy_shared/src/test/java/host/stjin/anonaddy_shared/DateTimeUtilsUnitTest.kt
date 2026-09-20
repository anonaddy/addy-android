package host.stjin.anonaddy_shared

import host.stjin.anonaddy_shared.utils.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class DateTimeUtilsUnitTest {

    @Test
    fun convertStringToLocalTimeZoneString_emptyOrNull() {
        assertEquals("", DateTimeUtils.convertStringToLocalTimeZoneString(null))
        assertEquals("", DateTimeUtils.convertStringToLocalTimeZoneString(""))
    }

    @Test
    fun convertStringToLocalTimeZoneString_invalidFormatReturnsFallback() {
        val invalid = "not-a-date"
        assertEquals("not-a-date (GMT)", DateTimeUtils.convertStringToLocalTimeZoneString(invalid))
    }

    @Test
    fun convertStringToLocalTimeZoneString_validFormats() {
        val serverDate = "2026-09-20 12:00:00"

        val dateTimeStr = DateTimeUtils.convertStringToLocalTimeZoneString(serverDate, DateTimeUtils.DatetimeFormat.DATETIME)
        assertNotNull(dateTimeStr)
        assertTrue(dateTimeStr!!.isNotEmpty())

        val dateStr = DateTimeUtils.convertStringToLocalTimeZoneString(serverDate, DateTimeUtils.DatetimeFormat.DATE)
        assertNotNull(dateStr)
        assertTrue(dateStr!!.isNotEmpty())

        val timeStr = DateTimeUtils.convertStringToLocalTimeZoneString(serverDate, DateTimeUtils.DatetimeFormat.TIME)
        assertNotNull(timeStr)
        assertTrue(timeStr!!.isNotEmpty())

        val shortDateStr = DateTimeUtils.convertStringToLocalTimeZoneString(serverDate, DateTimeUtils.DatetimeFormat.SHORT_DATE)
        assertNotNull(shortDateStr)
        assertTrue(shortDateStr!!.isNotEmpty())
    }

    @Test
    fun convertStringToLocalTimeZoneDate_validAndInvalid() {
        assertNull(DateTimeUtils.convertStringToLocalTimeZoneDate(null))
        assertNull(DateTimeUtils.convertStringToLocalTimeZoneDate(""))
        assertNull(DateTimeUtils.convertStringToLocalTimeZoneDate("invalid"))

        val parsed = DateTimeUtils.convertStringToLocalTimeZoneDate("2026-09-20 12:00:00")
        assertNotNull(parsed)
    }

    @Test
    fun convertDateToLocalTimeZoneDate_valid() {
        val now = Date()
        val localDateTime = DateTimeUtils.convertDateToLocalTimeZoneDate(now)
        assertNotNull(localDateTime)
    }
}
