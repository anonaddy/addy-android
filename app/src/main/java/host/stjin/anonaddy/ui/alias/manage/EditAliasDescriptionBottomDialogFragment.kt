package host.stjin.anonaddy.ui.alias.manage

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetEditDescriptionAliasBinding
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Aliases
import kotlinx.coroutines.launch


class EditAliasDescriptionBottomDialogFragment(
    private val aliasId: String?,
    private val description: String?
) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddEditAliasDescriptionBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditAliasDescriptionBottomDialogListener {
        fun descriptionEdited(alias: Aliases)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }


    private var _binding: BottomsheetEditDescriptionAliasBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditDescriptionAliasBinding.inflate(inflater, container, false)
        val root = binding.root

        // Check if aliasId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (aliasId != null) {
            listener = activity as AddEditAliasDescriptionBottomDialogListener

            // Set button listeners and current description
            binding.bsEditaliasAliasSaveButton.setOnClickListener(this)
            binding.bsEditaliasAliasDescTiet.setText(description)
        } else {
            dismiss()
        }
        return root

    }

    // Have an empty constructor the prevent the "could not find Fragment constructor when changing theme or rotating when the dialog is open"
    constructor() : this(null, null)

    companion object {
        fun newInstance(id: String, description: String?): EditAliasDescriptionBottomDialogFragment {
            return EditAliasDescriptionBottomDialogFragment(id, description)
        }
    }

    private fun editDescription(context: Context) {
        val description = binding.bsEditaliasAliasDescTiet.text.toString()

        // Animate the button to progress
        binding.bsEditaliasAliasSaveButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            editDescriptionHttp(context, description)
        }
    }

    private suspend fun editDescriptionHttp(context: Context, description: String) {
        val networkHelper = NetworkHelper(context)
        networkHelper.updateDescriptionSpecificAlias({ alias, error ->
            if (alias != null) {
                listener.descriptionEdited(alias)
            } else {
                // Animate the button to progress
                binding.bsEditaliasAliasSaveButton.revertAnimation()

                binding.bsEditaliasAliasDescTil.error =
                    context.resources.getString(R.string.error_edit_description) + "\n" + error
            }
            // aliasId is never null at this point, hence the !!
        }, aliasId!!, description)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_editalias_alias_save_button) {
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