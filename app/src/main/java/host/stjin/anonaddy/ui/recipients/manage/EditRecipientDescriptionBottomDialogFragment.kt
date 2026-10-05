package host.stjin.anonaddy.ui.recipients.manage

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
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetEditDescriptionRecipientBinding
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch


class EditRecipientDescriptionBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private val viewModel: ManageRecipientViewModel by activityViewModels()
    private var recipientId: String? = null
    private var description: String? = null

    private var listener: AddEditRecipientDescriptionBottomDialogListener? = null

    private var _binding: BottomsheetEditDescriptionRecipientBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            recipientId = it.getString(ARG_RECIPIENT_ID)
            description = it.getString(ARG_DESCRIPTION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditDescriptionRecipientBinding.inflate(inflater, container, false)
        val root = binding.root

        if (recipientId != null) {
            listener = (parentFragment as? AddEditRecipientDescriptionBottomDialogListener) ?: (activity as? AddEditRecipientDescriptionBottomDialogListener)

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
        when (val result = viewModel.updateDescriptionRecipient(recipientId!!, description)) {
            is NetworkResult.Success -> {
                listener?.descriptionEdited(result.data)
            }
            is NetworkResult.Error -> {
                binding.bsEditrecipientRecipientSaveButton.revertAnimation()
                binding.bsEditrecipientRecipientDescTil.error =
                    context.resources.getString(R.string.error_edit_description) + "\n" + result.error
            }
        }
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditRecipientDescriptionBottomDialogListener {
        fun descriptionEdited(recipient: Recipients)
    }

    companion object {
        private const val ARG_RECIPIENT_ID = "arg_recipient_id"
        private const val ARG_DESCRIPTION = "arg_description"

        fun newInstance(id: String?, description: String?): EditRecipientDescriptionBottomDialogFragment {
            return EditRecipientDescriptionBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_RECIPIENT_ID, id)
                    putString(ARG_DESCRIPTION, description)
                }
            }
        }
    }
}
