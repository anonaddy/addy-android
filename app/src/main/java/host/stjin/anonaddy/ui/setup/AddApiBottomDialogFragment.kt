package host.stjin.anonaddy.ui.setup

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import kotlinx.android.synthetic.main.bottomsheet_api.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class AddApiBottomDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddApiBottomDialogListener


    // 1. Defines the listener interface with a method passing back data result.
    interface AddApiBottomDialogListener {
        fun onClickSave(baseUrl: String, apiKey: String)
        fun onClickGetMyKey(baseUrl: String)
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
            R.layout.bottomsheet_api, container,
            false
        )
        listener = activity as AddApiBottomDialogListener

        // 2. Setup a callback when the "Done" button is pressed on keyboard
        root.bs_setup_apikey_sign_in_button.setOnClickListener(this)
        root.bs_setup_apikey_get_button.setOnClickListener(this)
        root.bs_setup_apikey_tiet.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                verifyKey(root, requireContext())
            }
            false
        }


        root.bs_setup_apikey_tiet.setOnTouchListener { view, motionEvent ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            if ((motionEvent.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                view.parent.requestDisallowInterceptTouchEvent(false)
            }
            return@setOnTouchListener false
        }

        return root

    }

    companion object {
        fun newInstance(): AddApiBottomDialogFragment {
            return AddApiBottomDialogFragment()
        }
    }

    private fun verifyKey(root: View, context: Context) {
        val apiKey = root.bs_setup_apikey_tiet.text.toString()
        val baseUrl = root.bs_setup_instance_tiet.text.toString()
        root.bs_setup_apikey_get_button.isEnabled = false
        root.bs_setup_apikey_sign_in_button.isEnabled = false
        root.bs_setup_apikey_get_progressbar.visibility = View.VISIBLE


        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            verifyApiKey(root, context, apiKey, baseUrl)
        }
    }

    private suspend fun verifyApiKey(root: View, context: Context, apiKey: String, baseUrl: String) {
        val networkHelper = NetworkHelper(context)
        networkHelper.verifyApiKey(baseUrl, apiKey) { result ->
            if (result == "200") {
                listener.onClickSave(baseUrl, apiKey)
            } else {
                root.bs_setup_apikey_get_button.isEnabled = true
                root.bs_setup_apikey_sign_in_button.isEnabled = true
                root.bs_setup_apikey_get_progressbar.visibility = View.INVISIBLE
                root.bs_setup_apikey_til.error =
                    context.resources.getString(R.string.api_invalid) + "\n" + result
            }
        }
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_setup_apikey_sign_in_button) {
                verifyKey(
                    requireView(),
                    requireContext()
                )
            } else if (p0.id == R.id.bs_setup_apikey_get_button) {
                val baseUrl = requireView().bs_setup_instance_tiet.text.toString()
                listener.onClickGetMyKey(baseUrl)
            }
        }
    }
}