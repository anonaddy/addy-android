package host.stjin.anonaddy.service

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.LoggingHelper
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec
import kotlin.system.measureTimeMillis


// isAppInForeground is being used to determine if a notification or a snackbar should be used
class BackupHelper(private val context: Context) {

    private val loggingHelper: LoggingHelper = LoggingHelper(context, LoggingHelper.LOGFILES.BACKUP_LOGS)
    private val settingsManager: SettingsManager = ServiceLocator.settingsManager
    private val encryptedSettingsManager: SettingsManager = ServiceLocator.encryptedSettingsManager


    fun getLatestBackupDate(): Long? {
        val backupDestinationPath = settingsManager.getSettingsString(SettingsManager.PREFS.BACKUPS_LOCATION)
            ?: return null
        try {
            val f = DocumentFile.fromTreeUri(context, backupDestinationPath.toUri())?.listFiles()
                ?.filter { (it.name?.substringAfterLast(".") ?: "") == "anon" }
            val sortedList = f?.sortedWith(compareBy { it.lastModified() })
            return sortedList?.lastOrNull()?.lastModified()
        } catch (e: Exception) {
            loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, e.toString(), "getLatestBackupDate", null)
        }
        return null
    }

    fun deleteBackupsOlderThanXDays(retentionPeriod: Long = 30): Boolean {
        val backupDestinationPath = settingsManager.getSettingsString(SettingsManager.PREFS.BACKUPS_LOCATION)
            ?: return false
        try {
            val f = DocumentFile.fromTreeUri(context, backupDestinationPath.toUri())?.listFiles()
                ?.filter { (it.name?.substringAfterLast(".") ?: "") == "anon" }
            var filesDeleted = 0
            for (file in f ?: emptyList()) {
                val date: LocalDate = Instant.ofEpochMilli(file.lastModified()).atZone(ZoneId.systemDefault()).toLocalDate()
                val today: LocalDate = LocalDate.now()

                if (date.isBefore(today.minusDays(retentionPeriod))) {
                    // The backup is *older* than retentionPeriod days. Delete it
                    file.delete()
                    filesDeleted++
                }
            }

            // Let the caller know the task is finished
            if (filesDeleted > 0) {
                loggingHelper.addLog(
                    LOGIMPORTANCE.WARNING.int,
                    context.resources.getQuantityString(R.plurals.log_backup_retention_deleted, filesDeleted, filesDeleted, retentionPeriod),
                    "deleteBackupsOlderThanXDays",
                    null
                )
            }
            return true
        } catch (e: Exception) {
            loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, e.toString(), "deleteBackupsOlderThanXDays", null)
        }


        return false
    }


    fun isBackupLocationAccessible(): Boolean {
        val backupDestinationPath = settingsManager.getSettingsString(SettingsManager.PREFS.BACKUPS_LOCATION)
            ?: return false
        try {
            val f = DocumentFile.fromTreeUri(context, backupDestinationPath.toUri())
            return f?.canRead() == true && f.canWrite()
        } catch (e: Exception) {
            loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, e.toString(), "isBackupLocationAccessible", null)
        }
        return false
    }

    private fun createEmptyFileAndGetOutputStream(path: String, name: String, password: String): OutputStream? {
        try {
            val f = DocumentFile.fromTreeUri(context, path.toUri())
            val uriOfFile = f?.createFile("application/octet-stream", name)?.uri

            val cipher = makeCipher(password.toCharArray(), encryptMode = true)
            return CipherOutputStream(uriOfFile?.let { context.contentResolver.openOutputStream(it) }, cipher)
        } catch (e: Exception) {
            loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, e.toString(), "createEmptyFileAndGetOutputStream", null)
        }
        return null
    }


    private fun makeCipher(pass: CharArray, encryptMode: Boolean): Cipher? {
        // Use a KeyFactory to derive the corresponding key from the passphrase:
        val keySpec = PBEKeySpec(pass)
        val keyFactory = SecretKeyFactory.getInstance("PBEWITHSHA256AND128BITAES-CBC-BC")
        val key = keyFactory.generateSecret(keySpec)

        // Create parameters from the salt and an arbitrary number of iterations:
        val pbeParamSpec = PBEParameterSpec("anonaddy".toByteArray(), 43)

        // Set up the cipher:
        val cipher = Cipher.getInstance("PBEWITHSHA256AND128BITAES-CBC-BC")

        // Set the cipher mode to decryption or encryption:
        if (encryptMode) {
            cipher.init(Cipher.ENCRYPT_MODE, key, pbeParamSpec)
        } else {
            cipher.init(Cipher.DECRYPT_MODE, key, pbeParamSpec)
        }
        return cipher
    }


    private fun getInputStream(uri: Uri, password: String = "anonaddy"): InputStream? {
        try {
            val cipher = makeCipher(password.toCharArray(), encryptMode = false)
            return CipherInputStream(uri.let { context.contentResolver.openInputStream(it) }, cipher)
        } catch (e: Exception) {
            loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, e.toString(), "getInputStream", null)
        }
        return null
    }

    fun createBackup(): Boolean {
        var backupCompleted = false
        val timeElapsed = measureTimeMillis {
            try {
                val backupDestinationPath = settingsManager.getSettingsString(SettingsManager.PREFS.BACKUPS_LOCATION)
                val backupDestinationStream = backupDestinationPath?.let {
                    // Default back to "anonaddy" as password
                    createEmptyFileAndGetOutputStream(
                        it,
                        "BACKUP_${getDateTime()}.anon",
                        encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.BACKUPS_PASSWORD) ?: "anonaddy"
                    )
                }

                backupDestinationStream?.use {
                    if (saveSharedPreferences(it, arrayListOf(settingsManager.prefs, encryptedSettingsManager.prefs))
                    ) {
                        // Backup for both sharedprefs succeeded!
                        // .use closes the stream :)
                        // Let the caller know the backup succeeded
                        backupCompleted = true
                    }
                }
            } catch (e: Exception) {
                loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, e.toString(), "createBackup", null)
            }
        }
        if (backupCompleted) {
            val seconds = TimeUnit.MILLISECONDS.toSeconds(timeElapsed).toInt()
            loggingHelper.addLog(
                LOGIMPORTANCE.INFO.int,
                context.resources.getQuantityString(R.plurals.log_backup_completed, seconds, seconds),
                "createBackup",
                null
            )
        }
        return backupCompleted
    }

    fun restoreBackup(uri: Uri, password: String): Boolean {
        try {
            val restoreDestinationStream = getInputStream(uri, password)

            restoreDestinationStream?.use {
                if (loadSharedPreferences(it)
                ) {
                    // Restore succeeded!
                    // .use closes the stream :)
                    // Let the caller know the restore succeeded
                    return true
                }
            }
        } catch (e: Exception) {
            loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, e.toString(), "restoreBackup", null)
        }

        // Let the caller know the backup failed, user should refer to the backup log to see what went wrong
        return false
    }


    // The sharedPreferences arraylist is there so that in a possible future there may
    // be the option not to export all sharedpreferences (think only the encrypted preferences)
    private fun saveSharedPreferences(outputStream: OutputStream, sharedPreferences: ArrayList<SharedPreferences>): Boolean {
        PrintWriter(outputStream).use { pw ->
            for (prefs in sharedPreferences) {
                try {
                    val prefsMap: Map<*, *> = prefs.all
                    for ((key, value) in prefsMap) {
                        // Skip keys that contain "cache_"
                        if (!key.toString().lowercase().contains("cache_")) {
                            pw.println("$key|||$value")
                        }
                    }
                } catch (e: Exception) {
                    loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, e.toString(), "saveSharedPreferences", null)
                    return false
                }
            }
        }
        return true
    }

    // Loading sharedpreferences auto matches settings that are encrypted or not
    // and places them in the correct files accordingly
    private fun loadSharedPreferences(inputStream: InputStream): Boolean {
        try {
            inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split("|||", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0]
                        val value = parts[1]

                        // Loop through all the settings available in the app and check for matches
                        for (enum in SettingsManager.PREFS.entries) {
                            // If a key from the sharedpreference file matches a key from the app
                            if (enum.key == key) {
                                // Check if settings is encrypted and place it in the right file accordingly
                                val settingsManagerToWriteTo = if (enum.encrypted) encryptedSettingsManager else settingsManager
                                when (enum.type) {
                                    SettingsManager.PREFTYPES.STRING -> settingsManagerToWriteTo.putSettingsString(enum, value)
                                    SettingsManager.PREFTYPES.INT -> value.toIntOrNull()?.let { settingsManagerToWriteTo.putSettingsInt(enum, it) }
                                    SettingsManager.PREFTYPES.STRINGSET -> settingsManagerToWriteTo.putStringSet(enum, getStringSetFromString(value))
                                    SettingsManager.PREFTYPES.BOOLEAN -> value.toBooleanStrictOrNull()
                                        ?.let { settingsManagerToWriteTo.putSettingsBool(enum, it) }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            loggingHelper.addLog(LOGIMPORTANCE.CRITICAL.int, e.toString(), "loadSharedPreferences", null)
            return false
        }
        return true
    }

    private fun getStringSetFromString(value: String): MutableSet<String> {
        // Delete the { and } characters. Trim away all the spaces and split by comma
        return value.replace("{", "").replace("}", "").replace(" ", "").trim().split(",").toMutableSet()
    }

    private fun getDateTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }
}