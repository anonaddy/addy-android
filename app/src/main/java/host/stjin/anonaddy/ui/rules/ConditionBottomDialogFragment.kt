package host.stjin.anonaddy.ui.rules

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetRulesConditionBinding
import host.stjin.anonaddy_shared.models.Condition


class ConditionBottomDialogFragment(private val conditionEditIndex: Int?, private val conditionEditObject: Condition?) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddConditionBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddConditionBottomDialogListener {
        fun onAddedCondition(conditionEditIndex: Int?, type: String, match: String, values: List<String>)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetRulesConditionBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetRulesConditionBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = activity as AddConditionBottomDialogListener


        fillSpinners(requireContext())
        binding.bsRuleConditionAddConditionButton.setOnClickListener(this)

        updateUi()



        return root
    }

    private fun updateUi() {
        // Check if there arguments (to be filled from the Create Rule Activity)

        if (conditionEditObject != null) {
            val typeText =
                conditionTypeNames[conditionTypes.indexOf(conditionEditObject.type)]
            binding.bsRuleConditionTypeMact.setText(typeText, false)

            val matchText =
                matchOperatorNames[matchOperators.indexOf(conditionEditObject.match)]
            binding.bsRuleConditionMatchMact.setText(matchText, false)


            binding.bsRuleConditionValuesTiet.setText(conditionEditObject.values.joinToString())
        }

    }


    private var conditionTypes: List<String> = listOf()
    private var matchOperators: List<String> = listOf()
    private var matchOperatorNames: List<String> = listOf()
    private var conditionTypeNames: List<String> = listOf()
    private fun fillSpinners(context: Context) {
        conditionTypes = this.resources.getStringArray(R.array.conditions_type).toList()
        matchOperators = this.resources.getStringArray(R.array.conditions_match).toList()
        conditionTypeNames = this.resources.getStringArray(R.array.conditions_type_name).toList()
        matchOperatorNames = this.resources.getStringArray(R.array.conditions_match_name).toList()

        val domainAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            R.layout.dropdown_menu_popup_item,
            conditionTypeNames
        )
        binding.bsRuleConditionTypeMact.setAdapter(domainAdapter)


        val formatAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            R.layout.dropdown_menu_popup_item,
            matchOperatorNames
        )
        binding.bsRuleConditionMatchMact.setAdapter(formatAdapter)
    }

    companion object {
        fun newInstance(conditionEditIndex: Int?, conditionEditObject: Condition?): ConditionBottomDialogFragment {
            return ConditionBottomDialogFragment(conditionEditIndex, conditionEditObject)
        }
    }

    private fun addCondition() {
        val type =
            conditionTypes[conditionTypeNames.indexOf(binding.bsRuleConditionTypeMact.text.toString())]
        val match =
            matchOperators[matchOperatorNames.indexOf(binding.bsRuleConditionMatchMact.text.toString())]

        // Split the textfield to an array (using , as a delimiter)
        val values = binding.bsRuleConditionValuesTiet.text.toString().split(",")

        listener.onAddedCondition(conditionEditIndex, type, match, values)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_rule_condition_add_condition_button) {
                addCondition()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}