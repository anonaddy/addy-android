package host.stjin.anonaddy

import host.stjin.anonaddy.service.BackupHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class BackupHelperTest {

    @Test
    fun testGetFileTimestamp_validBackupFileName() {
        val fileName = "BACKUP_2026-10-27 14:30:00.anon"
        val expectedEpochMillis = LocalDateTime.parse("2026-10-27 14:30:00", BackupHelper.DATE_TIME_FORMATTER)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val actualEpochMillis = BackupHelper.getFileTimestamp(fileName, lastModified = 0L)
        assertEquals(expectedEpochMillis, actualEpochMillis)
    }

    @Test
    fun testGetFileTimestamp_invalidDateInFileNameFallsBackToLastModified() {
        val fileName = "BACKUP_invalid-date.anon"
        val fallbackLastModified = 1730000000000L

        val actualEpochMillis = BackupHelper.getFileTimestamp(fileName, lastModified = fallbackLastModified)
        assertEquals(fallbackLastModified, actualEpochMillis)
    }

    @Test
    fun testGetFileTimestamp_nonBackupExtensionFallsBackToLastModified() {
        val fileName = "BACKUP_2026-10-27 14:30:00.txt"
        val fallbackLastModified = 1730000000000L

        val actualEpochMillis = BackupHelper.getFileTimestamp(fileName, lastModified = fallbackLastModified)
        assertEquals(fallbackLastModified, actualEpochMillis)
    }

    @Test
    fun testGetFileTimestamp_nullFileNameFallsBackToLastModified() {
        val fallbackLastModified = 1730000000000L

        val actualEpochMillis = BackupHelper.getFileTimestamp(null, lastModified = fallbackLastModified)
        assertEquals(fallbackLastModified, actualEpochMillis)
    }

    @Test
    fun testGetFileTimestamp_duplicateSAFFileNameFallsBackToLastModified() {
        val fileName = "BACKUP_2026-10-27 14:30:00 (1).anon"
        val fallbackLastModified = 1730000000000L

        val actualEpochMillis = BackupHelper.getFileTimestamp(fileName, lastModified = fallbackLastModified)
        assertEquals(fallbackLastModified, actualEpochMillis)
    }
}

