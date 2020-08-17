package host.stjin.anonaddy.ui.recipients.manage

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import kotlinx.android.synthetic.main.bottomsheet_edit_gpg_key_recipient.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class AddRecipientPublicGpgKeyBottomDialogFragment(
    private val aliasID: String,
    private val description: String
) : BottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddEditGpgKeyBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditGpgKeyBottomDialogListener {
        fun onKeyAdded()
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
            R.layout.bottomsheet_edit_gpg_key_recipient, container,
            false
        )
        listener = activity as ManageRecipientsActivity

        // Set button listeners and current description
        root.bs_edit_recipient_gpg_key_save_button.setOnClickListener(this)
        root.bs_edit_recipient_gpg_key_tiet.setText(description)


        root.bs_edit_recipient_gpg_key_tiet.setOnTouchListener { view, motionEvent ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            if ((motionEvent.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                view.parent.requestDisallowInterceptTouchEvent(false)
            }
            return@setOnTouchListener false
        }


        return root

    }


    companion object {
        fun newInstance(id: String, description: String): AddRecipientPublicGpgKeyBottomDialogFragment {
            return AddRecipientPublicGpgKeyBottomDialogFragment(id, description)
        }
    }

    private fun addKey(root: View, context: Context) {
        val description = root.bs_edit_recipient_gpg_key_tiet.text.toString()
        root.bs_edit_recipient_gpg_key_save_button.isEnabled = false
        root.bs_edit_recipient_gpg_key_save_progressbar.visibility = View.VISIBLE


        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            addGpgKeyHttp(root, context, description)
        }
    }

    private suspend fun addGpgKeyHttp(root: View, context: Context, description: String) {
        val networkHelper = NetworkHelper(context)
        networkHelper.addEncryptionKeyRecipient({ result ->
            if (result == "200") {
                listener.onKeyAdded()
            } else {
                root.bs_edit_recipient_gpg_key_save_button.isEnabled = true
                root.bs_edit_recipient_gpg_key_save_progressbar.visibility = View.INVISIBLE
                root.bs_edit_recipient_gpg_key_til.error =
                    context.resources.getString(R.string.error_add_gpg_key) + "\n" + result
            }
        }, aliasID, description)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_edit_recipient_gpg_key_save_button) {
                addKey(
                    requireView(),
                    requireContext()
                )
            }
        }
    }
}