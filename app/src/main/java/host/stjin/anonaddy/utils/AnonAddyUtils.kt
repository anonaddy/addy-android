package host.stjin.anonaddy.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.Nullable
import host.stjin.anonaddy.models.Aliases

object AnonAddyUtils {
    fun getSendAddress(recipientEmails: String, alias: Aliases): Array<String?> {
        val recipients = recipientEmails.split(",")
        val toAddresses = Array<String?>(recipients.size) { null }

        for ((i, email) in recipients.withIndex()) {
            // This method generates the to address for sending emails from this alias according to https://anonaddy.com/help/sending-email-from-an-alias/
            val leftPartOfAlias = alias.local_part
            val domain = alias.domain
            val recipientLeftPartOfEmail = email.substringBeforeLast("@", "")
            val recipientRightPartOfEmail = email.substringAfterLast("@", "")
            toAddresses[i] = "$leftPartOfAlias+$recipientLeftPartOfEmail=$recipientRightPartOfEmail@$domain"
        }

        return toAddresses
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
        @Nullable val defaultResolveInfo = packageManager.resolveActivity(intent, 0)
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