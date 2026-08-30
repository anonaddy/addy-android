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
import host.stjin.anonaddy.databinding.BottomsheetEditFromNameUsernameBinding
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy_shared.models.Usernames
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch


class EditUsernameFromNameBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private val viewModel: ManageUsernameViewModel by activityViewModels()
    private var usernameId: String? = null
    private var username: String? = null
    private var fromName: String? = null

    private var listener: AddEditUsernameFromNameBottomDialogListener? = null

    private var _binding: BottomsheetEditFromNameUsernameBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            usernameId = it.getString(ARG_USERNAME_ID)
            username = it.getString(ARG_USERNAME)
            fromName = it.getString(ARG_FROM_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditFromNameUsernameBinding.inflate(inflater, container, false)
        val root = binding.root

        if (usernameId != null) {
            listener = (parentFragment as? AddEditUsernameFromNameBottomDialogListener) ?: (activity as? AddEditUsernameFromNameBottomDialogListener)

            // Set button listeners and current description
            binding.bsEditFromNameUsernameSaveButton.setOnClickListener(this)
            binding.bsEditFromNameUsernameFromNameTiet.setText(fromName)

            binding.bsEditFromNameUsernameDesc.text = androidx.core.text.HtmlCompat.fromHtml(
                requireContext().resources.getString(R.string.edit_from_name_username_desc, username),
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
            if (p0.id == R.id.bs_edit_from_name_username_save_button) {
                save(
                    requireContext()
                )
            }
        }
    }

    private fun save(context: Context) {
        val description = binding.bsEditFromNameUsernameFromNameTiet.text.toString()

        // Animate the button to progress
        binding.bsEditFromNameUsernameSaveButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            editFromNameHttp(context, description)
        }
    }

    private suspend fun editFromNameHttp(context: Context, description: String) {
        when (val result = viewModel.updateFromNameUsername(usernameId!!, description)) {
            is NetworkResult.Success -> {
                listener?.fromNameEdited(result.data)
            }
            is NetworkResult.Error -> {
                binding.bsEditFromNameUsernameSaveButton.revertAnimation()
                binding.bsEditFromNameUsernameFromNameTil.error =
                    context.resources.getString(R.string.error_edit_from_name) + "\n" + (result.errorOrNull() ?: "")
            }
        }
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditUsernameFromNameBottomDialogListener {
        fun fromNameEdited(username: Usernames)
    }

    companion object {
        private const val ARG_USERNAME_ID = "arg_username_id"
        private const val ARG_USERNAME = "arg_username"
        private const val ARG_FROM_NAME = "arg_from_name"

        fun newInstance(id: String?, username: String?, description: String?): EditUsernameFromNameBottomDialogFragment {
            return EditUsernameFromNameBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USERNAME_ID, id)
                    putString(ARG_USERNAME, username)
                    putString(ARG_FROM_NAME, description)
                }
            }
        }
    }
}
