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


class ActionBottomDialogFragment(private val recipients: ArrayList<Recipients>, private val actionEditIndex: Int?, private val actionEditObject: Action?): BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddActionBottomDialogListener


    // 1. Defines the listener interface with a method passing back data result.
    interface AddActionBottomDialogListener {
        fun onAddedAction(actionEditIndex: Int?, type: String, value: String)
        fun onAddedAction(actionEditIndex: Int?, type: String, value: Boolean)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetRulesActionBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
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
        val typeText = actionTypeNames[actionTypes.indexOf(actionEditObject.type)]
        binding.bsRuleActionTypeMact.setText(typeText, false)
        binding.bsRuleActionValuesTiet.setText(actionEditObject.value)


        // If type is banner location, set value for it
        if (typeText == context.resources.getString(R.string.set_the_banner_information_location_to)) {
            binding.bsRuleActionValuesSpinnerBannerLocationMact.setText(actionEditObject.value, false)
        }

        // If type is banner location, set value for it
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
        viewLifecycleOwner.lifecycleScope.launch {
            getAllRecipients(null)
        }
    }

    checkIfTypeRequiresValueField(context)


    }

    /*
    Check if the type spinner matches any of the value-type type or spinner-type type
     */

    private fun spinnerChangeListener(context: Context) {
        binding.bsRuleActionTypeMact.setOnItemClickListener { _, _, _, _ ->
            checkIfTypeRequiresValueField(context)
        }
    }


    private fun checkIfTypeRequiresValueField(context: Context) {
        // If the type is set to set banner location show the spinner and hide the value field
        when {
            binding.bsRuleActionTypeMact.text.toString() == context.resources.getString(R.string.set_the_banner_information_location_to) -> {
                binding.bsRuleActionForwardToTil.visibility = View.GONE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.VISIBLE
                binding.bsRuleActionValuesTil.visibility = View.GONE
            }
            // If the type is set to block email hide all
            binding.bsRuleActionTypeMact.text.toString() == context.resources.getString(R.string.block_the_email) -> {
                binding.bsRuleActionForwardToTil.visibility = View.GONE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.GONE
            }
            // If the type is set to turn off PGP hide all
            binding.bsRuleActionTypeMact.text.toString() == context.resources.getString(R.string.turn_PGP_encryption_off) -> {
                binding.bsRuleActionForwardToTil.visibility = View.GONE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.GONE
            }
            // If the type is set to remove attachment hide all
            binding.bsRuleActionTypeMact.text.toString() == context.resources.getString(R.string.remove_attachments) -> {
                binding.bsRuleActionForwardToTil.visibility = View.GONE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.GONE
            }
            // If the type is set to forward to show recipients only
            binding.bsRuleActionTypeMact.text.toString() == context.resources.getString(R.string.forward_to) -> {
                binding.bsRuleActionForwardToTil.visibility = View.VISIBLE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.GONE
            }


            else -> {
                binding.bsRuleActionForwardToTil.visibility = View.GONE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.VISIBLE
            }
        }
    }


    private var actionTypes: List<String> = listOf()
    private var bannerLocations: List<String> = listOf()
    private var bannerLocationNames: List<String> = listOf()
    private var actionTypeNames: List<String> = listOf()
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

    companion object {
        fun newInstance(recipients: ArrayList<Recipients>, actionEditIndex: Int?, actionEditObject: Action?): ActionBottomDialogFragment {
            return ActionBottomDialogFragment(recipients, actionEditIndex, actionEditObject)
        }
    }

    private fun addAction(context: Context) {
        val type =
            actionTypes[actionTypeNames.indexOf(binding.bsRuleActionTypeMact.text.toString())]

        /*
        GET VALUES
         */

        when {
            // If the type is set to set banner information location get the value from the spinner
            binding.bsRuleActionTypeMact.text.toString() == context.resources.getString(R.string.set_the_banner_information_location_to) -> {
                val bannerLocation =
                    bannerLocations[bannerLocationNames.indexOf(binding.bsRuleActionValuesSpinnerBannerLocationMact.text.toString())]

                listener.onAddedAction(actionEditIndex, type, bannerLocation)
            }

            // If the type is set to block email send a true
            binding.bsRuleActionTypeMact.text.toString() == context.resources.getString(R.string.block_the_email) -> {
                listener.onAddedAction(actionEditIndex, type, true)
            }
            // If the type is set to turn off PGP send a true
            binding.bsRuleActionTypeMact.text.toString() == context.resources.getString(R.string.turn_PGP_encryption_off) -> {
                listener.onAddedAction(actionEditIndex, type, true)
            }
            // If the type is set to remove attachment send a true
            binding.bsRuleActionTypeMact.text.toString() == context.resources.getString(R.string.remove_attachments) -> {
                listener.onAddedAction(actionEditIndex, type, true)
            }
            // If the type is set to forward to send selected recipientID
            binding.bsRuleActionTypeMact.text.toString() == context.resources.getString(R.string.forward_to) -> {

                // Get selected chip
                var recipient: String
                val ids: List<Int> = binding.bsRuleActionForwardToChipgroup.checkedChipIds
                if (ids.isEmpty()){
                    binding.bsRuleActionForwardToTil.error = context.resources.getString(R.string.select_a_recipient)
                } else {
                    for (id in ids) {
                        val chip: Chip = binding.bsRuleActionForwardToChipgroup.findViewById(id)
                        recipient = chip.tag.toString()
                        listener.onAddedAction(actionEditIndex, type, recipient)
                    }
                }


            }
            else -> {
                // Else just get the textfield value
                val value = binding.bsRuleActionValuesTiet.text.toString()
                listener.onAddedAction(actionEditIndex, type, value)
            }
        }

    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_rule_action_add_action_button) {
                addAction(requireContext())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}