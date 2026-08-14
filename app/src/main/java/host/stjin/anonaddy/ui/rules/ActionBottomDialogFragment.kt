package host.stjin.anonaddy.ui.rules

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetRulesActionBinding
import host.stjin.anonaddy_shared.models.Action
import host.stjin.anonaddy_shared.models.Recipients
import kotlinx.coroutines.launch


class ActionBottomDialogFragment(
    private val recipients: ArrayList<Recipients>,
    private val actionEditIndex: Int?,
    private val actionEditObject: Action?
) : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var listener: AddActionBottomDialogListener

    private var _binding: BottomsheetRulesActionBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    /*
        Check if the type spinner matches any of the value-type type or spinner-type type
     */
    private fun spinnerChangeListener(context: Context) {
        binding.bsRuleActionTypeMact.setOnItemClickListener { _, _, _, _ ->
            checkIfTypeRequiresValueField(context)
            checkIfTypeShouldShowHint()
        }
    }

    private var actionTypes: List<String> = listOf()

    private var bannerLocations: List<String> = listOf()

    private var bannerLocationNames: List<String> = listOf()

    private var actionTypeNames: List<String> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetRulesActionBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = activity as AddActionBottomDialogListener


        fillSpinners(requireContext())
        binding.bsRuleActionAddActionButton.setOnClickListener(this)
        spinnerChangeListener(requireContext())

        updateUi(requireContext())

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
            if (p0.id == R.id.bs_rule_action_add_action_button) {
                addAction(requireContext())
            }
        }
    }

    private fun getAllRecipients(selectedRecipientId: String?) {

        // Remove the default "Loading recipients" chip
        binding.bsRuleActionForwardToChipgroup.removeAllViewsInLayout()
        binding.bsRuleActionForwardToChipgroup.requestLayout()
        binding.bsRuleActionForwardToChipgroup.invalidate()

        for (recipient in recipients) {
            if (recipient.email_verified_at != null) {
                val chip = layoutInflater.inflate(R.layout.chip_view, binding.bsRuleActionForwardToChipgroup, false) as Chip
                chip.text = recipient.email
                chip.tag = recipient.id
                chip.isChecked = selectedRecipientId.equals(recipient.id)

                binding.bsRuleActionForwardToChipgroup.addView(chip)
            }
        }

    }

    private fun updateUi(context: Context) {

        if (actionEditObject != null) {
            val typeIndex = actionTypes.indexOf(actionEditObject.type)
            if (typeIndex != -1) {
                binding.bsRuleActionTypeMact.setText(actionTypeNames[typeIndex], false)
            }
            binding.bsRuleActionValuesTiet.setText(actionEditObject.value)


            // If type is banner location, set value for it
            if (actionEditObject.type == "banner") {
                binding.bsRuleActionValuesSpinnerBannerLocationMact.setText(actionEditObject.value, false)
            }

            if (actionEditObject.type == "forwardTo") {
                viewLifecycleOwner.lifecycleScope.launch {
                    getAllRecipients(actionEditObject.value)
                }
            } else {
                // If not forward_to, get recipients without selected
                viewLifecycleOwner.lifecycleScope.launch {
                    getAllRecipients(null)
                }
            }

            // Show save instead of add when editing an object
            binding.bsRuleActionAddActionButton.setText(R.string.save)
        } else {
            if (actionTypeNames.isNotEmpty()) {
                binding.bsRuleActionTypeMact.setText(actionTypeNames[0], false)
            }
            viewLifecycleOwner.lifecycleScope.launch {
                getAllRecipients(null)
            }
        }

        checkIfTypeRequiresValueField(context)
        checkIfTypeShouldShowHint()
    }

    private fun checkIfTypeShouldShowHint() {
        val typeIndex = actionTypeNames.indexOf(binding.bsRuleActionTypeMact.text.toString())
        val type = if (typeIndex != -1) actionTypes[typeIndex] else ""
        if (type == "subject") {
            binding.bsRuleActionValuesTilSubjectHint.visibility = View.VISIBLE
        } else {
            binding.bsRuleActionValuesTilSubjectHint.visibility = View.GONE
        }
    }

    private fun checkIfTypeRequiresValueField(context: Context) {
        val typeIndex = actionTypeNames.indexOf(binding.bsRuleActionTypeMact.text.toString())
        val type = if (typeIndex != -1) actionTypes[typeIndex] else "subject"

        when (type) {
            "banner" -> {
                binding.bsRuleActionForwardToTil.visibility = View.GONE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.VISIBLE
                binding.bsRuleActionValuesTil.visibility = View.GONE
            }
            "forwardTo" -> {
                binding.bsRuleActionForwardToTil.visibility = View.VISIBLE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.GONE
            }
            "block", "encryption", "blocklistSender", "blocklistDomain", "removeAttachments", "deactivateAlias", "deleteAlias" -> {
                binding.bsRuleActionForwardToTil.visibility = View.GONE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.GONE
            }
            "addLabel", "removeLabel" -> {
                binding.bsRuleActionForwardToTil.visibility = View.GONE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.VISIBLE
                binding.bsRuleActionValuesTil.hint = context.resources.getString(R.string.label_name)
            }
            "setAliasDescription" -> {
                binding.bsRuleActionForwardToTil.visibility = View.GONE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.VISIBLE
                binding.bsRuleActionValuesTil.hint = context.resources.getString(R.string.enter_description)
            }
            else -> {
                binding.bsRuleActionForwardToTil.visibility = View.GONE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.VISIBLE
                binding.bsRuleActionValuesTil.hint = context.resources.getString(R.string.enter_value)
            }
        }

    }

    private fun fillSpinners(context: Context) {
        actionTypes = this.resources.getStringArray(R.array.actions_type).toList()
        actionTypeNames = this.resources.getStringArray(R.array.actions_type_name).toList()
        bannerLocations = this.resources.getStringArray(R.array.actions_type_bannerlocation_options).toList()
        bannerLocationNames = this.resources.getStringArray(R.array.actions_type_bannerlocation_options_name).toList()

        val domainAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            R.layout.dropdown_menu_popup_item,
            actionTypeNames
        )
        binding.bsRuleActionTypeMact.setAdapter(domainAdapter)


        val formatAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            R.layout.dropdown_menu_popup_item,
            bannerLocationNames
        )
        binding.bsRuleActionValuesSpinnerBannerLocationMact.setAdapter(formatAdapter)
    }

    private fun addAction(context: Context) {
        val typeIndex = actionTypeNames.indexOf(binding.bsRuleActionTypeMact.text.toString())
        if (typeIndex == -1) return
        val type = actionTypes[typeIndex]

        /*
        GET VALUES
         */

        when (type) {
            "banner" -> {
                val bannerLocation =
                    bannerLocations[bannerLocationNames.indexOf(binding.bsRuleActionValuesSpinnerBannerLocationMact.text.toString())]

                listener.onAddedAction(actionEditIndex, type, bannerLocation)
            }

            "block", "encryption", "blocklistSender", "blocklistDomain", "removeAttachments", "deactivateAlias", "deleteAlias" -> {
                listener.onAddedAction(actionEditIndex, type, true)
            }

            "forwardTo" -> {
                // Get selected chip
                val ids: List<Int> = binding.bsRuleActionForwardToChipgroup.checkedChipIds
                if (ids.isEmpty()) {
                    binding.bsRuleActionForwardToTil.error = context.resources.getString(R.string.select_a_recipient)
                } else {
                    for (id in ids) {
                        val chip: Chip = binding.bsRuleActionForwardToChipgroup.findViewById(id)
                        val recipient = chip.tag.toString()
                        listener.onAddedAction(actionEditIndex, type, recipient)
                    }
                }
            }

            else -> {
                val value = binding.bsRuleActionValuesTiet.text.toString()
                listener.onAddedAction(actionEditIndex, type, value)
            }
        }

    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddActionBottomDialogListener {
        fun onAddedAction(actionEditIndex: Int?, type: String, value: String)
        fun onAddedAction(actionEditIndex: Int?, type: String, value: Boolean)
    }

    companion object {
        fun newInstance(recipients: ArrayList<Recipients>, actionEditIndex: Int?, actionEditObject: Action?): ActionBottomDialogFragment {
            return ActionBottomDialogFragment(recipients, actionEditIndex, actionEditObject)
        }
    }
}
