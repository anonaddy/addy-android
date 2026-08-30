package host.stjin.anonaddy.ui.labels
import host.stjin.anonaddy_shared.utils.GsonTools

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.ColorPickerAdapter
import host.stjin.anonaddy.databinding.BottomsheetManageLabelsBinding
import host.stjin.anonaddy.ui.customviews.FlexboxItemDecoration
import host.stjin.anonaddy_shared.models.Labels
import host.stjin.anonaddy_shared.models.NewLabelEntry
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch



class AddLabelBottomDialogFragment : BaseBottomSheetDialogFragment() {
    private val viewModel: LabelsViewModel by viewModels()
    private var existingLabel: Labels? = null

    private var listener: AddLabelsBottomDialogListener? = null

    private var _binding: BottomsheetManageLabelsBinding? = null
    private val binding get() = _binding!!

    private lateinit var colorPickerAdapter: ColorPickerAdapter
    private var selectedColor: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_EXISTING_LABEL_JSON)?.let { json ->
            existingLabel = try {
                GsonTools.gson.fromJson(json, Labels::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetManageLabelsBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = (parentFragment as? AddLabelsBottomDialogListener) ?: (activity as? AddLabelsBottomDialogListener)

        setupColorDropdown()

        if (existingLabel != null) {
            binding.bsManageLabelsTitle.text = requireContext().resources.getString(R.string.edit_label)
            binding.bsManageLabelsAddButton.text = requireContext().resources.getString(R.string.save)
            binding.bsManageLabelsAddNameEdittext.setText(existingLabel?.name)
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
                val currentLabel = existingLabel
                if (currentLabel != null) {
                    when (val result = viewModel.updateLabel(currentLabel.id, NewLabelEntry(name, color))) {
                        is NetworkResult.Success -> {
                            listener?.onAddedLabelEntry(result.data)
                            dismissAllowingStateLoss()
                        }
                        is NetworkResult.Error -> {
                            binding.bsManageLabelsAddButton.revertAnimation()
                            binding.bsManageLabelsAddNameTil.error =
                                requireContext().resources.getString(R.string.error_creating_label) + "\n" + result.error
                        }
                    }
                } else {
                    when (val result = viewModel.addNewLabel(NewLabelEntry(name, color))) {
                        is NetworkResult.Success -> {
                            listener?.onAddedLabelEntry(result.data)
                            dismissAllowingStateLoss()
                        }
                        is NetworkResult.Error -> {
                            binding.bsManageLabelsAddButton.revertAnimation()
                            binding.bsManageLabelsAddNameTil.error =
                                requireContext().resources.getString(R.string.error_creating_label) + "\n" + result.error
                        }
                    }
                }
            }
        }

        return root
    }

    private fun setupColorDropdown() {
        val colors = listOf("#06b6d4", "#22c55e", "#eab308", "#f97316", "#ef4444", "#8b5cf6", "#64748b", "#ec4899", "#14b8a6", "#3b82f6")
        colorPickerAdapter = ColorPickerAdapter(colors)

        val currentLabel = existingLabel
        if (currentLabel != null) {
            selectedColor = currentLabel.colour
            colorPickerAdapter.selectedColor = currentLabel.colour
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
        private const val ARG_EXISTING_LABEL_JSON = "arg_existing_label_json"

        fun newInstance(label: Labels?): AddLabelBottomDialogFragment {
            return AddLabelBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    label?.let { putString(ARG_EXISTING_LABEL_JSON, GsonTools.gson.toJson(it)) }
                }
            }
        }
    }
}
