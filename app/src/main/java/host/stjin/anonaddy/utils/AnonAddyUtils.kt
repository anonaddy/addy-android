package host.stjin.anonaddy.utils

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
}