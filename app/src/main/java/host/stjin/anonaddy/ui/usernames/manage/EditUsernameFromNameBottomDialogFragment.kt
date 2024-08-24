package host.stjin.anonaddy.ui.usernames.manage

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetEditFromNameUsernameBinding
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Usernames
import kotlinx.coroutines.launch


class EditUsernameFromNameBottomDialogFragment(
    private val usernameId: String?,
    private val username: String?,
    private val fromName: String?
) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddEditUsernameFromNameBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditUsernameFromNameBottomDialogListener {
        fun fromNameEdited(username: Usernames)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }


    private var _binding: BottomsheetEditFromNameUsernameBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditFromNameUsernameBinding.inflate(inflater, container, false)
        val root = binding.root

        // Check if usernameId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (usernameId != null) {
            listener = activity as AddEditUsernameFromNameBottomDialogListener

            // Set button listeners and current description
            binding.bsEditFromNameUsernameSaveButton.setOnClickListener(this)
            binding.bsEditFromNameUsernameFromNameTiet.setText(fromName)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.bsEditFromNameUsernameDesc.text = (Html.fromHtml(
                    requireContext().resources.getString(R.string.edit_from_name_username_desc, username),
                    Html.FROM_HTML_MODE_COMPACT
                ))
            } else {
                binding.bsEditFromNameUsernameDesc.text =
                    (Html.fromHtml(requireContext().resources.getString(R.string.edit_from_name_username_desc, username)))
            }

        } else {
            dismiss()
        }
        return root

    }

    // Have an empty constructor the prevent the "could not find Fragment constructor when changing theme or rotating when the dialog is open"
    constructor() : this(null, null, null)

    companion object {
        fun newInstance(id: String, username: String, description: String?): EditUsernameFromNameBottomDialogFragment {
            return EditUsernameFromNameBottomDialogFragment(id, username, description)
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
        val networkHelper = NetworkHelper(context)
        networkHelper.updateFromNameSpecificUsername({ username, error ->
            if (username != null) {
                listener.fromNameEdited(username)
            } else {
                // Revert the button to normal
                binding.bsEditFromNameUsernameSaveButton.revertAnimation()

                binding.bsEditFromNameUsernameFromNameTil.error =
                    context.resources.getString(R.string.error_edit_from_name) + "\n" + error
            }
            // usernameId is never null at this point, hence the !!
        }, usernameId!!, description)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}