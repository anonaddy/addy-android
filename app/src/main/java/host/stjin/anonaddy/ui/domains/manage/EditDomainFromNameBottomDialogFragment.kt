package host.stjin.anonaddy.ui.domains.manage

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
import host.stjin.anonaddy.databinding.BottomsheetEditFromNameDomainBinding
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Domains
import kotlinx.coroutines.launch


class EditDomainFromNameBottomDialogFragment(
    private val domainId: String?,
    private val domain: String?,
    private val fromName: String?
) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddEditDomainFromNameBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditDomainFromNameBottomDialogListener {
        fun fromNameEdited(domain: Domains)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetEditFromNameDomainBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditFromNameDomainBinding.inflate(inflater, container, false)
        val root = binding.root

        // Check if domainId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (domainId != null) {
            listener = activity as AddEditDomainFromNameBottomDialogListener

            // Set button listeners and current description
            binding.bsEditFromNameDomainSaveButton.setOnClickListener(this)
            binding.bsEditFromNameDomainFromNameTiet.setText(fromName)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.bsEditFromNameDomainDesc.text =
                    (Html.fromHtml(requireContext().resources.getString(R.string.edit_from_name_domain_desc, domain), Html.FROM_HTML_MODE_COMPACT))
            } else {
                binding.bsEditFromNameDomainDesc.text =
                    (Html.fromHtml(requireContext().resources.getString(R.string.edit_from_name_domain_desc, domain)))
            }
        } else {
            dismiss()
        }

        return root
    }

    // Have an empty constructor the prevent the "could not find Fragment constructor when changing theme or rotating when the dialog is open"
    constructor() : this(null, null, null)

    companion object {
        fun newInstance(id: String, domain: String, description: String?): EditDomainFromNameBottomDialogFragment {
            return EditDomainFromNameBottomDialogFragment(id, domain, description)
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
        val networkHelper = NetworkHelper(context)
        networkHelper.updateFromNameSpecificDomain({ domain, error ->
            if (domain != null) {
                listener.fromNameEdited(domain)
            } else {

                // Revert the button to normal
                binding.bsEditFromNameDomainSaveButton.revertAnimation()

                binding.bsEditFromNameDomainFromNameTil.error =
                    context.resources.getString(R.string.error_edit_from_name) + "\n" + error
            }
            // domainId is never null at this point, hence the !!
        }, domainId!!, description)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}