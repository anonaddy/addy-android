package host.stjin.anonaddy.ui.intent

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.utils.AnonAddyUtils
import host.stjin.anonaddy.utils.AnonAddyUtils.startShareSheetActivityExcludingOwnApp
import host.stjin.anonaddy.utils.CustomPatterns
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.Aliases
import kotlinx.coroutines.launch
import java.net.URLDecoder


class IntentContextMenuAliasActivity : BaseActivity(), IntentSendMailRecipientBottomDialogFragment.AddIntentSendMailRecipientBottomDialogListener,
    IntentBottomDialogFragment.IntentBottomDialogListener {


    lateinit var networkHelper: NetworkHelper

    private lateinit var intentBottomDialogFragment: IntentBottomDialogFragment
    private var domainOptions: List<String> = listOf()

    private var subject: String? = null
    private var body: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Since this activity can be directly launched, set the dark mode.
        checkForDarkModeAndSetFlags()

        networkHelper = NetworkHelper(this)

        /**
         * This activity can be called by an URI or Widget/Notification Intent.
         * Protect this part
         */
        lifecycleScope.launch {
            isAuthenticated { isAuthenticated ->
                if (isAuthenticated) {

                    // Main fragment (the one with the text and loading indicator)
                    intentBottomDialogFragment = IntentBottomDialogFragment.newInstance()
                    if (!intentBottomDialogFragment.isAdded) {
                        intentBottomDialogFragment.show(
                            supportFragmentManager,
                            "intentBottomDialogFragment"
                        )
                    }

                    // Get all the data from intent datastring
                    // mailto: contains 7 chars
                    val recipients = intent.dataString?.substringBefore("?")?.substring(7)?.replace(";", ",")?.split(",")
                    subject = intent.dataString?.let { getParameter(it, "subject") }
                    val ccRecipients = intent.dataString?.let { getParameter(it, "cc")?.replace(";", ",")?.split(",") }
                    val bccRecipients = intent.dataString?.let { getParameter(it, "bcc")?.replace(";", ",")?.split(",") }
                    body = intent.dataString?.let { getParameter(it, "body") }

                    val attachment = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Parcelable::class.java)
                    } else {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }


                    if (attachment != null) {
                        Toast.makeText(
                            this@IntentContextMenuAliasActivity,
                            this@IntentContextMenuAliasActivity.resources.getString(R.string.intent_attachments_not_supported),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }

                    // Get all the data from bundle (some apps use bundle to pass addresses)
                    val bundle = intent.extras
                    val recipientsFromBundle = bundle?.getStringArray(Intent.EXTRA_EMAIL)
                    val ccRecipientsFromBundle = bundle?.getStringArray(Intent.EXTRA_CC)
                    val bccRecipientsFromBundle = bundle?.getStringArray(Intent.EXTRA_BCC)
                    val subjectFromBundle = bundle?.getString(Intent.EXTRA_SUBJECT)
                    if (!subjectFromBundle.isNullOrBlank()) {
                        subject = subjectFromBundle
                    }
                    val bodyFromBundle = bundle?.getString(Intent.EXTRA_TEXT)
                    if (!bodyFromBundle.isNullOrBlank()) {
                        body = bodyFromBundle
                    }

                    // Filter out invalid email addrsses
                    val validEmails = arrayListOf<String>()
                    val validCcRecipients = arrayListOf<String>()
                    val validBccRecipients = arrayListOf<String>()

                    if (recipients != null) {
                        for (email in recipients) {
                            if (CustomPatterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                validEmails.add(email)
                            }
                        }
                    }

                    if (recipientsFromBundle != null) {
                        for (email in recipientsFromBundle) {
                            if (CustomPatterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                validEmails.add(email)
                            }
                        }
                    }


                    if (ccRecipients != null) {
                        for (email in ccRecipients) {
                            if (CustomPatterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                validCcRecipients.add(email)
                            }
                        }
                    }

                    if (ccRecipientsFromBundle != null) {
                        for (email in ccRecipientsFromBundle) {
                            if (CustomPatterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                validCcRecipients.add(email)
                            }
                        }
                    }

                    if (bccRecipients != null) {
                        for (email in bccRecipients) {
                            if (CustomPatterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                validBccRecipients.add(email)
                            }
                        }
                    }

                    if (bccRecipientsFromBundle != null) {
                        for (email in bccRecipientsFromBundle) {
                            if (CustomPatterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                validBccRecipients.add(email)
                            }
                        }
                    }

                    lifecycleScope.launch {
                        // Figure out what to do next (passes the email address)
                        figureOutNextAction(validEmails, validCcRecipients, validBccRecipients)
                    }

                }
            }
        }
    }

    private fun getParameter(data: String, parameter: String): String? {
        if (data.contains("$parameter=")) {
            return data.substringAfter("$parameter=").substringBefore("&")
        }
        return null
    }

    override fun finish() {
        if (::intentSendMailRecipientBottomDialogFragment.isInitialized) {
            intentSendMailRecipientBottomDialogFragment.dismissAllowingStateLoss()
        }
        if (::intentBottomDialogFragment.isInitialized) {
            intentBottomDialogFragment.dismissAllowingStateLoss()
        }
        super.finish()
    }

    private suspend fun figureOutNextAction(emails: ArrayList<String>, validCcRecipients: ArrayList<String>, validBccRecipients: ArrayList<String>) {

        // Obtain domain options
        networkHelper.getDomainOptions { domainOptionsObject, _ ->
            if (domainOptionsObject != null) {
                // Set variable
                domainOptions = domainOptionsObject.data


                if (emails.isNotEmpty() && emails.size == 1) {
                    // Only 1 email address found.

                    // splittedEmailAddress[0] = custom part
                    // splittedEmailAddress[1] = domain name
                    val splittedEmailAddress = emails[0].split("@")

                    /*
                    Figure out if the selected email's domain name is part of the user's addy.io account or not
                     */

                    if (domainOptions.contains(splittedEmailAddress[1])) {
                        // The domain of the email address is linked to this addy.io account. User most likely wants to either manage or create this Alias.
                        intentBottomDialogFragment.setText(this.resources.getString(R.string.intent_creating_alias, emails[0]))
                        lifecycleScope.launch {
                            checkIfAliasExists(emails[0])
                        }
                    } else {
                        // The domain of the email address is not linked to this addy.io account. User most likely wants to send
                        // an email from an alias to this email address
                        sendEmailFromAlias(emails, validCcRecipients, validBccRecipients)
                    }
                } else {
                    // There are either multiple email addressed found, or no email addresses found.
                    // User most likely wants to send an email from an alias
                    sendEmailFromAlias(emails, validCcRecipients, validBccRecipients)
                }


            } else {
                Toast.makeText(this, this.resources.getString(R.string.something_went_wrong_retrieving_domains), Toast.LENGTH_LONG).show()
                finish()
            }
        }


    }


    private lateinit var intentSendMailRecipientBottomDialogFragment: IntentSendMailRecipientBottomDialogFragment
    private fun sendEmailFromAlias(emails: ArrayList<String>, validCcRecipients: ArrayList<String>, validBccRecipients: ArrayList<String>) {
        intentBottomDialogFragment.setText(this.resources.getString(R.string.intent_opening_send_mail_dialog))

        // Get aliases and pass it through to the send email bottomdialog
        intentSendMailRecipientBottomDialogFragment =
            IntentSendMailRecipientBottomDialogFragment.newInstance(emails, validCcRecipients, validBccRecipients, domainOptions)

        if (!intentSendMailRecipientBottomDialogFragment.isAdded) {
            intentSendMailRecipientBottomDialogFragment.show(
                supportFragmentManager,
                "intentSendMailRecipientBottomDialogFragment"
            )
        }


    }

    private suspend fun checkIfAliasExists(text: String) {
        networkHelper.getAliases(
            { result, _ ->
                if (result != null) {
                    // Check if there is an alias with this email address and get its ID
                    val aliasId: String? = result.data.firstOrNull { it.email.lowercase() == text.lowercase() }?.id
                    if (!aliasId.isNullOrEmpty()) {
                        // ID is not empty, thus there was a match
                        // Let the user know that an alias exists, wait 1s and open the ManageAliasActivity
                        intentBottomDialogFragment.setText(this.resources.getString(R.string.intent_alias_already_exists))
                        Handler(Looper.getMainLooper()).postDelayed({
                            intentBottomDialogFragment.dismissAllowingStateLoss()
                            // There is an alias with this exact email address. It already exists! Open the ManageAliasActivity
                            val intent = Intent(this, ManageAliasActivity::class.java)
                            // Pass data object in the bundle and populate details activity.
                            intent.putExtra("alias_id", aliasId)
                            startActivity(intent)
                            finish()
                        }, 1000)
                    } else {
                        // ID is empty, this alias is new! Let's create it
                        val splittedEmailAddress = text.split("@")
                        lifecycleScope.launch {
                            addAliasToAccount(splittedEmailAddress[1], "", "custom", splittedEmailAddress[0])
                        }
                    }
                } else {
                    Toast.makeText(this, this.resources.getString(R.string.something_went_wrong_retrieving_aliases), Toast.LENGTH_LONG).show()
                    finish()
                }
            },
            aliasSortFilter = AliasSortFilter(
                onlyActiveAliases = false,
                onlyDeletedAliases = false,
                onlyInactiveAliases = false,
                onlyWatchedAliases = false,
                sort = null,
                sortDesc = true,
                filter = text
            )
        )

    }

    private suspend fun addAliasToAccountAndShare(
        domain: String,
        description: String,
        format: String,
        local_part: String,
        alias: String,
        recipients: String,
        ccRecipients: String,
        bccRecipients: String
    ) {
        networkHelper.addAlias({ aliasObject, _ ->
            if (aliasObject != null) {
                Toast.makeText(this, this.resources.getString(R.string.alias_created), Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    onPressSend(alias, aliasObject, recipients, ccRecipients, bccRecipients)
                }
            } else {
                Toast.makeText(this, this.resources.getString(R.string.error_adding_alias), Toast.LENGTH_LONG).show()
                finish()
            }
        }, domain, description, format, local_part, null)
    }

    private suspend fun addAliasToAccount(
        domain: String,
        description: String,
        format: String,
        local_part: String
    ) {
        networkHelper.addAlias({ alias, _ ->
            if (alias != null) {
                Toast.makeText(this, this.resources.getString(R.string.alias_created), Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, this.resources.getString(R.string.error_adding_alias), Toast.LENGTH_LONG).show()
                finish()
            }
        }, domain, description, format, local_part, null)
    }

    override suspend fun onPressSend(
        alias: String,
        aliasObject: Aliases?,
        recipients: String,
        ccRecipients: String,
        bccRecipients: String,
        skipAndOpenDefaultMailApp: Boolean
    ) {
        intentSendMailRecipientBottomDialogFragment.dismissAllowingStateLoss()

        if (skipAndOpenDefaultMailApp) {
            openMailToShareSheet(
                recipients.split(",").toTypedArray(),
                ccRecipients.split(",").toTypedArray(),
                bccRecipients.split(",").toTypedArray()
            )
            finish()
        } else {
            // Check if this alias exists
            if (aliasObject != null) {
                // The entered alias exists!
                intentBottomDialogFragment.setText(this.resources.getString(R.string.intent_opening_sharesheet))

                // Get recipients
                val anonaddyRecipientAddresses = AnonAddyUtils.getSendAddress(recipients, aliasObject)

                val anonaddyCcRecipientAddresses = if (ccRecipients.isNotEmpty()) {
                    AnonAddyUtils.getSendAddress(ccRecipients, aliasObject)
                } else {
                    arrayOf()
                }

                val anonaddyBccRecipientAddresses = if (bccRecipients.isNotEmpty()) {
                    AnonAddyUtils.getSendAddress(bccRecipients, aliasObject)
                } else {
                    arrayOf()
                }


                // In case some email apps do not receive EXTRA_EMAIL properly. Copy the email addresses to clipboard as well
                val clipboard: ClipboardManager =
                    this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("recipients", anonaddyRecipientAddresses.joinToString(";"))
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, this.resources.getString(R.string.copied_recipients), Toast.LENGTH_LONG).show()

                /**
                 * SINCE Android 11, we can only query apps that support the mailto: intent :D
                 */

                openMailToShareSheet(anonaddyRecipientAddresses, anonaddyCcRecipientAddresses, anonaddyBccRecipientAddresses)
                finish()
            } else {
                intentBottomDialogFragment.setText(this.resources.getString(R.string.intent_creating_alias, alias))

                // Alias does not exist, perhaps the user wants to create it?
                val splittedEmailAddress = alias.split("@")
                lifecycleScope.launch {
                    addAliasToAccountAndShare(
                        splittedEmailAddress[1],
                        "",
                        "custom",
                        splittedEmailAddress[0],
                        alias,
                        recipients,
                        ccRecipients,
                        bccRecipients
                    )
                }
            }
        }
    }

    private fun openMailToShareSheet(
        recipients: Array<String?>,
        anonaddyCcRecipientAddresses: Array<String?>,
        anonaddyBccRecipientAddresses: Array<String?>
    ) {
        // Open the mailto app select sheet, but make sure to exclude ourselves!
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, recipients)
        intent.putExtra(Intent.EXTRA_CC, anonaddyCcRecipientAddresses)
        intent.putExtra(Intent.EXTRA_BCC, anonaddyBccRecipientAddresses)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject?.let { fromHtml(it) })
        intent.putExtra(Intent.EXTRA_TEXT, body?.let { fromHtml(it) })
        if (intent.resolveActivity(packageManager) != null) {
            startShareSheetActivityExcludingOwnApp(this, intent, this.resources.getString(R.string.send_mail))
        }
    }

    private fun fromHtml(source: String): String {
        return URLDecoder.decode(source, "UTF-8")
    }

    override fun onClose(result: Boolean) {
        if (!result) {
            finish()
        }
    }

    override fun onClose() {
        finish()
    }

}