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
import host.stjin.anonaddy.databinding.BottomsheetEditFromNameDomainBinding
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch


class EditDomainFromNameBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private val viewModel: ManageDomainViewModel by activityViewModels()
    private var domainId: String? = null
    private var domain: String? = null
    private var fromName: String? = null

    private var listener: AddEditDomainFromNameBottomDialogListener? = null

    private var _binding: BottomsheetEditFromNameDomainBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            domainId = it.getString(ARG_DOMAIN_ID)
            domain = it.getString(ARG_DOMAIN)
            fromName = it.getString(ARG_FROM_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditFromNameDomainBinding.inflate(inflater, container, false)
        val root = binding.root

        if (domainId != null) {
            listener = (parentFragment as? AddEditDomainFromNameBottomDialogListener) ?: (activity as? AddEditDomainFromNameBottomDialogListener)

            // Set button listeners and current description
            binding.bsEditFromNameDomainSaveButton.setOnClickListener(this)
            binding.bsEditFromNameDomainFromNameTiet.setText(fromName)

            binding.bsEditFromNameDomainDesc.text = androidx.core.text.HtmlCompat.fromHtml(
                requireContext().resources.getString(R.string.edit_from_name_domain_desc, domain),
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
            if (p0.id == R.id.bs_edit_from_name_domain_save_button) {
                save(
                    requireContext()
                )
            }
        }
    }

    private fun save(context: Context) {
        val description = binding.bsEditFromNameDomainFromNameTiet.text.toString()

        // Animate the button to progress
        binding.bsEditFromNameDomainSaveButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            editFromNameHttp(context, description)
        }
    }

    private suspend fun editFromNameHttp(context: Context, description: String) {
        when (val result = viewModel.updateFromNameDomain(domainId!!, description)) {
            is NetworkResult.Success -> {
                listener?.fromNameEdited(result.data)
            }
            is NetworkResult.Error -> {
                binding.bsEditFromNameDomainSaveButton.revertAnimation()
                binding.bsEditFromNameDomainFromNameTil.error =
                    context.resources.getString(R.string.error_edit_from_name) + "\n" + (result.errorOrNull() ?: "")
            }
        }
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditDomainFromNameBottomDialogListener {
        fun fromNameEdited(domain: Domains)
    }

    companion object {
        private const val ARG_DOMAIN_ID = "arg_domain_id"
        private const val ARG_DOMAIN = "arg_domain"
        private const val ARG_FROM_NAME = "arg_from_name"

        fun newInstance(id: String?, domain: String?, description: String?): EditDomainFromNameBottomDialogFragment {
            return EditDomainFromNameBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DOMAIN_ID, id)
                    putString(ARG_DOMAIN, domain)
                    putString(ARG_FROM_NAME, description)
                }
            }
        }
    }
}
