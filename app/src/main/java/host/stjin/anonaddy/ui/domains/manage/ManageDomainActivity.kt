package host.stjin.anonaddy.ui.domains.manage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.databinding.ActivityManageDomainBinding
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.repositories.AliasRepository
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


import androidx.activity.viewModels

class ManageDomainActivity : BaseActivity(),
    EditDomainDescriptionBottomDialogFragment.AddEditDomainDescriptionBottomDialogListener,
    EditDomainFromNameBottomDialogFragment.AddEditDomainFromNameBottomDialogListener,
    EditDomainRecipientBottomDialogFragment.AddEditDomainRecipientBottomDialogListener,
    EditDomainAutoCreateRegexBottomDialogFragment.AddEditDomainAutoCreateRegexBottomDialogListener {

    private lateinit var binding: ActivityManageDomainBinding
    private val viewModel: ManageDomainViewModel by viewModels()
    private lateinit var aliasRepository: AliasRepository

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

    private var aliasList: PaginatedResponse<Aliases>? = null
        set(value) {
            field = value
            value?.let { domain?.let { domain -> updateUi(domain, it) } }
        }

    private var workingAliasList: PaginatedResponse<Aliases>? = null
    private var aliasesEmailList: ArrayList<String> = arrayListOf()

    private var isAliasesExpanded = false
    private var forceSwitch = false

    private lateinit var deleteDomainSnackbar: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageDomainBinding.inflate(layoutInflater)
        InsetUtils.applyBottomInset(binding.activityManageDomainLL1)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.edit_domain,
            binding.activityManageDomainNSV,
            binding.activityManageDomainToolbar
        )
        aliasRepository = ServiceLocator.aliasRepository
        setRefreshLayout()

        val b = intent.extras
        val domainId = b?.getString("domain_id")

        if (domainId == null) {
            finish()
            return
        }
        setPage(domainId)
    }

    private fun setOnClickListeners() {
        binding.activityManageDomainActiveSwitchLayout.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityManageDomainActiveSwitchLayout.setSwitchChecked(!binding.activityManageDomainActiveSwitchLayout.getSwitchChecked())
        }

        binding.activityManageDomainCatchAllSwitchLayout.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityManageDomainCatchAllSwitchLayout.setSwitchChecked(!binding.activityManageDomainCatchAllSwitchLayout.getSwitchChecked())
        }

        binding.activityManageDomainSharedWithFamilySwitchLayout.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityManageDomainSharedWithFamilySwitchLayout.setSwitchChecked(!binding.activityManageDomainSharedWithFamilySwitchLayout.getSwitchChecked())
        }

        binding.activityManageDomainDescEdit.setOnLayoutClickedListener {
            if (!editDomainDescriptionBottomDialogFragment.isAdded) {
                editDomainDescriptionBottomDialogFragment.show(
                    supportFragmentManager,
                    "editDomainDescriptionBottomDialogFragment"
                )
            }
        }


        binding.activityManageDomainRecipientsEdit.setOnLayoutClickedListener {
            if (!editDomainRecipientBottomDialogFragment.isAdded) {
                editDomainRecipientBottomDialogFragment.show(
                    supportFragmentManager,
                    "editDomainRecipientsBottomDialogFragment"
                )
            }
        }

        binding.activityManageDomainFromNameEdit.setOnLayoutClickedListener {
            if (!editDomainFromNameBottomDialogFragment.isAdded) {
                editDomainFromNameBottomDialogFragment.show(
                    supportFragmentManager,
                    "editDomainFromNameBottomDialogFragment"
                )
            }
        }

        binding.activityManageDomainAutoCreateRegexEdit.setOnLayoutClickedListener {
            if (!editDomainAutoCreateRegexBottomDialogFragment.isAdded) {
                editDomainAutoCreateRegexBottomDialogFragment.show(
                    supportFragmentManager,
                    "editDomainAutoCreateRegexBottomDialogFragment"
                )
            }
        }


        binding.activityManageDomainDelete.setOnLayoutClickedListener { deleteDomain(this@ManageDomainActivity.domain!!.id) }

        binding.activityManageDomainCheckDns.setOnLayoutClickedListener {
            val url = "${AddyIo.API_BASE_URL}/domains"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = url.toUri()
            startActivity(i)
        }
        binding.activityManageDomainAliasesShowMoreLessButton.setOnClickListener {
            isAliasesExpanded = !isAliasesExpanded
            updateAliasesView()
        }
    }

    override fun finish() {
        val resultIntent = Intent()
        resultIntent.putExtra("shouldRefresh", shouldRefreshOnFinish)
        setResult(RESULT_OK, resultIntent)
        super.finish()
    }

    override fun descriptionEdited(domain: Domains) {
        editDomainDescriptionBottomDialogFragment.dismissAllowingStateLoss()
        shouldRefreshOnFinish = true

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

    private fun setRefreshLayout() {
        binding.activityManageDomainSwiperefresh.setOnRefreshListener {
            binding.activityManageDomainSwiperefresh.isRefreshing = true

            domain?.let { setPage(it.id) }
        }
    }

    private fun setPage(domainId: String) {
        // Get the domain
        lifecycleScope.launch {
            getDomainInfo(domainId)
        }
    }

    private fun setOnSwitchChangeListeners() {
        binding.activityManageDomainActiveSwitchLayout.setOnSwitchCheckedChangedListener { compoundButton, checked -> // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
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

        binding.activityManageDomainCatchAllSwitchLayout.setOnSwitchCheckedChangedListener { compoundButton, checked -> // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
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

        binding.activityManageDomainSharedWithFamilySwitchLayout.setOnSwitchCheckedChangedListener { compoundButton, checked -> // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
            if (compoundButton.isPressed || forceSwitch) {
                binding.activityManageDomainSharedWithFamilySwitchLayout.showProgressBar(true)
                forceSwitch = false
                if (checked) {
                    lifecycleScope.launch {
                        enableSharedWithFamily()
                    }
                } else {
                    lifecycleScope.launch {
                        disableSharedWithFamily()
                    }
                }
            }
        }
    }

    private suspend fun disableSharedWithFamily() {
        val result = viewModel.disableSharedWithFamilyDomain(this.domain!!.id)
        binding.activityManageDomainSharedWithFamilySwitchLayout.showProgressBar(false)
        if (result is NetworkResult.Success && result.data == "204") {
            this.domain!!.shared_with_family = false
            shouldRefreshOnFinish = true
            updateUi(this.domain!!)
        } else {
            binding.activityManageDomainSharedWithFamilySwitchLayout.setSwitchChecked(true)
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_edit_shared_with_family) + "\n" + (result.errorOrNull() ?: ""),
                binding.activityManageDomainCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun enableSharedWithFamily() {
        val result = viewModel.enableSharedWithFamilyDomain(this.domain!!.id)
        binding.activityManageDomainSharedWithFamilySwitchLayout.showProgressBar(false)
        if (result is NetworkResult.Success) {
            this.domain = result.data
            shouldRefreshOnFinish = true
        } else {
            binding.activityManageDomainSharedWithFamilySwitchLayout.setSwitchChecked(false)
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_edit_shared_with_family) + "\n" + (result.errorOrNull() ?: ""),
                binding.activityManageDomainCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun disableCatchAll() {
        val result = viewModel.disableCatchAllDomain(this.domain!!.id)
        binding.activityManageDomainCatchAllSwitchLayout.showProgressBar(false)
        if (result is NetworkResult.Success && result.data == "204") {
            this.domain!!.catch_all = false
            shouldRefreshOnFinish = true
            updateUi(this.domain!!)
        } else {
            binding.activityManageDomainCatchAllSwitchLayout.setSwitchChecked(true)
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_edit_catch_all) + "\n" + (result.errorOrNull() ?: ""),
                binding.activityManageDomainCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun enableCatchAll() {
        val result = viewModel.enableCatchAllDomain(this.domain!!.id)
        binding.activityManageDomainCatchAllSwitchLayout.showProgressBar(false)
        if (result is NetworkResult.Success) {
            this.domain = result.data
            shouldRefreshOnFinish = true
        } else {
            binding.activityManageDomainCatchAllSwitchLayout.setSwitchChecked(false)
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_edit_catch_all) + "\n" + (result.errorOrNull() ?: ""),
                binding.activityManageDomainCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun deactivateDomain() {
        val result = viewModel.deactivateDomain(this.domain!!.id)
        binding.activityManageDomainActiveSwitchLayout.showProgressBar(false)
        if (result is NetworkResult.Success && result.data == "204") {
            this.domain!!.active = false
            shouldRefreshOnFinish = true
            updateUi(this.domain!!)
        } else {
            binding.activityManageDomainActiveSwitchLayout.setSwitchChecked(true)
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_edit_active) + "\n" + (result.errorOrNull() ?: ""),
                binding.activityManageDomainCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun activateDomain() {
        val result = viewModel.activateDomain(this.domain!!.id)
        binding.activityManageDomainActiveSwitchLayout.showProgressBar(false)
        if (result is NetworkResult.Success) {
            this.domain = result.data
            shouldRefreshOnFinish = true
        } else {
            binding.activityManageDomainActiveSwitchLayout.setSwitchChecked(false)
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_edit_active) + "\n" + (result.errorOrNull() ?: ""),
                binding.activityManageDomainCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

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
                    deleteDomainHttpRequest(id, this@ManageDomainActivity)
                }
            }
        ).show()
    }

    private suspend fun deleteDomainHttpRequest(id: String, context: Context) {
        val result = viewModel.deleteDomain(id)
        if (result is NetworkResult.Success && result.data == "204") {
            deleteDomainSnackbar.dismiss()
            shouldRefreshOnFinish = true
            finish()
        } else {
            SnackbarHelper.createSnackbar(
                this,
                context.resources.getString(R.string.s_s, context.resources.getString(R.string.error_deleting_domain), result.errorOrNull() ?: ""),
                binding.activityManageDomainCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun getDomainInfo(id: String) {
        val result = viewModel.getDomain(id)
        if (result is NetworkResult.Success) {
            val domain = result.data
            // Triggers updateUi
            this.domain = domain

            // Now that we have the domain, obtain the aliases separately
            lifecycleScope.launch {
                getAliasesAndAddThemToList(domain)
            }
        } else {
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_obtaining_domains) + "\n" + (result.errorOrNull() ?: ""),
                binding.activityManageDomainCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()

            // Show error animations
            binding.activityManageDomainLL1.visibility = View.GONE
            binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
        }

        binding.activityManageDomainSwiperefresh.isRefreshing = false
    }

    private fun updateUi(domain: Domains, aliasesArray: PaginatedResponse<Aliases>? = null) {
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

        if (AddyIo.isUsingHostedInstance) {
            binding.activityManageDomainSharedWithFamilySwitchLayout.visibility = View.VISIBLE
            binding.activityManageDomainSharedWithFamilySwitchLayout.setSwitchChecked(domain.shared_with_family)
            val userResource = (this.application as AddyIoApp).userResource
            val hasFamilyPlanRole = !userResource.family_plan_role.isNullOrEmpty()
            if (hasFamilyPlanRole) {
                binding.activityManageDomainSharedWithFamilySwitchLayout.setLayoutEnabled(true)
                binding.activityManageDomainSharedWithFamilySwitchLayout.setDescription(
                    this.resources.getString(R.string.share_domain_with_family_desc)
                )
            } else {
                binding.activityManageDomainSharedWithFamilySwitchLayout.setLayoutEnabled(false)
                binding.activityManageDomainSharedWithFamilySwitchLayout.setDescription(
                    this.resources.getString(R.string.feature_not_available_subscription)
                )
            }
        } else {
            binding.activityManageDomainSharedWithFamilySwitchLayout.visibility = View.GONE
        }

        /**
         * TEXT
         */


        var totalForwarded = 0
        var totalBlocked = 0
        var totalReplies = 0
        var totalSent = 0

        if (aliasesArray != null) {
            binding.activityManageDomainAliasesCountTextview.apply {
                val total = aliasesArray.meta?.total ?: aliasesArray.data.size
                if (total > 0) {
                    text = String.format(java.util.Locale.getDefault(), "%d", total)
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }

            val emails = mutableListOf<String>()
            aliasesArray.data = ArrayList(aliasesArray.data.sortedBy { it.email })
            for (alias in aliasesArray.data) {
                totalForwarded += alias.emails_forwarded
                totalBlocked += alias.emails_blocked
                totalReplies += alias.emails_replied
                totalSent += alias.emails_sent
                emails.add(alias.email)
            }
            aliasesEmailList = ArrayList(emails)
            updateAliasesView()
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
        if ((this.application as? AddyIoApp)?.userResourceOrNull?.hasUserFreeSubscription == true) {
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
        if ((this.application as? AddyIoApp)?.userResourceOrNull?.hasUserFreeSubscription == true) {
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

    private fun updateAliasesView() {
        if (aliasesEmailList.size > 10) {
            binding.activityManageDomainAliasesShowMoreLessButton.visibility = View.VISIBLE
            if (isAliasesExpanded) {
                binding.activityManageDomainAliasesTextview.text = aliasesEmailList.joinToString("\n")
                binding.activityManageDomainAliasesShowMoreLessButton.text = getString(R.string.show_less)
            } else {
                binding.activityManageDomainAliasesTextview.text = aliasesEmailList.take(10).joinToString("\n")
                binding.activityManageDomainAliasesShowMoreLessButton.text = getString(R.string.show_more)
            }
        } else {
            binding.activityManageDomainAliasesShowMoreLessButton.visibility = View.GONE
            binding.activityManageDomainAliasesTextview.text = aliasesEmailList.joinToString("\n")
        }
    }

    private suspend fun getAliasesAndAddThemToList(domain: Domains) {
        binding.activityManageDomainAliasesShimmerframelayout.startShimmer()

        val result = aliasRepository.getAliases(
            aliasSortFilter = AliasSortFilter(
                onlyActiveAliases = false,
                onlyDeletedAliases = false,
                onlyInactiveAliases = false,
                onlyWatchedAliases = false,
                onlyPinnedAliases = false,
                sort = null,
                sortDesc = false,
                filter = null
            ),
            page = (workingAliasList?.meta?.current_page ?: 0) + 1,
            size = 100,
            domain = domain.id
        )

        when (result) {
            is NetworkResult.Success -> {
                addAliasesToList(domain, result.data)
            }
            is NetworkResult.Error -> {
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_obtaining_aliases) + "\n" + result.error,
                    binding.activityManageDomainCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }
    }

    private suspend fun addAliasesToList(domain: Domains, aliasesArray: PaginatedResponse<Aliases>) {
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
