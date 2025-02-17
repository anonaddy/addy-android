package host.stjin.anonaddy.ui.usernames

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
import host.stjin.anonaddy.databinding.BottomsheetAddusernameBinding
import host.stjin.anonaddy_shared.NetworkHelper
import kotlinx.coroutines.launch


class AddUsernameBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddUsernameBottomDialogListener

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
        listener = parentFragment as AddUsernameBottomDialogListener


        //TODO TEST IF THIS IS INDEED NOT REQUIRED ON ANDROID 14 AND OLDER

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            binding.bsAddusernameUsernameDesc.text =
//                (Html.fromHtml(requireContext().resources.getString(R.string.add_username_desc, usernameLimit), Html.FROM_HTML_MODE_COMPACT))
//        } else {
//            binding.bsAddusernameUsernameDesc.text =
//                (Html.fromHtml(requireContext().resources.getString(R.string.add_username_desc, usernameLimit)))
//        }

        // 2. Setup a callback when the "Done" button is pressed on keyboard
        binding.bsAddusernameUsernameAddUsernameButton.setOnClickListener(this)
        binding.bsAddusernameUsernameTiet.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                addUsername(requireContext())
            }
            false
        }


        return root

    }


    companion object {
        fun newInstance(usernameLimit: Int): AddUsernameBottomDialogFragment {
            return AddUsernameBottomDialogFragment()
        }
    }

    private fun addUsername(context: Context) {

        // Set error to null if username and alias is valid
        binding.bsAddusernameUsernameTil.error = null

        // Animate the button to progress
        binding.bsAddusernameUsernameAddUsernameButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            addUsernameToAccount(
                context,
                binding.bsAddusernameUsernameTiet.text.toString()
            )
        }
    }

    private suspend fun addUsernameToAccount(
        context: Context,
        address: String
    ) {
        val networkHelper = NetworkHelper(context)
        networkHelper.addUsername({ username, error ->
            if (username != null) {
                listener.onAdded()
            } else {
                // Revert the button to normal
                binding.bsAddusernameUsernameAddUsernameButton.revertAnimation()

                binding.bsAddusernameUsernameTil.error =
                    context.resources.getString(R.string.error_adding_username) + "\n" + error
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