package host.stjin.anonaddy.ui.recipients.manage

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
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityManageRecipientsBinding
import host.stjin.anonaddy.databinding.AnonaddyCustomDialogBinding
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.AttributeHelper
import host.stjin.anonaddy.utils.DateTimeUtils
import host.stjin.anonaddy.utils.LoggingHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import kotlinx.coroutines.launch


class ManageRecipientsActivity : BaseActivity(),
    AddRecipientPublicGpgKeyBottomDialogFragment.AddEditGpgKeyBottomDialogListener {

    lateinit var networkHelper: NetworkHelper

    private lateinit var addRecipientPublicGpgKeyBottomDialogFragment: AddRecipientPublicGpgKeyBottomDialogFragment

    private lateinit var recipientId: String
    private var forceSwitch = false


    private lateinit var binding: ActivityManageRecipientsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageRecipientsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(view, binding.activityManageRecipientNSVRL)

        setupToolbar(binding.activityManageRecipientToolbar.customToolbarOneHandedMaterialtoolbar, R.string.edit_recipient)
        networkHelper = NetworkHelper(this)


        val b = intent.extras
        val recipientId = b?.getString("recipient_id")

        if (recipientId == null) {
            finish()
            return
        }
        this.recipientId = recipientId

        setPage()
    }


    private fun setPage() {
        binding.activityManageRecipientRLLottieview.visibility = View.GONE

        // Get the recipient
        lifecycleScope.launch {
            getRecipientInfo(recipientId)
        }
    }

    private fun setOnSwitchChangeListeners(fingerprint: String?) {

        binding.activityManageRecipientActive.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    binding.activityManageRecipientActive.showProgressBar(true)
                    forceSwitch = false

                    if (checked) {
                        if (fingerprint != null) {
                            lifecycleScope.launch {
                                enableEncryption()
                            }
                        } else {
                            binding.activityManageRecipientActive.showProgressBar(false)
                            binding.activityManageRecipientActive.setSwitchChecked(false)
                            if (!addRecipientPublicGpgKeyBottomDialogFragment.isAdded) {
                                addRecipientPublicGpgKeyBottomDialogFragment.show(
                                    supportFragmentManager,
                                    "editrecipientDescriptionBottomDialogFragment"
                                )
                            }
                        }
                    } else {
                        lifecycleScope.launch {
                            disableEncryption()
                        }
                    }
                }
            }
        })
    }


    private suspend fun disableEncryption() {
        networkHelper.disableEncryptionRecipient({ result ->
            binding.activityManageRecipientActive.showProgressBar(false)
            if (result == "204") {
                binding.activityManageRecipientActive.setTitle(resources.getString(R.string.encryption_disabled))
            } else {
                binding.activityManageRecipientActive.setSwitchChecked(true)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    binding.activityManageRecipientCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, recipientId)
    }


    private suspend fun enableEncryption() {
        networkHelper.enableEncryptionRecipient({ result ->
            binding.activityManageRecipientActive.showProgressBar(false)
            if (result == "200") {
                binding.activityManageRecipientActive.setTitle(resources.getString(R.string.encryption_enabled))
            } else {
                binding.activityManageRecipientActive.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    binding.activityManageRecipientCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, recipientId)
    }


    private fun setOnClickListeners() {
        binding.activityManageRecipientChangeGpgKey.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!addRecipientPublicGpgKeyBottomDialogFragment.isAdded) {
                    addRecipientPublicGpgKeyBottomDialogFragment.show(
                        supportFragmentManager,
                        "editrecipientDescriptionBottomDialogFragment"
                    )
                }
            }
        })

        binding.activityManageRecipientRemoveGpgKey.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                removeGpgKey(recipientId)
            }
        })

        binding.activityManageRecipientDelete.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                deleteRecipient(recipientId)
            }
        })

        binding.activityManageRecipientActive.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityManageRecipientActive.setSwitchChecked(!binding.activityManageRecipientActive.getSwitchChecked())
            }
        })

    }


    private lateinit var removeGpgKeyDialog: AlertDialog
    private fun removeGpgKey(id: String) {
        val anonaddyCustomDialogBinding = AnonaddyCustomDialogBinding.inflate(LayoutInflater.from(this), null, false)

// create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(anonaddyCustomDialogBinding.root)
        removeGpgKeyDialog = builder.create()
        removeGpgKeyDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        anonaddyCustomDialogBinding.dialogTitle.text = resources.getString(R.string.remove_public_key)
        anonaddyCustomDialogBinding.dialogText.text = resources.getString(R.string.remove_public_key_desc)
        anonaddyCustomDialogBinding.dialogPositiveButton.text =
            resources.getString(R.string.remove)
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
                removeGpgKeyHttpRequest(id, this@ManageRecipientsActivity, anonaddyCustomDialogBinding)
            }
        }
        anonaddyCustomDialogBinding.dialogNegativeButton.setOnClickListener {
            removeGpgKeyDialog.dismiss()
        }
        // create and show the alert dialog
        removeGpgKeyDialog.show()
    }

    private lateinit var deleteRecipientDialog: AlertDialog
    private fun deleteRecipient(id: String) {
        val anonaddyCustomDialogBinding = AnonaddyCustomDialogBinding.inflate(LayoutInflater.from(this), null, false)

        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(anonaddyCustomDialogBinding.root)
        deleteRecipientDialog = builder.create()
        deleteRecipientDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        anonaddyCustomDialogBinding.dialogTitle.text = resources.getString(R.string.delete_recipient)
        anonaddyCustomDialogBinding.dialogText.text = resources.getString(R.string.delete_recipient_desc)
        anonaddyCustomDialogBinding.dialogPositiveButton.text =
            resources.getString(R.string.delete)
        anonaddyCustomDialogBinding.dialogPositiveButton.drawableBackground.setColorFilter(
            AttributeHelper.getValueByAttr(this, R.attr.colorError),
            PorterDuff.Mode.SRC_ATOP
        )
        anonaddyCustomDialogBinding.dialogPositiveButton.setTextColor(AttributeHelper.getValueByAttr(this, R.attr.colorOnError))
        anonaddyCustomDialogBinding.dialogPositiveButton.spinningBarColor = AttributeHelper.getValueByAttr(this, R.attr.colorOnError)

        anonaddyCustomDialogBinding.dialogPositiveButton.setOnClickListener {
            // Revert the button to normal
            anonaddyCustomDialogBinding.dialogPositiveButton.revertAnimation()

            anonaddyCustomDialogBinding.dialogError.visibility = View.GONE
            anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = false
            anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = false

            lifecycleScope.launch {
                deleteRecipientHttpRequest(id, this@ManageRecipientsActivity, anonaddyCustomDialogBinding)
            }
        }
        anonaddyCustomDialogBinding.dialogNegativeButton.setOnClickListener {
            deleteRecipientDialog.dismiss()
        }
        // create and show the alert dialog
        deleteRecipientDialog.show()
    }

    private suspend fun deleteRecipientHttpRequest(id: String, context: Context, anonaddyCustomDialogBinding: AnonaddyCustomDialogBinding) {
        networkHelper.deleteRecipient({ result ->
            if (result == "204") {
                deleteRecipientDialog.dismiss()
                finish()
            } else {
                // Revert the button to normal
                anonaddyCustomDialogBinding.dialogPositiveButton.revertAnimation()

                anonaddyCustomDialogBinding.dialogError.visibility = View.VISIBLE
                anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogError.text = context.resources.getString(
                    R.string.s_s,
                    context.resources.getString(R.string.error_deleting_recipient), result
                )
            }
        }, id)
    }

    private suspend fun removeGpgKeyHttpRequest(id: String, context: Context, anonaddyCustomDialogBinding: AnonaddyCustomDialogBinding) {
        networkHelper.removeEncryptionKeyRecipient({ result ->
            if (result == "204") {
                removeGpgKeyDialog.dismiss()
                setPage()
            } else {
                // Revert the button to normal
                anonaddyCustomDialogBinding.dialogPositiveButton.revertAnimation()

                anonaddyCustomDialogBinding.dialogError.visibility = View.VISIBLE
                anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogError.text = context.resources.getString(
                    R.string.s_s,
                    context.resources.getString(R.string.error_removing_gpg_key), result
                )
            }
        }, id)
    }


    private suspend fun getRecipientInfo(id: String) {
        networkHelper.getSpecificRecipient({ list, _ ->

            if (list != null) {

                /**
                 *  SWITCH STATUS
                 */

                binding.activityManageRecipientActive.setSwitchChecked(list.should_encrypt)
                binding.activityManageRecipientActive.setTitle(
                    if (list.should_encrypt) resources.getString(R.string.encryption_enabled) else resources.getString(
                        R.string.encryption_disabled
                    )
                )


                // Set switchlistener after loading
                setOnSwitchChangeListeners(list.fingerprint)

                // Set the fingerprint BottomDialogFragment
                addRecipientPublicGpgKeyBottomDialogFragment =
                    AddRecipientPublicGpgKeyBottomDialogFragment.newInstance(recipientId)

                /**
                 * Fingerprint LAYOUT
                 */

                // If there is a fingerprint, enable the remove button.
                // If there is no fingerptint, do not enable the remove button
                if (list.fingerprint != null) {
                    binding.activityManageRecipientRemoveGpgKey.setLayoutEnabled(true)
                    binding.activityManageRecipientChangeGpgKey.setTitle(resources.getString(R.string.change_public_gpg_key))
                    binding.activityManageRecipientEncryptionTextview.text = resources.getString(R.string.fingerprint_s, list.fingerprint)
                } else {
                    binding.activityManageRecipientRemoveGpgKey.setLayoutEnabled(false)
                    binding.activityManageRecipientChangeGpgKey.setTitle(resources.getString(R.string.add_public_gpg_key))
                    binding.activityManageRecipientEncryptionTextview.text = resources.getString(R.string.encryption_disabled)
                }


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

                binding.activityManageRecipientAliasesTitleTextview.text = resources.getString(R.string.recipient_aliases_d, totalAliases)
                binding.activityManageRecipientBasicTextview.text = resources.getString(
                    R.string.manage_recipient_basic_info,
                    list.email,
                    DateTimeUtils.turnStringIntoLocalString(list.created_at),
                    DateTimeUtils.turnStringIntoLocalString(list.updated_at),
                    totalForwarded, totalBlocked, totalReplies, totalSent
                )

                binding.activityManageRecipientAliasesTextview.text = aliases

                binding.activityManageRecipientRLProgressbar.visibility = View.GONE
                binding.activityManageRecipientLL1.visibility = View.VISIBLE

                setOnClickListeners()
            } else {
                binding.activityManageRecipientRLProgressbar.visibility = View.GONE
                binding.activityManageRecipientLL1.visibility = View.GONE

                // Show no internet animations
                binding.activityManageRecipientRLLottieview.visibility = View.VISIBLE
            }
        }, id)
    }


    override fun onKeyAdded() {
        setPage()
        addRecipientPublicGpgKeyBottomDialogFragment.dismiss()
    }
}