package host.stjin.anonaddy.ui.aliases.manage

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetEditLabelsAliasBinding
import host.stjin.anonaddy.ui.labels.AddLabelBottomDialogFragment
import host.stjin.anonaddy.utils.LabelUtils
import host.stjin.anonaddy_shared.models.Labels
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch

class EditAliasLabelsBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener, AddLabelBottomDialogFragment.AddLabelsBottomDialogListener {
    private val viewModel: ManageAliasViewModel by activityViewModels()
    private var aliasIds: ArrayList<String>? = null
    private var currentLabelIds: ArrayList<String>? = null

    private var listener: AddEditAliasLabelsBottomDialogListener? = null

    private var _binding: BottomsheetEditLabelsAliasBinding? = null
    private val binding get() = _binding!!

    private var allLabels: List<Labels>? = null
    private var recentlyAddedLabelId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            aliasIds = it.getStringArrayList(ARG_ALIAS_IDS)
            currentLabelIds = it.getStringArrayList(ARG_CURRENT_LABEL_IDS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditLabelsAliasBinding.inflate(inflater, container, false)
        val root = binding.root

        if (!aliasIds.isNullOrEmpty()) {
            listener = (parentFragment as? AddEditAliasLabelsBottomDialogListener) ?: (activity as? AddEditAliasLabelsBottomDialogListener)

            binding.bsEditLabelsAliasSaveButton.setOnClickListener(this)
            binding.bsEditLabelsAliasCreateButton.setOnClickListener(this)

            binding.bsEditLabelsAliasSaveButton.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                loadLabels()
            }
        } else {
            dismiss()
        }
        return root
    }

    override fun onAddedLabelEntry(label: Labels) {
        recentlyAddedLabelId = label.id
        binding.bsEditLabelsAliasSaveButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            loadLabels()
        }
    }

    private suspend fun loadLabels() {
        val result = viewModel.getAllLabels()
        binding.bsEditLabelsAliasSaveButton.isEnabled = true
        when (result) {
            is NetworkResult.Success -> {
                val labels = result.data.data
                allLabels = labels
                populateChips(labels)
            }
            is NetworkResult.Error -> {
                binding.bsEditLabelsAliasError.visibility = View.VISIBLE
                binding.bsEditLabelsAliasError.text = result.error
            }
        }
    }

    private fun populateChips(labels: List<Labels>) {
        binding.bsEditLabelsAliasChipgroup.removeAllViews()
        val labelIds = currentLabelIds?.toMutableList() ?: mutableListOf()
        if (recentlyAddedLabelId != null && !labelIds.contains(recentlyAddedLabelId)) {
            labelIds.add(recentlyAddedLabelId!!)
        }

        LabelUtils.populateLabelsChipGroup(
            requireContext(),
            binding.bsEditLabelsAliasChipgroup,
            labels,
            labelIds
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onClick(p0: View?) {
        if (p0?.id == R.id.bs_edit_labels_alias_save_button) {
            saveLabels()
        } else if (p0?.id == R.id.bs_edit_labels_alias_create_button) {
            val labelsAddBottomDialogFragment = AddLabelBottomDialogFragment.newInstance(null)
            labelsAddBottomDialogFragment.show(
                childFragmentManager,
                "labelsAddBottomDialogFragment"
            )
        }
    }

    private fun saveLabels() {
        val selectedLabelIds = ArrayList<String>()
        for (i in 0 until binding.bsEditLabelsAliasChipgroup.childCount) {
            val chip = binding.bsEditLabelsAliasChipgroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedLabelIds.add(chip.tag as String)
            }
        }

        binding.bsEditLabelsAliasSaveButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            saveLabelsHttp(selectedLabelIds)
        }
    }

    private suspend fun saveLabelsHttp(selectedLabelIds: ArrayList<String>) {
        val ids = ArrayList(aliasIds!!)
        when (val result = viewModel.bulkUpdateAliasesLabels(ids, selectedLabelIds)) {
            is NetworkResult.Success -> {
                listener?.labelsEdited()
            }
            is NetworkResult.Error -> {
                binding.bsEditLabelsAliasSaveButton.revertAnimation()
                binding.bsEditLabelsAliasError.visibility = View.VISIBLE
                binding.bsEditLabelsAliasError.text = result.error
            }
        }
    }

    interface AddEditAliasLabelsBottomDialogListener {
        fun labelsEdited()
    }

    companion object {
        private const val ARG_ALIAS_IDS = "arg_alias_ids"
        private const val ARG_CURRENT_LABEL_IDS = "arg_current_label_ids"

        fun newInstance(ids: List<String>, labels: List<Labels>?): EditAliasLabelsBottomDialogFragment {
            return EditAliasLabelsBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_ALIAS_IDS, ArrayList(ids))
                    putStringArrayList(ARG_CURRENT_LABEL_IDS, labels?.map { it.id }?.let { ArrayList(it) } ?: arrayListOf())
                }
            }
        }
    }
}
