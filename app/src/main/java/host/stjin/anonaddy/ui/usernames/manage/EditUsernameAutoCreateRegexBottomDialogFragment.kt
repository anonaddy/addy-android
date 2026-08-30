package host.stjin.anonaddy.ui.usernames.manage

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetEditAutoCreateRegexUsernameBinding
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy_shared.models.Usernames
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch


class EditUsernameAutoCreateRegexBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private val viewModel: ManageUsernameViewModel by activityViewModels()
    private var usernameId: String? = null
    private var autoCreateRegex: String? = null

    private var listener: AddEditUsernameAutoCreateRegexBottomDialogListener? = null

    private var _binding: BottomsheetEditAutoCreateRegexUsernameBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            usernameId = it.getString(ARG_USERNAME_ID)
            autoCreateRegex = it.getString(ARG_AUTO_CREATE_REGEX)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditAutoCreateRegexUsernameBinding.inflate(inflater, container, false)
        val root = binding.root

        if (usernameId != null) {
            listener = (parentFragment as? AddEditUsernameAutoCreateRegexBottomDialogListener) ?: (activity as? AddEditUsernameAutoCreateRegexBottomDialogListener)

            // Set button listeners and current description
            binding.bsEditusernameUsernameSaveButton.setOnClickListener(this)
            binding.bsEditusernameUsernameAutoCreateRegexTiet.setText(autoCreateRegex)

            binding.bsEditAutoCreateRegexUsernameDesc.text = androidx.core.text.HtmlCompat.fromHtml(
                requireContext().resources.getString(R.string.edit_auto_create_regex_desc),
                androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
            )
        } else {
            dismiss()
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
            if (p0.id == R.id.bs_editusername_username_save_button) {
                save(
                    requireContext()
                )
            }
        }
    }

    private fun save(context: Context) {
        val description = binding.bsEditusernameUsernameAutoCreateRegexTiet.text.toString()

        // Animate the button to progress
        binding.bsEditusernameUsernameSaveButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            editAutoCreateRegexHttp(context, description)
        }
    }

    private suspend fun editAutoCreateRegexHttp(context: Context, description: String) {
        when (val result = viewModel.updateAutoCreateRegexUsername(usernameId!!, description)) {
            is NetworkResult.Success -> {
                listener?.autoCreateRegexEdited(result.data)
            }
            is NetworkResult.Error -> {
                binding.bsEditusernameUsernameSaveButton.revertAnimation()
                binding.bsEditusernameUsernameAutoCreateRegexTil.error =
                    context.resources.getString(R.string.error_edit_auto_create_regex) + "\n" + (result.errorOrNull() ?: "")
            }
        }
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditUsernameAutoCreateRegexBottomDialogListener {
        fun autoCreateRegexEdited(username: Usernames)
    }

    companion object {
        private const val ARG_USERNAME_ID = "arg_username_id"
        private const val ARG_AUTO_CREATE_REGEX = "arg_auto_create_regex"

        fun newInstance(id: String?, autoCreateRegex: String?): EditUsernameAutoCreateRegexBottomDialogFragment {
            return EditUsernameAutoCreateRegexBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USERNAME_ID, id)
                    putString(ARG_AUTO_CREATE_REGEX, autoCreateRegex)
                }
            }
        }
    }
}
