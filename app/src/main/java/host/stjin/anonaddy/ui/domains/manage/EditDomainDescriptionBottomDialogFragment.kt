package host.stjin.anonaddy.ui.domains.manage

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
import host.stjin.anonaddy.databinding.BottomsheetEditDescriptionDomainBinding
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch


class EditDomainDescriptionBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private val viewModel: ManageDomainViewModel by activityViewModels()
    private var domainId: String? = null
    private var description: String? = null

    private var listener: AddEditDomainDescriptionBottomDialogListener? = null

    private var _binding: BottomsheetEditDescriptionDomainBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            domainId = it.getString(ARG_DOMAIN_ID)
            description = it.getString(ARG_DESCRIPTION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditDescriptionDomainBinding.inflate(inflater, container, false)
        val root = binding.root

        if (domainId != null) {
            listener = (parentFragment as? AddEditDomainDescriptionBottomDialogListener) ?: (activity as? AddEditDomainDescriptionBottomDialogListener)

            // Set button listeners and current description
            binding.bsEditdomainDomainSaveButton.setOnClickListener(this)
            binding.bsEditdomainDomainDescTiet.setText(description)
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
            if (p0.id == R.id.bs_editdomain_domain_save_button) {
                save(
                    requireContext()
                )
            }
        }
    }

    private fun save(context: Context) {
        val description = binding.bsEditdomainDomainDescTiet.text.toString()

        // Animate the button to progress
        binding.bsEditdomainDomainSaveButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            editDescriptionHttp(context, description)
        }
    }

    private suspend fun editDescriptionHttp(context: Context, description: String) {
        when (val result = viewModel.updateDescriptionDomain(domainId!!, description)) {
            is NetworkResult.Success -> {
                listener?.descriptionEdited(result.data)
            }
            is NetworkResult.Error -> {
                binding.bsEditdomainDomainSaveButton.revertAnimation()
                binding.bsEditdomainDomainDescTil.error =
                    context.resources.getString(R.string.error_edit_description) + "\n" + (result.errorOrNull() ?: "")
            }
        }
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditDomainDescriptionBottomDialogListener {
        fun descriptionEdited(domain: Domains)
    }

    companion object {
        private const val ARG_DOMAIN_ID = "arg_domain_id"
        private const val ARG_DESCRIPTION = "arg_description"

        fun newInstance(id: String?, description: String?): EditDomainDescriptionBottomDialogFragment {
            return EditDomainDescriptionBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DOMAIN_ID, id)
                    putString(ARG_DESCRIPTION, description)
                }
            }
        }
    }
}
