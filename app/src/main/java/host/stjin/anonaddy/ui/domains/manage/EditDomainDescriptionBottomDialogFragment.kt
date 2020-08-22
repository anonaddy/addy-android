package host.stjin.anonaddy.ui.domains.manage

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import kotlinx.android.synthetic.main.bottomsheet_edit_description_domain.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class EditDomainDescriptionBottomDialogFragment(
    private val domainID: String,
    private val description: String?
) : BottomSheetDialogFragment(), View.OnClickListener {


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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // get the views and attach the listener
        val root = inflater.inflate(
            R.layout.bottomsheet_edit_description_domain, container,
            false
        )
        listener = activity as AddEditDomainDescriptionBottomDialogListener

        // Set button listeners and current description
        root.bs_editdomain_domain_save_button.setOnClickListener(this)
        root.bs_editdomain_domain_desc_tiet.setText(description)

        return root

    }

    companion object {
        fun newInstance(id: String, description: String?): EditDomainDescriptionBottomDialogFragment {
            return EditDomainDescriptionBottomDialogFragment(id, description)
        }
    }

    private fun verifyKey(root: View, context: Context) {
        val description = root.bs_editdomain_domain_desc_tiet.text.toString()
        root.bs_editdomain_domain_save_button.isEnabled = false
        root.bs_editdomain_domain_save_progressbar.visibility = View.VISIBLE


        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            editDescriptionHttp(root, context, description)
        }
    }

    private suspend fun editDescriptionHttp(root: View, context: Context, description: String) {
        val networkHelper = NetworkHelper(context)
        networkHelper.updateDescriptionSpecificDomain({ result ->
            if (result == "200") {
                listener.descriptionEdited(description)
            } else {
                root.bs_editdomain_domain_save_button.isEnabled = true
                root.bs_editdomain_domain_save_progressbar.visibility = View.INVISIBLE
                root.bs_editdomain_domain_desc_til.error =
                    context.resources.getString(R.string.error_edit_description) + "\n" + result
            }
        }, domainID, description)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_editdomain_domain_save_button) {
                verifyKey(
                    requireView(),
                    requireContext()
                )
            }
        }
    }
}