package host.stjin.anonaddy.ui.intent

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetSendMailFromIntentAliasBinding
import host.stjin.anonaddy.models.Aliases
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.stream.Collectors

class IntentSendMailRecipientBottomDialogFragment(
    private val recipientEmail: String?, private val aliases: ArrayList<Aliases>, private val domainOptions: List<String>
) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddIntentSendMailRecipientBottomDialogListener

    // True if the bottomsheet succeeded it's action and the DialogFragment should stay up after this sheet closes
    // False if bottomsheet was closed by user, thus the other sheet should close as well
    private var bottomSheetResult = false

    // 1. Defines the listener interface with a method passing back data result.
    interface AddIntentSendMailRecipientBottomDialogListener {
        suspend fun onPressSend(alias: String, toString: String)
        fun onClose(result: Boolean)
    }

    override fun onCancel(dialog: DialogInterface) {
        listener.onClose(bottomSheetResult)
        super.onCancel(dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetSendMailFromIntentAliasBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetSendMailFromIntentAliasBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = activity as AddIntentSendMailRecipientBottomDialogListener

        // Set domains
        if (!aliases.isNullOrEmpty()) {
            binding.bsSendMailFromIntentAliasesMact.setAdapter(
                ArrayAdapter(
                    requireContext(), android.R.layout.simple_list_item_1, aliases.stream().map { it.email }.collect(
                        Collectors.toList()
                    )
                )
            )
        }

        // Set recipient text
        if (!recipientEmail.isNullOrEmpty()) {
            binding.bsSendMailFromIntentAliasRecipientTiet.setText(recipientEmail)
        }

        // 2. Setup a callback when the "Done" button is pressed on keyboard
        binding.bsSendMailFromIntentAliasSendMailButton.setOnClickListener(this)
        binding.bsSendMailFromIntentAliasRecipientTiet.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                sendMail(requireContext())
            }
            false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setIMEAnimation(binding.bsSendMailFromIntentAliasRoot)
        }
        return root

    }

    constructor() : this(null, arrayListOf(), listOf())

    companion object {
        fun newInstance(
            recipientEmail: String?,
            aliases: ArrayList<Aliases>,
            domainOptions: List<String>
        ): IntentSendMailRecipientBottomDialogFragment {
            return IntentSendMailRecipientBottomDialogFragment(recipientEmail, aliases, domainOptions)
        }
    }

    private fun sendMail(context: Context) {
        val recipientsTiet = binding.bsSendMailFromIntentAliasRecipientTiet.text.toString()
        val recipients = recipientsTiet.split(",")

        // Check if all the entered recipients are valid email addresses
        for (email in recipients) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                    .matches()
            ) {
                binding.bsSendMailFromIntentAliasRecipientTil.error =
                    context.resources.getString(R.string.not_a_valid_address)
                return
            }
        }

        // Check if the alias is a valid email address
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(binding.bsSendMailFromIntentAliasesMact.text.toString())
                .matches()
        ) {
            binding.bsSendMailFromIntentAliasesTil.error =
                context.resources.getString(R.string.not_a_valid_address)
            return
        }

        // As we can dynamically create aliases, we need to check if the entered alias has a domain name that we can use
        val splittedEmailAddress = binding.bsSendMailFromIntentAliasesMact.text.toString().split("@")
        if (domainOptions.contains(splittedEmailAddress[1])) {
            // This is a valid domain name the user has added to their AnonAddy account

            // Set error to null if domain and alias is valid
            binding.bsSendMailFromIntentAliasRecipientTil.error = null
            binding.bsSendMailFromIntentAliasSendMailButton.isEnabled = false
            // Get the first alias that matched the email address with the one entered in the adapter
            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                bottomSheetResult = true
                listener.onPressSend(
                    binding.bsSendMailFromIntentAliasesMact.text.toString(),
                    binding.bsSendMailFromIntentAliasRecipientTiet.text.toString()
                )
            }
        } else {
            // This is not a domain the user owns
            binding.bsSendMailFromIntentAliasesTil.error =
                context.resources.getString(R.string.you_do_not_own_this_domain)
            return
        }


    }


    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_send_mail_from_intent_alias_send_mail_button) {
                sendMail(requireContext())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}