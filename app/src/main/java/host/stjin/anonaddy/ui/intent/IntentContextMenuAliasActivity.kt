package host.stjin.anonaddy.ui.intent

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.Nullable
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityIntentCreateAliasBinding
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.utils.AnonAddyUtils
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class IntentContextMenuAliasActivity : BaseActivity(), IntentSendMailRecipientBottomDialogFragment.AddIntentSendMailRecipientBottomDialogListener,
    IntentBottomDialogFragment.IntentBottomDialogListener {


    private lateinit var binding: ActivityIntentCreateAliasBinding
    lateinit var networkHelper: NetworkHelper

    private lateinit var intentBottomDialogFragment: IntentBottomDialogFragment
    private var domainOptions: List<String> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntentCreateAliasBinding.inflate(layoutInflater)
        val view = binding.root
        // Since this activity can be directly launched, set the dark mode.
        checkForDarkModeAndSetFlags()
        setContentView(view)
        networkHelper = NetworkHelper(this)

        /**
         * This activity can be called by an URI or Widget/Notification Intent.
         * Protect this part
         */
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
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

                    // Get the selected or clicked on email address
                    val text = intent.dataString
                    // process the text
                    if (!text.isNullOrEmpty()) {
                        if (android.util.Patterns.EMAIL_ADDRESS.matcher(text.substring(7))
                                .matches()
                        ) {
                            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                                // Figure out what to do next (passes the email address)
                                figureOutNextAction(text.substring(7))
                            }
                        } else {
                            Toast.makeText(
                                this@IntentContextMenuAliasActivity,
                                this@IntentContextMenuAliasActivity.resources.getString(R.string.not_a_valid_address),
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                    } else {
                        Toast.makeText(
                            this@IntentContextMenuAliasActivity,
                            this@IntentContextMenuAliasActivity.resources.getString(R.string.nothing_found),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }
        }
    }

    override fun finish() {
        if (::intentSendMailRecipientBottomDialogFragment.isInitialized) {
            intentSendMailRecipientBottomDialogFragment.dismiss()
        }
        if (::intentBottomDialogFragment.isInitialized) {
            intentBottomDialogFragment.dismiss()
        }
        super.finish()
    }

    private suspend fun figureOutNextAction(text: CharSequence) {
        // splittedEmailAddress[0] = custom part
        // splittedEmailAddress[1] = domain name
        val splittedEmailAddress = text.split("@")

        /*
        Figure out if the selected email's domain name is part of the user's AnonAddy account or not
         */

        networkHelper.getDomainOptions { result ->
            if (result != null) {
                // Set variable
                domainOptions = result.data

                if (result.data.contains(splittedEmailAddress[1])) {
                    // The domain of the email address is linked to this AnonAddy account. User most likely wants to either manage or create this Alias.
                    intentBottomDialogFragment.setText(this.resources.getString(R.string.intent_creating_alias, text))
                    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                        checkIfAliasExists(text)
                    }
                } else {
                    // The domain of the email address is not linked to this AnonAddy account. User most likely wants to send
                    // an email from an alias to this email address
                    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                        sendEmailFromAlias(text)
                    }
                }
            } else {
                Toast.makeText(this, this.resources.getString(R.string.something_went_wrong_retrieving_domains), Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private lateinit var intentSendMailRecipientBottomDialogFragment: IntentSendMailRecipientBottomDialogFragment
    private suspend fun sendEmailFromAlias(text: CharSequence) {
        intentBottomDialogFragment.setText(this.resources.getString(R.string.intent_opening_send_mail_dialog))

        // Get aliases and pass it through to the send email bottomdialog
        networkHelper.getAliases({ result ->
            if (result != null) {
                intentSendMailRecipientBottomDialogFragment =
                    IntentSendMailRecipientBottomDialogFragment.newInstance(text.toString(), result, domainOptions)

                if (!intentSendMailRecipientBottomDialogFragment.isAdded) {
                    intentSendMailRecipientBottomDialogFragment.show(
                        supportFragmentManager,
                        "intentSendMailRecipientBottomDialogFragment"
                    )
                }
            } else {
                Toast.makeText(this, this.resources.getString(R.string.something_went_wrong_retrieving_aliases), Toast.LENGTH_LONG).show()
                finish()
            }
        }, activeOnly = true, includeDeleted = false)

    }

    private suspend fun checkIfAliasExists(text: CharSequence) {
        networkHelper.getAliases({ result ->
            if (result != null) {

                // Check if there is an alias with this email address and get its ID
                val aliasId: String? = result.firstOrNull { it.email == text }?.id
                if (!aliasId.isNullOrEmpty()) {
                    // ID is not empty, thus there was a match
                    // Let the user know that an alias exists, wait 1s and open the ManageAliasActivity
                    intentBottomDialogFragment.setText(this.resources.getString(R.string.intent_alias_already_exists))
                    Handler(Looper.getMainLooper()).postDelayed({
                        intentBottomDialogFragment.dismiss()
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
                    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                        addAliasToAccount(splittedEmailAddress[1], "", "custom", splittedEmailAddress[0])
                    }
                }
            }
        }, activeOnly = false, includeDeleted = true)

    }

    private suspend fun addAliasToAccountAndShare(
        domain: String,
        description: String,
        format: String,
        local_part: String,
        alias: String,
        toString: String
    ) {
        networkHelper.addAlias({ result ->
            if (result == "201") {
                Toast.makeText(this, this.resources.getString(R.string.alias_created), Toast.LENGTH_LONG).show()
                GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                    onPressSend(alias, toString)
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
        networkHelper.addAlias({ result ->
            if (result == "201") {
                Toast.makeText(this, this.resources.getString(R.string.alias_created), Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, this.resources.getString(R.string.error_adding_alias), Toast.LENGTH_LONG).show()
                finish()
            }
        }, domain, description, format, local_part, null)
    }

    override suspend fun onPressSend(alias: String, toString: String) {
        intentSendMailRecipientBottomDialogFragment.dismiss()

        networkHelper.getAliases({ result ->
            // Check if this alias exists
            if (result != null) {
                if (result.count { it.email == alias } > 0) {
                    // The entered alias exists!
                    intentBottomDialogFragment.setText(this.resources.getString(R.string.intent_opening_sharesheet))


                    // Get actual alias object
                    val aliasObject = result.first { it.email == alias }
                    // Get recipients
                    val recipients = AnonAddyUtils.getSendAddress(toString, aliasObject)

                    // In case some email apps do not receive EXTRA_EMAIL properly. Copy the email addresses to clipboard as well
                    val clipboard: ClipboardManager =
                        this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("recipients", recipients.joinToString(";"))
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, this.resources.getString(R.string.copied_recipients), Toast.LENGTH_LONG).show()

                    /**
                     * SINCE Android 11, we can only query apps that support the mailto: intent :D
                     */

                    // Open the mailto app select sheet, but make sure to exclude ourselves!
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = Uri.parse("mailto:") // only email apps should handle this
                    intent.putExtra(Intent.EXTRA_EMAIL, recipients)
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivityExcludingOwnApp(this, intent, this.resources.getString(R.string.send_mail))
                    }
                    finish()
                } else {
                    intentBottomDialogFragment.setText(this.resources.getString(R.string.intent_creating_alias, alias))

                    // Alias does not exist, perhaps the user wants to create it?
                    val splittedEmailAddress = alias.split("@")
                    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                        addAliasToAccountAndShare(splittedEmailAddress[1], "", "custom", splittedEmailAddress[0], alias, toString)
                    }
                }
            } else {
                Toast.makeText(this, this.resources.getString(R.string.something_went_wrong_retrieving_aliases), Toast.LENGTH_LONG).show()
                finish()
            }
        }, true, includeDeleted = false)

    }

    override fun onClose(result: Boolean) {
        if (!result) {
            finish()
        }
    }

    override fun onClose() {
        finish()
    }

    private fun startActivityExcludingOwnApp(context: Context, intent: Intent, chooserTitle: String) {
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