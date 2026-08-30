package host.stjin.anonaddy.ui.recipients.manage

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetEditGpgKeyRecipientBinding
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch


class AddRecipientPublicGpgKeyBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private val viewModel: ManageRecipientViewModel by activityViewModels()
    private var recipientId: String? = null

    private var listener: AddEditGpgKeyBottomDialogListener? = null

    private var _binding: BottomsheetEditGpgKeyRecipientBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            recipientId = it.getString(ARG_RECIPIENT_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditGpgKeyRecipientBinding.inflate(inflater, container, false)
        val root = binding.root

        if (recipientId != null) {
            listener = (parentFragment as? AddEditGpgKeyBottomDialogListener) ?: (activity as? AddEditGpgKeyBottomDialogListener)

            // Set button listeners and current description
            binding.bsEditRecipientGpgKeySaveButton.setOnClickListener(this)


            binding.bsEditRecipientGpgKeyTiet.setOnTouchListener { view, motionEvent ->
                view.parent.requestDisallowInterceptTouchEvent(true)
                if ((motionEvent.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                }
                return@setOnTouchListener false
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
            if (p0.id == R.id.bs_edit_recipient_gpg_key_save_button) {
                addKey(
                    requireContext()
                )
            }
        }
    }

    private fun addKey(context: Context) {
        val description = binding.bsEditRecipientGpgKeyTiet.text.toString()

        // Animate the button to progress
        binding.bsEditRecipientGpgKeySaveButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            addGpgKeyHttp(context, description)
        }
    }

    private suspend fun addGpgKeyHttp(context: Context, publicPgpKey: String) {
        when (val result = viewModel.addEncryptionKeyRecipient(recipientId!!, publicPgpKey)) {
            is NetworkResult.Success -> {
                listener?.onKeyAdded(result.data)
            }
            is NetworkResult.Error -> {
                // Revert the button to normal
                binding.bsEditRecipientGpgKeySaveButton.revertAnimation()

                binding.bsEditRecipientGpgKeyTil.error =
                    context.resources.getString(R.string.error_add_gpg_key) + "\n" + result.error
            }
        }
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditGpgKeyBottomDialogListener {
        fun onKeyAdded(recipient: Recipients)
    }

    companion object {
        private const val ARG_RECIPIENT_ID = "arg_recipient_id"

        fun newInstance(id: String?): AddRecipientPublicGpgKeyBottomDialogFragment {
            return AddRecipientPublicGpgKeyBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_RECIPIENT_ID, id)
                }
            }
        }
    }
}
