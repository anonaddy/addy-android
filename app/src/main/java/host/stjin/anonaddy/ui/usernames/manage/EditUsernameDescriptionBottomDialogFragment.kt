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
import host.stjin.anonaddy.databinding.BottomsheetEditDescriptionUsernameBinding
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy_shared.models.Usernames
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch


class EditUsernameDescriptionBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private val viewModel: ManageUsernameViewModel by activityViewModels()
    private var usernameId: String? = null
    private var description: String? = null

    private var listener: AddEditUsernameDescriptionBottomDialogListener? = null

    private var _binding: BottomsheetEditDescriptionUsernameBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            usernameId = it.getString(ARG_USERNAME_ID)
            description = it.getString(ARG_DESCRIPTION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditDescriptionUsernameBinding.inflate(inflater, container, false)
        val root = binding.root

        if (usernameId != null) {
            listener = (parentFragment as? AddEditUsernameDescriptionBottomDialogListener) ?: (activity as? AddEditUsernameDescriptionBottomDialogListener)

            // Set button listeners and current description
            binding.bsEditusernameUsernameSaveButton.setOnClickListener(this)
            binding.bsEditusernameUsernameDescTiet.setText(description)
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
        val description = binding.bsEditusernameUsernameDescTiet.text.toString()

        // Animate the button to progress
        binding.bsEditusernameUsernameSaveButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            editDescriptionHttp(context, description)
        }
    }

    private suspend fun editDescriptionHttp(context: Context, description: String) {
        when (val result = viewModel.updateDescriptionUsername(usernameId!!, description)) {
            is NetworkResult.Success -> {
                listener?.descriptionEdited(result.data)
            }
            is NetworkResult.Error -> {
                binding.bsEditusernameUsernameSaveButton.revertAnimation()
                binding.bsEditusernameUsernameDescTil.error =
                    context.resources.getString(R.string.error_edit_description) + "\n" + (result.errorOrNull() ?: "")
            }
        }
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditUsernameDescriptionBottomDialogListener {
        fun descriptionEdited(username: Usernames)
    }

    companion object {
        private const val ARG_USERNAME_ID = "arg_username_id"
        private const val ARG_DESCRIPTION = "arg_description"

        fun newInstance(id: String?, description: String?): EditUsernameDescriptionBottomDialogFragment {
            return EditUsernameDescriptionBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USERNAME_ID, id)
                    putString(ARG_DESCRIPTION, description)
                }
            }
        }
    }
}
