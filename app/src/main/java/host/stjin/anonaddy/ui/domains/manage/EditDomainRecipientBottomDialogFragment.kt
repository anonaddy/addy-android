package host.stjin.anonaddy.ui.domains.manage

import android.app.Dialog
import android.content.Context
import android.os.Build
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
import host.stjin.anonaddy.databinding.BottomsheetEditRecipientDomainBinding
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Domains
import kotlinx.coroutines.launch


class EditDomainRecipientBottomDialogFragment(
    private val domainId: String?,
    private val defaultRecipient: String?
) :
    BaseBottomSheetDialogFragment(),
    View.OnClickListener {


    private lateinit var listener: AddEditDomainRecipientBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditDomainRecipientBottomDialogListener {
        fun recipientEdited(domain: Domains)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetEditRecipientDomainBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditRecipientDomainBinding.inflate(inflater, container, false)
        val root = binding.root

        // Check if domainId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (domainId != null) {
            listener = activity as AddEditDomainRecipientBottomDialogListener

            // Set button listeners and current description
            binding.bsEditrecipientSaveButton.setOnClickListener(this)

            viewLifecycleOwner.lifecycleScope.launch {
                getAllRecipients(requireContext())
            }
        } else {
            dismiss()
        }

        return root
    }

    private suspend fun getAllRecipients(context: Context) {
        val networkHelper = NetworkHelper(context)

        networkHelper.getRecipients({ result, _ ->
            if (result != null) {
                // Remove the default "Loading recipients" chip
                binding.bsEditrecipientChipgroup.removeAllViewsInLayout()
                binding.bsEditrecipientChipgroup.requestLayout()
                binding.bsEditrecipientChipgroup.invalidate()

                for (recipient in result) {
                    val chip = layoutInflater.inflate(R.layout.chip_view, binding.bsEditrecipientChipgroup, false) as Chip
                    chip.text = recipient.email
                    chip.tag = recipient.id
                    chip.isChecked = defaultRecipient.equals(recipient.email)

                    binding.bsEditrecipientChipgroup.addView(chip)
                }
            }

        }, true)
    }


    // Have an empty constructor the prevent the "could not find Fragment constructor when changing theme or rotating when the dialog is open"
    constructor() : this(null, null)

    companion object {
        fun newInstance(
            id: String,
            recipient: String?
        ): EditDomainRecipientBottomDialogFragment {
            return EditDomainRecipientBottomDialogFragment(id, recipient)
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
        aliasId: String,
        recipient: String
    ) {
        val networkHelper = NetworkHelper(context)
        networkHelper.updateDefaultRecipientForSpecificDomain({ domain, error ->
            if (domain != null) {
                listener.recipientEdited(domain)
            } else {
                // Revert the button to normal
                binding.bsEditrecipientSaveButton.revertAnimation()

                binding.bsEditrecipientTil.error =
                    context.resources.getString(R.string.error_edit_recipient) + "\n" + error
            }
        }, aliasId, recipient)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}