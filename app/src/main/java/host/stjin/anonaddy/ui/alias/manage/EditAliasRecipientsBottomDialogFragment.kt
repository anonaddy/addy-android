package host.stjin.anonaddy.ui.alias.manage

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
import host.stjin.anonaddy.models.Recipients
import kotlinx.android.synthetic.main.bottomsheet_edit_recipients_alias.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class EditAliasRecipientsBottomDialogFragment(
    private val aliasId: String,
    private val recipients: List<Recipients>?
) :
    BottomSheetDialogFragment(),
    View.OnClickListener {


    private lateinit var listener: AddEditAliasRecipientsBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditAliasRecipientsBottomDialogListener {
        fun recipientsEdited()
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
            R.layout.bottomsheet_edit_recipients_alias, container,
            false
        )
        listener = activity as AddEditAliasRecipientsBottomDialogListener

        // Set button listeners and current description
        root.bs_editrecipients_save_button.setOnClickListener(this)

        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllRecipients(root, requireContext())
        }

        return root

    }

    private suspend fun getAllRecipients(root: View, context: Context) {
        val networkHelper = NetworkHelper(context)

        val recipientUnderThisAliasList = arrayListOf<String>()

        if (recipients != null) {
            for (recipient in recipients) {
                recipientUnderThisAliasList.add(recipient.email)
            }
        }

        networkHelper.getRecipients({ result ->
            if (result != null) {
                for (recipient in result) {
                    val chip = Chip(root.bs_editrecipients_chipgroup.context)
                    chip.text = recipient.email
                    chip.tag = recipient.id
                    chip.isClickable = true
                    chip.isCheckable = true


                    chip.isChecked = recipientUnderThisAliasList.contains(recipient.email)


                    root.bs_editrecipients_chipgroup.addView(chip)
                }
            }

        }, true)
    }


    companion object {
        fun newInstance(
            id: String,
            recipients: List<Recipients>?
        ): EditAliasRecipientsBottomDialogFragment {
            return EditAliasRecipientsBottomDialogFragment(id, recipients)
        }
    }

    private fun editRecipients(root: View, context: Context) {
        root.bs_editrecipients_save_button.isEnabled = false
        root.bs_editrecipients_save_progressbar.visibility = View.VISIBLE

        val recipients = arrayListOf<String>()
        val ids: List<Int> = root.bs_editrecipients_chipgroup.checkedChipIds
        for (id in ids) {
            val chip: Chip = root.bs_editrecipients_chipgroup.findViewById(id)
            recipients.add(chip.tag.toString())
        }


        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            editRecipientsHttp(root, context, aliasId, recipients)
        }
    }

    private suspend fun editRecipientsHttp(
        root: View,
        context: Context,
        aliasId: String,
        recipients: ArrayList<String>
    ) {
        val networkHelper = NetworkHelper(context)
        networkHelper.updateRecipientsSpecificAlias({ result ->
            if (result == "200") {
                listener.recipientsEdited()
            } else {
                root.bs_editrecipients_save_button.isEnabled = true
                root.bs_editrecipients_save_progressbar.visibility = View.INVISIBLE
                root.bs_editrecipients_til.error =
                    context.resources.getString(R.string.error_edit_recipients) + "\n" + result
            }
        }, aliasId, recipients)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_editrecipients_save_button) {
                editRecipients(
                    requireView(),
                    requireContext()
                )
            }
        }
    }
}