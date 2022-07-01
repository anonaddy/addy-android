package host.stjin.anonaddy.ui.recipients.manage

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetEditGpgKeyRecipientBinding
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Recipients
import kotlinx.coroutines.launch


class AddRecipientPublicGpgKeyBottomDialogFragment(
    private val aliasId: String?
) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddEditGpgKeyBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditGpgKeyBottomDialogListener {
        fun onKeyAdded(recipient: Recipients)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetEditGpgKeyRecipientBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditGpgKeyRecipientBinding.inflate(inflater, container, false)
        val root = binding.root

        // Check if aliasId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (aliasId != null) {
            listener = activity as ManageRecipientsActivity

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setIMEAnimation(binding.bsEditRecipientGpgKeyRoot)
        }

        return root
    }

    // Have an empty constructor the prevent the "could not find Fragment constructor when changing theme or rotating when the dialog is open"
    constructor() : this(null)

    companion object {
        fun newInstance(id: String): AddRecipientPublicGpgKeyBottomDialogFragment {
            return AddRecipientPublicGpgKeyBottomDialogFragment(id)
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

    private suspend fun addGpgKeyHttp(context: Context, description: String) {
        val networkHelper = NetworkHelper(context)
        networkHelper.addEncryptionKeyRecipient({ recipient, error ->
            if (recipient != null) {
                listener.onKeyAdded(recipient)
            } else {

                // Revert the button to normal
                binding.bsEditRecipientGpgKeySaveButton.revertAnimation()

                binding.bsEditRecipientGpgKeyTil.error =
                    context.resources.getString(R.string.error_add_gpg_key) + "\n" + error
            }
            // aliasId is never null at this point, hence the !!
        }, aliasId!!, description)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}