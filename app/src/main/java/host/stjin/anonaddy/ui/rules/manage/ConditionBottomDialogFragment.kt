package host.stjin.anonaddy.ui.rules.manage
import host.stjin.anonaddy_shared.utils.GsonTools

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetRulesConditionBinding
import host.stjin.anonaddy_shared.models.Condition


class ConditionBottomDialogFragment :
    BaseBottomSheetDialogFragment(), View.OnClickListener {
    private var conditionEditIndex: Int? = null
    private var conditionEditObject: Condition? = null

    private var listener: AddConditionBottomDialogListener? = null

    private var _binding: BottomsheetRulesConditionBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_CONDITION_EDIT_INDEX)) {
                conditionEditIndex = it.getInt(ARG_CONDITION_EDIT_INDEX)
            }
            it.getString(ARG_CONDITION_EDIT_OBJECT_JSON)?.let { json ->
                conditionEditObject = GsonTools.gson.fromJson(json, Condition::class.java)
            }
        }
    }

    private var conditionTypes: List<String> = listOf()
    private var conditionTypeNames: List<String> = listOf()

    private var stringMatchOperators: List<String> = listOf()
    private var stringMatchOperatorNames: List<String> = listOf()

    private var headerMatchOperators: List<String> = listOf()
    private var headerMatchOperatorNames: List<String> = listOf()

    private var numericMatchOperators: List<String> = listOf()
    private var numericMatchOperatorNames: List<String> = listOf()

    private enum class ConditionCategory {
        STRING,
        HEADER,
        NUMERIC,
        BOOLEAN
    }

    private fun getConditionCategory(type: String): ConditionCategory {
        return when (type) {
            "sender", "subject", "alias", "alias_description", "alias_label", "display_from" -> ConditionCategory.STRING
            "header" -> ConditionCategory.HEADER
            "email_size", "alias_emails_forwarded" -> ConditionCategory.NUMERIC
            "alias_created_by_catch_all", "alias_not_created_by_catch_all",
            "has_attachments", "has_no_attachments",
            "email_is_spam", "email_is_not_spam",
            "dmarc_failed", "dmarc_did_not_fail" -> ConditionCategory.BOOLEAN
            else -> ConditionCategory.STRING
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetRulesConditionBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = (parentFragment as? AddConditionBottomDialogListener) ?: (activity as? AddConditionBottomDialogListener)

        fillSpinners(requireContext())
        binding.bsRuleConditionAddConditionButton.setOnClickListener(this)

        binding.bsRuleConditionTypeMact.setOnItemClickListener { _, _, _, _ ->
            updateMatchAndValuesForSelectedType(requireContext())
        }

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
            if (p0.id == R.id.bs_rule_condition_add_condition_button) {
                addCondition(requireContext())
            }
        }
    }

    private fun updateUi(context: Context) {
        val editObj = conditionEditObject
        if (editObj != null) {
            val typeIndex = conditionTypes.indexOf(editObj.type)
            if (typeIndex != -1) {
                binding.bsRuleConditionTypeMact.setText(conditionTypeNames[typeIndex], false)
            }
            updateMatchAndValuesForSelectedType(context, editObj.match, editObj.values)
            // Show save instead of add when editing an object
            binding.bsRuleConditionAddConditionButton.setText(R.string.save)
        } else {
            if (conditionTypeNames.isNotEmpty()) {
                binding.bsRuleConditionTypeMact.setText(conditionTypeNames[0], false)
            }
            updateMatchAndValuesForSelectedType(context)
        }
    }

    private fun updateMatchAndValuesForSelectedType(
        context: Context,
        matchToSelect: String? = null,
        valuesToSet: List<String>? = null
    ) {
        binding.bsRuleConditionValuesTil.error = null

        val typeIndex = conditionTypeNames.indexOf(binding.bsRuleConditionTypeMact.text.toString())
        val type = if (typeIndex != -1) conditionTypes[typeIndex] else "sender"
        val category = getConditionCategory(type)

        when (category) {
            ConditionCategory.BOOLEAN -> {
                binding.bsRuleConditionMatchTil.visibility = View.GONE
                binding.bsRuleConditionValuesTil.visibility = View.GONE
            }

            ConditionCategory.HEADER -> {
                binding.bsRuleConditionMatchTil.visibility = View.VISIBLE
                binding.bsRuleConditionValuesTil.visibility = View.VISIBLE

                val formatAdapter = ArrayAdapter(
                    context,
                    R.layout.dropdown_menu_popup_item,
                    headerMatchOperatorNames
                )
                binding.bsRuleConditionMatchMact.setAdapter(formatAdapter)

                val selectedMatchIndex = if (matchToSelect != null) headerMatchOperators.indexOf(matchToSelect) else -1
                if (selectedMatchIndex != -1) {
                    binding.bsRuleConditionMatchMact.setText(headerMatchOperatorNames[selectedMatchIndex], false)
                } else {
                    binding.bsRuleConditionMatchMact.setText(headerMatchOperatorNames.firstOrNull() ?: "", false)
                }

                binding.bsRuleConditionValuesTil.hint = context.resources.getString(R.string.enter_header_name)
                binding.bsRuleConditionValuesTiet.inputType = InputType.TYPE_CLASS_TEXT
                binding.bsRuleConditionValuesTiet.minLines = 1
                binding.bsRuleConditionValuesTiet.maxLines = 1
                binding.bsRuleConditionValuesTiet.setLines(1)

                if (valuesToSet != null) {
                    binding.bsRuleConditionValuesTiet.setText(valuesToSet.joinToString(", "))
                }
            }

            ConditionCategory.NUMERIC -> {
                binding.bsRuleConditionMatchTil.visibility = View.VISIBLE
                binding.bsRuleConditionValuesTil.visibility = View.VISIBLE

                val formatAdapter = ArrayAdapter(
                    context,
                    R.layout.dropdown_menu_popup_item,
                    numericMatchOperatorNames
                )
                binding.bsRuleConditionMatchMact.setAdapter(formatAdapter)

                val selectedMatchIndex = if (matchToSelect != null) numericMatchOperators.indexOf(matchToSelect) else -1
                if (selectedMatchIndex != -1) {
                    binding.bsRuleConditionMatchMact.setText(numericMatchOperatorNames[selectedMatchIndex], false)
                } else {
                    binding.bsRuleConditionMatchMact.setText(numericMatchOperatorNames.firstOrNull() ?: "", false)
                }

                binding.bsRuleConditionValuesTil.hint = if (type == "email_size") {
                    context.resources.getString(R.string.enter_size_in_bytes)
                } else {
                    context.resources.getString(R.string.enter_number_of_emails)
                }
                binding.bsRuleConditionValuesTiet.inputType = InputType.TYPE_CLASS_NUMBER
                binding.bsRuleConditionValuesTiet.minLines = 1
                binding.bsRuleConditionValuesTiet.maxLines = 1
                binding.bsRuleConditionValuesTiet.setLines(1)

                if (valuesToSet != null) {
                    binding.bsRuleConditionValuesTiet.setText(valuesToSet.joinToString(", "))
                }
            }

            ConditionCategory.STRING -> {
                binding.bsRuleConditionMatchTil.visibility = View.VISIBLE
                binding.bsRuleConditionValuesTil.visibility = View.VISIBLE

                val formatAdapter = ArrayAdapter(
                    context,
                    R.layout.dropdown_menu_popup_item,
                    stringMatchOperatorNames
                )
                binding.bsRuleConditionMatchMact.setAdapter(formatAdapter)

                val selectedMatchIndex = if (matchToSelect != null) stringMatchOperators.indexOf(matchToSelect) else -1
                if (selectedMatchIndex != -1) {
                    binding.bsRuleConditionMatchMact.setText(stringMatchOperatorNames[selectedMatchIndex], false)
                } else {
                    binding.bsRuleConditionMatchMact.setText(stringMatchOperatorNames.firstOrNull() ?: "", false)
                }

                binding.bsRuleConditionValuesTil.hint = context.resources.getString(R.string.enter_values_comma_separated)
                binding.bsRuleConditionValuesTiet.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                binding.bsRuleConditionValuesTiet.minLines = 5
                binding.bsRuleConditionValuesTiet.maxLines = 5
                binding.bsRuleConditionValuesTiet.setLines(5)

                if (valuesToSet != null) {
                    binding.bsRuleConditionValuesTiet.setText(valuesToSet.joinToString(", "))
                }
            }
        }
    }

    private fun fillSpinners(context: Context) {
        conditionTypes = this.resources.getStringArray(R.array.conditions_type).toList()
        conditionTypeNames = this.resources.getStringArray(R.array.conditions_type_name).toList()

        stringMatchOperators = this.resources.getStringArray(R.array.conditions_match_string).toList()
        stringMatchOperatorNames = this.resources.getStringArray(R.array.conditions_match_string_name).toList()

        headerMatchOperators = this.resources.getStringArray(R.array.conditions_match_header).toList()
        headerMatchOperatorNames = this.resources.getStringArray(R.array.conditions_match_header_name).toList()

        numericMatchOperators = this.resources.getStringArray(R.array.conditions_match_numeric).toList()
        numericMatchOperatorNames = this.resources.getStringArray(R.array.conditions_match_numeric_name).toList()

        val domainAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            R.layout.dropdown_menu_popup_item,
            conditionTypeNames
        )
        binding.bsRuleConditionTypeMact.setAdapter(domainAdapter)
    }

    private fun addCondition(context: Context) {
        val typeIndex = conditionTypeNames.indexOf(binding.bsRuleConditionTypeMact.text.toString())
        if (typeIndex == -1) return
        val type = conditionTypes[typeIndex]
        val category = getConditionCategory(type)

        when (category) {
            ConditionCategory.BOOLEAN -> {
                listener?.onAddedCondition(conditionEditIndex, type, null, null)
            }

            ConditionCategory.HEADER -> {
                val matchIndex = headerMatchOperatorNames.indexOf(binding.bsRuleConditionMatchMact.text.toString())
                val match = if (matchIndex != -1) headerMatchOperators[matchIndex] else headerMatchOperators[0]
                val value = binding.bsRuleConditionValuesTiet.text.toString().trim()
                if (value.isEmpty()) {
                    binding.bsRuleConditionValuesTil.error = context.resources.getString(R.string.enter_value)
                    return
                }
                listener?.onAddedCondition(conditionEditIndex, type, match, listOf(value))
            }

            ConditionCategory.NUMERIC -> {
                val matchIndex = numericMatchOperatorNames.indexOf(binding.bsRuleConditionMatchMact.text.toString())
                val match = if (matchIndex != -1) numericMatchOperators[matchIndex] else numericMatchOperators[0]
                val value = binding.bsRuleConditionValuesTiet.text.toString().trim()
                if (value.isEmpty()) {
                    binding.bsRuleConditionValuesTil.error = context.resources.getString(R.string.enter_value)
                    return
                }
                listener?.onAddedCondition(conditionEditIndex, type, match, listOf(value))
            }

            ConditionCategory.STRING -> {
                val matchIndex = stringMatchOperatorNames.indexOf(binding.bsRuleConditionMatchMact.text.toString())
                val match = if (matchIndex != -1) stringMatchOperators[matchIndex] else stringMatchOperators[0]
                val rawText = binding.bsRuleConditionValuesTiet.text.toString()
                val values = rawText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (values.isEmpty()) {
                    binding.bsRuleConditionValuesTil.error = context.resources.getString(R.string.enter_value)
                    return
                }
                listener?.onAddedCondition(conditionEditIndex, type, match, values)
            }
        }
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface AddConditionBottomDialogListener {
        fun onAddedCondition(conditionEditIndex: Int?, type: String, match: String?, values: List<String>?)
    }

    companion object {
        private const val ARG_CONDITION_EDIT_INDEX = "arg_condition_edit_index"
        private const val ARG_CONDITION_EDIT_OBJECT_JSON = "arg_condition_edit_object_json"

        fun newInstance(conditionEditIndex: Int?, conditionEditObject: Condition?): ConditionBottomDialogFragment {
            return ConditionBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    conditionEditIndex?.let { putInt(ARG_CONDITION_EDIT_INDEX, it) }
                    conditionEditObject?.let { putString(ARG_CONDITION_EDIT_OBJECT_JSON, GsonTools.gson.toJson(it)) }
                }
            }
        }
    }
}
