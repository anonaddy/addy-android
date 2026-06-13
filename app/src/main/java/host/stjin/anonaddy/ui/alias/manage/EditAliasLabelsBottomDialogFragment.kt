package host.stjin.anonaddy.ui.alias.manage

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetEditLabelsAliasBinding
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.Labels
import kotlinx.coroutines.launch

import host.stjin.anonaddy.ui.labels.ManageLabelsAddBottomDialogFragment

class EditAliasLabelsBottomDialogFragment(
    private val aliasIds: List<String>?,
    private val currentLabels: List<Labels>?
) : BaseBottomSheetDialogFragment(), View.OnClickListener, ManageLabelsAddBottomDialogFragment.AddLabelsBottomDialogListener {
    private lateinit var listener: AddEditAliasLabelsBottomDialogListener

    private var _binding: BottomsheetEditLabelsAliasBinding? = null
    private val binding get() = _binding!!

    private var allLabels: List<Labels>? = null
    private var recentlyAddedLabelId: String? = null

    constructor() : this(null, null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditLabelsAliasBinding.inflate(inflater, container, false)
        val root = binding.root

        if (!aliasIds.isNullOrEmpty()) {
            listener = if (parentFragment != null && parentFragment is AddEditAliasLabelsBottomDialogListener) {
                parentFragment as AddEditAliasLabelsBottomDialogListener
            } else {
                activity as AddEditAliasLabelsBottomDialogListener
            }

            binding.bsEditLabelsAliasSaveButton.setOnClickListener(this)
            binding.bsEditLabelsAliasCreateButton.setOnClickListener(this)
            
            binding.bsEditLabelsAliasSaveButton.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                loadLabels(requireContext())
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
            loadLabels(requireContext())
        }
    }

    private suspend fun loadLabels(context: Context) {
        val networkHelper = NetworkHelper(context)
        networkHelper.getAllLabels { labels, error ->
            binding.bsEditLabelsAliasProgressbar.visibility = View.GONE
            binding.bsEditLabelsAliasSaveButton.isEnabled = true
            if (labels != null) {
                allLabels = labels
                populateChips(labels)
            } else {
                binding.bsEditLabelsAliasError.visibility = View.VISIBLE
                binding.bsEditLabelsAliasError.text = error
            }
        }
    }

    private fun populateChips(labels: List<Labels>) {
        val currentLabelIds = currentLabels?.map { it.id }?.toMutableList() ?: mutableListOf()
        if (recentlyAddedLabelId != null && !currentLabelIds.contains(recentlyAddedLabelId)) {
            currentLabelIds.add(recentlyAddedLabelId!!)
        }

        binding.bsEditLabelsAliasChipgroup.removeAllViews()

        for (label in labels) {
            val chip = Chip(requireContext())
            chip.text = label.name
            chip.isCheckable = true
            chip.isChecked = currentLabelIds.contains(label.id)
            chip.tag = label.id
            try {
                val colorInt = android.graphics.Color.parseColor(label.colour)
                val alphaColor = android.graphics.Color.argb(
                    (0.2 * 255).toInt(),
                    android.graphics.Color.red(colorInt),
                    android.graphics.Color.green(colorInt),
                    android.graphics.Color.blue(colorInt)
                )
                chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(alphaColor)
                chip.chipStrokeWidth = 0f

                val dotDrawable = android.graphics.drawable.GradientDrawable()
                dotDrawable.shape = android.graphics.drawable.GradientDrawable.OVAL
                dotDrawable.setColor(colorInt)
                dotDrawable.setSize(24, 24)
                
                val insetDrawable = android.graphics.drawable.InsetDrawable(dotDrawable, 6, 6, 6, 6)
                chip.chipIcon = insetDrawable
                chip.isChipIconVisible = true
                chip.checkedIconTint = android.content.res.ColorStateList.valueOf(colorInt)
            } catch (e: Exception) {
            }
            binding.bsEditLabelsAliasChipgroup.addView(chip)
        }
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
            saveLabels(requireContext())
        } else if (p0?.id == R.id.bs_edit_labels_alias_create_button) {
            val manageLabelsAddBottomDialogFragment = ManageLabelsAddBottomDialogFragment.newInstance(null)
            manageLabelsAddBottomDialogFragment.show(
                childFragmentManager,
                "manageLabelsAddBottomDialogFragment"
            )
        }
    }

    private fun saveLabels(context: Context) {
        val selectedLabelIds = ArrayList<String>()
        for (i in 0 until binding.bsEditLabelsAliasChipgroup.childCount) {
            val chip = binding.bsEditLabelsAliasChipgroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedLabelIds.add(chip.tag as String)
            }
        }

        binding.bsEditLabelsAliasSaveButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            saveLabelsHttp(context, selectedLabelIds)
        }
    }

    private suspend fun saveLabelsHttp(context: Context, selectedLabelIds: ArrayList<String>) {
        val networkHelper = NetworkHelper(context)
        val ids = ArrayList(aliasIds!!)
        networkHelper.bulkUpdateAliasesLabels({ response, error ->
            if (response != null) {
                listener.labelsEdited()
            } else {
                binding.bsEditLabelsAliasSaveButton.revertAnimation()
                binding.bsEditLabelsAliasError.visibility = View.VISIBLE
                binding.bsEditLabelsAliasError.text = error
            }
        }, ids, selectedLabelIds)
    }

    interface AddEditAliasLabelsBottomDialogListener {
        fun labelsEdited()
    }

    companion object {
        fun newInstance(ids: List<String>, labels: List<Labels>?): EditAliasLabelsBottomDialogFragment {
            return EditAliasLabelsBottomDialogFragment(ids, labels)
        }
    }
}
