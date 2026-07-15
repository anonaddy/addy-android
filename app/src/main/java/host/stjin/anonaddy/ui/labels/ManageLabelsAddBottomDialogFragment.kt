package host.stjin.anonaddy.ui.labels

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.ColorPickerAdapter
import host.stjin.anonaddy.databinding.BottomsheetManageLabelsBinding
import host.stjin.anonaddy.ui.customviews.FlexboxItemDecoration
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Labels
import host.stjin.anonaddy_shared.models.NewLabelEntry
import kotlinx.coroutines.launch


class ManageLabelsAddBottomDialogFragment(
    private val existingLabel: Labels?
) : BaseBottomSheetDialogFragment() {
    private lateinit var listener: AddLabelsBottomDialogListener

    private var _binding: BottomsheetManageLabelsBinding? = null
    private val binding get() = _binding!!

    private lateinit var colorPickerAdapter: ColorPickerAdapter
    private var selectedColor: String? = null

    // Keep an empty constructor for Android component instantiation
    constructor() : this(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetManageLabelsBinding.inflate(inflater, container, false)
        val root = binding.root

        if (parentFragment != null && parentFragment is AddLabelsBottomDialogListener) {
            listener = parentFragment as AddLabelsBottomDialogListener
        } else if (activity != null && activity is AddLabelsBottomDialogListener) {
            listener = activity as AddLabelsBottomDialogListener
        }

        setupColorDropdown()

        if (existingLabel != null) {
            binding.bsManageLabelsTitle.text = requireContext().resources.getString(R.string.edit_label)
            binding.bsManageLabelsAddButton.text = requireContext().resources.getString(R.string.save)
            binding.bsManageLabelsAddNameEdittext.setText(existingLabel.name)
        } else {
            binding.bsManageLabelsTitle.text = requireContext().resources.getString(R.string.new_label)
            binding.bsManageLabelsAddButton.text = requireContext().resources.getString(R.string.add)
        }

        binding.bsManageLabelsAddButton.setOnClickListener {
            val name = binding.bsManageLabelsAddNameEdittext.text.toString().trim()
            val color = selectedColor

            if (name.isEmpty()) {
                binding.bsManageLabelsAddNameTil.error = requireContext().resources.getString(R.string.cannot_be_empty)
                return@setOnClickListener
            } else {
                binding.bsManageLabelsAddNameTil.error = null
            }

            if (color.isNullOrEmpty()) {
                // Default to a color or show error.
                return@setOnClickListener
            }

            binding.bsManageLabelsAddButton.startAnimation()

            viewLifecycleOwner.lifecycleScope.launch {
                val networkHelper = NetworkHelper(requireContext())
                if (existingLabel != null) {
                    networkHelper.updateLabel({ label, error ->
                        if (label != null) {
                            if (::listener.isInitialized) {
                                listener.onAddedLabelEntry(label)
                            }
                            dismissAllowingStateLoss()
                        } else {
                            binding.bsManageLabelsAddButton.revertAnimation()
                            binding.bsManageLabelsAddNameTil.error =
                                requireContext().resources.getString(R.string.error_creating_label) + "\n" + error
                        }
                    }, existingLabel.id, NewLabelEntry(name, color))
                } else {
                    networkHelper.addNewLabel({ label, error ->
                        if (label != null) {
                            if (::listener.isInitialized) {
                                listener.onAddedLabelEntry(label)
                            }
                            dismissAllowingStateLoss()
                        } else {
                            binding.bsManageLabelsAddButton.revertAnimation()
                            binding.bsManageLabelsAddNameTil.error =
                                requireContext().resources.getString(R.string.error_creating_label) + "\n" + error
                        }
                    }, NewLabelEntry(name, color))
                }
            }
        }

        return root
    }

    private fun setupColorDropdown() {
        val colors = listOf("#06b6d4", "#22c55e", "#eab308", "#f97316", "#ef4444", "#8b5cf6", "#64748b", "#ec4899", "#14b8a6", "#3b82f6")
        colorPickerAdapter = ColorPickerAdapter(requireContext(), colors)

        if (existingLabel != null) {
            selectedColor = existingLabel.colour
            colorPickerAdapter.selectedColor = existingLabel.colour
        } else {
            selectedColor = colors[0]
            colorPickerAdapter.selectedColor = colors[0]
        }

        val layoutManager = FlexboxLayoutManager(context)
        layoutManager.flexWrap = FlexWrap.WRAP
        layoutManager.justifyContent = JustifyContent.FLEX_START
        binding.bsManageLabelsAddColorRv.layoutManager = layoutManager
        binding.bsManageLabelsAddColorRv.adapter = colorPickerAdapter
        // Add spacing
        val spacing = resources.getDimensionPixelSize(R.dimen.layout_padding)
        binding.bsManageLabelsAddColorRv.addItemDecoration(FlexboxItemDecoration(spacing))

        colorPickerAdapter.setClickListener(object : ColorPickerAdapter.ClickListener {
            override fun onClick(pos: Int, color: String) {
                selectedColor = color
            }
        })
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

    interface AddLabelsBottomDialogListener {
        fun onAddedLabelEntry(label: Labels)
    }

    companion object {
        fun newInstance(label: Labels?): ManageLabelsAddBottomDialogFragment {
            return ManageLabelsAddBottomDialogFragment(label)
        }
    }
}