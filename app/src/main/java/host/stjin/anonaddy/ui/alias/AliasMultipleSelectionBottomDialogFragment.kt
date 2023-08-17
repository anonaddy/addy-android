package host.stjin.anonaddy.ui.alias

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetMultipleSelectionAliasBinding
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Aliases
import kotlinx.coroutines.launch


class AliasMultipleSelectionBottomDialogFragment(private val selectedAliases: List<Aliases>) : BaseBottomSheetDialogFragment() {

    lateinit var networkHelper: NetworkHelper
    private lateinit var listener: AddAliasMultipleSelectionBottomDialogListener
    private lateinit var aliasWatcher: AliasWatcher
    private var forceSwitch = false
    private var networkAction: NetworkAction? = null

    enum class NetworkAction {
        CHANGE_ACTIVE_STATE,
        DELETE_STATE,
        RESTORE_STATE,
        FORGET_STATE
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddAliasMultipleSelectionBottomDialogListener {
        fun onCloseMultipleSelectionBottomDialogFragment(shouldRefreshData: Boolean)
        fun onCancelMultipleSelectionBottomDialogFragment(shouldRefreshData: Boolean)
    }

    private var shouldRefreshData = false
    override fun onCancel(dialog: DialogInterface) {
        listener.onCloseMultipleSelectionBottomDialogFragment(shouldRefreshData)
        super.onCancel(dialog)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetMultipleSelectionAliasBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetMultipleSelectionAliasBinding.inflate(inflater, container, false)
        val root = binding.root
        listener = parentFragment as AddAliasMultipleSelectionBottomDialogListener

        networkHelper = NetworkHelper(requireContext())
        aliasWatcher = AliasWatcher(requireContext())

        updateUi()
        setOnSwitchChangeListeners()
        setOnClickListeners()

        return root

    }

    private fun updateUi() {
        binding.bsMultipleSelectionAliasTitle.text = resources.getString(R.string.multiple_alias_selected, selectedAliases.count())

        // No need for the created and updated views
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasCreatedAt.visibility = View.GONE
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasUpdatedAt.visibility = View.GONE
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRecipientsEdit.visibility = View.GONE
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasDescEdit.visibility = View.GONE

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

            NetworkAction.DELETE_STATE -> {
                // Show the progressbar if
                // the amount of network calls does not match the total network calls that have to be done (which is the selected aliases amount)
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasDelete.showProgressBar(selectedAliases.count() != amountOfNetworkCallsDone)

                // If the state is DELETE state that means that delete calls have been initiated
                // When all the calls are done, close the dialog
                if (selectedAliases.count() == amountOfNetworkCallsDone) {
                    listener.onCloseMultipleSelectionBottomDialogFragment(true)
                }
            }

            NetworkAction.RESTORE_STATE -> {
                // Show the progressbar if
                // the amount of network calls does not match the total network calls that have to be done (which is the selected aliases amount)
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRestore.showProgressBar(selectedAliases.count() != amountOfNetworkCallsDone)

                // If the state is FORGET state that means that delete calls have been initiated
                // When all the calls are done, close the dialog
                if (selectedAliases.count() == amountOfNetworkCallsDone) {
                    listener.onCloseMultipleSelectionBottomDialogFragment(true)
                }
            }

            NetworkAction.FORGET_STATE -> {
                // Show the progressbar if
                // the amount of network calls does not match the total network calls that have to be done (which is the selected aliases amount)
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasForget.showProgressBar(selectedAliases.count() != amountOfNetworkCallsDone)

                // If the state is FORGET state that means that forget calls have been initiated
                // When all the calls are done, close the dialog
                if (selectedAliases.count() == amountOfNetworkCallsDone) {
                    listener.onCloseMultipleSelectionBottomDialogFragment(true)
                }
            }

            else -> { /* Do nothing */
            }
        }


