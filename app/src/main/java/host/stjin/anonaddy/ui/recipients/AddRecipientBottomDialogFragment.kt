package host.stjin.anonaddy.ui.recipients

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetAddrecipientBinding
import host.stjin.anonaddy.utils.CustomPatterns
import host.stjin.anonaddy_shared.NetworkHelper
import kotlinx.coroutines.launch


class AddRecipientBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddRecipientBottomDialogListener


    // 1. Defines the listener interface with a method passing back data result.
    interface AddRecipientBottomDialogListener {
        fun onAdded()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetAddrecipientBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetAddrecipientBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = parentFragment as AddRecipientBottomDialogListener


        // 2. Setup a callback when the "Done" button is pressed on keyboard
        binding.bsAddrecipientRecipientAddRecipientButton.setOnClickListener(this)
        binding.bsAddrecipientRecipientTiet.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                addRecipient(requireContext())
            }
            false
        }

        return root

    }


    companion object {
        fun newInstance(): AddRecipientBottomDialogFragment {
            return AddRecipientBottomDialogFragment()
        }
    }

    private fun addRecipient(context: Context) {

        if (!CustomPatterns.EMAIL_ADDRESS.matcher(binding.bsAddrecipientRecipientTiet.text.toString())
                .matches()
        ) {
            binding.bsAddrecipientRecipientTil.error =
                context.resources.getString(R.string.not_a_valid_address)
            return
        }

        // Set error to null if domain and alias is valid
        binding.bsAddrecipientRecipientTil.error = null

        // Animate the button to progress
        binding.bsAddrecipientRecipientAddRecipientButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            addRecipientToAccount(
                context,
                binding.bsAddrecipientRecipientTiet.text.toString()
            )
        }
    }

    private suspend fun addRecipientToAccount(
        context: Context,
        address: String
    ) {
        val networkHelper = NetworkHelper(context)
        networkHelper.addRecipient({ recipient, error ->
            if (recipient != null) {
                listener.onAdded()
            } else {
                // Revert the button to normal
                binding.bsAddrecipientRecipientAddRecipientButton.revertAnimation()

                binding.bsAddrecipientRecipientTil.error =
                    context.resources.getString(R.string.error_adding_recipient) + "\n" + error
            }
        }, address)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_addrecipient_recipient_add_recipient_button) {
                addRecipient(requireContext())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}