package host.stjin.anonaddy.ui.intent

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.core.content.res.getDrawableOrThrow
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetSendMailFromIntentAliasBinding
import host.stjin.anonaddy.utils.CustomPatterns
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.Aliases
import kotlinx.coroutines.launch
import java.util.stream.Collectors

class IntentSendMailRecipientBottomDialogFragment(
    private val recipientEmails: ArrayList<String>,
    private val recipientCcEmails: ArrayList<String>,
    private val recipientBccEmails: ArrayList<String>,
    private val domainOptions: List<String>
) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddIntentSendMailRecipientBottomDialogListener

    // True if the bottomsheet succeeded it's action and the DialogFragment should stay up after this sheet closes
    // False if bottomsheet was closed by user, thus the other sheet should close as well
    private var bottomSheetResult = false

    // 1. Defines the listener interface with a method passing back data result.
    interface AddIntentSendMailRecipientBottomDialogListener {
        suspend fun onPressSend(
            alias: String,
            aliasObject: Aliases?,
            recipients: String,
            ccRecipients: String,
            bccRecipients: String,
            skipAndOpenDefaultMailApp: Boolean = false
        )

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
    private val binding get() = _binding!!

    private var lastMactText: String = ""

    private var aliases: ArrayList<Aliases> = arrayListOf()


    private val TYPING_TIMEOUT = 1000 // 1 seconds timeout
    private var isTyping: Boolean = false
    private val timeoutHandler: Handler = Handler(Looper.getMainLooper())
    private val typingTimeout = Runnable {
        isTyping = false
        setAdapterData(binding.bsSendMailFromIntentAliasesMact.text.toString())
    }

    override fun onPause() {
        super.onPause()
        timeoutHandler.removeCallbacks(typingTimeout)
    }

    private fun Context.getProgressBarDrawable(): Drawable {
        val value = TypedValue()
        theme.resolveAttribute(android.R.attr.progressBarStyleSmall, value, false)
        val progressBarStyle = value.data
        val attributes = intArrayOf(android.R.attr.indeterminateDrawable)
        val array = obtainStyledAttributes(progressBarStyle, attributes)
        val drawable = array.getDrawableOrThrow(0)
        array.recycle()
        return drawable
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetSendMailFromIntentAliasBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = activity as AddIntentSendMailRecipientBottomDialogListener

        val progressDrawable = context?.getProgressBarDrawable()

        binding.bsSendMailFromIntentAliasesTil.endIconMode = TextInputLayout.END_ICON_CUSTOM
        binding.bsSendMailFromIntentAliasesMact.addTextChangedListener {
            if (it.toString() != lastMactText) {
                if (binding.bsSendMailFromIntentAliasesTil.endIconDrawable != progressDrawable) {
                    binding.bsSendMailFromIntentAliasesTil.endIconDrawable = progressDrawable
                    (binding.bsSendMailFromIntentAliasesTil.endIconDrawable as? Animatable)?.start()
                }

                // reset the timeout
                timeoutHandler.removeCallbacks(typingTimeout)
                // schedule the timeout
                timeoutHandler.postDelayed(typingTimeout, TYPING_TIMEOUT.toLong())
            }
            lastMactText = it.toString()
        }

        // Set recipient text
        if (recipientEmails.any()) {
            binding.bsSendMailFromIntentAliasRecipientTiet.setText(recipientEmails.joinToString(","))
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

    private fun setAdapterData(searchQuery: String) {
        if (searchQuery.count() >= 3) {
            binding.bsSendMailFromIntentAliasesTil.hint = context?.resources?.getString(R.string.alias)
            lifecycleScope.launch {
                NetworkHelper(requireContext()).getAliases(
                    { list, _ ->
                        if (list != null) {
                            aliases = list.data
                            setAliasesAdapter()
                            binding.bsSendMailFromIntentAliasesMact.showDropDown()
                        } else {
                            binding.bsSendMailFromIntentAliasesTil.error =
                                requireContext().resources.getString(R.string.something_went_wrong_retrieving_aliases)
                        }

                        binding.bsSendMailFromIntentAliasesTil.endIconDrawable = null
                    },
                    aliasSortFilter = AliasSortFilter(
                        onlyActiveAliases = false,
                        onlyInactiveAliases = false,
                        includeDeleted = true,
                        onlyWatchedAliases = false,
                        sort = null,
                        sortDesc = true,
                        filter = searchQuery
                    ),
                    size = 100
                )
            }
        } else {
            binding.bsSendMailFromIntentAliasesTil.endIconDrawable = null
            binding.bsSendMailFromIntentAliasesTil.hint = context?.resources?.getString(R.string.start_typing_to_show_aliases)
        }
    }

    private fun setAliasesAdapter() {
        // Set domains
        if (aliases.isNotEmpty()) {
            binding.bsSendMailFromIntentAliasesMact.setAdapter(
                ArrayAdapter(
                    requireContext(), android.R.layout.simple_list_item_1, aliases.stream().map { it.email }.collect(
                        Collectors.toList()
                    )
                )
            )
        }

    }

    constructor() : this(arrayListOf(), arrayListOf(), arrayListOf(), listOf())

    companion object {
        fun newInstance(
            recipientEmail: ArrayList<String>,
            recipientCcEmail: ArrayList<String>,
            recipientBccEmail: ArrayList<String>,
            domainOptions: List<String>
        ): IntentSendMailRecipientBottomDialogFragment {
            return IntentSendMailRecipientBottomDialogFragment(recipientEmail, recipientCcEmail, recipientBccEmail, domainOptions)
        }
    }

    private fun sendMail(context: Context) {
        val recipientsTiet = binding.bsSendMailFromIntentAliasRecipientTiet.text.toString()
        val recipients = recipientsTiet.split(",")

        // Check if all the entered recipients are valid email addresses
        for (email in recipients) {
            if (!CustomPatterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.bsSendMailFromIntentAliasRecipientTil.error =
                    context.resources.getString(R.string.not_a_valid_address)
                return
            }
        }

        // Check if alias is empty, if alias is empty just forward the recipient to the default mail app without generating an alias
        if (binding.bsSendMailFromIntentAliasesMact.text.toString().isEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                listener.onPressSend(
                    binding.bsSendMailFromIntentAliasesMact.text.toString(),
                    aliases.firstOrNull { it.email == binding.bsSendMailFromIntentAliasesMact.text.toString() },
                    binding.bsSendMailFromIntentAliasRecipientTiet.text.toString(),
                    recipientCcEmails.joinToString(","),
                    recipientBccEmails.joinToString(","),
                    true
                )
            }
        } else {
            // Check if the alias is a valid email address
            if (!CustomPatterns.EMAIL_ADDRESS.matcher(binding.bsSendMailFromIntentAliasesMact.text.toString()).matches()) {
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
                viewLifecycleOwner.lifecycleScope.launch {
                    bottomSheetResult = true
                    listener.onPressSend(
                        binding.bsSendMailFromIntentAliasesMact.text.toString(),
                        aliases.firstOrNull { it.email == binding.bsSendMailFromIntentAliasesMact.text.toString() },
                        binding.bsSendMailFromIntentAliasRecipientTiet.text.toString(),
                        recipientCcEmails.joinToString(","),
                        recipientBccEmails.joinToString(","),
                    )
                }
            } else {
                // This is not a domain the user owns
                binding.bsSendMailFromIntentAliasesTil.error =
                    context.resources.getString(R.string.you_do_not_own_this_domain)
                return
            }

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