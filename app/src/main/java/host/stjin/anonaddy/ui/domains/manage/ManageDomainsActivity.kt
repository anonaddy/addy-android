package host.stjin.anonaddy.ui.domains.manage

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.*
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.utils.DateTimeUtils
import kotlinx.android.synthetic.main.activity_manage_domains.*
import kotlinx.android.synthetic.main.anonaddy_custom_dialog.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class ManageDomainsActivity : BaseActivity(),
    EditDomainDescriptionBottomDialogFragment.AddEditDomainDescriptionBottomDialogListener,
    EditDomainRecipientBottomDialogFragment.AddEditDomainRecipientBottomDialogListener {

    lateinit var networkHelper: NetworkHelper

    private lateinit var editDomainDescriptionBottomDialogFragment: EditDomainDescriptionBottomDialogFragment
    private lateinit var editDomainRecipientBottomDialogFragment: EditDomainRecipientBottomDialogFragment

    private lateinit var domainId: String
    private var forceSwitch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_domains)
        setupToolbar(activity_manage_domain_toolbar)
        networkHelper = NetworkHelper(this)


        val b = intent.extras
        val domainId = b?.getString("domain_id")

        if (domainId == null) {
            finish()
            return
        }
        this.domainId = domainId
        setPage()
    }


    private fun setPage() {
        activity_manage_domain_RL_lottieview.visibility = View.GONE

        // Get the domain
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getDomainInfo(domainId)
        }
    }

    private fun setOnSwitchChangeListeners() {
        activity_manage_domain_active_switch.setOnCheckedChangeListener { compoundButton, b ->
            // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
            if (compoundButton.isPressed || forceSwitch) {
                activity_manage_domain_active_switch_progressbar.visibility = View.VISIBLE
                forceSwitch = false
                if (b) {
                    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                        activateDomain()
                    }
                } else {
                    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                        deactivateDomain()
                    }
                }
            }
        }

        activity_manage_catch_all_switch.setOnCheckedChangeListener { compoundButton, b ->
            // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
            if (compoundButton.isPressed || forceSwitch) {
                activity_manage_catch_all_switch_progressbar.visibility = View.VISIBLE
                forceSwitch = false
                if (b) {
                    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                        enableCatchAll()
                    }
                } else {
                    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                        disableCatchAll()
                    }
                }
            }
        }
    }

    private suspend fun disableCatchAll() {
        networkHelper.disableCatchAllSpecificDomain({ result ->
            activity_manage_catch_all_switch_progressbar.visibility = View.GONE
            if (result == "204") {
                activity_manage_catch_all_textview.text = resources.getString(R.string.catch_all_disabled)
            } else {
                activity_manage_catch_all_switch.isChecked = true
                val snackbar = Snackbar.make(
                    findViewById(R.id.activity_manage_domain_LL),
                    this.resources.getString(R.string.error_edit_catch_all) + "\n" + result,
                    Snackbar.LENGTH_SHORT
                )
                if (SettingsManager(false, this).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
                    snackbar.setAction(R.string.logs) {
                        val intent = Intent(this, LogViewerActivity::class.java)
                        startActivity(intent)
                    }
                }
                snackbar.show()

            }
        }, domainId)
    }

    private suspend fun enableCatchAll() {
        networkHelper.enableCatchAllSpecificDomain({ result ->
            activity_manage_catch_all_switch_progressbar.visibility = View.GONE
            if (result == "200") {
                activity_manage_catch_all_textview.text = resources.getString(R.string.catch_all_enabled)
            } else {
                activity_manage_catch_all_switch.isChecked = false
                val snackbar = Snackbar.make(
                    findViewById(R.id.activity_manage_domain_LL),
                    this.resources.getString(R.string.error_edit_catch_all) + "\n" + result,
                    Snackbar.LENGTH_SHORT
                )
                if (SettingsManager(false, this).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
                    snackbar.setAction(R.string.logs) {
                        val intent = Intent(this, LogViewerActivity::class.java)
                        startActivity(intent)
                    }
                }
                snackbar.show()
            }
        }, domainId)
    }

    private suspend fun deactivateDomain() {
        networkHelper.deactivateSpecificDomain({ result ->
            activity_manage_domain_active_switch_progressbar.visibility = View.GONE
            if (result == "204") {
                activity_manage_domain_status_textview.text = resources.getString(R.string.domain_deactivated)
            } else {
                activity_manage_domain_active_switch.isChecked = true
                val snackbar = Snackbar.make(
                    findViewById(R.id.activity_manage_domain_LL),
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    Snackbar.LENGTH_SHORT
                )
                if (SettingsManager(false, this).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
                    snackbar.setAction(R.string.logs) {
                        val intent = Intent(this, LogViewerActivity::class.java)
                        startActivity(intent)
                    }
                }
                snackbar.show()

            }
        }, domainId)
    }

    private suspend fun activateDomain() {
        networkHelper.activateSpecificDomain({ result ->
            activity_manage_domain_active_switch_progressbar.visibility = View.GONE
            if (result == "200") {
                activity_manage_domain_status_textview.text = resources.getString(R.string.domain_activated)
            } else {
                activity_manage_domain_active_switch.isChecked = false
                val snackbar = Snackbar.make(
                    findViewById(R.id.activity_manage_domain_LL),
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    Snackbar.LENGTH_SHORT
                )
                if (SettingsManager(false, this).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
                    snackbar.setAction(R.string.logs) {
                        val intent = Intent(this, LogViewerActivity::class.java)
                        startActivity(intent)
                    }
                }
                snackbar.show()
            }
        }, domainId)
    }

    private fun setOnClickListeners() {

        activity_manage_domain_active_switch_layout.setOnClickListener {
            forceSwitch = true
            activity_manage_domain_active_switch.isChecked = !activity_manage_domain_active_switch.isChecked
        }

        activity_manage_domain_catch_all_switch_layout.setOnClickListener {
            forceSwitch = true
            activity_manage_catch_all_switch.isChecked = !activity_manage_catch_all_switch.isChecked
        }

        activity_manage_domain_desc_edit.setOnClickListener {
            if (!editDomainDescriptionBottomDialogFragment.isAdded) {
                editDomainDescriptionBottomDialogFragment.show(
                    supportFragmentManager,
                    "editDomainDescriptionBottomDialogFragment"
                )
            }
        }

        activity_manage_domain_recipients_edit.setOnClickListener {
            if (!editDomainRecipientBottomDialogFragment.isAdded) {
                editDomainRecipientBottomDialogFragment.show(
                    supportFragmentManager,
                    "editDomainRecipientsBottomDialogFragment"
                )
            }
        }

        activity_manage_domain_delete.setOnClickListener {
            deletedomain(domainId)
        }

        activity_manage_domain_check_dns.setOnClickListener {
            val url = "${AnonAddy.API_BASE_URL}/domains"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
    }

    lateinit var dialog: AlertDialog
    private lateinit var customLayout: View
    private fun deletedomain(id: String) {
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        // set the custom layout
        customLayout =
            layoutInflater.inflate(R.layout.anonaddy_custom_dialog, null)
        builder.setView(customLayout)
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        customLayout.dialog_title.text = resources.getString(R.string.delete_domain)
        customLayout.dialog_text.text = resources.getString(R.string.delete_domain_desc_confirm)
        customLayout.dialog_positive_button.text =
            resources.getString(R.string.delete_domain)
        customLayout.dialog_positive_button.setOnClickListener {
            customLayout.dialog_progressbar.visibility = View.VISIBLE
            customLayout.dialog_error.visibility = View.GONE
            customLayout.dialog_negative_button.isEnabled = false
            customLayout.dialog_positive_button.isEnabled = false

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                deleteDomainHttpRequest(id, this@ManageDomainsActivity)
            }
        }
        customLayout.dialog_negative_button.setOnClickListener {
            dialog.dismiss()
        }
        // create and show the alert dialog
        dialog.show()
    }


    private suspend fun deleteDomainHttpRequest(id: String, context: Context) {
        networkHelper.deleteDomain(id) { result ->
            if (result == "204") {
                dialog.dismiss()
                finish()
            } else {
                customLayout.dialog_progressbar.visibility = View.INVISIBLE
                customLayout.dialog_error.visibility = View.VISIBLE
                customLayout.dialog_negative_button.isEnabled = true
                customLayout.dialog_positive_button.isEnabled = true
                customLayout.dialog_error.text =
                    context.resources.getString(R.string.s_s, context.resources.getString(R.string.error_deleting_domain), result)
            }
        }
    }


    private suspend fun getDomainInfo(id: String) {
        networkHelper.getSpecificDomain({ list ->

            if (list != null) {
                /**
                 *  SWITCH STATUS
                 */

                activity_manage_domain_active_switch.isChecked = list.active
                activity_manage_domain_status_textview.text =
                    if (list.active) resources.getString(R.string.domain_activated) else resources.getString(R.string.domain_deactivated)

                activity_manage_catch_all_switch.isChecked = list.catch_all


                /**
                 * TEXT
                 */

                var totalForwarded = 0
                var totalBlocked = 0
                var totalReplies = 0
                var totalSent = 0
                val totalAliases = list.aliases?.size
                var aliases = ""

                val buf = StringBuilder()

                if (list.aliases != null) {
                    for (alias in list.aliases) {
                        totalForwarded += alias.emails_forwarded
                        totalBlocked += alias.emails_blocked
                        totalReplies += alias.emails_replied
                        totalSent += alias.emails_sent

                        if (buf.isNotEmpty()) {
                            buf.append("\n")
                        }
                        buf.append(alias.email)
                    }
                    aliases = buf.toString()
                }

                activity_manage_domain_aliases_title_textview.text = resources.getString(R.string.domain_aliases_d, totalAliases)
                activity_manage_domain_basic_textview.text = resources.getString(
                    R.string.manage_domain_basic_info,
                    list.domain,
                    DateTimeUtils.turnStringIntoLocalString(list.created_at),
                    DateTimeUtils.turnStringIntoLocalString(list.updated_at),
                    DateTimeUtils.turnStringIntoLocalString(list.domain_verified_at),
                    DateTimeUtils.turnStringIntoLocalString(list.domain_sending_verified_at),
                    totalForwarded, totalBlocked, totalReplies, totalSent
                )

                activity_manage_domain_aliases_textview.text = aliases

                /**
                 * RECIPIENTS
                 */

                // Set recipient
                val recipients: String = list.default_recipient?.email ?: this.resources.getString(
                    R.string.default_recipient
                )

                activity_manage_domain_recipients.text = recipients


                // Initialise the bottomdialog
                editDomainRecipientBottomDialogFragment =
                    EditDomainRecipientBottomDialogFragment.newInstance(domainId, list.default_recipient?.email)


                /**
                 * DESCRIPTION
                 */

                // Set description and initialise the bottomDialogFragment
                if (list.description != null) {
                    activity_manage_domain_desc.text = list.description
                } else {
                    activity_manage_domain_desc.text = this.resources.getString(
                        R.string.domain_no_description
                    )
                }

                // reset this value as it now includes the description
                editDomainDescriptionBottomDialogFragment = EditDomainDescriptionBottomDialogFragment.newInstance(
                    id,
                    list.description
                )

                /**
                 * Check DNS
                 */

                if (list.domain_sending_verified_at == null) {
                    activity_manage_domain_dns_icon.setImageResource(R.drawable.ic_outline_dns_alert)
                    activity_manage_domain_check_dns_subtext.text = resources.getString(R.string.check_dns_desc_incorrect)
                } else {
                    activity_manage_domain_dns_icon.setImageResource(R.drawable.ic_outline_dns_24)
                    activity_manage_domain_check_dns_subtext.text = resources.getString(R.string.check_dns_desc)
                }


                activity_manage_domain_RL_progressbar.visibility = View.GONE
                activity_manage_domain_LL1.visibility = View.VISIBLE


                setOnSwitchChangeListeners()
                setOnClickListeners()
            } else {
                activity_manage_domain_RL_progressbar.visibility = View.GONE
                activity_manage_domain_LL1.visibility = View.GONE

                // Show no internet animations
                activity_manage_domain_RL_lottieview.visibility = View.VISIBLE
            }
        }, id)
    }

    override fun onDestroy() {
        super.onDestroy()
        supportFinishAfterTransition()
    }

    override fun descriptionEdited(description: String) {
        setPage()
        editDomainDescriptionBottomDialogFragment.dismiss()
    }

    override fun recipientEdited() {
        setPage()
        editDomainRecipientBottomDialogFragment.dismiss()
    }
}