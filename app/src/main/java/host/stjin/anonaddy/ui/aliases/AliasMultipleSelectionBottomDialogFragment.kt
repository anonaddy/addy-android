package host.stjin.anonaddy.ui.aliases
import host.stjin.anonaddy_shared.utils.GsonTools


import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.databinding.BottomsheetMultipleSelectionAliasBinding
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy.ui.aliases.manage.EditAliasLabelsBottomDialogFragment
import host.stjin.anonaddy.ui.aliases.manage.EditAliasRecipientsBottomDialogFragment
import host.stjin.anonaddy.ui.aliases.manage.ManageAliasViewModel
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch

class AliasMultipleSelectionBottomDialogFragment : BaseBottomSheetDialogFragment(),
    EditAliasLabelsBottomDialogFragment.AddEditAliasLabelsBottomDialogListener,
    EditAliasRecipientsBottomDialogFragment.AddEditAliasRecipientsBottomDialogListener {
    private val viewModel: ManageAliasViewModel by activityViewModels()
    private var selectedAliases: List<Aliases> = emptyList()

    private var listener: AddAliasMultipleSelectionBottomDialogListener? = null

    private lateinit var editAliasLabelsBottomDialogFragment: EditAliasLabelsBottomDialogFragment
    private lateinit var editAliasRecipientsBottomDialogFragment: EditAliasRecipientsBottomDialogFragment

    private lateinit var aliasWatcher: AliasWatcher

    private var forceSwitch = false

    private var networkAction: NetworkAction? = null

    private var shouldRefreshData = false

    private var _binding: BottomsheetMultipleSelectionAliasBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var amountOfNetworkCallsDone = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val json = arguments?.getString(ARG_SELECTED_ALIASES)
        if (json != null) {
            val type = object : TypeToken<List<Aliases>>() {}.type
            selectedAliases = GsonTools.gson.fromJson(json, type) ?: emptyList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetMultipleSelectionAliasBinding.inflate(inflater, container, false)
        val root = binding.root
        listener = (parentFragment as? AddAliasMultipleSelectionBottomDialogListener) ?: (activity as? AddAliasMultipleSelectionBottomDialogListener)

        aliasWatcher = ServiceLocator.aliasWatcher

        updateUi()
        setOnSwitchChangeListeners()
        setOnClickListeners()

        return root

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setOnClickListeners() {

        binding.bsMultipleSelectionAliasCancel.setOnClickListener {
            listener?.onCancelMultipleSelectionBottomDialogFragment(shouldRefreshData)
        }

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setOnLayoutClickedListener {
            forceSwitch = true
            binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(!binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.getSwitchChecked())
        }

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasLabelsEdit.setOnLayoutClickedListener {
            val aliasIds = selectedAliases.map { it.id }
            editAliasLabelsBottomDialogFragment = EditAliasLabelsBottomDialogFragment.newInstance(aliasIds, null)
            editAliasLabelsBottomDialogFragment.show(
                childFragmentManager,
                "editAliasLabelsBottomDialogFragment"
            )
        }

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRecipientsEdit.setOnLayoutClickedListener {
            editAliasRecipientsBottomDialogFragment = EditAliasRecipientsBottomDialogFragment.newInstance(null, null)
            editAliasRecipientsBottomDialogFragment.show(
                childFragmentManager,
                "editAliasRecipientsBottomDialogFragment"
            )
        }


        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasWatchSwitchLayout.setOnLayoutClickedListener {
            forceSwitch = true
            binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasWatchSwitchLayout.setSwitchChecked(!binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasWatchSwitchLayout.getSwitchChecked())
        }

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasDelete.setOnLayoutClickedListener { deleteAlias() }

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasForget.setOnLayoutClickedListener { forgetAlias() }

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRestore.setOnLayoutClickedListener { restoreAlias() }
    }

    override fun onCancel(dialog: DialogInterface) {
        listener?.onCloseMultipleSelectionBottomDialogFragment(shouldRefreshData)
        super.onCancel(dialog)
    }

    override fun labelsEdited() {
        shouldRefreshData = true
        editAliasLabelsBottomDialogFragment.dismissAllowingStateLoss()
        listener?.onCloseMultipleSelectionBottomDialogFragment(shouldRefreshData)
        dismissAllowingStateLoss()
    }

    override fun recipientsEdited(alias: Aliases) {
        // Not used in bulk mode
    }

    override fun bulkRecipientsEdited(recipientIds: ArrayList<String>) {
        amountOfNetworkCallsDone = 0
        networkAction = NetworkAction.CHANGE_RECIPIENTS_STATE
        updateUi()

        lifecycleScope.launch {
            bulkUpdateRecipients(selectedAliases, recipientIds)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private fun updateUi() {
        binding.bsMultipleSelectionAliasTitle.text = resources.getQuantityString(R.plurals.multiple_alias_selected, selectedAliases.count(), selectedAliases.count())

        // No need for the created and updated views
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasLastForwarded.visibility = View.GONE
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasLastReplied.visibility = View.GONE
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasLastSent.visibility = View.GONE
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasLastBlocked.visibility = View.GONE
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasCreatedAt.visibility = View.GONE
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasUpdatedAt.visibility = View.GONE
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRecipientsEdit.visibility = View.VISIBLE
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRecipientsEdit.setDescription(resources.getString(R.string.select_recipients_for_this_alias))
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasDescEdit.visibility = View.GONE
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasLabelsEdit.setDescription(resources.getString(R.string.add_label_description))
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasFromNameEdit.visibility = View.GONE
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasLimitAttachedRecipientsSwitchLayout.visibility = View.GONE

        // Pinned
        if ((activity?.application as? AddyIoApp)?.userResource?.subscription != null) {
            binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasPinnedSwitchLayout.visibility = View.VISIBLE
        } else {
            binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasPinnedSwitchLayout.visibility = View.GONE
        }

        // Check if there are any aliases that are NOT deleted
        // if there is any alias that is not deleted, show the delete section. Else all aliases are deleted so hide the section
        if (selectedAliases.any { it.deleted_at == null }) {
            binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasDelete.visibility = View.VISIBLE
        } else {
            binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasDelete.visibility = View.GONE
        }

        // Now do the same for deleted aliases (restore section)
        if (selectedAliases.any { it.deleted_at != null }) {
            binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRestore.visibility = View.VISIBLE
        } else {
            binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRestore.visibility = View.GONE
        }

        // if all aliases are deleted, disable the active section
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setLayoutEnabled(!selectedAliases.all { it.deleted_at != null })


        // If all aliases are active, check the switch by default
        // For the active switch it's important to only count non-deleted aliases
        val selectedAliasesWithoutDeletedAliases = selectedAliases.filter { it.deleted_at == null }
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(selectedAliasesWithoutDeletedAliases.all { it.active })


        // Progressbars (only show if a action is being performed obviously
        when (networkAction) {
            NetworkAction.CHANGE_ACTIVE_STATE -> {
                // Show the progressbar if
                // the amount of network calls does not match the total network calls that have to be done (which is the selected aliases amount)
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.showProgressBar(selectedAliases.count() != amountOfNetworkCallsDone)
            }

            NetworkAction.CHANGE_PINNED_STATE -> {
                // Show the progressbar if
                // the amount of network calls does not match the total network calls that have to be done (which is the selected aliases amount)
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasPinnedSwitchLayout.showProgressBar(selectedAliases.count() != amountOfNetworkCallsDone)
            }

            NetworkAction.DELETE_STATE -> {
                // Show the progressbar if
                // the amount of network calls does not match the total network calls that have to be done (which is the selected aliases amount)
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasDelete.showProgressBar(selectedAliases.count() != amountOfNetworkCallsDone)

                // If the state is DELETE state that means that delete calls have been initiated
                // When all the calls are done, close the dialog
                if (selectedAliases.count() == amountOfNetworkCallsDone) {
                    listener?.onCloseMultipleSelectionBottomDialogFragment(true)
                }
            }

            NetworkAction.RESTORE_STATE -> {
                // Show the progressbar if
                // the amount of network calls does not match the total network calls that have to be done (which is the selected aliases amount)
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRestore.showProgressBar(selectedAliases.count() != amountOfNetworkCallsDone)

                // If the state is FORGET state that means that delete calls have been initiated
                // When all the calls are done, close the dialog
                if (selectedAliases.count() == amountOfNetworkCallsDone) {
                    listener?.onCloseMultipleSelectionBottomDialogFragment(true)
                }
            }

            NetworkAction.FORGET_STATE -> {
                // Show the progressbar if
                // the amount of network calls does not match the total network calls that have to be done (which is the selected aliases amount)
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasForget.showProgressBar(selectedAliases.count() != amountOfNetworkCallsDone)

                // If the state is FORGET state that means that forget calls have been initiated
                // When all the calls are done, close the dialog
                if (selectedAliases.count() == amountOfNetworkCallsDone) {
                    listener?.onCloseMultipleSelectionBottomDialogFragment(true)
                }
            }

            NetworkAction.CHANGE_RECIPIENTS_STATE -> {
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRecipientsEdit.showProgressBar(selectedAliases.count() != amountOfNetworkCallsDone)

                if (selectedAliases.count() == amountOfNetworkCallsDone) {
                    listener?.onCloseMultipleSelectionBottomDialogFragment(true)
                }
            }

            else -> { /* Do nothing */
            }
        }


        // Get watched aliases
        val watchedAliases = ServiceLocator.aliasWatcher.getAliasesToWatch()
        // If all aliases are on the watchlist, check the switch by default
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasWatchSwitchLayout.setSwitchChecked(selectedAliases.all {
            watchedAliases.contains(
                it.id
            )
        })

        // Get pinned aliases
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasPinnedSwitchLayout.setSwitchChecked(selectedAliases.all { it.pinned })

    }

    private suspend fun bulkUpdateRecipients(aliases: List<Aliases>, recipientIds: List<String>) {
        val result = viewModel.bulkUpdateAliasesRecipients(aliases.map { it.id }, recipientIds)
        shouldRefreshData = true
        amountOfNetworkCallsDone = aliases.size
        when (result) {
            is NetworkResult.Success -> {
                updateUi()
            }
            is NetworkResult.Error -> {
                showError(
                    requireContext().resources.getString(
                        R.string.s_s,
                        this.resources.getString(R.string.error_edit_recipients), result.error,
                    )
                )
            }
        }
    }

    private suspend fun deactivateAlias(aliases: List<Aliases>) {
        val result = viewModel.bulkDeactivateAlias(aliases.map { it.id })
        amountOfNetworkCallsDone = aliases.size
        shouldRefreshData = true
        when (result) {
            is NetworkResult.Success -> {
                selectedAliases.forEach { it.active = false }
                // Recheck the UI (this will finish the activity in updateUI)
                updateUi()
            }
            is NetworkResult.Error -> {
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(true)
                showError(
                    requireContext().resources.getString(
                        R.string.s_s,
                        this.resources.getString(R.string.error_edit_active), result.error,
                    )
                )
            }
        }
    }

    private suspend fun activateAlias(aliases: List<Aliases>) {
        val result = viewModel.bulkActivateAlias(aliases.map { it.id })
        amountOfNetworkCallsDone = aliases.size
        shouldRefreshData = true
        when (result) {
            is NetworkResult.Success -> {
                selectedAliases.forEach { it.active = true }
                // Recheck the UI (makes sure the switch only switches whenever all aliases have the same state)
                updateUi()
            }
            is NetworkResult.Error -> {
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(false)
                showError(
                    requireContext().resources.getString(
                        R.string.s_s,
                        requireContext().resources.getString(R.string.error_edit_active), result.error
                    )
                )
            }
        }
    }

    private suspend fun unpinAlias(aliases: List<Aliases>) {
        val result = viewModel.bulkUnpinAlias(aliases.map { it.id })
        amountOfNetworkCallsDone = aliases.size
        shouldRefreshData = true
        when (result) {
            is NetworkResult.Success -> {
                selectedAliases.forEach { it.pinned = false }
                // Recheck the UI (this will finish the activity in updateUI)
                updateUi()
            }
            is NetworkResult.Error -> {
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasPinnedSwitchLayout.setSwitchChecked(true)
                showError(
                    requireContext().resources.getString(
                        R.string.s_s,
                        this.resources.getString(R.string.error_edit_pinned), result.error,
                    )
                )
            }
        }
    }

    private suspend fun pinAlias(aliases: List<Aliases>) {
        val result = viewModel.bulkPinAlias(aliases.map { it.id })
        amountOfNetworkCallsDone = aliases.size
        shouldRefreshData = true
        when (result) {
            is NetworkResult.Success -> {
                selectedAliases.forEach { it.pinned = true }
                // Recheck the UI (makes sure the switch only switches whenever all aliases have the same state)
                updateUi()
            }
            is NetworkResult.Error -> {
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasPinnedSwitchLayout.setSwitchChecked(false)
                showError(
                    requireContext().resources.getString(
                        R.string.s_s,
                        requireContext().resources.getString(R.string.error_edit_pinned), result.error
                    )
                )
            }
        }
    }

    private fun setOnSwitchChangeListeners() {
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
            if (compoundButton.isPressed || forceSwitch) {
                amountOfNetworkCallsDone = 0
                networkAction = NetworkAction.CHANGE_ACTIVE_STATE
                updateUi()
                forceSwitch = false
                if (checked) {
                    // If the alias is already active or deleted, don't make an unnecessary call and increment amountOfNetworkCallsDone
                    // Deleted aliases cannot be activated
                    lifecycleScope.launch {
                        activateAlias(selectedAliases)
                    }

                } else {
                    // If the alias is already inactive, don't make an unnecessary call and increment amountOfNetworkCallsDone
                    // Deleted aliases cannot be deactivated, they are always deactivated
                    lifecycleScope.launch {
                        deactivateAlias(selectedAliases)
                    }

                }
            }
        }

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasWatchSwitchLayout.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
            if (compoundButton.isPressed || forceSwitch) {
                forceSwitch = false
                shouldRefreshData = true
                if (checked) {
                    for (alias in selectedAliases) {
                        // In case the alias could not be added to watchlist, the switch will be reverted
                        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasWatchSwitchLayout.setSwitchChecked(
                            aliasWatcher.addAliasToWatch(
                                alias.id
                            )
                        )
                    }
                } else {
                    for (alias in selectedAliases) {
                        aliasWatcher.removeAliasToWatch(alias.id)
                    }
                }
                listener?.onWatchedAliasesChanged()
                // No need to update the UI here, the switch already does the switching and its all done at this point
            }
        }


        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasPinnedSwitchLayout.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
            if (compoundButton.isPressed || forceSwitch) {
                amountOfNetworkCallsDone = 0
                networkAction = NetworkAction.CHANGE_PINNED_STATE
                updateUi()
                forceSwitch = false
                if (checked) {
                    // If the alias is already active or deleted, don't make an unnecessary call and increment amountOfNetworkCallsDone
                    // Deleted aliases cannot be activated
                    lifecycleScope.launch {
                        pinAlias(selectedAliases)
                    }

                } else {
                    // If the alias is already inactive, don't make an unnecessary call and increment amountOfNetworkCallsDone
                    // Deleted aliases cannot be deactivated, they are always deactivated
                    lifecycleScope.launch {
                        unpinAlias(selectedAliases)
                    }

                }
            }
        }
    }

    private fun restoreAlias() {
        MaterialDialogHelper.aliasRestoreDialog(
            context = requireContext()
        ) {
            amountOfNetworkCallsDone = 0
            networkAction = NetworkAction.RESTORE_STATE
            updateUi()


            lifecycleScope.launch {
                restoreAliasHttpRequest(selectedAliases, requireContext())
            }

        }
    }

    private fun deleteAlias() {
        MaterialDialogHelper.aliasDeleteDialog(
            context = requireContext()
        ) {
            amountOfNetworkCallsDone = 0
            networkAction = NetworkAction.DELETE_STATE
            updateUi()


            lifecycleScope.launch {
                deleteAliasHttpRequest(selectedAliases, requireContext())
            }
        }
    }

    private fun forgetAlias() {
        MaterialDialogHelper.aliasForgetDialog(
            context = requireContext()
        ) {
            amountOfNetworkCallsDone = 0
            networkAction = NetworkAction.FORGET_STATE
            updateUi()

            // There is no need to check if any of the requests are necessary, forgetting is a one-way action
            lifecycleScope.launch {
                forgetAliasHttpRequest(selectedAliases, requireContext())
            }

        }
    }

    private suspend fun deleteAliasHttpRequest(aliases: List<Aliases>, context: Context) {
        val result = viewModel.bulkDeleteAlias(aliases.map { it.id })
        shouldRefreshData = true
        amountOfNetworkCallsDone = aliases.size

        when (result) {
            is NetworkResult.Success -> {
                // Recheck the UI (this will finished the activity in updateUI)
                updateUi()
            }
            is NetworkResult.Error -> {
                showError(
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_deleting_alias), result.error
                    )
                )
            }
        }
    }

    private fun showError(string: String) {
        binding.bsMultipleSelectionAliasError.visibility = View.VISIBLE
        binding.bsMultipleSelectionAliasError.setOnClickListener {
            showErrorMessage(string)
        }
    }

    private fun showErrorMessage(error: String?) {
        MaterialDialogHelper.showMaterialDialog(
            context = requireContext(),
            title = resources.getString(R.string.error_details),
            message = error ?: resources.getString(R.string.no_error_message),
            neutralButtonText = resources.getString(R.string.close),
        ).show()
    }

    private suspend fun forgetAliasHttpRequest(aliases: List<Aliases>, context: Context) {
        val result = viewModel.bulkForgetAlias(aliases.map { it.id })
        shouldRefreshData = true
        amountOfNetworkCallsDone = aliases.size

        when (result) {
            is NetworkResult.Success -> {
                // Recheck the UI (this will finished the activity in updateUI)
                updateUi()
            }
            is NetworkResult.Error -> {
                showError(
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_forgetting_alias), result.error
                    )
                )
            }
        }
    }

    private suspend fun restoreAliasHttpRequest(aliases: List<Aliases>, context: Context) {
        val result = viewModel.bulkRestoreAlias(aliases.map { it.id })
        shouldRefreshData = true
        amountOfNetworkCallsDone = aliases.size

        when (result) {
            is NetworkResult.Success -> {
                selectedAliases.forEach { it.deleted_at = null }

                // Restoring an alias automatically makes it active
                selectedAliases.forEach { it.active = true }

                // Recheck the UI (makes sure the switch only switches whenever all aliases have the same state)
                updateUi()
            }
            is NetworkResult.Error -> {
                showError(
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_restoring_alias), result.error
                    )
                )
            }
        }
    }

    enum class NetworkAction {
        CHANGE_ACTIVE_STATE,
        CHANGE_PINNED_STATE,
        DELETE_STATE,
        RESTORE_STATE,
        FORGET_STATE,
        CHANGE_RECIPIENTS_STATE
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddAliasMultipleSelectionBottomDialogListener {
        fun onCloseMultipleSelectionBottomDialogFragment(shouldRefreshData: Boolean)
        fun onCancelMultipleSelectionBottomDialogFragment(shouldRefreshData: Boolean)
        fun onWatchedAliasesChanged() {}
    }

    companion object {
        private const val ARG_SELECTED_ALIASES = "arg_selected_aliases"

        fun newInstance(selectedAliases: List<Aliases>): AliasMultipleSelectionBottomDialogFragment {
            return AliasMultipleSelectionBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_ALIASES, GsonTools.gson.toJson(selectedAliases))
                }
            }
        }
    }
}
