package host.stjin.anonaddy.ui.domains.manage

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
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetEditDescriptionDomainBinding
import kotlinx.coroutines.launch


class EditDomainDescriptionBottomDialogFragment(
    private val domainId: String?,
    private val description: String?
) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddEditDomainDescriptionBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditDomainDescriptionBottomDialogListener {
        fun descriptionEdited(description: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetEditDescriptionDomainBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditDescriptionDomainBinding.inflate(inflater, container, false)
        val root = binding.root

        // Check if domainId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (domainId != null) {
            listener = activity as AddEditDomainDescriptionBottomDialogListener

            // Set button listeners and current description
            binding.bsEditdomainDomainSaveButton.setOnClickListener(this)
            binding.bsEditdomainDomainDescTiet.setText(description)
        } else {
            dismiss()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setIMEAnimation(binding.bsEditdomainDomainRoot)
        }

        return root
    }

    // Have an empty constructor the prevent the "could not find Fragment constructor when changing theme or rotating when the dialog is open"
    constructor() : this(null, null)

    companion object {
        fun newInstance(id: String, description: String?): EditDomainDescriptionBottomDialogFragment {
            return EditDomainDescriptionBottomDialogFragment(id, description)
        }
    }

    private fun verifyKey(context: Context) {
        val description = binding.bsEditdomainDomainDescTiet.text.toString()

        // Animate the button to progress
        binding.bsEditdomainDomainSaveButton.startAnimation()


        viewLifecycleOwner.lifecycleScope.launch {
            editDescriptionHttp(context, description)
        }
    }

    private suspend fun editDescriptionHttp(context: Context, description: String) {
        val networkHelper = NetworkHelper(context)
        networkHelper.updateDescriptionSpecificDomain({ result ->
            if (result == "200") {
                listener.descriptionEdited(description)
            } else {

                // Revert the button to normal
                binding.bsEditdomainDomainSaveButton.revertAnimation()

                binding.bsEditdomainDomainDescTil.error =
                    context.resources.getString(R.string.error_edit_description) + "\n" + result
            }
            // domainId is never null at this point, hence the !!
        }, domainId!!, description)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_editdomain_domain_save_button) {
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