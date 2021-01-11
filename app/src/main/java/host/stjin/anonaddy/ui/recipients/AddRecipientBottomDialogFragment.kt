package host.stjin.anonaddy.ui.recipients

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetAddrecipientBinding
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class AddRecipientBottomDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {


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

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(binding.bsAddrecipientRecipientTiet.text.toString())
                .matches()
        ) {
            binding.bsAddrecipientRecipientTil.error =
                context.resources.getString(R.string.not_a_valid_address)
            return
        }

        // Set error to null if domain and alias is valid
        binding.bsAddrecipientRecipientTil.error = null
        binding.bsAddrecipientRecipientAddRecipientButton.isEnabled = false
        binding.bsAddrecipientRecipientProgressbar.visibility = View.VISIBLE
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
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
        networkHelper.addRecipient({ result ->
            when (result) {
                "201" -> {
                    listener.onAdded()
                }
                else -> {
                    binding.bsAddrecipientRecipientAddRecipientButton.isEnabled = true
                    binding.bsAddrecipientRecipientProgressbar.visibility = View.INVISIBLE
                    binding.bsAddrecipientRecipientTil.error =
                        context.resources.getString(R.string.error_adding_recipient) + "\n" + result
                }
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