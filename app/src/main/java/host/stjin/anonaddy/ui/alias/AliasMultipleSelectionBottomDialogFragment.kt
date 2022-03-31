package host.stjin.anonaddy.ui.alias

import android.app.Dialog
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
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


class AliasMultipleSelectionBottomDialogFragment(private val selectedAliases: List<Aliases>) : BaseBottomSheetDialogFragment() {

    lateinit var networkHelper: NetworkHelper
    private lateinit var listener: AddAliasMultipleSelectionBottomDialogListener
    private lateinit var aliasWatcher: AliasWatcher
    private var forceSwitch = false

    // 1. Defines the listener interface with a method passing back data result.
    interface AddAliasMultipleSelectionBottomDialogListener {
        fun onCloseMultipleSelectionBottomDialogFragment()
    }

    override fun onCancel(dialog: DialogInterface) {
        listener.onCloseMultipleSelectionBottomDialogFragment()
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
        binding.bsMultipleSelectionAliasTextview.text = resources.getString(R.string.multiple_alias_selected, selectedAliases.count())

        // No need for the created and updated views
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasCreatedAt.visibility = View.GONE
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasUpdatedAt.visibility = View.GONE

        // Check if there are any aliases that are NOT deleted
        // if there is any alias that is not deleted, enable the delete section. Else all aliases are deleted so disable the section
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasDelete.setLayoutEnabled(selectedAliases.any { it.deleted_at != null })
        // Now do the same for deleted aliases (restore section)
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRestore.setLayoutEnabled(selectedAliases.any { it.deleted_at == null })

        // If all aliases are active, check the switch by default
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(selectedAliases.all { it.active })

        // Show the progessbar if
        // 1. The value is not -1 (which is the default value, before any call)
        // AND
        // 2. the amount of network calls does not match the total network calls that have to be done (which is the selected aliases amount)
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.showProgressBar(selectedAliases.count() != amountOfNetworkCallsDone && amountOfNetworkCallsDone != -1)


        // Get watched aliases
        val watchedAliases = AliasWatcher(requireContext()).getAliasesToWatch()
        // If all aliases are on the watchlist, check the switch by default
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasWatchSwitchLayout.setSwitchChecked(selectedAliases.all {
            watchedAliases.contains(
                it.id
            )
        })

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRecipientsEdit.setDescription(resources.getString(R.string.multiple_alias_recipient_desc))
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasDescEdit.setDescription(resources.getString(R.string.multiple_alias_description_desc))

    }

    private var amountOfNetworkCallsDone = -1
    private suspend fun deactivateAlias(aliasId: String) {
        networkHelper.deactivateSpecificAlias({ result ->
            amountOfNetworkCallsDone++
            if (result == "204") {
                selectedAliases.first { it.id == aliasId }.active = false
                updateUi()
            } else {
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(true)
                SnackbarHelper.createSnackbar(
                    requireContext(),
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    binding.root,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, aliasId)
    }


    private suspend fun activateAlias(aliasId: String) {
        networkHelper.activateSpecificAlias({ alias, result ->
            amountOfNetworkCallsDone++
            if (alias != null) {
                selectedAliases.first { it.id == aliasId }.active = true
                updateUi()
            } else {
                binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    requireContext(),
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    binding.bsMultipleSelectionAliasRoot,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, aliasId)
    }

    private fun setOnSwitchChangeListeners() {
        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    amountOfNetworkCallsDone = 0
                    binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasActiveSwitchLayout.showProgressBar(true)
                    forceSwitch = false
                    if (checked) {
                        for (alias in selectedAliases) {
                            // If the alias is already active, don't make an unnecessary call and increment amountOfNetworkCallsDone
                            if (alias.active) {
                                amountOfNetworkCallsDone++
                                continue
                            }
                            lifecycleScope.launch {
                                activateAlias(alias.id)
                            }
                        }
                    } else {
                        for (alias in selectedAliases) {
                            // If the alias is already inactive, don't make an unnecessary call and increment amountOfNetworkCallsDone
                            if (!alias.active) {
                                amountOfNetworkCallsDone++
                                continue
                            }

                            lifecycleScope.launch {
                                deactivateAlias(alias.id)
                            }
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
                    if (checked) {
                        for (alias in selectedAliases) {
                            aliasWatcher.addAliasToWatch(alias.id)
                        }
                    } else {
                        for (alias in selectedAliases) {
                            aliasWatcher.removeAliasToWatch(alias.id)
                        }
                    }
                }
            }
        })
    }

    private fun setOnClickListeners() {
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

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasDescEdit.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                /*if (!editAliasDescriptionBottomDialogFragment.isAdded) {
                    editAliasDescriptionBottomDialogFragment.show(
                        supportFragmentManager,
                        "editAliasDescriptionBottomDialogFragment"
                    )
                }*/
            }
        })

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRecipientsEdit.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                /*if (!editAliasRecipientsBottomDialogFragment.isAdded) {
                    editAliasRecipientsBottomDialogFragment.show(
                        supportFragmentManager,
                        "editAliasRecipientsBottomDialogFragment"
                    )
                }*/
            }
        })

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasDelete.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                //deleteAlias()
            }
        })

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasForget.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                //forgetAlias()
            }
        })

        binding.bsMultipleSelectionAliasGeneralActions.activityManageAliasRestore.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                //restoreAlias()
            }
        })
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