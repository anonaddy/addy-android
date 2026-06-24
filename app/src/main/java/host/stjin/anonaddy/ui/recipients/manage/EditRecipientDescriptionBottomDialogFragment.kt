package host.stjin.anonaddy.ui.recipients.manage

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetEditDescriptionRecipientBinding
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Recipients
import kotlinx.coroutines.launch


class EditRecipientDescriptionBottomDialogFragment(
    private val recipientId: String?,
    private val description: String?
) : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var listener: AddEditRecipientDescriptionBottomDialogListener

    private var _binding: BottomsheetEditDescriptionRecipientBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    // Have an empty constructor the prevent the "could not find Fragment constructor when changing theme or rotating when the dialog is open"
    constructor() : this(null, null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditDescriptionRecipientBinding.inflate(inflater, container, false)
        val root = binding.root

        // Check if recipientId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (recipientId != null) {
            listener = activity as AddEditRecipientDescriptionBottomDialogListener

            // Set button listeners and current description
            binding.bsEditrecipientRecipientSaveButton.setOnClickListener(this)
            binding.bsEditrecipientRecipientDescTiet.setText(description)
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
            if (p0.id == R.id.bs_editrecipient_recipient_save_button) {
                save(
                    requireContext()
                )
            }
        }
    }

    private fun save(context: Context) {
        val description = binding.bsEditrecipientRecipientDescTiet.text.toString()

        // Animate the button to progress
        binding.bsEditrecipientRecipientSaveButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            editDescriptionHttp(context, description)
        }
    }

    private suspend fun editDescriptionHttp(context: Context, description: String) {
        val networkHelper = NetworkHelper(context)
        networkHelper.updateDescriptionSpecificRecipient({ recipient, error ->
            if (recipient != null) {
                listener.descriptionEdited(recipient)
            } else {
                // Revert the button to normal
                binding.bsEditrecipientRecipientSaveButton.revertAnimation()

                binding.bsEditrecipientRecipientDescTil.error =
                    context.resources.getString(R.string.error_edit_description) + "\n" + error
            }
            // recipientId is never null at this point, hence the !!
        }, recipientId!!, description)
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditRecipientDescriptionBottomDialogListener {
        fun descriptionEdited(recipient: Recipients)
    }

    companion object {
        fun newInstance(id: String, description: String?): EditRecipientDescriptionBottomDialogFragment {
            return EditRecipientDescriptionBottomDialogFragment(id, description)
        }
    }
}
