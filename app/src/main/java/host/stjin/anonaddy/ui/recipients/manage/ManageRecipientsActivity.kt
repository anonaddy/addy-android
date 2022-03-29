package host.stjin.anonaddy.ui.recipients.manage

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityManageRecipientsBinding
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


class ManageRecipientsActivity : BaseActivity(),
    AddRecipientPublicGpgKeyBottomDialogFragment.AddEditGpgKeyBottomDialogListener {

    lateinit var networkHelper: NetworkHelper
    private lateinit var addRecipientPublicGpgKeyBottomDialogFragment: AddRecipientPublicGpgKeyBottomDialogFragment

    private var recipient: Recipients? = null
        set(value) {
            field = value
            value?.let { updateUi(it) }
        }

    private var forceSwitch = false
    private lateinit var binding: ActivityManageRecipientsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageRecipientsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activityManageRecipientLL1)
        )

        setupToolbar(
            R.string.edit_recipient,
            binding.activityManageRecipientNSV,
            binding.activityManageRecipientToolbar
        )
        networkHelper = NetworkHelper(this)


        val b = intent.extras
        val recipientId = b?.getString("recipient_id")

        if (recipientId == null) {
            finish()
            return
        }
        setPage(recipientId)
    }


    private fun setPage(recipientId: String) {
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


        binding.activityManageRecipientCanReplySend.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    binding.activityManageRecipientCanReplySend.showProgressBar(true)
                    forceSwitch = false
                    if (checked) {
                        lifecycleScope.launch {
                            allowRecipient()
                        }
                    } else {
                        lifecycleScope.launch {
                            disallowRecipient()
                        }
                    }
                }
            }
        })


    }

    private suspend fun disallowRecipient() {
        networkHelper.disallowRecipientToReplySend({ result ->
            binding.activityManageRecipientCanReplySend.showProgressBar(false)
            if (result == "204") {
                binding.activityManageRecipientCanReplySend.setTitle(resources.getString(R.string.cannot_reply_send))
            } else {
                binding.activityManageRecipientCanReplySend.setSwitchChecked(true)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    binding.activityManageRecipientCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this.recipient!!.id)
    }

    private suspend fun allowRecipient() {
        networkHelper.allowRecipientToReplySend({ recipient, error ->
            binding.activityManageRecipientCanReplySend.showProgressBar(false)
            if (recipient != null) {
                binding.activityManageRecipientCanReplySend.setTitle(resources.getString(R.string.can_reply_send))
            } else {
                binding.activityManageRecipientCanReplySend.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + error,
                    binding.activityManageRecipientCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this.recipient!!.id)
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
        }, this.recipient!!.id)
    }


    private suspend fun enableEncryption() {
        networkHelper.enableEncryptionRecipient({ recipient, error ->
            binding.activityManageRecipientActive.showProgressBar(false)
            if (recipient != null) {
                binding.activityManageRecipientActive.setTitle(resources.getString(R.string.encryption_enabled))
            } else {
                binding.activityManageRecipientActive.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + error,
                    binding.activityManageRecipientCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this.recipient!!.id)
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
                removeGpgKey(this@ManageRecipientsActivity.recipient!!.id)
            }
        })

        binding.activityManageRecipientDelete.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                deleteRecipient(this@ManageRecipientsActivity.recipient!!.id)
            }
        })

        binding.activityManageRecipientActive.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityManageRecipientActive.setSwitchChecked(!binding.activityManageRecipientActive.getSwitchChecked())
            }
        })

        binding.activityManageRecipientCanReplySend.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityManageRecipientCanReplySend.setSwitchChecked(!binding.activityManageRecipientCanReplySend.getSwitchChecked())
            }
        })

    }


    private lateinit var removeGpgKeySnackbar: Snackbar
    private fun removeGpgKey(id: String) {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Catalog_MaterialAlertDialog_Centered_FullWidthButtons)
            .setTitle(resources.getString(R.string.remove_public_key))
            .setIcon(R.drawable.ic_forbid)
            .setMessage(resources.getString(R.string.remove_public_key_desc))
            .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(resources.getString(R.string.remove)) { _, _ ->
                removeGpgKeySnackbar = SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.removing_public_key),
                    binding.activityManageRecipientCL,
                    length = Snackbar.LENGTH_INDEFINITE
                )
                removeGpgKeySnackbar.show()
                lifecycleScope.launch {
                    removeGpgKeyHttpRequest(id, this@ManageRecipientsActivity)
                }
            }
            .show()
    }

    private lateinit var deleteRecipientSnackbar: Snackbar
    private fun deleteRecipient(id: String) {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Catalog_MaterialAlertDialog_Centered_FullWidthButtons)
            .setTitle(resources.getString(R.string.delete_recipient))
            .setIcon(R.drawable.ic_trash)
            .setMessage(resources.getString(R.string.delete_recipient_desc))
            .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ ->
                deleteRecipientSnackbar = SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.deleting_recipient),
                    binding.activityManageRecipientCL,
                    length = Snackbar.LENGTH_INDEFINITE
                )
                deleteRecipientSnackbar.show()
                lifecycleScope.launch {
                    deleteRecipientHttpRequest(id, this@ManageRecipientsActivity)
                }
            }
            .show()
    }

    private suspend fun deleteRecipientHttpRequest(id: String, context: Context) {
        networkHelper.deleteRecipient({ result ->
            if (result == "204") {
                deleteRecipientSnackbar.dismiss()
                finish()
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_deleting_recipient), result
                    ),
                    binding.activityManageRecipientCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, id)
    }

    private suspend fun removeGpgKeyHttpRequest(id: String, context: Context) {
        networkHelper.removeEncryptionKeyRecipient({ result ->
            if (result == "204") {
                removeGpgKeySnackbar.dismiss()

                // Since this call does not have a response, re-retrieve the recipient object

                //IMPROVE edit should_encrypt and fingerprint?
                setPage(this.recipient!!.id)
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_removing_gpg_key), result
                    ),
                    binding.activityManageRecipientCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, id)
    }


    private suspend fun getRecipientInfo(id: String) {
        networkHelper.getSpecificRecipient({ recipient, error ->

            if (recipient != null) {
                // Triggers updateUi
                this.recipient = recipient
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_obtaining_recipient) + "\n" + error,
                    binding.activityManageRecipientCL
                ).show()

                // Show error animations
                binding.activityManageRecipientLL1.visibility = View.GONE
                binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
            }
        }, id)
    }

    private fun updateUi(recipient: Recipients) {

        /**
         *  SWITCH STATUS
         */

        binding.activityManageRecipientCanReplySend.setSwitchChecked(recipient.can_reply_send)
        binding.activityManageRecipientCanReplySend.setTitle(
            if (recipient.can_reply_send) resources.getString(R.string.can_reply_send) else resources.getString(
                R.string.cannot_reply_send
            )
        )

        binding.activityManageRecipientActive.setSwitchChecked(recipient.should_encrypt)
        binding.activityManageRecipientActive.setTitle(
            if (recipient.should_encrypt) resources.getString(R.string.encryption_enabled) else resources.getString(
                R.string.encryption_disabled
            )
        )


        // Set switchlistener after loading
        setOnSwitchChangeListeners(recipient.fingerprint)

        // Set the fingerprint BottomDialogFragment
        addRecipientPublicGpgKeyBottomDialogFragment =
            AddRecipientPublicGpgKeyBottomDialogFragment.newInstance(this@ManageRecipientsActivity.recipient!!.id)

        /**
         * Fingerprint LAYOUT
         */

        // If there is a fingerprint, enable the remove button.
        // If there is no fingerptint, do not enable the remove button
        if (recipient.fingerprint != null) {
            binding.activityManageRecipientRemoveGpgKey.setLayoutEnabled(true)
            binding.activityManageRecipientChangeGpgKey.setTitle(resources.getString(R.string.change_public_gpg_key))
            binding.activityManageRecipientEncryptionTextview.text = resources.getString(R.string.fingerprint_s, recipient.fingerprint)
        } else {
            binding.activityManageRecipientRemoveGpgKey.setLayoutEnabled(false)
            binding.activityManageRecipientChangeGpgKey.setTitle(resources.getString(R.string.add_public_gpg_key))
            binding.activityManageRecipientEncryptionTextview.text = resources.getString(R.string.encryption_disabled)
        }


        var totalForwarded = 0
        var totalBlocked = 0
        var totalReplies = 0
        var totalSent = 0
        val totalAliases = recipient.aliases?.size
        var aliases = ""

        val buf = StringBuilder()

        if (recipient.aliases != null) {
            for (alias in recipient.aliases!!) {
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
            recipient.email,
            DateTimeUtils.turnStringIntoLocalString(recipient.created_at),
            DateTimeUtils.turnStringIntoLocalString(recipient.updated_at),
            totalForwarded, totalBlocked, totalReplies, totalSent
        )

        binding.activityManageRecipientAliasesTextview.text = aliases


        binding.animationFragment.stopAnimation()
        binding.activityManageRecipientNSV.animate().alpha(1.0f)
        setOnClickListeners()
    }


    override fun onKeyAdded(recipient: Recipients) {
        addRecipientPublicGpgKeyBottomDialogFragment.dismissAllowingStateLoss()

        // Do this last, will trigger updateUI as well as re-init addRecipientPublicGpgKeyBottomDialogFragment
        this.recipient = recipient
    }
}