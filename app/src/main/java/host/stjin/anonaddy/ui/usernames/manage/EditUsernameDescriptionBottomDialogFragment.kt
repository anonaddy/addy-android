package host.stjin.anonaddy.ui.usernames.manage

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetEditDescriptionUsernameBinding
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Usernames
import kotlinx.coroutines.launch


class EditUsernameDescriptionBottomDialogFragment(
    private val usernameId: String?,
    private val description: String?
) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddEditUsernameDescriptionBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditUsernameDescriptionBottomDialogListener {
        fun descriptionEdited(username: Usernames)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }


    private var _binding: BottomsheetEditDescriptionUsernameBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditDescriptionUsernameBinding.inflate(inflater, container, false)
        val root = binding.root

        // Check if usernameId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (usernameId != null) {
            listener = activity as AddEditUsernameDescriptionBottomDialogListener

            // Set button listeners and current description
            binding.bsEditusernameUsernameSaveButton.setOnClickListener(this)
            binding.bsEditusernameUsernameDescTiet.setText(description)
        } else {
            dismiss()
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setIMEAnimation(binding.bsEditusernameUsernameRoot)
        }
        return root

    }

    // Have an empty constructor the prevent the "could not find Fragment constructor when changing theme or rotating when the dialog is open"
    constructor() : this(null, null)

    companion object {
        fun newInstance(id: String, description: String?): EditUsernameDescriptionBottomDialogFragment {
            return EditUsernameDescriptionBottomDialogFragment(id, description)
        }
    }

    private fun verifyKey(context: Context) {
        val description = binding.bsEditusernameUsernameDescTiet.text.toString()

        // Animate the button to progress
        binding.bsEditusernameUsernameSaveButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            editDescriptionHttp(context, description)
        }
    }

    private suspend fun editDescriptionHttp(context: Context, description: String) {
        val networkHelper = NetworkHelper(context)
        networkHelper.updateDescriptionSpecificUsername({ username, error ->
            if (username != null) {
                listener.descriptionEdited(username)
            } else {
                // Revert the button to normal
                binding.bsEditusernameUsernameSaveButton.revertAnimation()

                binding.bsEditusernameUsernameDescTil.error =
                    context.resources.getString(R.string.error_edit_description) + "\n" + error
            }
            // usernameId is never null at this point, hence the !!
        }, usernameId!!, description)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_editusername_username_save_button) {
                verifyKey(
                    requireContext()
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}