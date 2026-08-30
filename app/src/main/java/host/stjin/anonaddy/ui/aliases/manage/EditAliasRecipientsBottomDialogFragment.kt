package host.stjin.anonaddy.ui.aliases.manage
import host.stjin.anonaddy_shared.utils.GsonTools

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetEditRecipientsAliasBinding
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch


class EditAliasRecipientsBottomDialogFragment :
    BaseBottomSheetDialogFragment(),
    View.OnClickListener {
    private val viewModel: ManageAliasViewModel by activityViewModels()
    private var aliasId: String? = null
    private var recipients: List<Recipients>? = null

    private var listener: AddEditAliasRecipientsBottomDialogListener? = null

    private var _binding: BottomsheetEditRecipientsAliasBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            aliasId = it.getString(ARG_ALIAS_ID)
            it.getString(ARG_RECIPIENTS_JSON)?.let { json ->
                val type = object : TypeToken<List<Recipients>>() {}.type
                recipients = GsonTools.gson.fromJson(json, type)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditRecipientsAliasBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = (parentFragment as? AddEditAliasRecipientsBottomDialogListener) ?: (activity as? AddEditAliasRecipientsBottomDialogListener)

        // Set button listeners and current description
        binding.bsEditrecipientsSaveButton.setOnClickListener(this)

        binding.bsEditrecipientsSaveButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            getAllRecipients()
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
            if (p0.id == R.id.bs_editrecipients_save_button) {
                editRecipients(
                    requireContext()
                )
            }
        }
    }

    private suspend fun getAllRecipients() {
        val recipientUnderThisAliasList = arrayListOf<String>()

        if (recipients != null) {
            for (recipient in recipients) {
                recipientUnderThisAliasList.add(recipient.email)
            }
        }

        val result = viewModel.getVerifiedRecipients()
        binding.bsEditrecipientsSaveButton.isEnabled = true
        if (result is NetworkResult.Success) {
            // Remove the default "Loading recipients" chip
            binding.bsEditrecipientsChipgroup.removeAllViewsInLayout()
            binding.bsEditrecipientsChipgroup.requestLayout()
            binding.bsEditrecipientsChipgroup.invalidate()

            for (recipient in result.data.data) {
                val chip = layoutInflater.inflate(R.layout.chip_view, binding.bsEditrecipientsChipgroup, false) as Chip
                chip.text = recipient.email
                chip.tag = recipient.id
                binding.bsEditrecipientsChipgroup.addView(chip)
                chip.isChecked = recipientUnderThisAliasList.contains(recipient.email)
            }
        }
    }

    private fun editRecipients(context: Context) {

        // Animate the button to progress
        binding.bsEditrecipientsSaveButton.startAnimation()

        val recipients = arrayListOf<String>()
        for (child in binding.bsEditrecipientsChipgroup.children) {
            val chip: Chip = child as Chip
            if (chip.isChecked) recipients.add(chip.tag.toString())
        }

        if (aliasId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                editRecipientsHttp(context, aliasId!!, recipients)
            }
        } else {
            listener?.bulkRecipientsEdited(recipients)
            dismiss()
        }
    }

    private suspend fun editRecipientsHttp(
        context: Context,
        aliasId: String,
        recipients: ArrayList<String>
    ) {
        when (val result = viewModel.updateRecipientsAlias(aliasId, recipients)) {
            is NetworkResult.Success -> {
                listener?.recipientsEdited(result.data)
            }
            is NetworkResult.Error -> {
                // Revert the button to normal
                binding.bsEditrecipientsSaveButton.revertAnimation()

                binding.bsEditrecipientsTil.error =
                    context.resources.getString(R.string.error_edit_recipients) + "\n" + result.error
            }
        }
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddEditAliasRecipientsBottomDialogListener {
        fun recipientsEdited(alias: Aliases)
        fun bulkRecipientsEdited(recipientIds: ArrayList<String>)
    }

    companion object {
        private const val ARG_ALIAS_ID = "arg_alias_id"
        private const val ARG_RECIPIENTS_JSON = "arg_recipients_json"

        fun newInstance(
            id: String?,
            recipients: List<Recipients>?
        ): EditAliasRecipientsBottomDialogFragment {
            return EditAliasRecipientsBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ALIAS_ID, id)
                    recipients?.let { putString(ARG_RECIPIENTS_JSON, GsonTools.gson.toJson(it)) }
                }
            }
        }
    }
}
