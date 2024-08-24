package host.stjin.anonaddy.ui.usernames.manage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityManageUsernamesBinding
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.InsetUtil
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.AliasesArray
import host.stjin.anonaddy_shared.models.Usernames
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


class ManageUsernamesActivity : BaseActivity(),
    EditUsernameDescriptionBottomDialogFragment.AddEditUsernameDescriptionBottomDialogListener,
    EditUsernameFromNameBottomDialogFragment.AddEditUsernameFromNameBottomDialogListener,
    EditUsernameRecipientBottomDialogFragment.AddEditUsernameRecipientBottomDialogListener,
    EditUsernameAutoCreateRegexBottomDialogFragment.AddEditUsernameAutoCreateRegexBottomDialogListener {

    lateinit var networkHelper: NetworkHelper

    private lateinit var editUsernameDescriptionBottomDialogFragment: EditUsernameDescriptionBottomDialogFragment
    private lateinit var editUsernameRecipientBottomDialogFragment: EditUsernameRecipientBottomDialogFragment
    private lateinit var editUserNameFromNameBottomDialogFragment: EditUsernameFromNameBottomDialogFragment
    private lateinit var editUsernameAutoCreateRegexBottomDialogFragment: EditUsernameAutoCreateRegexBottomDialogFragment


    private var shouldRefreshOnFinish = false
    private var username: Usernames? = null
        set(value) {
            field = value
            value?.let { updateUi(it) }
        }
    private var aliasList: AliasesArray? = null
        set(value) {
            field = value
            value?.let { username?.let { username -> updateUi(username, it) } }
        }

    private var workingAliasList: AliasesArray? = null


    private var forceSwitch = false


    private lateinit var binding: ActivityManageUsernamesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUsernamesBinding.inflate(layoutInflater)
        InsetUtil.applyBottomInset(binding.activityManageUsernameLL1)

        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.edit_username,
            binding.activityManageUsernameNSV,
            binding.activityManageUsernameToolbar
        )
        networkHelper = NetworkHelper(this)


        val b = intent.extras
        val usernameId = b?.getString("username_id")

        if (usernameId == null) {
            finish()
            return
        }

        setPage(usernameId)
    }

    override fun finish() {
        val resultIntent = Intent()
        resultIntent.putExtra("shouldRefresh", shouldRefreshOnFinish)
        setResult(RESULT_OK, resultIntent)
        super.finish()
    }

    private fun setPage(usernameId: String) {
        // Get the username
        lifecycleScope.launch {
            getUsernameInfo(usernameId)
        }
    }

    private fun setOnSwitchChangeListeners() {
        binding.activityManageUsernameActiveSwitchLayout.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    binding.activityManageUsernameActiveSwitchLayout.showProgressBar(true)
                    forceSwitch = false
                    if (checked) {
                        lifecycleScope.launch {
                            activateUsername()
                        }
                    } else {
                        lifecycleScope.launch {
                            deactivateUsername()
                        }
                    }
                }
            }
        })

        binding.activityManageUsernameCatchAllSwitchLayout.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    binding.activityManageUsernameCatchAllSwitchLayout.showProgressBar(true)
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

        binding.activityManageUsernameCanLoginSwitchLayout.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    binding.activityManageUsernameCanLoginSwitchLayout.showProgressBar(true)
                    forceSwitch = false
                    if (checked) {
                        lifecycleScope.launch {
                            enableCanLogin()
                        }
                    } else {
                        lifecycleScope.launch {
                            disableCanLogin()
                        }
                    }
                }
            }
        })
    }


    private suspend fun disableCanLogin() {
        networkHelper.disableCanLoginSpecificUsername({ result ->
            binding.activityManageUsernameCanLoginSwitchLayout.showProgressBar(false)
            if (result == "204") {
                this.username!!.can_login = false
                shouldRefreshOnFinish = true
                updateUi(this.username!!)
            } else {
                binding.activityManageUsernameCanLoginSwitchLayout.setSwitchChecked(true)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_can_login) + "\n" + result,
                    binding.activityManageUsernameCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this.username!!.id)
    }


    private suspend fun enableCanLogin() {
        networkHelper.enableCanLoginSpecificUsername({ username, error ->
            binding.activityManageUsernameCanLoginSwitchLayout.showProgressBar(false)
            if (username != null) {
                this.username = username
                shouldRefreshOnFinish = true
            } else {
                binding.activityManageUsernameCanLoginSwitchLayout.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_can_login) + "\n" + error,
                    binding.activityManageUsernameCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this.username!!.id)
    }

    private suspend fun disableCatchAll() {
        networkHelper.disableCatchAllSpecificUsername({ result ->
            binding.activityManageUsernameCatchAllSwitchLayout.showProgressBar(false)
            if (result == "204") {
                this.username!!.catch_all = false
                shouldRefreshOnFinish = true
                updateUi(this.username!!)
            } else {
                binding.activityManageUsernameCatchAllSwitchLayout.setSwitchChecked(true)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_catch_all) + "\n" + result,
                    binding.activityManageUsernameCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this.username!!.id)
    }


    private suspend fun enableCatchAll() {
        networkHelper.enableCatchAllSpecificUsername({ username, error ->
            binding.activityManageUsernameCatchAllSwitchLayout.showProgressBar(false)
            if (username != null) {
                this.username = username
                shouldRefreshOnFinish = true
            } else {
                binding.activityManageUsernameCatchAllSwitchLayout.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_catch_all) + "\n" + error,
                    binding.activityManageUsernameCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this.username!!.id)
    }

    private suspend fun deactivateUsername() {
        networkHelper.deactivateSpecificUsername({ result ->
            binding.activityManageUsernameActiveSwitchLayout.showProgressBar(false)
            if (result == "204") {
                this.username!!.active = false
                shouldRefreshOnFinish = true
                updateUi(this.username!!)
            } else {
                binding.activityManageUsernameActiveSwitchLayout.setSwitchChecked(true)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    binding.activityManageUsernameCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this.username!!.id)
    }


    private suspend fun activateUsername() {
        networkHelper.activateSpecificUsername({ username, error ->
            binding.activityManageUsernameActiveSwitchLayout.showProgressBar(false)
            if (username != null) {
                this.username = username
                shouldRefreshOnFinish = true
            } else {
                binding.activityManageUsernameActiveSwitchLayout.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + error,
                    binding.activityManageUsernameCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this.username!!.id)
    }


    private fun setOnClickListeners() {
        binding.activityManageUsernameActiveSwitchLayout.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityManageUsernameActiveSwitchLayout.setSwitchChecked(!binding.activityManageUsernameActiveSwitchLayout.getSwitchChecked())
            }
        })

        binding.activityManageUsernameCatchAllSwitchLayout.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityManageUsernameCatchAllSwitchLayout.setSwitchChecked(!binding.activityManageUsernameCatchAllSwitchLayout.getSwitchChecked())
            }
        })

        binding.activityManageUsernameCanLoginSwitchLayout.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityManageUsernameCanLoginSwitchLayout.setSwitchChecked(!binding.activityManageUsernameCanLoginSwitchLayout.getSwitchChecked())
            }
        })

        binding.activityManageUsernameDescEdit.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editUsernameDescriptionBottomDialogFragment.isAdded) {
                    editUsernameDescriptionBottomDialogFragment.show(
                        supportFragmentManager,
                        "editUsernameDescriptionBottomDialogFragment"
                    )
                }
            }
        })


        binding.activityManageUsernameRecipientsEdit.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editUsernameRecipientBottomDialogFragment.isAdded) {
                    editUsernameRecipientBottomDialogFragment.show(
                        supportFragmentManager,
                        "editUsernameRecipientsBottomDialogFragment"
                    )
                }
            }
        })
        binding.activityManageUsernameFromNameEdit.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editUserNameFromNameBottomDialogFragment.isAdded) {
                    editUserNameFromNameBottomDialogFragment.show(
                        supportFragmentManager,
                        "editUserNamFromNameBottomDialogFragment"
                    )
                }
            }
        })

        binding.activityManageUsernameAutoCreateRegexEdit.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editUsernameAutoCreateRegexBottomDialogFragment.isAdded) {
                    editUsernameAutoCreateRegexBottomDialogFragment.show(
                        supportFragmentManager,
                        "editUsernameAutoCreateRegexBottomDialogFragment"
                    )
                }
            }
        })


        binding.activityManageUsernameDelete.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                deleteUsername(this@ManageUsernamesActivity.username!!.id)
            }
        })
    }


    private lateinit var deleteUsernameSnackbar: Snackbar
    private fun deleteUsername(id: String) {
        MaterialDialogHelper.showMaterialDialog(
            context = this,
            title = resources.getString(R.string.delete_username),
            message = resources.getString(R.string.delete_username_desc_confirm),
            icon = R.drawable.ic_trash,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.delete),
            positiveButtonAction = {
                deleteUsernameSnackbar = SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.deleting_username),
                    binding.activityManageUsernameCL,
                    length = Snackbar.LENGTH_INDEFINITE
                )
                deleteUsernameSnackbar.show()
                lifecycleScope.launch {
                    deleteUsernameHttpRequest(id, this@ManageUsernamesActivity)
                }
            }
        ).show()
    }


    private suspend fun deleteUsernameHttpRequest(id: String, context: Context) {
        networkHelper.deleteUsername({ result ->
            if (result == "204") {
                deleteUsernameSnackbar.dismiss()
                shouldRefreshOnFinish = true
                finish()
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_deleting_username), result
                    ),
                    binding.activityManageUsernameCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, id)
    }


    private suspend fun getUsernameInfo(id: String) {
        networkHelper.getSpecificUsername({ username, error ->

            if (username != null) {
                // Triggers updateUi
                this.username = username

                // Now that we have the username, obtain the aliases separately
                lifecycleScope.launch {
                    getAliasesAndAddThemToList(username)
                }

            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_obtaining_username) + "\n" + error,
                    binding.activityManageUsernameCL
                ).show()

                // Show error animations
                binding.activityManageUsernameLL1.visibility = View.GONE
                binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
            }
        }, id)
    }

    private fun updateUi(username: Usernames, aliasesArray: AliasesArray? = null) {
        /**
         *  SWITCH STATUS
         */

        binding.activityManageUsernameActiveSwitchLayout.setSwitchChecked(username.active)
        binding.activityManageUsernameActiveSwitchLayout.setTitle(
            if (username.active) resources.getString(R.string.username_activated) else resources.getString(R.string.username_deactivated)
        )

        binding.activityManageUsernameCatchAllSwitchLayout.setSwitchChecked(username.catch_all)
        binding.activityManageUsernameCatchAllSwitchLayout.setTitle(
            if (username.catch_all) resources.getString(R.string.catch_all_enabled) else resources.getString(R.string.catch_all_disabled)
        )

        binding.activityManageUsernameCanLoginSwitchLayout.setSwitchChecked(username.can_login)
        binding.activityManageUsernameCanLoginSwitchLayout.setTitle(
            if (username.can_login) resources.getString(R.string.can_login_enabled) else resources.getString(R.string.can_login_disabled)
        )

        /**
         * TEXT
         */

        var totalForwarded = 0
        var totalBlocked = 0
        var totalReplies = 0
        var totalSent = 0
        val totalAliases = username.aliases_count
        var aliases = ""

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
            binding.activityManageUsernameAliasesTextview.text = aliases
            binding.activityManageUsernameAliasesShimmerframelayout.hideShimmer()
            binding.activityManageUsernameBasicShimmerframelayout.hideShimmer() // Stop shimmer only after this info is loaded

        }

        binding.activityManageUsernameBasicTextview.text = resources.getString(
            R.string.manage_username_basic_info,
            username.username,
            DateTimeUtils.turnStringIntoLocalString(username.created_at),
            DateTimeUtils.turnStringIntoLocalString(username.updated_at),
            totalForwarded, totalBlocked, totalReplies, totalSent
        )

        binding.activityManageUsernameAliasesTitleTextview.text = resources.getString(R.string.username_aliases_d, totalAliases)


        /**
         * RECIPIENTS
         */

        // Set recipient
        val recipients: String = username.default_recipient?.email ?: this.resources.getString(
            R.string.default_recipient_s, (this.application as AddyIoApp).userResourceExtended.default_recipient_email
        )

        binding.activityManageUsernameRecipientsEdit.setDescription(recipients)


        // Set this value as it now includes the default email
        editUsernameRecipientBottomDialogFragment =
            EditUsernameRecipientBottomDialogFragment.newInstance(this.username!!.id, username.default_recipient?.email)


        /**
         * DESCRIPTION
         */

        // Set description and initialise the bottomDialogFragment
        if (username.description != null) {
            binding.activityManageUsernameDescEdit.setDescription(username.description)
        } else {
            binding.activityManageUsernameDescEdit.setDescription(
                this.resources.getString(
                    R.string.username_no_description
                )
            )
        }

        // Set this value as it now includes the description
        editUsernameDescriptionBottomDialogFragment = EditUsernameDescriptionBottomDialogFragment.newInstance(
            this.username!!.id,
            username.description
        )

        /**
         * FROM NAME
         */


        // Not available for free subscriptions
        if ((this.application as AddyIoApp).userResource.hasUserFreeSubscription) {
            binding.activityManageUsernameFromNameEdit.setLayoutEnabled(false)
            binding.activityManageUsernameFromNameEdit.setDescription(
                this.resources.getString(
                    R.string.feature_not_available_subscription
                )
            )
        } else {
            // Set description and initialise the bottomDialogFragment
            if (username.from_name != null) {
                binding.activityManageUsernameFromNameEdit.setDescription(username.from_name)
            } else {
                binding.activityManageUsernameFromNameEdit.setDescription(
                    this.resources.getString(
                        R.string.username_no_from_name
                    )
                )
            }

            // reset this value as it now includes the description
            editUserNameFromNameBottomDialogFragment = EditUsernameFromNameBottomDialogFragment.newInstance(
                username.id,
                username.username,
                username.from_name
            )


        }

        /**
         * AUTO CREATE REGEX
         */


        // Not available for free subscriptions
        if ((this.application as AddyIoApp).userResource.hasUserFreeSubscription) {
            binding.activityManageUsernameAutoCreateRegexEdit.setLayoutEnabled(false)
            binding.activityManageUsernameAutoCreateRegexEdit.setDescription(
                this.resources.getString(
                    R.string.feature_not_available_subscription
                )
            )
        } else {
            // Set description and initialise the bottomDialogFragment
            if (username.auto_create_regex != null) {
                binding.activityManageUsernameAutoCreateRegexEdit.setDescription(username.auto_create_regex)
            } else {
                binding.activityManageUsernameAutoCreateRegexEdit.setDescription(
                    this.resources.getString(
                        R.string.username_no_auto_create_regex
                    )
                )
            }

            // reset this value as it now includes the description
            editUsernameAutoCreateRegexBottomDialogFragment = EditUsernameAutoCreateRegexBottomDialogFragment.newInstance(
                username.id,
                username.auto_create_regex,
            )


        }


        binding.animationFragment.stopAnimation()
        binding.activityManageUsernameNSV.animate().alpha(1.0f)
        setOnSwitchChangeListeners()
        setOnClickListeners()
    }


    override fun descriptionEdited(username: Usernames) {
        editUsernameDescriptionBottomDialogFragment.dismissAllowingStateLoss()

        // Do this last, will trigger updateUI as well as re-init editUsernameDescriptionBottomDialogFragment
        this.username = username
    }

    override fun recipientEdited(username: Usernames) {
        editUsernameRecipientBottomDialogFragment.dismissAllowingStateLoss()

        // Do this last, will trigger updateUI as well as re-init editUsernameRecipientBottomDialogFragment
        this.username = username
    }

    override fun fromNameEdited(username: Usernames) {
        editUserNameFromNameBottomDialogFragment.dismissAllowingStateLoss()

        // Do this last, will trigger updateUI as well as re-init editUserNameFromNameBottomDialogFragment
        this.username = username
    }



    override fun autoCreateRegexEdited(username: Usernames) {
        editUsernameAutoCreateRegexBottomDialogFragment.dismissAllowingStateLoss()

        // Do this last, will trigger updateUI as well as re-init editUsernameAutoCreateRegexBottomDialogFragment
        this.username = username
    }


    private suspend fun getAliasesAndAddThemToList(username: Usernames) {
        binding.activityManageUsernameAliasesShimmerframelayout.startShimmer()

        networkHelper.getAliases(
            { list: AliasesArray?, result: String? ->
                if (list != null) {
                    lifecycleScope.launch {
                        addAliasesToList(username, list)
                    }
                } else {
                    SnackbarHelper.createSnackbar(
                        this,
                        this.resources.getString(R.string.error_obtaining_aliases) + "\n" + result,
                        binding.activityManageUsernameCL,
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
            username = username.id
        )


    }

    private suspend fun addAliasesToList(username: Usernames, aliasesArray: AliasesArray) {
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
            getAliasesAndAddThemToList(username)
        } else {
            // Else, set aliasList to call updateUi()
            this.aliasList = workingAliasList
            // Clear workingAliasList to free up space
            workingAliasList = null
        }
    }
}