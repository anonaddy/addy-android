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
import com.google.android.material.chip.Chip
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import kotlinx.android.synthetic.main.bottomsheet_edit_recipient_domain.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class EditDomainRecipientBottomDialogFragment(
    private val domainId: String,
    private val defaultRecipient: String?
) :
    BottomSheetDialogFragment(),
    View.OnClickListener {


    private lateinit var listener: AddEditDomainRecipientBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditDomainRecipientBottomDialogListener {
        fun recipientEdited()
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
            R.layout.bottomsheet_edit_recipient_domain, container,
            false
        )
        listener = activity as AddEditDomainRecipientBottomDialogListener

        // Set button listeners and current description
        root.bs_editrecipient_save_button.setOnClickListener(this)

        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllRecipients(root, requireContext())
        }

        return root

    }

    private suspend fun getAllRecipients(root: View, context: Context) {
        val networkHelper = NetworkHelper(context)

        networkHelper.getRecipients({ result ->
            if (result != null) {
                for (recipient in result) {
                    val chip = Chip(root.bs_editrecipient_chipgroup.context)
                    chip.text = recipient.email
                    chip.tag = recipient.id
                    chip.isClickable = true
                    chip.isCheckable = true

                    chip.isChecked = defaultRecipient.equals(recipient.email)

                    root.bs_editrecipient_chipgroup.addView(chip)
                }
            }

        }, true)
    }


    companion object {
        fun newInstance(
            id: String,
            recipient: String?
        ): EditDomainRecipientBottomDialogFragment {
            return EditDomainRecipientBottomDialogFragment(id, recipient)
        }
    }

    private fun editRecipient(root: View, context: Context) {
        root.bs_editrecipient_save_button.isEnabled = false
        root.bs_editrecipient_save_progressbar.visibility = View.VISIBLE

        var recipient = ""
        val ids: List<Int> = root.bs_editrecipient_chipgroup.checkedChipIds
        for (id in ids) {
            val chip: Chip = root.bs_editrecipient_chipgroup.findViewById(id)
            recipient = chip.tag.toString()
        }


        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            editRecipientHttp(root, context, domainId, recipient)
        }
    }

    private suspend fun editRecipientHttp(
        root: View,
        context: Context,
        aliasId: String,
        recipient: String
    ) {
        val networkHelper = NetworkHelper(context)
        networkHelper.updateDefaultRecipientForSpecificDomain({ result ->
            if (result == "200") {
                listener.recipientEdited()
            } else {
                root.bs_editrecipient_save_button.isEnabled = true
                root.bs_editrecipient_save_progressbar.visibility = View.INVISIBLE
                root.bs_editrecipient_til.error =
                    context.resources.getString(R.string.error_edit_recipient) + "\n" + result
            }
        }, aliasId, recipient)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_editrecipient_save_button) {
                editRecipient(
                    requireView(),
                    requireContext()
                )
            }
        }
    }
}