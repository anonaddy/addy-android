package host.stjin.anonaddy.ui.aliases.manage

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
import host.stjin.anonaddy.databinding.BottomsheetEditFromNameAliasBinding
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch


class EditAliasFromNameBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private val viewModel: ManageAliasViewModel by activityViewModels()
    private var aliasId: String? = null
    private var aliasEmail: String? = null
    private var fromName: String? = null

    private var listener: AddEditAliasFromNameBottomDialogListener? = null

    private var _binding: BottomsheetEditFromNameAliasBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            aliasId = it.getString(ARG_ALIAS_ID)
            aliasEmail = it.getString(ARG_ALIAS_EMAIL)
            fromName = it.getString(ARG_FROM_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditFromNameAliasBinding.inflate(inflater, container, false)
        val root = binding.root

        if (aliasId != null) {
            listener = (parentFragment as? AddEditAliasFromNameBottomDialogListener) ?: (activity as? AddEditAliasFromNameBottomDialogListener)

            // Set button listeners and current description
            binding.bsEditFromNameAliasSaveButton.setOnClickListener(this)
            binding.bsEditFromNameAliasFromNameTiet.setText(fromName)

            binding.bsEditFromNameAliasDesc.text = androidx.core.text.HtmlCompat.fromHtml(
                requireContext().resources.getString(R.string.edit_from_name_alias_desc, aliasEmail),
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
            if (p0.id == R.id.bs_edit_from_name_alias_save_button) {
                editDescription(
                    requireContext()
                )
            }
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
        when (val result = viewModel.updateFromNameAlias(aliasId!!, description)) {
            is NetworkResult.Success -> {
                listener?.fromNameEdited(result.data)
            }
            is NetworkResult.Error -> {
                // Animate the button to progress
                binding.bsEditFromNameAliasSaveButton.revertAnimation()

                binding.bsEditFromNameAliasFromNameTil.error =
                    context.resources.getString(R.string.error_edit_from_name) + "\n" + result.error
            }
        }
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditAliasFromNameBottomDialogListener {
        fun fromNameEdited(alias: Aliases)
    }

    companion object {
        private const val ARG_ALIAS_ID = "arg_alias_id"
        private const val ARG_ALIAS_EMAIL = "arg_alias_email"
        private const val ARG_FROM_NAME = "arg_from_name"

        fun newInstance(id: String?, aliasEmail: String?, fromName: String?): EditAliasFromNameBottomDialogFragment {
            return EditAliasFromNameBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ALIAS_ID, id)
                    putString(ARG_ALIAS_EMAIL, aliasEmail)
                    putString(ARG_FROM_NAME, fromName)
                }
            }
        }
    }
}
