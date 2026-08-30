package host.stjin.anonaddy.ui.domains.manage

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetEditRecipientDomainBinding
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch


class EditDomainRecipientBottomDialogFragment :
    BaseBottomSheetDialogFragment(),
    View.OnClickListener {
    private val viewModel: ManageDomainViewModel by activityViewModels()
    private var domainId: String? = null
    private var defaultRecipient: String? = null

    private var listener: AddEditDomainRecipientBottomDialogListener? = null

    private var _binding: BottomsheetEditRecipientDomainBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            domainId = it.getString(ARG_DOMAIN_ID)
            defaultRecipient = it.getString(ARG_DEFAULT_RECIPIENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditRecipientDomainBinding.inflate(inflater, container, false)
        val root = binding.root

        // Check if domainId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (domainId != null) {
            listener = (parentFragment as? AddEditDomainRecipientBottomDialogListener) ?: (activity as? AddEditDomainRecipientBottomDialogListener)

            // 2. Setup a callback when the "Done" button is pressed on keyboard
            binding.bsEditrecipientSaveButton.setOnClickListener(this)

            viewLifecycleOwner.lifecycleScope.launch {
                getAllRecipients()
            }
        } else {
            dismiss()
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

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_editrecipient_save_button) {
                editRecipient(
                    requireContext()
                )
            }
        }
    }

    private suspend fun getAllRecipients() {
        val result = viewModel.getVerifiedRecipients()
        binding.bsEditrecipientSaveButton.isEnabled = true
        if (result is NetworkResult.Success) {
            // Remove the default "Loading recipients" chip
            binding.bsEditrecipientChipgroup.removeAllViewsInLayout()
            binding.bsEditrecipientChipgroup.requestLayout()
            binding.bsEditrecipientChipgroup.invalidate()

            for (recipient in result.data.data) {
                val chip = layoutInflater.inflate(R.layout.chip_view, binding.bsEditrecipientChipgroup, false) as Chip
                chip.text = recipient.email
                chip.tag = recipient.id
                chip.isChecked = defaultRecipient.equals(recipient.email)

                binding.bsEditrecipientChipgroup.addView(chip)
            }
        }
    }

    private fun editRecipient(context: Context) {
        // Animate the button to progress
        binding.bsEditrecipientSaveButton.startAnimation()

        var recipient = ""
        val ids: List<Int> = binding.bsEditrecipientChipgroup.checkedChipIds
        for (id in ids) {
            val chip: Chip = binding.bsEditrecipientChipgroup.findViewById(id)
            recipient = chip.tag.toString()
        }

        // domainId is never null at this point, hence the !!
        viewLifecycleOwner.lifecycleScope.launch {
            editRecipientHttp(context, domainId!!, recipient)
        }
    }

    private suspend fun editRecipientHttp(
        context: Context,
        domainId: String,
        recipient: String
    ) {
        when (val result = viewModel.updateDefaultRecipientDomain(domainId, recipient)) {
            is NetworkResult.Success -> {
                listener?.recipientEdited(result.data)
            }
            is NetworkResult.Error -> {
                binding.bsEditrecipientSaveButton.revertAnimation()
                binding.bsEditrecipientTil.error =
                    context.resources.getString(R.string.error_edit_recipient) + "\n" + (result.errorOrNull() ?: "")
            }
        }
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditDomainRecipientBottomDialogListener {
        fun recipientEdited(domain: Domains)
    }

    companion object {
        private const val ARG_DOMAIN_ID = "arg_domain_id"
        private const val ARG_DEFAULT_RECIPIENT = "arg_default_recipient"

        fun newInstance(
            id: String,
            recipient: String?
        ): EditDomainRecipientBottomDialogFragment {
            return EditDomainRecipientBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DOMAIN_ID, id)
                    putString(ARG_DEFAULT_RECIPIENT, recipient)
                }
            }
        }
    }
}
