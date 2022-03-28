package host.stjin.anonaddy.ui.domains.manage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityManageDomainsBinding
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.AnonAddy
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


class ManageDomainsActivity : BaseActivity(),
    EditDomainDescriptionBottomDialogFragment.AddEditDomainDescriptionBottomDialogListener,
    EditDomainRecipientBottomDialogFragment.AddEditDomainRecipientBottomDialogListener {

    lateinit var networkHelper: NetworkHelper

    private lateinit var editDomainDescriptionBottomDialogFragment: EditDomainDescriptionBottomDialogFragment
    private lateinit var editDomainRecipientBottomDialogFragment: EditDomainRecipientBottomDialogFragment

    private var domain: Domains? = null
        set(value) {
            field = value
            value?.let { updateUi(it) }
        }

    private var forceSwitch = false

    private lateinit var binding: ActivityManageDomainsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageDomainsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activityManageDomainNSVRL)
        )

        setupToolbar(
            R.string.edit_domain,
            binding.activityManageDomainNSV,
            binding.activityManageDomainToolbar
        )
        networkHelper = NetworkHelper(this)


        val b = intent.extras
        val domainId = b?.getString("domain_id")

        if (domainId == null) {
            finish()
            return
        }
        setPage(domainId)
    }


    private fun setPage(domainId: String) {
        // Get the domain
        lifecycleScope.launch {
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
                        lifecycleScope.launch {
                            activateDomain()
                        }
                    } else {
                        lifecycleScope.launch {
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
                        lifecycleScope.launch {
                            enableCatchAll()
                        }
                    } else {
                        lifecycleScope.launch {
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
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_catch_all) + "\n" + result,
                    binding.activityManageDomainCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this.domain!!.id)
    }

    private suspend fun enableCatchAll() {
        networkHelper.enableCatchAllSpecificDomain({ domain, error ->
            binding.activityManageDomainCatchAllSwitchLayout.showProgressBar(false)
            if (domain != null) {
                binding.activityManageDomainCatchAllSwitchLayout.setTitle(resources.getString(R.string.catch_all_enabled))
            } else {
                binding.activityManageDomainCatchAllSwitchLayout.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_catch_all) + "\n" + error,
                    binding.activityManageDomainCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this.domain!!.id)
    }

    private suspend fun deactivateDomain() {
        networkHelper.deactivateSpecificDomain({ result ->
            binding.activityManageDomainActiveSwitchLayout.showProgressBar(false)
            if (result == "204") {
                binding.activityManageDomainActiveSwitchLayout.setTitle(resources.getString(R.string.domain_deactivated))
            } else {
                binding.activityManageDomainActiveSwitchLayout.setSwitchChecked(true)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    binding.activityManageDomainCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this.domain!!.id)
    }

    private suspend fun activateDomain() {
        networkHelper.activateSpecificDomain({ domain, error ->
            binding.activityManageDomainActiveSwitchLayout.showProgressBar(false)
            if (domain != null) {
                binding.activityManageDomainActiveSwitchLayout.setTitle(resources.getString(R.string.domain_activated))
            } else {
                binding.activityManageDomainActiveSwitchLayout.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + error,
                    binding.activityManageDomainCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this.domain!!.id)
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
                deleteDomain(this@ManageDomainsActivity.domain!!.id)
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

    private lateinit var deleteDomainSnackbar: Snackbar
    private fun deleteDomain(id: String) {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Catalog_MaterialAlertDialog_Centered_FullWidthButtons)
            .setTitle(resources.getString(R.string.delete_domain))
            .setIcon(R.drawable.ic_trash)
            .setMessage(resources.getString(R.string.delete_domain_desc_confirm))
            .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ ->
                deleteDomainSnackbar = SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.deleting_domain),
                    binding.activityManageDomainCL,
                    length = Snackbar.LENGTH_INDEFINITE
                )
                deleteDomainSnackbar.show()
                lifecycleScope.launch {
                    deleteDomainHttpRequest(id, this@ManageDomainsActivity)
                }
            }
            .show()
    }


    private suspend fun deleteDomainHttpRequest(id: String, context: Context) {
        networkHelper.deleteDomain({ result ->
            if (result == "204") {
                deleteDomainSnackbar.dismiss()
                finish()
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    context.resources.getString(R.string.s_s, context.resources.getString(R.string.error_deleting_domain), result),
                    binding.activityManageDomainCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, id)
    }


    private suspend fun getDomainInfo(id: String) {
        networkHelper.getSpecificDomain({ domain, error ->

            if (domain != null) {
                // Triggers updateUi
                this.domain = domain
            } else {

                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_obtaining_domains) + "\n" + error,
                    binding.activityManageDomainCL
                ).show()

                // Show error animations
                binding.activityManageDomainLL1.visibility = View.GONE
                binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
            }
        }, id)
    }

    private fun updateUi(domain: Domains) {
        /**
         *  SWITCH STATUS
         */

        binding.activityManageDomainActiveSwitchLayout.setSwitchChecked(domain.active)
        binding.activityManageDomainActiveSwitchLayout.setTitle(
            if (domain.active) resources.getString(R.string.domain_activated) else resources.getString(R.string.domain_deactivated)
        )

        binding.activityManageDomainCatchAllSwitchLayout.setSwitchChecked(domain.catch_all)

        /**
         * TEXT
         */

        var totalForwarded = 0
        var totalBlocked = 0
        var totalReplies = 0
        var totalSent = 0
        val totalAliases = domain.aliases?.size
        var aliases = ""

        val buf = StringBuilder()

        if (domain.aliases != null) {
            for (alias in domain.aliases!!) {
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
            domain.domain,
            DateTimeUtils.turnStringIntoLocalString(domain.created_at),
            DateTimeUtils.turnStringIntoLocalString(domain.updated_at),
            DateTimeUtils.turnStringIntoLocalString(domain.domain_verified_at),
            DateTimeUtils.turnStringIntoLocalString(domain.domain_sending_verified_at),
            totalForwarded, totalBlocked, totalReplies, totalSent
        )

        binding.activityManageDomainAliasesTextview.text = aliases

        /**
         * RECIPIENTS
         */

        // Set recipient
        val recipients: String = domain.default_recipient?.email ?: this.resources.getString(
            R.string.default_recipient
        )


        binding.activityManageDomainRecipientsEdit.setDescription(recipients)

        // Initialise the bottomdialog
        editDomainRecipientBottomDialogFragment =
            EditDomainRecipientBottomDialogFragment.newInstance(this.domain!!.id, domain.default_recipient?.email)


        /**
         * DESCRIPTION
         */

        // Set description and initialise the bottomDialogFragment
        if (domain.description != null) {
            binding.activityManageDomainDescEdit.setDescription(domain.description)
        } else {
            binding.activityManageDomainDescEdit.setDescription(
                this.resources.getString(
                    R.string.domain_no_description
                )
            )
        }

        // reset this value as it now includes the description
        editDomainDescriptionBottomDialogFragment = EditDomainDescriptionBottomDialogFragment.newInstance(
            this.domain!!.id,
            domain.description
        )

        /**
         * Check DNS
         */

        if (domain.domain_sending_verified_at == null) {
            binding.activityManageDomainCheckDns.setImageResourceIcons(R.drawable.ic_dns_alert, null)
            binding.activityManageDomainCheckDns.setDescription(resources.getString(R.string.check_dns_desc_incorrect))
            binding.activityManageDomainCheckDns.setSectionAlert(true)
        } else {
            binding.activityManageDomainCheckDns.setImageResourceIcons(R.drawable.ic_dns, null)
            binding.activityManageDomainCheckDns.setDescription(resources.getString(R.string.check_dns_desc))
            binding.activityManageDomainCheckDns.setSectionAlert(false)
        }

        binding.animationFragment.stopAnimation()
        binding.activityManageDomainNSV.animate().alpha(1.0f)

        setOnSwitchChangeListeners()
        setOnClickListeners()
    }

    override fun descriptionEdited(domain: Domains) {
        editDomainDescriptionBottomDialogFragment.dismissAllowingStateLoss()
        // Do this last, will trigger updateUI as well as re-init editDomainDescriptionBottomDialogFragment
        this.domain = domain
    }

    override fun recipientEdited(domain: Domains) {
        editDomainRecipientBottomDialogFragment.dismissAllowingStateLoss()
        // Do this last, will trigger updateUI as well as re-init editDomainRecipientBottomDialogFragment
        this.domain = domain
    }
}