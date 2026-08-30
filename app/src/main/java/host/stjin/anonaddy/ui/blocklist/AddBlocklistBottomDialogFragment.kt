package host.stjin.anonaddy.ui.blocklist

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetManageBlocklistAddBinding
import host.stjin.anonaddy_shared.models.NewBlocklistEntry
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch


class AddBlocklistBottomDialogFragment : BaseBottomSheetDialogFragment() {
    private val viewModel: BlocklistViewModel by activityViewModels()
    private var listener: AddBlocklistBottomDialogListener? = null

    private var _binding: BottomsheetManageBlocklistAddBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetManageBlocklistAddBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = (parentFragment as? AddBlocklistBottomDialogListener) ?: (activity as? AddBlocklistBottomDialogListener)

        setupDropdown()

        binding.bsManageBlocklistAddButton.setOnClickListener {
            validateAndAdd()
        }

        return root
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

    private fun validateAndAdd() {
        val type = binding.bsManageBlocklistAddTypeAutocomplete.text.toString().lowercase()
        val value = binding.bsManageBlocklistAddValueEdittext.text.toString().trim()

        if (value.isEmpty()) {
            binding.bsManageBlocklistAddValueTil.error = getString(R.string.this_field_cannot_be_empty)
            return
        }

        val isValid = if (type == "email") {
            Patterns.EMAIL_ADDRESS.matcher(value).matches()
        } else {
            Patterns.DOMAIN_NAME.matcher(value).matches()
        }

        if (isValid) {
            binding.bsManageBlocklistAddValueTil.error = null
            binding.bsManageBlocklistAddButton.startAnimation()

            viewLifecycleOwner.lifecycleScope.launch {
                addBlocklistEntry(requireContext(), NewBlocklistEntry(type, value))
            }
        } else {
            binding.bsManageBlocklistAddValueTil.error = if (type == "email") {
                getString(R.string.not_a_valid_address)
            } else {
                getString(R.string.not_a_valid_domain)
            }
        }
    }

    private suspend fun addBlocklistEntry(
        context: Context,
        newBlocklistEntry: NewBlocklistEntry
    ) {
        when (val result = viewModel.addBlocklistEntry(newBlocklistEntry)) {
            is NetworkResult.Success -> {
                listener?.onAddedBlocklistEntry(newBlocklistEntry)
            }
            is NetworkResult.Error -> {
                binding.bsManageBlocklistAddButton.revertAnimation()
                binding.bsManageBlocklistAddValueTil.error =
                    context.resources.getString(R.string.error_adding_blocklist_entry) + "\n" + result.error
            }
        }
    }

    private fun setupDropdown() {
        val types = arrayOf(resources.getString(R.string.email), resources.getString(R.string.domain))
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, types)
        binding.bsManageBlocklistAddTypeAutocomplete.setAdapter(adapter)
        binding.bsManageBlocklistAddTypeAutocomplete.setText(types[0], false)

        // Clear error when changing type
        binding.bsManageBlocklistAddTypeAutocomplete.setOnItemClickListener { _, _, _, _ ->
            binding.bsManageBlocklistAddValueTil.error = null
        }
    }

    interface AddBlocklistBottomDialogListener {
        fun onAddedBlocklistEntry(newBlocklistEntry: NewBlocklistEntry)
    }

    companion object {
        fun newInstance(): AddBlocklistBottomDialogFragment {
            return AddBlocklistBottomDialogFragment()
        }
    }
}
