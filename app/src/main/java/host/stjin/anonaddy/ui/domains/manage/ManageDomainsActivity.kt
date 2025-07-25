package host.stjin.anonaddy.ui.domains.manage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityManageDomainsBinding
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.InsetUtil
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.AliasesArray
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch
import androidx.core.net.toUri


class ManageDomainsActivity : BaseActivity(),
    EditDomainDescriptionBottomDialogFragment.AddEditDomainDescriptionBottomDialogListener,
    EditDomainFromNameBottomDialogFragment.AddEditDomainFromNameBottomDialogListener,
    EditDomainRecipientBottomDialogFragment.AddEditDomainRecipientBottomDialogListener,
    EditDomainAutoCreateRegexBottomDialogFragment.AddEditDomainAutoCreateRegexBottomDialogListener {

    lateinit var networkHelper: NetworkHelper
    private var shouldRefreshOnFinish = false

    private lateinit var editDomainDescriptionBottomDialogFragment: EditDomainDescriptionBottomDialogFragment
    private lateinit var editDomainRecipientBottomDialogFragment: EditDomainRecipientBottomDialogFragment
    private lateinit var editDomainFromNameBottomDialogFragment: EditDomainFromNameBottomDialogFragment
    private lateinit var editDomainAutoCreateRegexBottomDialogFragment: EditDomainAutoCreateRegexBottomDialogFragment

    private var domain: Domains? = null
        set(value) {
            field = value
            value?.let { updateUi(it) }
        }


    private var aliasList: AliasesArray? = null
        set(value) {
            field = value
            value?.let { domain?.let { domain -> updateUi(domain, it) } }
        }

    private var workingAliasList: AliasesArray? = null


    private var forceSwitch = false

    private lateinit var binding: ActivityManageDomainsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageDomainsBinding.inflate(layoutInflater)
        InsetUtil.applyBottomInset(binding.activityManageDomainLL1)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.edit_domain,
            binding.activityManageDomainNSV,
            binding.activityManageDomainToolbar
        )
        networkHelper = NetworkHelper(this)
        setRefreshLayout()

        val b = intent.extras
        val domainId = b?.getString("domain_id")

        if (domainId == null) {
            finish()
            return
        }
        setPage(domainId)
    }

    private fun setRefreshLayout() {
        binding.activityManageDomainSwiperefresh.setOnRefreshListener {
            binding.activityManageDomainSwiperefresh.isRefreshing = true

            domain?.let { setPage(it.id) }
        }
    }


    override fun finish() {
        val resultIntent = Intent()
        resultIntent.putExtra("shouldRefresh", shouldRefreshOnFinish)
        setResult(RESULT_OK, resultIntent)
        super.finish()
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
                this.domain!!.catch_all = false
                shouldRefreshOnFinish = true
                updateUi(this.domain!!)
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
                this.domain = domain
                shouldRefreshOnFinish = true
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
                this.domain!!.active = false
                shouldRefreshOnFinish = true
                updateUi(this.domain!!)
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
                this.domain = domain
                shouldRefreshOnFinish = true
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

        binding.activityManageDomainFromNameEdit.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editDomainFromNameBottomDialogFragment.isAdded) {
                    editDomainFromNameBottomDialogFragment.show(
                        supportFragmentManager,
                        "editDomainFromNameBottomDialogFragment"
                    )
                }
            }
        })

        binding.activityManageDomainAutoCreateRegexEdit.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editDomainAutoCreateRegexBottomDialogFragment.isAdded) {
                    editDomainAutoCreateRegexBottomDialogFragment.show(
                        supportFragmentManager,
                        "editDomainAutoCreateRegexBottomDialogFragment"
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
                val url = "${AddyIo.API_BASE_URL}/domains"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = url.toUri()
                startActivity(i)
            }
        })

    }

    private lateinit var deleteDomainSnackbar: Snackbar
    private fun deleteDomain(id: String) {
        MaterialDialogHelper.showMaterialDialog(
            context = this,
            title = resources.getString(R.string.delete_domain),
            message = resources.getString(R.string.delete_domain_confirmation_desc),
            icon = R.drawable.ic_trash,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.delete),
            positiveButtonAction = {
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
        ).show()
    }


    private suspend fun deleteDomainHttpRequest(id: String, context: Context) {
        networkHelper.deleteDomain({ result ->
            if (result == "204") {
                deleteDomainSnackbar.dismiss()
                shouldRefreshOnFinish = true
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

                // Now that we have the domain, obtain the aliases separately
                lifecycleScope.launch {
                    getAliasesAndAddThemToList(domain)
                }
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_obtaining_domains) + "\n" + error,
                    binding.activityManageDomainCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()

                // Show error animations
                binding.activityManageDomainLL1.visibility = View.GONE
                binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
            }

            binding.activityManageDomainSwiperefresh.isRefreshing = false
        }, id)
    }

    private fun updateUi(domain: Domains, aliasesArray: AliasesArray? = null) {
        /**
         *  SWITCH STATUS
         */

        binding.activityManageDomainActiveSwitchLayout.setSwitchChecked(domain.active)
        binding.activityManageDomainActiveSwitchLayout.setTitle(
            if (domain.active) resources.getString(R.string.domain_activated) else resources.getString(R.string.domain_deactivated)
        )

        binding.activityManageDomainCatchAllSwitchLayout.setSwitchChecked(domain.catch_all)
        binding.activityManageDomainCatchAllSwitchLayout.setTitle(
            if (domain.catch_all) resources.getString(R.string.catch_all_enabled) else resources.getString(R.string.catch_all_disabled)
        )

        /**
         * TEXT
         */


        var totalForwarded = 0
        var totalBlocked = 0
        var totalReplies = 0
        var totalSent = 0
        val totalAliases = domain.aliases_count
        var aliases: String

        val buf = StringBuilder()

        if (aliasesArray != null) {
            aliasesArray.data = ArrayList(aliasesArray.data.sortedBy { it.email })
            for (alias in aliasesArray.data) {
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
            binding.activityManageDomainAliasesTextview.text = aliases
            binding.activityManageDomainAliasesShimmerframelayout.hideShimmer()
            binding.activityManageDomainBasicShimmerframelayout.hideShimmer() // Stop shimmer only after this info is loaded

        }

        binding.activityManageDomainBasicTextview.text = resources.getString(
            R.string.manage_domain_basic_info,
            domain.domain,
            DateTimeUtils.convertStringToLocalTimeZoneString(domain.created_at),
            DateTimeUtils.convertStringToLocalTimeZoneString(domain.updated_at),
            DateTimeUtils.convertStringToLocalTimeZoneString(domain.domain_verified_at),
            DateTimeUtils.convertStringToLocalTimeZoneString(domain.domain_mx_validated_at),
            DateTimeUtils.convertStringToLocalTimeZoneString(domain.domain_sending_verified_at),
            totalForwarded, totalBlocked, totalReplies, totalSent
        )
        binding.activityManageDomainAliasesTitleTextview.text = resources.getString(R.string.domain_aliases_d, totalAliases)


        /**
         * RECIPIENTS
         */

        // Set recipient
        val recipients: String = domain.default_recipient?.email ?: this.resources.getString(
            R.string.default_recipient_s, (this.application as AddyIoApp).userResourceExtended.default_recipient_email
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
         * FROM NAME
         */


        // Not available for free subscriptions
        if ((this.application as AddyIoApp).userResource.hasUserFreeSubscription) {
            binding.activityManageDomainFromNameEdit.setLayoutEnabled(false)
            binding.activityManageDomainFromNameEdit.setDescription(
                this.resources.getString(
                    R.string.feature_not_available_subscription
                )
            )
        } else {
            // Set description and initialise the bottomDialogFragment
            if (domain.from_name != null) {
                binding.activityManageDomainFromNameEdit.setDescription(domain.from_name)
            } else {
                binding.activityManageDomainFromNameEdit.setDescription(
                    this.resources.getString(
                        R.string.domain_no_from_name
                    )
                )
            }
            // reset this value as it now includes the description
            editDomainFromNameBottomDialogFragment = EditDomainFromNameBottomDialogFragment.newInstance(
                this.domain!!.id,
                this.domain!!.domain,
                domain.from_name
            )
        }


        /**
         * AUTO CREATE REGEX
         */


        // Not available for free subscriptions
        if ((this.application as AddyIoApp).userResource.hasUserFreeSubscription) {
            binding.activityManageDomainAutoCreateRegexEdit.setLayoutEnabled(false)
            binding.activityManageDomainAutoCreateRegexEdit.setDescription(
                this.resources.getString(
                    R.string.feature_not_available_subscription
                )
            )
        } else {
            // Set description and initialise the bottomDialogFragment
            if (domain.auto_create_regex != null) {
                binding.activityManageDomainAutoCreateRegexEdit.setDescription(domain.auto_create_regex)
            } else {
                binding.activityManageDomainAutoCreateRegexEdit.setDescription(
                    this.resources.getString(
                        R.string.domain_no_auto_create_regex
                    )
                )
            }
            // reset this value as it now includes the description
            editDomainAutoCreateRegexBottomDialogFragment = EditDomainAutoCreateRegexBottomDialogFragment.newInstance(
                this.domain!!.id,
                domain.auto_create_regex
            )
        }



        // Please note that the "Catch-all" feature is also only available for paid subcriptions. However, you cannot add your own domains
        // on the free plan, making a check useless


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

    override fun fromNameEdited(domain: Domains) {
        editDomainFromNameBottomDialogFragment.dismissAllowingStateLoss()
        // Do this last, will trigger updateUI as well as re-init editDomainFromNameBottomDialogFragment
        this.domain = domain
    }


    override fun autoCreateRegexEdited(domain: Domains) {
        editDomainAutoCreateRegexBottomDialogFragment.dismissAllowingStateLoss()
        // Do this last, will trigger updateUI as well as re-init editDomainAutoCreateRegexBottomDialogFragment
        this.domain = domain
    }


    private suspend fun getAliasesAndAddThemToList(domain: Domains) {
        binding.activityManageDomainAliasesShimmerframelayout.startShimmer()

        networkHelper.getAliases(
            { list: AliasesArray?, result: String? ->
                if (list != null) {
                    lifecycleScope.launch {
                        addAliasesToList(domain, list)
                    }
                } else {
                    SnackbarHelper.createSnackbar(
                        this,
                        this.resources.getString(R.string.error_obtaining_aliases) + "\n" + result,
                        binding.activityManageDomainCL,
                        LoggingHelper.LOGFILES.DEFAULT
                    ).show()
                }
            },
            aliasSortFilter = AliasSortFilter(
                onlyActiveAliases = false,
                onlyDeletedAliases = false,
                onlyInactiveAliases = false,
                onlyWatchedAliases = false,
                sort = null,
                sortDesc = false,
                filter = null
            ),
            page = (workingAliasList?.meta?.current_page ?: 0) + 1,
            size = 100,
            domain = domain.id
        )


    }

    private suspend fun addAliasesToList(domain: Domains, aliasesArray: AliasesArray) {
        // If the aliasList is null, completely set it
        if (workingAliasList == null) {
            workingAliasList = aliasesArray
        } else {
            // If not, update meta,links and append aliases
            workingAliasList?.meta = aliasesArray.meta
            workingAliasList?.links = aliasesArray.links
            workingAliasList?.data?.addAll(aliasesArray.data)
        }

        // Check if there are more aliases to obtain (are there more pages)
        // If so, repeat.
        if ((workingAliasList?.meta?.current_page ?: 0) < (workingAliasList?.meta?.last_page ?: 0)) {
            getAliasesAndAddThemToList(domain)
        } else {
            // Else, set aliasList to call updateUi()
            this.aliasList = workingAliasList
            // Clear workingAliasList to free up space
            workingAliasList = null
        }
    }
}