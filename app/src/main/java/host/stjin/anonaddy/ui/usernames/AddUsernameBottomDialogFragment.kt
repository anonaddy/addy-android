package host.stjin.anonaddy.ui.usernames

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetAddusernameBinding
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class AddUsernameBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {


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

    private var _binding: BottomsheetAddusernameBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetAddusernameBinding.inflate(inflater, container, false)
        val root = binding.root
        listener = activity as AddUsernameBottomDialogListener


        // 2. Setup a callback when the "Done" button is pressed on keyboard
        binding.bsAddusernameUsernameAddUsernameButton.setOnClickListener(this)
        binding.bsAddusernameUsernameTiet.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                addUsername(requireContext())
            }
            false
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setIMEAnimation(binding.bsAddusernameUsernameRoot)
        }

        return root

    }


    companion object {
        fun newInstance(): AddUsernameBottomDialogFragment {
            return AddUsernameBottomDialogFragment()
        }
    }

    private fun addUsername(context: Context) {

        this.username = binding.bsAddusernameUsernameTiet.text.toString()
        // Set error to null if username and alias is valid
        binding.bsAddusernameUsernameTil.error = null
        binding.bsAddusernameUsernameAddUsernameButton.isEnabled = false
        binding.bsAddusernameUsernameProgressbar.visibility = View.VISIBLE
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            addUsernameToAccount(
                context,
                this@AddUsernameBottomDialogFragment.username
            )
        }
    }

    private suspend fun addUsernameToAccount(
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
                    binding.bsAddusernameUsernameAddUsernameButton.isEnabled = true
                    binding.bsAddusernameUsernameProgressbar.visibility = View.INVISIBLE
                    binding.bsAddusernameUsernameTil.error =
                        context.resources.getString(R.string.error_adding_username) + "\n" + result
                }
            }
        }, address)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_addusername_username_add_username_button) {
                addUsername(requireContext())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}