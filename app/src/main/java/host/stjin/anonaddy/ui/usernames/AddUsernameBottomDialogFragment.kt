package host.stjin.anonaddy.ui.usernames

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetAddusernameBinding
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch


class AddUsernameBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private val viewModel: UsernamesViewModel by activityViewModels()
    private var usernameLimit: Int = 0
    private var listener: AddUsernameBottomDialogListener? = null

    private var _binding: BottomsheetAddusernameBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usernameLimit = arguments?.getInt(ARG_USERNAME_LIMIT) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetAddusernameBinding.inflate(inflater, container, false)
        val root = binding.root
        listener = (parentFragment as? AddUsernameBottomDialogListener) ?: (activity as? AddUsernameBottomDialogListener)


        binding.bsAddusernameUsernameDesc.text = androidx.core.text.HtmlCompat.fromHtml(
            requireContext().resources.getString(R.string.add_username_desc, usernameLimit),
            androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
        )

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_addusername_username_add_username_button) {
                addUsername(requireContext())
            }
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
        when (val result = viewModel.addUsername(address)) {
            is NetworkResult.Success -> {
                listener?.onAdded()
            }
            is NetworkResult.Error -> {
                binding.bsAddusernameUsernameAddUsernameButton.revertAnimation()
                binding.bsAddusernameUsernameTil.error =
                    context.resources.getString(R.string.error_adding_username) + "\n" + (result.errorOrNull() ?: "")
            }
        }
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddUsernameBottomDialogListener {
        fun onAdded()
    }

    companion object {
        private const val ARG_USERNAME_LIMIT = "arg_username_limit"

        fun newInstance(usernameLimit: Int): AddUsernameBottomDialogFragment {
            return AddUsernameBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_USERNAME_LIMIT, usernameLimit)
                }
            }
        }
    }
}
