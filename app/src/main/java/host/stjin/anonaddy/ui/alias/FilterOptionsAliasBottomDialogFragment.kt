package host.stjin.anonaddy.ui.alias

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetFilterOptionsAliasBinding
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy_shared.models.AliasSortFilter


class FilterOptionsAliasBottomDialogFragment(
    private val aliasSortFilter: AliasSortFilter
) : BaseBottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var listener: AddFilterOptionsAliasBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddFilterOptionsAliasBottomDialogListener {
        fun setFilterAndSortingSettings(
            aliasSortFilter: AliasSortFilter
        )

        fun onDismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener.onDismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetFilterOptionsAliasBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetFilterOptionsAliasBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = parentFragment as AddFilterOptionsAliasBottomDialogListener

        // Set button listeners and current description
        binding.bsFilteroptionsAliasesSaveButton.setOnClickListener(this)

        loadFilter()
        setOnFilterListeners()
        setOnSortingListeners()

        return root
    }


    private fun loadFilter() {
        disableOptionsWhenWatched()

        binding.bsFilteroptionsAliasesActiveButton.isChecked = aliasSortFilter.onlyActiveAliases
        binding.bsFilteroptionsAliasesInactiveButton.isChecked = aliasSortFilter.onlyInactiveAliases
        binding.bsFilteroptionsAliasesDeletedButton.isChecked = aliasSortFilter.onlyDeletedAliases
        binding.bsFilteroptionsAliasesAllButton.isChecked =
            !aliasSortFilter.onlyActiveAliases && !aliasSortFilter.onlyInactiveAliases && !aliasSortFilter.onlyDeletedAliases

        binding.bsFilteroptionsAliasesWatchedOnlyButton.isChecked = aliasSortFilter.onlyWatchedAliases
        binding.bsFilteroptionsAliasesWatchedOnlyAllAliasesButton.isChecked = !aliasSortFilter.onlyWatchedAliases

        if (aliasSortFilter.sortDesc) {
            binding.bsFilteroptionsAliasesSortOrder.text = binding.bsFilteroptionsAliasesSortOrder.context.resources.getString(R.string.sort_desc)
            binding.bsFilteroptionsAliasesSortOrder.icon =
                ContextCompat.getDrawable(binding.bsFilteroptionsAliasesSortOrder.context, R.drawable.ic_sort_descending)
        } else {
            binding.bsFilteroptionsAliasesSortOrder.text = binding.bsFilteroptionsAliasesSortOrder.context.resources.getString(R.string.sort_asc)
            binding.bsFilteroptionsAliasesSortOrder.icon =
                ContextCompat.getDrawable(binding.bsFilteroptionsAliasesSortOrder.context, R.drawable.ic_sort_ascending)
        }


        binding.bsFilteroptionsAliasesSortingLocalPart.isChecked = aliasSortFilter.sort == "local_part"
        binding.bsFilteroptionsAliasesSortingDomain.isChecked = aliasSortFilter.sort == "domain"
        binding.bsFilteroptionsAliasesSortingEmail.isChecked = aliasSortFilter.sort == "email"
        binding.bsFilteroptionsAliasesSortingEmailForwarded.isChecked = aliasSortFilter.sort == "emails_forwarded"
        binding.bsFilteroptionsAliasesSortingEmailBlocked.isChecked = aliasSortFilter.sort == "emails_blocked"
        binding.bsFilteroptionsAliasesSortingEmailReplied.isChecked = aliasSortFilter.sort == "emails_replied"
        binding.bsFilteroptionsAliasesSortingEmailSent.isChecked = aliasSortFilter.sort == "emails_sent"
        binding.bsFilteroptionsAliasesSortingEmailActive.isChecked = aliasSortFilter.sort == "active"
        binding.bsFilteroptionsAliasesSortingEmailCreatedAt.isChecked = aliasSortFilter.sort == "created_at" || aliasSortFilter.sort == null
        binding.bsFilteroptionsAliasesSortingEmailUpdatedAt.isChecked = aliasSortFilter.sort == "updated_at"
        binding.bsFilteroptionsAliasesSortingEmailDeletedAt.isChecked = aliasSortFilter.sort == "deleted_at"


    }

    private fun setOnSortingListeners() {
        binding.bsFilteroptionsAliasesSortingLocalPart.setOnClickListener {
            aliasSortFilter.sort = "local_part"
        }
        binding.bsFilteroptionsAliasesSortingDomain.setOnClickListener {
            aliasSortFilter.sort = "domain"
        }
        binding.bsFilteroptionsAliasesSortingEmail.setOnClickListener {
            aliasSortFilter.sort = "email"
        }
        binding.bsFilteroptionsAliasesSortingEmailForwarded.setOnClickListener {
            aliasSortFilter.sort = "emails_forwarded"
        }
        binding.bsFilteroptionsAliasesSortingEmailBlocked.setOnClickListener {
            aliasSortFilter.sort = "emails_blocked"
        }
        binding.bsFilteroptionsAliasesSortingEmailReplied.setOnClickListener {
            aliasSortFilter.sort = "emails_replied"
        }
        binding.bsFilteroptionsAliasesSortingEmailSent.setOnClickListener {
            aliasSortFilter.sort = "emails_sent"
        }
        binding.bsFilteroptionsAliasesSortingEmailActive.setOnClickListener {
            aliasSortFilter.sort = "active"
        }
        binding.bsFilteroptionsAliasesSortingEmailCreatedAt.setOnClickListener {
            aliasSortFilter.sort = "created_at"
        }
        binding.bsFilteroptionsAliasesSortingEmailUpdatedAt.setOnClickListener {
            aliasSortFilter.sort = "updated_at"
        }
        binding.bsFilteroptionsAliasesSortingEmailDeletedAt.setOnClickListener {
            aliasSortFilter.sort = "deleted_at"
        }
        binding.bsFilteroptionsAliasesSortOrder.setOnClickListener {
            aliasSortFilter.sortDesc = !aliasSortFilter.sortDesc
            loadFilter()
        }
    }


    private fun setOnFilterListeners() {
        binding.bsFilteroptionsAliasesClearFilter.setOnClickListener {
            aliasSortFilter.onlyActiveAliases = false
            aliasSortFilter.onlyInactiveAliases = false
            aliasSortFilter.onlyWatchedAliases = false
            aliasSortFilter.onlyDeletedAliases = false
            aliasSortFilter.sort = null
            aliasSortFilter.sortDesc = false

            loadFilter()
        }

        binding.bsFilteroptionsAliasesAllButton.setOnClickListener {
            aliasSortFilter.onlyActiveAliases = false
            aliasSortFilter.onlyInactiveAliases = false
            aliasSortFilter.onlyDeletedAliases = false
            loadFilter()
        }

        binding.bsFilteroptionsAliasesActiveButton.setOnClickListener {
            aliasSortFilter.onlyActiveAliases = true
            aliasSortFilter.onlyInactiveAliases = false
            aliasSortFilter.onlyDeletedAliases = false
            loadFilter()
        }

        binding.bsFilteroptionsAliasesInactiveButton.setOnClickListener {
            aliasSortFilter.onlyActiveAliases = false
            aliasSortFilter.onlyInactiveAliases = true
            aliasSortFilter.onlyDeletedAliases = false
            loadFilter()
        }

        binding.bsFilteroptionsAliasesDeletedButton.setOnClickListener {
            aliasSortFilter.onlyActiveAliases = false
            aliasSortFilter.onlyInactiveAliases = false
            aliasSortFilter.onlyDeletedAliases = true
            loadFilter()
        }

        binding.bsFilteroptionsAliasesWatchedOnlyButton.setOnClickListener {
            aliasSortFilter.onlyWatchedAliases = true
            loadFilter()
        }

        binding.bsFilteroptionsAliasesWatchedOnlyAllAliasesButton.setOnClickListener {
            aliasSortFilter.onlyWatchedAliases = false
            loadFilter()
        }
    }

    // Have an empty constructor the prevent the "could not find Fragment constructor when changing theme or rotating when the dialog is open"
    constructor() : this(
        AliasSortFilter(
            onlyActiveAliases = false,
            onlyDeletedAliases = false,
            onlyInactiveAliases = false,
            onlyWatchedAliases = false,
            sort = null,
            sortDesc = false,
            filter = null
        )
    ) {
        loadFilter()
    }

    companion object {
        fun newInstance(
            aliasSortFilter: AliasSortFilter
        ): FilterOptionsAliasBottomDialogFragment {
            return FilterOptionsAliasBottomDialogFragment(
                aliasSortFilter
            )
        }
    }


    override fun onResume() {
        super.onResume()
        disableWatchedOption()
    }

    private fun disableOptionsWhenWatched() {
        binding.bsFilteroptionsAliasesMbtg.isEnabled = !aliasSortFilter.onlyWatchedAliases
        binding.bsFilteroptionsAliasesSortingChipgroup.children.forEach { it.isEnabled = !aliasSortFilter.onlyWatchedAliases }
        binding.bsFilteroptionsAliasesSortOrder.isEnabled = !aliasSortFilter.onlyWatchedAliases
    }

    private fun disableWatchedOption() {
        val aliasWatcher = AliasWatcher(requireContext())
        val aliasesToWatch = aliasWatcher.getAliasesToWatch().toList()
        binding.bsFilteroptionsAliasesWatchedMbtg.isEnabled = aliasesToWatch.isNotEmpty()
    }


    override fun onClick(p0: View?) {
        if (p0 != null) {
            when (p0.id) {
                R.id.bs_filteroptions_aliases_save_button -> {
                    listener.setFilterAndSortingSettings(aliasSortFilter)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}