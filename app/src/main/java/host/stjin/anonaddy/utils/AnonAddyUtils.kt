package host.stjin.anonaddy.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.fragment.app.FragmentManager
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.aliases.manage.SendMailAppChooserBottomDialogFragment
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Aliases
import androidx.core.net.toUri

object AnonAddyUtils {
    fun buildEmailIntent(
        recipients: Array<String?>? = null,
        cc: Array<String?>? = null,
        bcc: Array<String?>? = null,
        subject: String? = null,
        body: String? = null
    ): Intent {
        val toList = recipients?.filterNotNull()?.filter { it.isNotBlank() } ?: emptyList()
        val ccList = cc?.filterNotNull()?.filter { it.isNotBlank() } ?: emptyList()
        val bccList = bcc?.filterNotNull()?.filter { it.isNotBlank() } ?: emptyList()

        val to = toList.joinToString(",")
        val queryParams = mutableListOf<String>()

        if (ccList.isNotEmpty()) {
            queryParams.add("cc=" + Uri.encode(ccList.joinToString(",")))
        }
        if (bccList.isNotEmpty()) {
            queryParams.add("bcc=" + Uri.encode(bccList.joinToString(",")))
        }
        if (!subject.isNullOrEmpty()) {
            queryParams.add("subject=" + Uri.encode(subject))
        }
        if (!body.isNullOrEmpty()) {
            queryParams.add("body=" + Uri.encode(body))
        }

        val uriString = StringBuilder("mailto:").append(to)
        if (queryParams.isNotEmpty()) {
            uriString.append("?").append(queryParams.joinToString("&"))
        }

        val uri = uriString.toString().toUri()
        return Intent(Intent.ACTION_SENDTO, uri).apply {
            if (toList.isNotEmpty()) {
                putExtra(Intent.EXTRA_EMAIL, toList.toTypedArray())
            }
            if (ccList.isNotEmpty()) {
                putExtra(Intent.EXTRA_CC, ccList.toTypedArray())
            }
            if (bccList.isNotEmpty()) {
                putExtra(Intent.EXTRA_BCC, bccList.toTypedArray())
            }
            if (!subject.isNullOrEmpty()) {
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            if (!body.isNullOrEmpty()) {
                putExtra(Intent.EXTRA_TEXT, body)
            }
        }
    }

    fun getSendAddress(recipientEmails: String, alias: Aliases): Array<String?> {
        val recipients = recipientEmails.split(",")
        val toAddresses = Array<String?>(recipients.size) { null }

        for ((i, email) in recipients.withIndex()) {
            // This method generates the to address for sending emails from this alias according to https://addy.io/help/sending-email-from-an-alias/
            val leftPartOfAlias = alias.local_part
            val domain = alias.domain
            val recipientLeftPartOfEmail = email.substringBeforeLast("@", "")
            val recipientRightPartOfEmail = email.substringAfterLast("@", "")
            toAddresses[i] = "$leftPartOfAlias+$recipientLeftPartOfEmail=$recipientRightPartOfEmail@$domain"
        }

        return toAddresses
    }

    fun sendEmail(
        context: Context,
        intent: Intent,
        chooserTitle: String,
        fragmentManager: FragmentManager? = null
    ): Boolean {
        val packageManager = context.packageManager
        val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
        val preferredPackage = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.DEFAULT_EMAIL_CLIENT)

        // Find available packages handling this intent (excluding own app)
        val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
        val possiblePackages = resolveInfoList
            .map { it.activityInfo.packageName }
            .filter { it != context.packageName }
            .distinct()

        if (possiblePackages.isEmpty()) {
            throw ActivityNotFoundException()
        }

        // 1. If user set a preferred app and it is still installed, use it directly
        if (!preferredPackage.isNullOrEmpty()) {
            if (possiblePackages.contains(preferredPackage)) {
                val directIntent = Intent(intent).apply {
                    setPackage(preferredPackage)
                }
                context.startActivity(directIntent)
                return true
            } else {
                // Fallback: The preferred app was uninstalled or no longer handles mailto.
                // Clear the stale preference so the user is prompted again.
                encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.DEFAULT_EMAIL_CLIENT, "")
            }
        }

        // 2. If only one email client is available, open it directly
        if (possiblePackages.size == 1) {
            val directIntent = Intent(intent).apply {
                setPackage(possiblePackages[0])
            }
            context.startActivity(directIntent)
            return true
        }

        // 3. If fragmentManager is provided, show custom BottomSheet chooser with "Always use this app" checkbox
        if (fragmentManager != null) {
            val chooserDialog = SendMailAppChooserBottomDialogFragment.newInstance(intent)
            chooserDialog.show(fragmentManager, "SendMailAppChooserBottomDialogFragment")
            return false
        }

        // 4. Fallback to standard share sheet
        startShareSheetActivityExcludingOwnApp(context, intent, chooserTitle)
        return true
    }

    fun getAppNameFromPackage(context: Context, packageName: String): String? {
        return try {
            val packageManager = context.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            null
        }
    }

    fun startShareSheetActivityExcludingOwnApp(context: Context, intent: Intent, chooserTitle: String) {
        val packageManager = context.packageManager
        val possibleIntents: MutableList<Intent> = ArrayList()
        val possiblePackageNames: MutableSet<String> = HashSet()
        for (resolveInfo in packageManager.queryIntentActivities(intent, 0)) {
            val packageName = resolveInfo.activityInfo.packageName
            if (packageName != context.packageName) {
                val possibleIntent = Intent(intent)
                possibleIntent.setPackage(resolveInfo.activityInfo.packageName)
                possiblePackageNames.add(resolveInfo.activityInfo.packageName)
                possibleIntents.add(possibleIntent)
            }
        }
        val defaultResolveInfo = packageManager.resolveActivity(intent, 0)
        if (defaultResolveInfo == null || possiblePackageNames.isEmpty()) {
            throw ActivityNotFoundException()
        }

        // If there is a default app to handle the intent (which is not this app), use it.
        if (possiblePackageNames.contains(defaultResolveInfo.activityInfo.packageName)) {
            context.startActivity(intent)
        } else { // Otherwise, let the user choose.
            val intentChooser = Intent.createChooser(possibleIntents.removeAt(0), chooserTitle)
            intentChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, possibleIntents.toTypedArray())
            context.startActivity(intentChooser)
        }
    }

}