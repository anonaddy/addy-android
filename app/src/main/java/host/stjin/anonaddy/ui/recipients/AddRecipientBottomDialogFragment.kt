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
import kotlinx.android.synthetic.main.bottomsheet_addrecipient.view.*
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // get the views and attach the listener
        val root = inflater.inflate(
            R.layout.bottomsheet_addrecipient, container,
            false
        )
        listener = parentFragment as AddRecipientBottomDialogListener


        // 2. Setup a callback when the "Done" button is pressed on keyboard
        root.bs_addrecipient_recipient_add_recipient_button.setOnClickListener(this)
        root.bs_addrecipient_recipient_tiet.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                addRecipient(root, requireContext())
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

    private fun addRecipient(root: View, context: Context) {

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(root.bs_addrecipient_recipient_tiet.text.toString())
                .matches()
        ) {
            root.bs_addrecipient_recipient_til.error =
                context.resources.getString(R.string.not_a_valid_address)
            return
        }

        // Set error to null if domain and alias is valid
        root.bs_addrecipient_recipient_til.error = null
        root.bs_addrecipient_recipient_add_recipient_button.isEnabled = false
        root.bs_addrecipient_recipient_progressbar.visibility = View.VISIBLE
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            addRecipientToAccount(
                root,
                context,
                root.bs_addrecipient_recipient_tiet.text.toString()
            )
        }
    }

    private suspend fun addRecipientToAccount(
        root: View,
        context: Context,
        address: String
    ) {
        val networkHelper = NetworkHelper(context)
        networkHelper.addRecipient(address) { result ->
            when (result) {
                "201" -> {
                    listener.onAdded()
                }
                else -> {
                    root.bs_addrecipient_recipient_add_recipient_button.isEnabled = true
                    root.bs_addrecipient_recipient_progressbar.visibility = View.INVISIBLE
                    root.bs_addrecipient_recipient_til.error =
                        context.resources.getString(R.string.error_adding_recipient) + "\n" + result
                }
            }
        }
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_addrecipient_recipient_add_recipient_button) {
                addRecipient(requireView(), requireContext())
            }
        }
    }
}