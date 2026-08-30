package host.stjin.anonaddy.ui.rules.manage
import host.stjin.anonaddy_shared.utils.GsonTools

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetRulesActionBinding
import host.stjin.anonaddy.ui.labels.AddLabelBottomDialogFragment
import host.stjin.anonaddy.ui.labels.LabelsViewModel
import host.stjin.anonaddy_shared.models.Action
import host.stjin.anonaddy_shared.models.Labels
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.network.NetworkResult
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import kotlin.math.max
import kotlin.math.min


class ActionBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener, AddLabelBottomDialogFragment.AddLabelsBottomDialogListener {
    private val viewModel: LabelsViewModel by viewModels()
    private var recipients: ArrayList<Recipients> = arrayListOf()
    private var actionEditIndex: Int? = null
    private var actionEditObject: Action? = null

    private var listener: AddActionBottomDialogListener? = null

    private var _binding: BottomsheetRulesActionBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var allLabels: List<Labels>? = null
    private var selectedLabelName: String? = null
    private var recentlyAddedLabelName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            it.getString(ARG_RECIPIENTS_JSON)?.let { json ->
                try {
                    val type = object : TypeToken<ArrayList<Recipients>>() {}.type
                    recipients = GsonTools.gson.fromJson(json, type) ?: arrayListOf()
                } catch (e: Exception) {
                    recipients = arrayListOf()
                }
            }
            if (it.containsKey(ARG_ACTION_EDIT_INDEX)) {
                actionEditIndex = it.getInt(ARG_ACTION_EDIT_INDEX)
            }
            it.getString(ARG_ACTION_EDIT_OBJECT_JSON)?.let { json ->
                actionEditObject = try {
                    GsonTools.gson.fromJson(json, Action::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

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

        listener = (parentFragment as? AddActionBottomDialogListener) ?: (activity as? AddActionBottomDialogListener)


        fillSpinners(requireContext())
        binding.bsRuleActionAddActionButton.setOnClickListener(this)
        binding.bsRuleActionLabelCreateButton.setOnClickListener(this)
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
            } else if (p0.id == R.id.bs_rule_action_label_create_button) {
                val labelsAddBottomDialogFragment = AddLabelBottomDialogFragment.newInstance(null)
                labelsAddBottomDialogFragment.show(
                    childFragmentManager,
                    "labelsAddBottomDialogFragment"
                )
            }
        }
    }

    override fun onAddedLabelEntry(label: Labels) {
        recentlyAddedLabelName = label.name
        viewLifecycleOwner.lifecycleScope.launch {
            loadLabels(requireContext())
        }
    }

    private suspend fun loadLabels(context: Context) {
        when (val result = viewModel.getAllLabels()) {
            is NetworkResult.Success -> {
                val labels = result.data.data
                allLabels = labels
                populateLabelChips(context, labels)
            }
            is NetworkResult.Error -> {
                binding.bsRuleActionLabelError.visibility = View.VISIBLE
                binding.bsRuleActionLabelError.text = result.error
            }
        }
    }

