package host.stjin.anonaddy.ui.alias.manage

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
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetSendMailAliasBinding
import host.stjin.anonaddy.utils.CustomPatterns

class EditAliasSendMailRecipientBottomDialogFragment(
    private val aliasEmail: String?
) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddEditAliasSendMailRecipientBottomDialogListener


    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditAliasSendMailRecipientBottomDialogListener {
        fun onPressSend(toString: String)
        fun onPressCopy(toString: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetSendMailAliasBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetSendMailAliasBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = activity as AddEditAliasSendMailRecipientBottomDialogListener

        // Set text
        binding.bsSendMailAliasRecipientDesc.text = this.resources.getString(R.string.send_mail_from_alias_desc, aliasEmail)

        // 2. Setup a callback when the "Done" button is pressed on keyboard
        binding.bsSendMailAliasSendMailButton.setOnClickListener(this)
        binding.bsSendMailAliasSendMailCopyButton.setOnClickListener(this)
        binding.bsSendMailAliasRecipientTiet.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                sendMail(requireContext())
            }
            false
        }

        return root

    }

    constructor() : this(null)

    companion object {
        fun newInstance(aliasEmail: String?): EditAliasSendMailRecipientBottomDialogFragment {
            return EditAliasSendMailRecipientBottomDialogFragment(aliasEmail)
        }
    }

    private fun sendMail(context: Context) {
        val recipientsTiet = binding.bsSendMailAliasRecipientTiet.text.toString()
        val recipients = recipientsTiet.split(",")

        // Check if all the entered recipients are valid email addresses
        for (email in recipients) {
            if (!CustomPatterns.EMAIL_ADDRESS.matcher(email)
                    .matches()
            ) {
                binding.bsSendMailAliasRecipientTil.error =
                    context.resources.getString(R.string.not_a_valid_address)
                return
            }
        }

        // Set error to null if domain and alias is valid
        binding.bsSendMailAliasRecipientTil.error = null
        binding.bsSendMailAliasSendMailButton.isEnabled = false
        listener.onPressSend(binding.bsSendMailAliasRecipientTiet.text.toString())
    }

    private fun copyToAddress(context: Context) {
        val recipientsTiet = binding.bsSendMailAliasRecipientTiet.text.toString()
        val recipients = recipientsTiet.split(",")

        // Check if all the entered recipients are valid email addresses
        for (email in recipients) {
            if (!CustomPatterns.EMAIL_ADDRESS.matcher(email)
                    .matches()
            ) {
                binding.bsSendMailAliasRecipientTil.error =
                    context.resources.getString(R.string.not_a_valid_address)
                return
            }
        }

        // Set error to null if domain and alias is valid
        binding.bsSendMailAliasRecipientTil.error = null
        binding.bsSendMailAliasSendMailButton.isEnabled = false
        listener.onPressCopy(binding.bsSendMailAliasRecipientTiet.text.toString())
    }


    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_send_mail_alias_send_mail_button) {
                sendMail(requireContext())
            }   else if (p0.id == R.id.bs_send_mail_alias_send_mail_copy_button) {
                copyToAddress(requireContext())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}