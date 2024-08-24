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
import host.stjin.anonaddy.databinding.BottomsheetEditAutoCreateRegexUsernameBinding
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Usernames
import kotlinx.coroutines.launch


class EditUsernameAutoCreateRegexBottomDialogFragment(
    private val usernameId: String?,
    private val autoCreateRegex: String?
) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddEditUsernameAutoCreateRegexBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditUsernameAutoCreateRegexBottomDialogListener {
        fun autoCreateRegexEdited(username: Usernames)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetEditAutoCreateRegexUsernameBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditAutoCreateRegexUsernameBinding.inflate(inflater, container, false)
        val root = binding.root

        // Check if usernameId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (usernameId != null) {
            listener = activity as AddEditUsernameAutoCreateRegexBottomDialogListener

            // Set button listeners and current description
            binding.bsEditusernameUsernameSaveButton.setOnClickListener(this)
            binding.bsEditusernameUsernameAutoCreateRegexTiet.setText(autoCreateRegex)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.bsEditAutoCreateRegexUsernameDesc.text =
                    (Html.fromHtml(requireContext().resources.getString(R.string.edit_auto_create_regex_desc), Html.FROM_HTML_MODE_COMPACT))
            } else {
                binding.bsEditAutoCreateRegexUsernameDesc.text =
                    (Html.fromHtml(requireContext().resources.getString(R.string.edit_auto_create_regex_desc)))
            }
        } else {
            dismiss()
        }

        return root
    }

    // Have an empty constructor the prevent the "could not find Fragment constructor when changing theme or rotating when the dialog is open"
    constructor() : this(null, null)

    companion object {
        fun newInstance(id: String, autoCreateRegex: String?): EditUsernameAutoCreateRegexBottomDialogFragment {
            return EditUsernameAutoCreateRegexBottomDialogFragment(id, autoCreateRegex)
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
        val networkHelper = NetworkHelper(context)
        networkHelper.updateAutoCreateRegexSpecificUsername({ username, error ->
            if (username != null) {
                listener.autoCreateRegexEdited(username)
            } else {

                // Revert the button to normal
                binding.bsEditusernameUsernameSaveButton.revertAnimation()

                binding.bsEditusernameUsernameAutoCreateRegexTil.error =
                    context.resources.getString(R.string.error_edit_auto_create_regex) + "\n" + error
            }
            // usernameId is never null at this point, hence the !!
        }, usernameId!!, description)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}