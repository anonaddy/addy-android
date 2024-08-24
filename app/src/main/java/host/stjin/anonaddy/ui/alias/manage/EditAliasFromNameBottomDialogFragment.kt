package host.stjin.anonaddy.ui.alias.manage

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
import host.stjin.anonaddy.databinding.BottomsheetEditFromNameAliasBinding
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Aliases
import kotlinx.coroutines.launch


class EditAliasFromNameBottomDialogFragment(
    private val aliasId: String?,
    private val aliasEmail: String?,
    private val fromName: String?
) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddEditAliasFromNameBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditAliasFromNameBottomDialogListener {
        fun fromNameEdited(alias: Aliases)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }


    private var _binding: BottomsheetEditFromNameAliasBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditFromNameAliasBinding.inflate(inflater, container, false)
        val root = binding.root

        // Check if aliasId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (aliasId != null) {
            listener = activity as AddEditAliasFromNameBottomDialogListener

            // Set button listeners and current description
            binding.bsEditFromNameAliasSaveButton.setOnClickListener(this)
            binding.bsEditFromNameAliasFromNameTiet.setText(fromName)


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.bsEditFromNameAliasDesc.text =
                    (Html.fromHtml(requireContext().resources.getString(R.string.edit_from_name_alias_desc, aliasEmail), Html.FROM_HTML_MODE_COMPACT))
            } else {
                binding.bsEditFromNameAliasDesc.text =
                    (Html.fromHtml(requireContext().resources.getString(R.string.edit_from_name_alias_desc, aliasEmail)))
            }

        } else {
            dismiss()
        }

        return root

    }

    // Have an empty constructor the prevent the "could not find Fragment constructor when changing theme or rotating when the dialog is open"
    constructor() : this(null, null, null)

    companion object {
        fun newInstance(id: String, aliasEmail: String, fromName: String?): EditAliasFromNameBottomDialogFragment {
            return EditAliasFromNameBottomDialogFragment(id, aliasEmail, fromName)
        }
    }

    private fun editDescription(context: Context) {
        val description = binding.bsEditFromNameAliasFromNameTiet.text.toString()

        // Animate the button to progress
        binding.bsEditFromNameAliasSaveButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            editFromNameHttp(context, description)
        }
    }

    private suspend fun editFromNameHttp(context: Context, description: String) {
        val networkHelper = NetworkHelper(context)
        networkHelper.updateFromNameSpecificAlias({ alias, error ->
            if (alias != null) {
                listener.fromNameEdited(alias)
            } else {
                // Animate the button to progress
                binding.bsEditFromNameAliasSaveButton.revertAnimation()

                binding.bsEditFromNameAliasFromNameTil.error =
                    context.resources.getString(R.string.error_edit_from_name) + "\n" + error
            }
            // aliasId is never null at this point, hence the !!
        }, aliasId!!, description)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_edit_from_name_alias_save_button) {
                editDescription(
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