    private fun populateLabelChips(context: Context, labels: List<Labels>) {
        binding.bsRuleActionLabelChipgroup.removeAllViewsInLayout()
        binding.bsRuleActionLabelChipgroup.requestLayout()
        binding.bsRuleActionLabelChipgroup.invalidate()

        val labelToSelect = recentlyAddedLabelName ?: selectedLabelName

        for (label in labels) {
            val chip = layoutInflater.inflate(R.layout.chip_view, binding.bsRuleActionLabelChipgroup, false) as Chip
            chip.text = label.name
            chip.tag = label.name
            chip.isCheckable = true
            chip.isChecked = labelToSelect.equals(label.name, ignoreCase = true)

            try {
                val colorInt = label.colour.toColorInt()
                val alphaColor = Color.argb(
                    (0.2 * 255).toInt(),
                    Color.red(colorInt),
                    Color.green(colorInt),
                    Color.blue(colorInt)
                )

                val isDarkMode =
                    (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                val hsv = FloatArray(3)
                Color.colorToHSV(colorInt, hsv)
                if (isDarkMode) {
                    hsv[2] = 1.0f
                    hsv[1] = max(0f, hsv[1] - 0.2f)
                } else {
                    hsv[2] = min(1f, hsv[2] * 0.7f)
                    hsv[1] = min(1f, hsv[1] * 1.2f)
                }
                val textColorInt = Color.HSVToColor(hsv)

                val defaultBgColor = chip.chipBackgroundColor?.defaultColor ?: Color.TRANSPARENT
                val defaultTextColor = chip.textColors?.defaultColor ?: Color.GRAY

                val states = arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                )

                val bgColors = intArrayOf(alphaColor, defaultBgColor)
                val textColors = intArrayOf(textColorInt, defaultTextColor)

                chip.chipBackgroundColor = ColorStateList(states, bgColors)
                chip.setTextColor(ColorStateList(states, textColors))
                chip.chipStrokeWidth = 0f
                chip.checkedIconTint = ColorStateList.valueOf(textColorInt)
                chip.isChipIconVisible = false
            } catch (e: Exception) {
                // Fallback
            }

            binding.bsRuleActionLabelChipgroup.addView(chip)
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
        val editObject = actionEditObject
        if (editObject != null) {
            val typeIndex = actionTypes.indexOf(editObject.type)
            if (typeIndex != -1) {
                binding.bsRuleActionTypeMact.setText(actionTypeNames[typeIndex], false)
            }
            binding.bsRuleActionValuesTiet.setText(editObject.value)


            // If type is banner location, set value for it
            if (editObject.type == "banner") {
                binding.bsRuleActionValuesSpinnerBannerLocationMact.setText(editObject.value, false)
            }

            if (editObject.type == "forwardTo") {
                viewLifecycleOwner.lifecycleScope.launch {
                    getAllRecipients(editObject.value)
                }
            } else {
                // If not forward_to, get recipients without selected
                viewLifecycleOwner.lifecycleScope.launch {
                    getAllRecipients(null)
                }
            }

            if (editObject.type == "addLabel" || editObject.type == "removeLabel") {
                selectedLabelName = editObject.value
                viewLifecycleOwner.lifecycleScope.launch {
                    loadLabels(context)
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
                binding.bsRuleActionLabelTil.visibility = View.GONE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.VISIBLE
                binding.bsRuleActionValuesTil.visibility = View.GONE
            }
            "forwardTo" -> {
                binding.bsRuleActionForwardToTil.visibility = View.VISIBLE
                binding.bsRuleActionLabelTil.visibility = View.GONE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.GONE
            }
            "block", "encryption", "blocklistSender", "blocklistDomain", "removeAttachments", "deactivateAlias", "deleteAlias" -> {
                binding.bsRuleActionForwardToTil.visibility = View.GONE
                binding.bsRuleActionLabelTil.visibility = View.GONE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.GONE
            }
            "addLabel", "removeLabel" -> {
                binding.bsRuleActionForwardToTil.visibility = View.GONE
                binding.bsRuleActionLabelTil.visibility = View.VISIBLE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.GONE
                if (allLabels == null) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        loadLabels(context)
                    }
                }
            }
            "setAliasDescription" -> {
                binding.bsRuleActionForwardToTil.visibility = View.GONE
                binding.bsRuleActionLabelTil.visibility = View.GONE
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.VISIBLE
                binding.bsRuleActionValuesTil.hint = context.resources.getString(R.string.enter_description)
            }
            else -> {
                binding.bsRuleActionForwardToTil.visibility = View.GONE
                binding.bsRuleActionLabelTil.visibility = View.GONE
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
        /*
        GET VALUES
         */

        when (val type = actionTypes[typeIndex]) {
            "banner" -> {
                val bannerLocation =
                    bannerLocations[bannerLocationNames.indexOf(binding.bsRuleActionValuesSpinnerBannerLocationMact.text.toString())]

                listener?.onAddedAction(actionEditIndex, type, bannerLocation)
            }

            "block", "encryption", "blocklistSender", "blocklistDomain", "removeAttachments", "deactivateAlias", "deleteAlias" -> {
                listener?.onAddedAction(actionEditIndex, type, true)
            }

            "forwardTo" -> {
                // Get selected chip
                val ids: List<Int> = binding.bsRuleActionForwardToChipgroup.checkedChipIds
                if (ids.isEmpty()) {
                    binding.bsRuleActionForwardToTil.error = context.resources.getString(R.string.select_a_recipient)
                } else {
                    binding.bsRuleActionForwardToTil.error = null
                    val chip: Chip = binding.bsRuleActionForwardToChipgroup.findViewById(ids[0])
                    val recipient = chip.tag.toString()
                    listener?.onAddedAction(actionEditIndex, type, recipient)
                }
            }

            "addLabel", "removeLabel" -> {
                val ids: List<Int> = binding.bsRuleActionLabelChipgroup.checkedChipIds
                if (ids.isEmpty()) {
                    binding.bsRuleActionLabelTil.error = context.resources.getString(R.string.select_a_label)
                } else {
                    binding.bsRuleActionLabelTil.error = null
                    val chip: Chip = binding.bsRuleActionLabelChipgroup.findViewById(ids[0])
                    val labelName = chip.text.toString()
                    listener?.onAddedAction(actionEditIndex, type, labelName)
                }
            }

            else -> {
                val value = binding.bsRuleActionValuesTiet.text.toString()
                listener?.onAddedAction(actionEditIndex, type, value)
            }
        }

    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddActionBottomDialogListener {
        fun onAddedAction(actionEditIndex: Int?, type: String, value: String)
        fun onAddedAction(actionEditIndex: Int?, type: String, value: Boolean)
    }

    companion object {
        private const val ARG_RECIPIENTS_JSON = "arg_recipients_json"
        private const val ARG_ACTION_EDIT_INDEX = "arg_action_edit_index"
        private const val ARG_ACTION_EDIT_OBJECT_JSON = "arg_action_edit_object_json"

        fun newInstance(recipients: ArrayList<Recipients>, actionEditIndex: Int?, actionEditObject: Action?): ActionBottomDialogFragment {
            return ActionBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_RECIPIENTS_JSON, GsonTools.gson.toJson(recipients))
                    actionEditIndex?.let { putInt(ARG_ACTION_EDIT_INDEX, it) }
                    actionEditObject?.let { putString(ARG_ACTION_EDIT_OBJECT_JSON, GsonTools.gson.toJson(it)) }
                }
            }
        }
    }
}
