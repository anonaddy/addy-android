package host.stjin.anonaddy.ui.domains.manage

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.*
import host.stjin.anonaddy.databinding.ActivityManageDomainsBinding
import host.stjin.anonaddy.databinding.AnonaddyCustomDialogBinding
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.DateTimeUtils
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

    private lateinit var binding: ActivityManageDomainsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageDomainsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(binding.activityManageDomainToolbar.customToolbarOneHandedMaterialtoolbar, R.string.edit_domain)
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
        binding.activityManageDomainRLLottieview.visibility = View.GONE
        // Get the domain
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getDomainInfo(domainId)
        }
    }

    private fun setOnSwitchChangeListeners() {
        binding.activityManageDomainActiveSwitchLayout.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    binding.activityManageDomainActiveSwitchLayout.showProgressBar(true)
                    forceSwitch = false
                    if (checked) {
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
        })

        binding.activityManageDomainCatchAllSwitchLayout.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    binding.activityManageDomainCatchAllSwitchLayout.showProgressBar(true)
                    forceSwitch = false
                    if (checked) {
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
        })
    }

    private suspend fun disableCatchAll() {
        networkHelper.disableCatchAllSpecificDomain({ result ->
            binding.activityManageDomainCatchAllSwitchLayout.showProgressBar(false)
            if (result == "204") {
                binding.activityManageDomainCatchAllSwitchLayout.setTitle(resources.getString(R.string.catch_all_disabled))
            } else {
                binding.activityManageDomainCatchAllSwitchLayout.setSwitchChecked(true)
                val snackbar = Snackbar.make(
                    findViewById(R.id.activity_manage_domain_CL),
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
            binding.activityManageDomainCatchAllSwitchLayout.showProgressBar(false)
            if (result == "200") {
                binding.activityManageDomainCatchAllSwitchLayout.setTitle(resources.getString(R.string.catch_all_enabled))
            } else {
                binding.activityManageDomainCatchAllSwitchLayout.setSwitchChecked(false)
                val snackbar = Snackbar.make(
                    findViewById(R.id.activity_manage_domain_CL),
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
            binding.activityManageDomainActiveSwitchLayout.showProgressBar(false)
            if (result == "204") {
                binding.activityManageDomainActiveSwitchLayout.setTitle(resources.getString(R.string.domain_deactivated))
            } else {
                binding.activityManageDomainActiveSwitchLayout.setSwitchChecked(true)
                val snackbar = Snackbar.make(
                    findViewById(R.id.activity_manage_domain_CL),
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
            binding.activityManageDomainActiveSwitchLayout.showProgressBar(false)
            if (result == "200") {
                binding.activityManageDomainActiveSwitchLayout.setTitle(resources.getString(R.string.domain_activated))
            } else {
                binding.activityManageDomainActiveSwitchLayout.setSwitchChecked(false)
                val snackbar = Snackbar.make(
                    findViewById(R.id.activity_manage_domain_CL),
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
        binding.activityManageDomainActiveSwitchLayout.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityManageDomainActiveSwitchLayout.setSwitchChecked(!binding.activityManageDomainActiveSwitchLayout.getSwitchChecked())
            }
        })

        binding.activityManageDomainCatchAllSwitchLayout.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityManageDomainCatchAllSwitchLayout.setSwitchChecked(!binding.activityManageDomainCatchAllSwitchLayout.getSwitchChecked())
            }
        })

        binding.activityManageDomainDescEdit.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editDomainDescriptionBottomDialogFragment.isAdded) {
                    editDomainDescriptionBottomDialogFragment.show(
                        supportFragmentManager,
                        "editDomainDescriptionBottomDialogFragment"
                    )
                }
            }
        })


        binding.activityManageDomainRecipientsEdit.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editDomainRecipientBottomDialogFragment.isAdded) {
                    editDomainRecipientBottomDialogFragment.show(
                        supportFragmentManager,
                        "editDomainRecipientsBottomDialogFragment"
                    )
                }
            }
        })


        binding.activityManageDomainDelete.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                deleteDomain(domainId)
            }
        })

        binding.activityManageDomainCheckDns.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val url = "${AnonAddy.API_BASE_URL}/domains"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })

    }

    private lateinit var deleteDomainDialog: AlertDialog
    private fun deleteDomain(id: String) {
        val anonaddyCustomDialogBinding = AnonaddyCustomDialogBinding.inflate(LayoutInflater.from(this), null, false)
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(anonaddyCustomDialogBinding.root)
        deleteDomainDialog = builder.create()
        deleteDomainDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        anonaddyCustomDialogBinding.dialogTitle.text = resources.getString(R.string.delete_domain)
        anonaddyCustomDialogBinding.dialogText.text = resources.getString(R.string.delete_domain_desc_confirm)
        anonaddyCustomDialogBinding.dialogPositiveButton.text =
            resources.getString(R.string.delete_domain)
        anonaddyCustomDialogBinding.dialogPositiveButton.setOnClickListener {
            // Animate the button to progress
            anonaddyCustomDialogBinding.dialogPositiveButton.startAnimation()

            anonaddyCustomDialogBinding.dialogError.visibility = View.GONE
            anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = false
            anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = false

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                deleteDomainHttpRequest(id, this@ManageDomainsActivity, anonaddyCustomDialogBinding)
            }
        }
        anonaddyCustomDialogBinding.dialogNegativeButton.setOnClickListener {
            deleteDomainDialog.dismiss()
        }
        // create and show the alert dialog
        deleteDomainDialog.show()
    }


    private suspend fun deleteDomainHttpRequest(id: String, context: Context, anonaddyCustomDialogBinding: AnonaddyCustomDialogBinding) {
        networkHelper.deleteDomain({ result ->
            if (result == "204") {
                deleteDomainDialog.dismiss()
                finish()
            } else {
                // Revert the button to normal
                anonaddyCustomDialogBinding.dialogPositiveButton.revertAnimation()

                anonaddyCustomDialogBinding.dialogError.visibility = View.VISIBLE
                anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogError.text =
                    context.resources.getString(R.string.s_s, context.resources.getString(R.string.error_deleting_domain), result)
            }
        }, id)
    }


    private suspend fun getDomainInfo(id: String) {
        networkHelper.getSpecificDomain({ list ->

            if (list != null) {
                /**
                 *  SWITCH STATUS
                 */

                binding.activityManageDomainActiveSwitchLayout.setSwitchChecked(list.active)
                binding.activityManageDomainActiveSwitchLayout.setTitle(
                    if (list.active) resources.getString(R.string.domain_activated) else resources.getString(R.string.domain_deactivated)
                )

                binding.activityManageDomainCatchAllSwitchLayout.setSwitchChecked(list.catch_all)

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

                binding.activityManageDomainAliasesTitleTextview.text = resources.getString(R.string.domain_aliases_d, totalAliases)
                binding.activityManageDomainBasicTextview.text = resources.getString(
                    R.string.manage_domain_basic_info,
                    list.domain,
                    DateTimeUtils.turnStringIntoLocalString(list.created_at),
                    DateTimeUtils.turnStringIntoLocalString(list.updated_at),
                    DateTimeUtils.turnStringIntoLocalString(list.domain_verified_at),
                    DateTimeUtils.turnStringIntoLocalString(list.domain_sending_verified_at),
                    totalForwarded, totalBlocked, totalReplies, totalSent
                )

                binding.activityManageDomainAliasesTextview.text = aliases

                /**
                 * RECIPIENTS
                 */

                // Set recipient
                val recipients: String = list.default_recipient?.email ?: this.resources.getString(
                    R.string.default_recipient
                )


                binding.activityManageDomainRecipientsEdit.setDescription(recipients)

                // Initialise the bottomdialog
                editDomainRecipientBottomDialogFragment =
                    EditDomainRecipientBottomDialogFragment.newInstance(domainId, list.default_recipient?.email)


                /**
                 * DESCRIPTION
                 */

                // Set description and initialise the bottomDialogFragment
                if (list.description != null) {
                    binding.activityManageDomainDescEdit.setDescription(list.description)
                } else {
                    binding.activityManageDomainDescEdit.setDescription(
                        this.resources.getString(
                            R.string.domain_no_description
                        )
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
                    binding.activityManageDomainCheckDns.setImageResourceIcons(R.drawable.ic_dns_alert, null)
                    binding.activityManageDomainCheckDns.setDescription(resources.getString(R.string.check_dns_desc_incorrect))
                } else {
                    binding.activityManageDomainCheckDns.setImageResourceIcons(R.drawable.ic_dns_24dp, null)
                    binding.activityManageDomainCheckDns.setDescription(resources.getString(R.string.check_dns_desc))
                }


                binding.activityManageDomainRLProgressbar.visibility = View.GONE
                binding.activityManageDomainLL1.visibility = View.VISIBLE


                setOnSwitchChangeListeners()
                setOnClickListeners()
            } else {
                binding.activityManageDomainRLProgressbar.visibility = View.GONE
                binding.activityManageDomainLL1.visibility = View.GONE

                // Show no internet animations
                binding.activityManageDomainRLLottieview.visibility = View.VISIBLE
            }
        }, id)
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