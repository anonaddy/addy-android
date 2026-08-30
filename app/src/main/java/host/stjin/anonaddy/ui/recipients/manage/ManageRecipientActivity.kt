package host.stjin.anonaddy.ui.recipients.manage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.databinding.ActivityManageRecipientBinding
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.repositories.AliasRepository
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


import androidx.activity.viewModels

class ManageRecipientActivity : BaseActivity(),
    AddRecipientPublicGpgKeyBottomDialogFragment.AddEditGpgKeyBottomDialogListener,
    EditRecipientDescriptionBottomDialogFragment.AddEditRecipientDescriptionBottomDialogListener {
    private val viewModel: ManageRecipientViewModel by viewModels()
    private lateinit var aliasRepository: AliasRepository

    private lateinit var addRecipientPublicGpgKeyBottomDialogFragment: AddRecipientPublicGpgKeyBottomDialogFragment

    private lateinit var editRecipientDescriptionBottomDialogFragment: EditRecipientDescriptionBottomDialogFragment

    private var shouldRefreshOnFinish = false

    private var recipient: Recipients? = null
        set(value) {
            field = value
            value?.let { updateUi(it) }
        }

    private var aliasList: PaginatedResponse<Aliases>? = null
        set(value) {
            field = value
            value?.let { recipient?.let { recipient -> updateUi(recipient, it) } }
        }

    private var workingAliasList: PaginatedResponse<Aliases>? = null
    private var aliasesEmailList: List<String> = emptyList()

    private var isAliasesExpanded = false
    private var forceSwitch = false

    private lateinit var binding: ActivityManageRecipientBinding

    private lateinit var removeGpgKeySnackbar: Snackbar

    private lateinit var deleteRecipientSnackbar: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageRecipientBinding.inflate(layoutInflater)
        InsetUtils.applyBottomInset(binding.activityManageRecipientLL1)

        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.edit_recipient,
            binding.activityManageRecipientNSV,
            binding.activityManageRecipientToolbar,
            null
        )
        aliasRepository = ServiceLocator.aliasRepository
        setRefreshLayout()


        val b = intent.extras
        val recipientId = b?.getString("recipient_id")

        if (recipientId == null) {
            finish()
            return
        }
        setPage(recipientId)
    }

    private fun setOnClickListeners() {
        binding.activityManageRecipientDescEdit.setOnLayoutClickedListener {
            if (!editRecipientDescriptionBottomDialogFragment.isAdded) {
                editRecipientDescriptionBottomDialogFragment.show(
                    supportFragmentManager,
                    "editRecipientDescriptionBottomDialogFragment"
                )
            }
        }

        binding.activityManageRecipientChangePgpKey.setOnLayoutClickedListener {
            if (!addRecipientPublicGpgKeyBottomDialogFragment.isAdded) {
                addRecipientPublicGpgKeyBottomDialogFragment.show(
                    supportFragmentManager,
                    "addRecipientPublicGpgKeyBottomDialogFragment"
                )
            }
        }

        binding.activityManageRecipientRemovePgpKey.setOnLayoutClickedListener { removeGpgKey(this@ManageRecipientActivity.recipient!!.id) }

        binding.activityManageRecipientDelete.setOnLayoutClickedListener { deleteRecipient(this@ManageRecipientActivity.recipient!!.id) }

        binding.activityManageRecipientStatus.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityManageRecipientStatus.setSwitchChecked(!binding.activityManageRecipientStatus.getSwitchChecked())
        }

        binding.activityManageRecipientActive.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityManageRecipientActive.setSwitchChecked(!binding.activityManageRecipientActive.getSwitchChecked())
        }

        binding.activityManageRecipientCanReplySend.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityManageRecipientCanReplySend.setSwitchChecked(!binding.activityManageRecipientCanReplySend.getSwitchChecked())
        }

        binding.activityManageRecipientPgpInline.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityManageRecipientPgpInline.setSwitchChecked(!binding.activityManageRecipientPgpInline.getSwitchChecked())
        }

        binding.activityManageRecipientProtectedHeaders.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityManageRecipientProtectedHeaders.setSwitchChecked(!binding.activityManageRecipientProtectedHeaders.getSwitchChecked())
        }

        binding.activityManageRecipientRemovePgpKeysFromRs.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityManageRecipientRemovePgpKeysFromRs.setSwitchChecked(!binding.activityManageRecipientRemovePgpKeysFromRs.getSwitchChecked())
        }

        binding.activityManageRecipientRemovePgpSignaturesKeyFromRs.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityManageRecipientRemovePgpSignaturesKeyFromRs.setSwitchChecked(!binding.activityManageRecipientRemovePgpSignaturesKeyFromRs.getSwitchChecked())
        }
        binding.activityManageRecipientAliasesShowMoreLessButton.setOnClickListener {
            isAliasesExpanded = !isAliasesExpanded
            updateAliasesView()
        }

        val showAliasCountInfo = View.OnClickListener {
            MaterialDialogHelper.showMaterialDialog(
                context = this,
                title = resources.getString(R.string.recipient_aliases),
                message = resources.getString(R.string.recipient_aliases_count_info),
                icon = R.drawable.ic_info,
                neutralButtonText = resources.getString(R.string.close)
            ).show()
        }
        binding.activityManageRecipientAliasesInfoButton.setOnClickListener(showAliasCountInfo)
        binding.activityManageRecipientAliasesCountTextview.setOnClickListener(showAliasCountInfo)
    }

    override fun finish() {
        val resultIntent = Intent()
        resultIntent.putExtra("shouldRefresh", shouldRefreshOnFinish)
        setResult(RESULT_OK, resultIntent)
        super.finish()
    }

    override fun onKeyAdded(recipient: Recipients) {
        addRecipientPublicGpgKeyBottomDialogFragment.dismissAllowingStateLoss()

        // Do this last, will trigger updateUI as well as re-init addRecipientPublicGpgKeyBottomDialogFragment
        this.recipient = recipient
    }

    override fun descriptionEdited(recipient: Recipients) {
        editRecipientDescriptionBottomDialogFragment.dismissAllowingStateLoss()
        shouldRefreshOnFinish = true

        // Do this last, will trigger updateUI as well as re-init editRecipientDescriptionBottomDialogFragment
        this.recipient = recipient
    }

    private fun setRefreshLayout() {
        binding.activityManageRecipientSwiperefresh.setOnRefreshListener {
            binding.activityManageRecipientSwiperefresh.isRefreshing = true

            recipient?.let { setPage(it.id) }
        }
    }

    private fun setPage(recipientId: String) {
        // Get the recipient
        lifecycleScope.launch {
            getRecipientInfo(recipientId)
        }
    }

    private fun setOnSwitchChangeListeners(fingerprint: String?) {
        binding.activityManageRecipientStatus.setOnSwitchCheckedChangedListener { compoundButton, checked -> // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
            if (compoundButton.isPressed || forceSwitch) {
                binding.activityManageRecipientStatus.showProgressBar(true)
                forceSwitch = false

                if (checked) {
                    lifecycleScope.launch {
                        activateRecipient()
                    }
                } else {
                    lifecycleScope.launch {
                        deactivateRecipient()
                    }
                }
            }
        }

        binding.activityManageRecipientActive.setOnSwitchCheckedChangedListener { compoundButton, checked -> // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
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


        binding.activityManageRecipientCanReplySend.setOnSwitchCheckedChangedListener { compoundButton, checked -> // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
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

        binding.activityManageRecipientPgpInline.setOnSwitchCheckedChangedListener { compoundButton, checked -> // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
            if (compoundButton.isPressed || forceSwitch) {
                binding.activityManageRecipientPgpInline.showProgressBar(true)
                forceSwitch = false
                if (checked) {
                    lifecycleScope.launch {
                        enablePGPInline()
                    }
                } else {
                    lifecycleScope.launch {
                        disablePGPInline()
                    }
                }
            }
        }

        binding.activityManageRecipientProtectedHeaders.setOnSwitchCheckedChangedListener { compoundButton, checked -> // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
            if (compoundButton.isPressed || forceSwitch) {
                binding.activityManageRecipientProtectedHeaders.showProgressBar(true)
                forceSwitch = false
                if (checked) {
                    lifecycleScope.launch {
                        enableProtectedHeaders()
                    }
                } else {
                    lifecycleScope.launch {
                        disableProtectedHeaders()
                    }
                }
            }
        }

        binding.activityManageRecipientRemovePgpKeysFromRs.setOnSwitchCheckedChangedListener { compoundButton, checked -> // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
            if (compoundButton.isPressed || forceSwitch) {
                binding.activityManageRecipientRemovePgpKeysFromRs.showProgressBar(true)
                forceSwitch = false
                if (checked) {
                    lifecycleScope.launch {
                        enableRemovePGPKeysForASpecificRecipient()
                    }
                } else {
                    lifecycleScope.launch {
                        disableRemovePGPKeysForASpecificRecipient()
                    }
                }
            }
        }

        binding.activityManageRecipientRemovePgpSignaturesKeyFromRs.setOnSwitchCheckedChangedListener { compoundButton, checked -> // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
            if (compoundButton.isPressed || forceSwitch) {
                binding.activityManageRecipientRemovePgpSignaturesKeyFromRs.showProgressBar(true)
                forceSwitch = false
                if (checked) {
                    lifecycleScope.launch {
                        enableRemovePGPSignaturesForASpecificRecipient()
                    }
                } else {
                    lifecycleScope.launch {
                        disableRemovePGPSignaturesForASpecificRecipient()
                    }
                }
            }
        }


    }

    private suspend fun disallowRecipient() {
        val result = viewModel.disallowRecipientToReplySend(this.recipient!!.id)
        binding.activityManageRecipientCanReplySend.showProgressBar(false)
        if (result is NetworkResult.Success && result.data == "204") {
            this.recipient!!.can_reply_send = false
            shouldRefreshOnFinish = true
            updateUi(this.recipient!!)
        } else {
            binding.activityManageRecipientCanReplySend.setSwitchChecked(true)
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_edit_active) + "\n" + (result.errorOrNull() ?: ""),
                binding.activityManageRecipientCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun allowRecipient() {
        val result = viewModel.allowRecipientToReplySend(this.recipient!!.id)
        binding.activityManageRecipientCanReplySend.showProgressBar(false)
        when (result) {
            is NetworkResult.Success -> {
                this.recipient = result.data
                shouldRefreshOnFinish = true
            }
            is NetworkResult.Error -> {
                binding.activityManageRecipientCanReplySend.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result.error,
                    binding.activityManageRecipientCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }
    }

    private suspend fun disableEncryption() {
        val result = viewModel.disableEncryptionRecipient(this.recipient!!.id)
        binding.activityManageRecipientActive.showProgressBar(false)
        if (result is NetworkResult.Success && result.data == "204") {
            this.recipient!!.should_encrypt = false
            shouldRefreshOnFinish = true
            updateUi(this.recipient!!)
        } else {
            binding.activityManageRecipientActive.setSwitchChecked(true)
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_edit_active) + "\n" + (result.errorOrNull() ?: ""),
                binding.activityManageRecipientCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun enableEncryption() {
        val result = viewModel.enableEncryptionRecipient(this.recipient!!.id)
        binding.activityManageRecipientActive.showProgressBar(false)
        when (result) {
            is NetworkResult.Success -> {
                this.recipient = result.data
                shouldRefreshOnFinish = true
            }
            is NetworkResult.Error -> {
                binding.activityManageRecipientActive.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result.error,
                    binding.activityManageRecipientCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }
    }

    private suspend fun disablePGPInline() {
        val result = viewModel.disablePgpInlineRecipient(this.recipient!!.id)
        binding.activityManageRecipientPgpInline.showProgressBar(false)
        if (result is NetworkResult.Success && result.data == "204") {
            this.recipient!!.inline_encryption = false
            shouldRefreshOnFinish = true
            updateUi(this.recipient!!)
        } else {
            binding.activityManageRecipientPgpInline.setSwitchChecked(true)
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_edit_active) + "\n" + (result.errorOrNull() ?: ""),
                binding.activityManageRecipientCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun disableRemovePGPKeysForASpecificRecipient() {
        val result = viewModel.disableRemovePgpKeysRecipients(this.recipient!!.id)
        binding.activityManageRecipientRemovePgpKeysFromRs.showProgressBar(false)
        if (result is NetworkResult.Success && result.data == "204") {
            this.recipient!!.remove_pgp_keys = false
            shouldRefreshOnFinish = true
            updateUi(this.recipient!!)
        } else {
            binding.activityManageRecipientRemovePgpKeysFromRs.setSwitchChecked(true)
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_edit_active) + "\n" + (result.errorOrNull() ?: ""),
                binding.activityManageRecipientCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun disableRemovePGPSignaturesForASpecificRecipient() {
        val result = viewModel.disableRemovePgpSignaturesRecipients(this.recipient!!.id)
        binding.activityManageRecipientRemovePgpSignaturesKeyFromRs.showProgressBar(false)
        if (result is NetworkResult.Success && result.data == "204") {
            this.recipient!!.remove_pgp_signatures = false
            shouldRefreshOnFinish = true
            updateUi(this.recipient!!)
        } else {
            binding.activityManageRecipientRemovePgpSignaturesKeyFromRs.setSwitchChecked(true)
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_edit_active) + "\n" + (result.errorOrNull() ?: ""),
                binding.activityManageRecipientCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun enablePGPInline() {
        val result = viewModel.enablePgpInlineRecipient(this.recipient!!.id)
        binding.activityManageRecipientPgpInline.showProgressBar(false)
        when (result) {
            is NetworkResult.Success -> {
                this.recipient = result.data
                shouldRefreshOnFinish = true
            }
            is NetworkResult.Error -> {
                binding.activityManageRecipientPgpInline.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result.error,
                    binding.activityManageRecipientCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }
    }

    private suspend fun enableRemovePGPKeysForASpecificRecipient() {
        val result = viewModel.enableRemovePgpKeysRecipients(this.recipient!!.id)
        binding.activityManageRecipientRemovePgpKeysFromRs.showProgressBar(false)
        when (result) {
            is NetworkResult.Success -> {
                this.recipient = result.data
                shouldRefreshOnFinish = true
            }
            is NetworkResult.Error -> {
                binding.activityManageRecipientRemovePgpKeysFromRs.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result.error,
                    binding.activityManageRecipientCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }
    }

    private suspend fun enableRemovePGPSignaturesForASpecificRecipient() {
        val result = viewModel.enableRemovePgpSignaturesRecipients(this.recipient!!.id)
        binding.activityManageRecipientRemovePgpSignaturesKeyFromRs.showProgressBar(false)
        when (result) {
            is NetworkResult.Success -> {
                this.recipient = result.data
                shouldRefreshOnFinish = true
            }
            is NetworkResult.Error -> {
                binding.activityManageRecipientRemovePgpSignaturesKeyFromRs.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result.error,
                    binding.activityManageRecipientCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }
    }

    private suspend fun disableProtectedHeaders() {
        val result = viewModel.disableProtectedHeadersRecipient(this.recipient!!.id)
        binding.activityManageRecipientProtectedHeaders.showProgressBar(false)
        if (result is NetworkResult.Success && result.data == "204") {
            this.recipient!!.protected_headers = false
            shouldRefreshOnFinish = true
            updateUi(this.recipient!!)
        } else {
            binding.activityManageRecipientProtectedHeaders.setSwitchChecked(true)
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_edit_active) + "\n" + (result.errorOrNull() ?: ""),
                binding.activityManageRecipientCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun enableProtectedHeaders() {
        val result = viewModel.enableProtectedHeadersRecipient(this.recipient!!.id)
        binding.activityManageRecipientProtectedHeaders.showProgressBar(false)
        when (result) {
            is NetworkResult.Success -> {
                this.recipient = result.data
                shouldRefreshOnFinish = true
            }
            is NetworkResult.Error -> {
                binding.activityManageRecipientProtectedHeaders.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result.error,
                    binding.activityManageRecipientCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }
    }

    private fun removeGpgKey(id: String) {
        MaterialDialogHelper.showMaterialDialog(
            context = this,
            title = resources.getString(R.string.remove_public_key),
            message = resources.getString(R.string.remove_public_key_desc),
            icon = R.drawable.ic_forbid,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.remove),
            positiveButtonAction = {
                removeGpgKeySnackbar = SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.removing_public_key),
                    binding.activityManageRecipientCL,
                    length = Snackbar.LENGTH_INDEFINITE
                )
                removeGpgKeySnackbar.show()
                lifecycleScope.launch {
                    removeGpgKeyHttpRequest(id, this@ManageRecipientActivity)
                }
            }
        ).show()
    }

    private fun deleteRecipient(id: String) {
        MaterialDialogHelper.showMaterialDialog(
            context = this,
            title = resources.getString(R.string.delete_recipient),
            message = resources.getString(R.string.delete_recipient_desc),
            icon = R.drawable.ic_trash,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.delete),
            positiveButtonAction = {
                deleteRecipientSnackbar = SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.deleting_recipient),
                    binding.activityManageRecipientCL,
                    length = Snackbar.LENGTH_INDEFINITE
                )
                deleteRecipientSnackbar.show()
                lifecycleScope.launch {
                    deleteRecipientHttpRequest(id, this@ManageRecipientActivity)
                }
            }
        ).show()
    }

    private suspend fun deleteRecipientHttpRequest(id: String, context: Context) {
        val result = viewModel.deleteRecipient(id)
        if (result is NetworkResult.Success && result.data == "204") {
            deleteRecipientSnackbar.dismiss()
            shouldRefreshOnFinish = true
            finish()
        } else {
            val error = (result as? NetworkResult.Error)?.error ?: ""
            SnackbarHelper.createSnackbar(
                this,
                context.resources.getString(
                    R.string.s_s,
                    context.resources.getString(R.string.error_deleting_recipient), error
                ),
                binding.activityManageRecipientCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun removeGpgKeyHttpRequest(id: String, context: Context) {
        val result = viewModel.removeEncryptionKeyRecipient(id)
        if (result is NetworkResult.Success && result.data == "204") {
            removeGpgKeySnackbar.dismiss()
            this.recipient!!.should_encrypt = false
            this.recipient!!.fingerprint = null
            shouldRefreshOnFinish = true
            // Recheck the UI
            updateUi(this.recipient!!)
        } else {
            val error = (result as? NetworkResult.Error)?.error ?: ""
            SnackbarHelper.createSnackbar(
                this,
                context.resources.getString(
                    R.string.s_s,
                    context.resources.getString(R.string.error_removing_gpg_key), error
                ),
                binding.activityManageRecipientCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun deactivateRecipient() {
        val result = viewModel.deactivateRecipient(this.recipient!!.id)
        binding.activityManageRecipientStatus.showProgressBar(false)
        if (result is NetworkResult.Success && result.data == "204") {
            this.recipient!!.active = false
            shouldRefreshOnFinish = true
            updateUi(this.recipient!!)
        } else {
            val error = (result as? NetworkResult.Error)?.error ?: ""
            binding.activityManageRecipientStatus.setSwitchChecked(true)
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_edit_active) + "\n" + error,
                binding.activityManageRecipientCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun activateRecipient() {
        val result = viewModel.activateRecipient(this.recipient!!.id)
        binding.activityManageRecipientStatus.showProgressBar(false)
        if (result is NetworkResult.Success) {
            this.recipient!!.active = true
            shouldRefreshOnFinish = true
            updateUi(this.recipient!!)
        } else {
            val error = (result as? NetworkResult.Error)?.error ?: ""
            binding.activityManageRecipientStatus.setSwitchChecked(false)
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.error_edit_active) + "\n" + error,
                binding.activityManageRecipientCL,
                LoggingHelper.LOGFILES.DEFAULT
            ).show()
        }
    }

    private suspend fun getRecipientInfo(id: String) {
        when (val result = viewModel.getRecipient(id)) {
            is NetworkResult.Success -> {
                val recipient = result.data
                // Triggers updateUi
                this.recipient = recipient

                // Now that we have the recipient, obtain the aliases separately
                lifecycleScope.launch {
                    getAliasesAndAddThemToList(recipient)
                }
            }
            is NetworkResult.Error -> {
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_obtaining_recipient) + "\n" + result.error,
                    binding.activityManageRecipientCL
                ).show()

                // Show error animations
                binding.activityManageRecipientLL1.visibility = View.GONE
                binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
            }
        }
        binding.activityManageRecipientSwiperefresh.isRefreshing = false
    }

    private fun updateUi(recipient: Recipients, aliasesArray: PaginatedResponse<Aliases>? = null) {

        /**
         *  SWITCH STATUS
         */

        binding.activityManageRecipientStatus.setSwitchChecked(recipient.active)
        binding.activityManageRecipientStatus.setTitle(
            if (recipient.active) resources.getString(R.string.recipient_status_active) else resources.getString(
                R.string.recipient_status_inactive
            )
        )

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

        binding.activityManageRecipientPgpInline.setSwitchChecked(recipient.inline_encryption)
        binding.activityManageRecipientProtectedHeaders.setSwitchChecked(recipient.protected_headers)
        binding.activityManageRecipientRemovePgpKeysFromRs.setSwitchChecked(recipient.remove_pgp_keys)
        binding.activityManageRecipientRemovePgpSignaturesKeyFromRs.setSwitchChecked(recipient.remove_pgp_signatures)

        if (recipient.fingerprint == null) {
            binding.activityManageRecipientPgpInline.setLayoutEnabled(false)
            binding.activityManageRecipientProtectedHeaders.setLayoutEnabled(false)
        } else if (recipient.inline_encryption) {
            binding.activityManageRecipientPgpInline.setLayoutEnabled(true)
            binding.activityManageRecipientPgpInline.setDescription(this.resources.getString(R.string.pgp_inline_desc))
            binding.activityManageRecipientProtectedHeaders.setLayoutEnabled(false)
            binding.activityManageRecipientProtectedHeaders.setDescription(this.resources.getString(R.string.prerequisite_disable_pgp_inline))
        } else if (recipient.protected_headers) {
            binding.activityManageRecipientProtectedHeaders.setLayoutEnabled(true)
            binding.activityManageRecipientProtectedHeaders.setDescription(this.resources.getString(R.string.protected_headers_subject_desc))
            binding.activityManageRecipientPgpInline.setLayoutEnabled(false)
            binding.activityManageRecipientPgpInline.setDescription(this.resources.getString(R.string.prerequisite_disable_protected_headers))
        } else {
            binding.activityManageRecipientProtectedHeaders.setLayoutEnabled(true)
            binding.activityManageRecipientProtectedHeaders.setDescription(this.resources.getString(R.string.protected_headers_subject_desc))

            binding.activityManageRecipientPgpInline.setLayoutEnabled(true)
            binding.activityManageRecipientPgpInline.setDescription(this.resources.getString(R.string.pgp_inline_desc))
        }

        if ((this.application as? AddyIoApp)?.userResourceOrNull?.hasUserFreeSubscription == true) {
            binding.activityManageRecipientProtectedHeaders.setLayoutEnabled(false)
            binding.activityManageRecipientProtectedHeaders.setDescription(this.resources.getString(R.string.feature_not_available_subscription))
        }


        // Set switchlistener after loading
        setOnSwitchChangeListeners(recipient.fingerprint)

        // Set the fingerprint BottomDialogFragment
        addRecipientPublicGpgKeyBottomDialogFragment =
            AddRecipientPublicGpgKeyBottomDialogFragment.newInstance(this@ManageRecipientActivity.recipient!!.id)

        /**
         * DESCRIPTION
         */

        // Set description and initialise the bottomDialogFragment
        if (recipient.description != null) {
            binding.activityManageRecipientDescEdit.setDescription(recipient.description)
        } else {
            binding.activityManageRecipientDescEdit.setDescription(
                this.resources.getString(
                    R.string.recipient_no_description
                )
            )
        }

        // Set this value as it now includes the description
        editRecipientDescriptionBottomDialogFragment = EditRecipientDescriptionBottomDialogFragment.newInstance(
            this.recipient!!.id,
            recipient.description
        )

        /**
         * Fingerprint LAYOUT
         */

        // If there is a fingerprint, enable the remove button.
        // If there is no fingerptint, do not enable the remove button
        if (recipient.fingerprint != null) {
            binding.activityManageRecipientRemovePgpKey.setLayoutEnabled(true)
            binding.activityManageRecipientChangePgpKey.setTitle(resources.getString(R.string.change_public_gpg_key))
            binding.activityManageRecipientEncryptionTextview.text = resources.getString(R.string.fingerprint_s, recipient.fingerprint)
        } else {
            binding.activityManageRecipientRemovePgpKey.setLayoutEnabled(false)
            binding.activityManageRecipientChangePgpKey.setTitle(resources.getString(R.string.add_public_gpg_key))
            binding.activityManageRecipientEncryptionTextview.text = resources.getString(R.string.encryption_disabled)
        }


        var totalForwarded = 0
        var totalBlocked = 0
        var totalReplies = 0
        var totalSent = 0

        if (aliasesArray != null) {
            binding.activityManageRecipientAliasesCountTextview.apply {
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
            aliasesEmailList = emails
            updateAliasesView()
            binding.activityManageRecipientAliasesShimmerframelayout.hideShimmer()
            binding.activityManageRecipientBasicShimmerframelayout.hideShimmer() // Stop shimmer only after this info is loaded

        }

        binding.activityManageRecipientBasicTextview.text = resources.getString(
            R.string.manage_recipient_basic_info,
            recipient.email,
            DateTimeUtils.convertStringToLocalTimeZoneString(recipient.created_at),
            DateTimeUtils.convertStringToLocalTimeZoneString(recipient.updated_at),
            totalForwarded, totalBlocked, totalReplies, totalSent
        )

        binding.animationFragment.stopAnimation()
        binding.activityManageRecipientNSV.animate().alpha(1.0f)
        setOnClickListeners()
    }

    private fun updateAliasesView() {
        if (aliasesEmailList.size > 10) {
            binding.activityManageRecipientAliasesShowMoreLessButton.visibility = View.VISIBLE
            if (isAliasesExpanded) {
                binding.activityManageRecipientAliasesTextview.text = aliasesEmailList.joinToString("\n")
                binding.activityManageRecipientAliasesShowMoreLessButton.text = getString(R.string.show_less)
            } else {
                binding.activityManageRecipientAliasesTextview.text = aliasesEmailList.take(10).joinToString("\n")
                binding.activityManageRecipientAliasesShowMoreLessButton.text = getString(R.string.show_more)
            }
        } else {
            binding.activityManageRecipientAliasesShowMoreLessButton.visibility = View.GONE
            binding.activityManageRecipientAliasesTextview.text = aliasesEmailList.joinToString("\n")
        }
    }
    
    private suspend fun getAliasesAndAddThemToList(recipient: Recipients) {
        binding.activityManageRecipientAliasesShimmerframelayout.startShimmer()

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
            recipient = recipient.id
        )

        when (result) {
            is NetworkResult.Success -> {
                lifecycleScope.launch {
                    addAliasesToList(recipient, result.data)
                }
            }
            is NetworkResult.Error -> {
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_obtaining_aliases) + "\n" + result.error,
                    binding.activityManageRecipientCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }
    }

    private suspend fun addAliasesToList(recipient: Recipients, aliasesArray: PaginatedResponse<Aliases>) {
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
            getAliasesAndAddThemToList(recipient)
        } else {
            // Else, set aliasList to call updateUi()
            this.aliasList = workingAliasList
            // Clear workingAliasList to free up space
            workingAliasList = null
        }
    }
}
