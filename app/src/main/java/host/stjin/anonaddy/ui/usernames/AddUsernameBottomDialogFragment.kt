package host.stjin.anonaddy.ui.usernames

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
import kotlinx.android.synthetic.main.bottomsheet_addusername.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class AddUsernameBottomDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddUsernameBottomDialogListener
    private lateinit var username: String

    // 1. Defines the listener interface with a method passing back data result.
    interface AddUsernameBottomDialogListener {
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
            R.layout.bottomsheet_addusername, container,
            false
        )
        listener = activity as AddUsernameBottomDialogListener


        // 2. Setup a callback when the "Done" button is pressed on keyboard
        root.bs_addusername_username_add_username_button.setOnClickListener(this)
        root.bs_addusername_username_tiet.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                addUsername(root, requireContext())
            }
            false
        }

        return root

    }


    companion object {
        fun newInstance(): AddUsernameBottomDialogFragment {
            return AddUsernameBottomDialogFragment()
        }
    }

    private fun addUsername(root: View, context: Context) {

        this.username = root.bs_addusername_username_tiet.text.toString()
        // Set error to null if username and alias is valid
        root.bs_addusername_username_til.error = null
        root.bs_addusername_username_add_username_button.isEnabled = false
        root.bs_addusername_username_progressbar.visibility = View.VISIBLE
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            addUsernameToAccount(
                root,
                context,
                this@AddUsernameBottomDialogFragment.username
            )
        }
    }

    private suspend fun addUsernameToAccount(
        root: View,
        context: Context,
        address: String
    ) {
        val networkHelper = NetworkHelper(context)
        networkHelper.addUsername({ result, _ ->
            when (result) {
                "201" -> {
                    listener.onAdded()
                }
                else -> {
                    root.bs_addusername_username_add_username_button.isEnabled = true
                    root.bs_addusername_username_progressbar.visibility = View.INVISIBLE
                    root.bs_addusername_username_til.error =
                        context.resources.getString(R.string.error_adding_username) + "\n" + result
                }
            }
        }, address)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_addusername_username_add_username_button) {
                addUsername(requireView(), requireContext())
            }
        }
    }
}