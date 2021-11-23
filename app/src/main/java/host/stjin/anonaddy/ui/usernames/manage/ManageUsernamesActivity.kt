package host.stjin.anonaddy.ui.usernames.manage

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.AnonAddyForAndroid.User
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityManageUsernamesBinding
import host.stjin.anonaddy.databinding.AnonaddyCustomDialogBinding
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.AttributeHelper
import host.stjin.anonaddy.utils.DateTimeUtils
import host.stjin.anonaddy.utils.LoggingHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import kotlinx.coroutines.launch


class ManageUsernamesActivity : BaseActivity(),
    EditUsernameDescriptionBottomDialogFragment.AddEditUsernameDescriptionBottomDialogListener,
    EditUsernameRecipientBottomDialogFragment.AddEditUsernameRecipientBottomDialogListener {

    lateinit var networkHelper: NetworkHelper

    private lateinit var editUsernameDescriptionBottomDialogFragment: EditUsernameDescriptionBottomDialogFragment
    private lateinit var editUsernameRecipientBottomDialogFragment: EditUsernameRecipientBottomDialogFragment

    private lateinit var usernameId: String
    private var forceSwitch = false


    private lateinit var binding: ActivityManageUsernamesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUsernamesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(view, binding.activityManageUsernameNSVRL)

        setupToolbar(binding.activityManageUsernameToolbar.customToolbarOneHandedMaterialtoolbar, R.string.edit_username)
        networkHelper = NetworkHelper(this)


        val b = intent.extras
        val usernameId = b?.getString("username_id")

        if (usernameId == null) {
            finish()
            return
        }
        this.usernameId = usernameId
        setPage()
    }


    private fun setPage() {
        binding.activityManageUsernameRLLottieview.visibility = View.GONE
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
    }

    private suspend fun deactivateUsername() {
        networkHelper.deactivateSpecificUsername({ result ->
            binding.activityManageUsernameActiveSwitchLayout.showProgressBar(false)
            if (result == "204") {
                binding.activityManageUsernameActiveSwitchLayout.setTitle(resources.getString(R.string.username_deactivated))
            } else {
                binding.activityManageUsernameActiveSwitchLayout.setSwitchChecked(true)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    binding.activityManageUsernameCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, usernameId)
    }


    private suspend fun activateUsername() {
        networkHelper.activateSpecificUsername({ result ->
            binding.activityManageUsernameActiveSwitchLayout.showProgressBar(false)
            if (result == "200") {
                binding.activityManageUsernameActiveSwitchLayout.setTitle(resources.getString(R.string.username_activated))
            } else {
                binding.activityManageUsernameActiveSwitchLayout.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    binding.activityManageUsernameCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, usernameId)
    }


    private fun setOnClickListeners() {
        binding.activityManageUsernameActiveSwitchLayout.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityManageUsernameActiveSwitchLayout.setSwitchChecked(!binding.activityManageUsernameActiveSwitchLayout.getSwitchChecked())
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


        binding.activityManageUsernameDelete.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                deleteUsername(usernameId)
            }
        })
    }


    private lateinit var deleteUsernameDialog: AlertDialog
    private fun deleteUsername(id: String) {
        val anonaddyCustomDialogBinding = AnonaddyCustomDialogBinding.inflate(LayoutInflater.from(this), null, false)

// create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(anonaddyCustomDialogBinding.root)
        deleteUsernameDialog = builder.create()
        deleteUsernameDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        anonaddyCustomDialogBinding.dialogTitle.text = resources.getString(R.string.delete_username)
        anonaddyCustomDialogBinding.dialogText.text = resources.getString(R.string.delete_username_desc_confirm)
        anonaddyCustomDialogBinding.dialogPositiveButton.text =
            resources.getString(R.string.delete)
        anonaddyCustomDialogBinding.dialogPositiveButton.drawableBackground.setColorFilter(
            AttributeHelper.getValueByAttr(this, R.attr.colorError),
            PorterDuff.Mode.SRC_ATOP
        )
        anonaddyCustomDialogBinding.dialogPositiveButton.setTextColor(AttributeHelper.getValueByAttr(this, R.attr.colorOnError))
        anonaddyCustomDialogBinding.dialogPositiveButton.spinningBarColor = AttributeHelper.getValueByAttr(this, R.attr.colorOnError)

        anonaddyCustomDialogBinding.dialogPositiveButton.setOnClickListener {
            // Animate the button to progress
            anonaddyCustomDialogBinding.dialogPositiveButton.startAnimation()

            anonaddyCustomDialogBinding.dialogError.visibility = View.GONE
            anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = false
            anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = false

            lifecycleScope.launch {
                deleteUsernameHttpRequest(id, this@ManageUsernamesActivity, anonaddyCustomDialogBinding)
            }
        }
        anonaddyCustomDialogBinding.dialogNegativeButton.setOnClickListener {
            deleteUsernameDialog.dismiss()
        }
        // create and show the alert dialog
        deleteUsernameDialog.show()
    }


    private suspend fun deleteUsernameHttpRequest(id: String, context: Context, anonaddyCustomDialogBinding: AnonaddyCustomDialogBinding) {
        networkHelper.deleteUsername({ result ->
            if (result == "204") {
                deleteUsernameDialog.dismiss()
                finish()
            } else {
                // Revert the button to normal
                anonaddyCustomDialogBinding.dialogPositiveButton.revertAnimation()

                anonaddyCustomDialogBinding.dialogError.visibility = View.VISIBLE
                anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogError.text = context.resources.getString(
                    R.string.s_s,
                    context.resources.getString(R.string.error_deleting_username), result
                )
            }
        }, id)
    }


    private suspend fun getUsernameInfo(id: String) {
        networkHelper.getSpecificUsername({ list ->

            if (list != null) {
                /**
                 *  SWITCH STATUS
                 */

                binding.activityManageUsernameActiveSwitchLayout.setSwitchChecked(list.active)
                binding.activityManageUsernameActiveSwitchLayout.setTitle(
                    if (list.active) resources.getString(R.string.username_activated) else resources.getString(R.string.username_deactivated)
                )


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

                binding.activityManageUsernameAliasesTitleTextview.text = resources.getString(R.string.username_aliases_d, totalAliases)
                binding.activityManageUsernameBasicTextview.text = resources.getString(
                    R.string.manage_username_basic_info,
                    list.username,
                    DateTimeUtils.turnStringIntoLocalString(list.created_at),
                    DateTimeUtils.turnStringIntoLocalString(list.updated_at),
                    totalForwarded, totalBlocked, totalReplies, totalSent
                )

                binding.activityManageUsernameAliasesTextview.text = aliases

                /**
                 * RECIPIENTS
                 */

                // Set recipient
                val recipients: String = list.default_recipient?.email ?: this.resources.getString(
                    R.string.default_recipient_s, User.userResourceExtended.default_recipient_email
                )

                binding.activityManageUsernameRecipientsEdit.setDescription(recipients)


                // Set this value as it now includes the default email
                editUsernameRecipientBottomDialogFragment =
                    EditUsernameRecipientBottomDialogFragment.newInstance(usernameId, list.default_recipient?.email)


                /**
                 * DESCRIPTION
                 */

                // Set description and initialise the bottomDialogFragment
                if (list.description != null) {
                    binding.activityManageUsernameDescEdit.setDescription(list.description)
                } else {
                    binding.activityManageUsernameDescEdit.setDescription(
                        this.resources.getString(
                            R.string.username_no_description
                        )
                    )
                }

                // Set this value as it now includes the description
                editUsernameDescriptionBottomDialogFragment = EditUsernameDescriptionBottomDialogFragment.newInstance(
                    id,
                    list.description
                )


                binding.activityManageUsernameRLProgressbar.visibility = View.GONE
                binding.activityManageUsernameLL1.visibility = View.VISIBLE

                setOnSwitchChangeListeners()
                setOnClickListeners()
            } else {
                binding.activityManageUsernameRLProgressbar.visibility = View.GONE
                binding.activityManageUsernameLL1.visibility = View.GONE

                // Show no internet animations
                binding.activityManageUsernameRLLottieview.visibility = View.VISIBLE
            }
        }, id)
    }


    override fun descriptionEdited(description: String) {
        setPage()
        editUsernameDescriptionBottomDialogFragment.dismiss()
    }

    override fun recipientEdited() {
        setPage()
        editUsernameRecipientBottomDialogFragment.dismiss()
    }
}