        // Get watched aliases
        val watchedAliases = AliasWatcher(requireContext()).getAliasesToWatch()
        // If all aliases are on the watchlist, check the switch by default
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasWatchSwitchLayout.setSwitchChecked(selectedAliases.all {
            watchedAliases.contains(
                it.id
            )
        })

    }


    private var amountOfNetworkCallsDone = -1
    private suspend fun deactivateAlias(aliases: List<Aliases>) {
        networkHelper.bulkDeactivateAlias({ alias, error ->
            amountOfNetworkCallsDone = aliases.size
            shouldRefreshData = true
            if (alias != null) {
                selectedAliases.forEach { it.active = false }
                // Recheck the UI (this will finished the activity in updateUI)
                updateUi()
            } else {
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(true)
                showError(
                    requireContext().resources.getString(
                        R.string.s_s,
                        this.resources.getString(R.string.error_edit_active), error,
                    )
                )
            }
        }, aliases)
    }


    private suspend fun activateAlias(aliases: List<Aliases>) {
        networkHelper.bulkActivateAlias({ alias, error ->
            amountOfNetworkCallsDone = aliases.size
            shouldRefreshData = true
            if (alias != null) {
                selectedAliases.forEach { it.active = true }
                // Recheck the UI (makes sure the switch only switches whenever all aliases have the same state)
                updateUi()
            } else {
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(false)
                showError(
                    requireContext().resources.getString(
                        R.string.s_s,
                        requireContext().resources.getString(R.string.error_edit_active), error
                    )
                )
            }
        }, aliases)
    }

    private fun setOnSwitchChangeListeners() {
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
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
        })

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasWatchSwitchLayout.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
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
                    // No need to update the UI here, the switch already does the switching and its all done at this point
                }
            }
        })
    }

    private fun setOnClickListeners() {

        binding.bsMultipleSelectionAliasCancel.setOnClickListener {
            listener.onCancelMultipleSelectionBottomDialogFragment(shouldRefreshData)
        }

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(!binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.getSwitchChecked())
            }
        })


        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasWatchSwitchLayout.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasWatchSwitchLayout.setSwitchChecked(!binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasWatchSwitchLayout.getSwitchChecked())
            }
        })

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasDelete.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                deleteAlias()
            }
        })

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasForget.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forgetAlias()
            }
        })

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRestore.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                restoreAlias()
            }
        })
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
        networkHelper.bulkDeleteAlias({ alias, error ->
            shouldRefreshData = true
            amountOfNetworkCallsDone = aliases.size

            if (alias != null) {
                // Recheck the UI (this will finished the activity in updateUI)
                updateUi()
            } else {
                showError(
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_deleting_alias), error
                    )
                )
            }
        }, aliases)
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
        networkHelper.bulkForgetAlias({ alias, error ->
            shouldRefreshData = true
            amountOfNetworkCallsDone = aliases.size

            if (alias != null) {
                // Recheck the UI (this will finished the activity in updateUI)
                updateUi()
            } else {
                showError(
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_forgetting_alias), error
                    )
                )
            }
        }, aliases)
    }

    private suspend fun restoreAliasHttpRequest(aliases: List<Aliases>, context: Context) {
        networkHelper.bulkRestoreAlias({ alias, error ->
            shouldRefreshData = true
            amountOfNetworkCallsDone = aliases.size

            if (alias != null) {
                selectedAliases.forEach { it.deleted_at = null }

                // Restoring an alias automatically makes it active
                selectedAliases.forEach { it.active = true }

                // Recheck the UI (makes sure the switch only switches whenever all aliases have the same state)
                updateUi()
            } else {
                showError(
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_restoring_alias), error
                    )
                )
            }
        }, aliases)
    }

    companion object {
        fun newInstance(selectedAliases: List<Aliases>): AliasMultipleSelectionBottomDialogFragment {
            return AliasMultipleSelectionBottomDialogFragment(selectedAliases)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}