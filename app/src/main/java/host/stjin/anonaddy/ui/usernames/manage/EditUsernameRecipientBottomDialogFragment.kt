package host.stjin.anonaddy.ui.usernames.manage

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
import host.stjin.anonaddy.databinding.BottomsheetEditRecipientUsernameBinding
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy_shared.models.Usernames
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch


class EditUsernameRecipientBottomDialogFragment :
    BaseBottomSheetDialogFragment(),
    View.OnClickListener {
    private val viewModel: ManageUsernameViewModel by activityViewModels()
    private var usernameId: String? = null
    private var defaultRecipient: String? = null

    private var listener: AddEditUsernameRecipientBottomDialogListener? = null

    private var _binding: BottomsheetEditRecipientUsernameBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            usernameId = it.getString(ARG_USERNAME_ID)
            defaultRecipient = it.getString(ARG_DEFAULT_RECIPIENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditRecipientUsernameBinding.inflate(inflater, container, false)
        val root = binding.root

        // Check if usernameId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (usernameId != null) {
            listener = (parentFragment as? AddEditUsernameRecipientBottomDialogListener) ?: (activity as? AddEditUsernameRecipientBottomDialogListener)

            // Set button listeners and current description
            binding.bsEditrecipientSaveButton.setOnClickListener(this)
            binding.bsEditrecipientSaveButton.isEnabled = false

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


        viewLifecycleOwner.lifecycleScope.launch {
            // usernameId is never null at this point, hence the !!
            editRecipientHttp(context, usernameId!!, recipient)
        }
    }

    private suspend fun editRecipientHttp(
        context: Context,
        usernameId: String,
        recipient: String
    ) {
        when (val result = viewModel.updateDefaultRecipientUsername(usernameId, recipient)) {
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
    interface AddEditUsernameRecipientBottomDialogListener {
        fun recipientEdited(username: Usernames)
    }

    companion object {
        private const val ARG_USERNAME_ID = "arg_username_id"
        private const val ARG_DEFAULT_RECIPIENT = "arg_default_recipient"

        fun newInstance(
            id: String,
            recipient: String?
        ): EditUsernameRecipientBottomDialogFragment {
            return EditUsernameRecipientBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USERNAME_ID, id)
                    putString(ARG_DEFAULT_RECIPIENT, recipient)
                }
            }
        }
    }
}
