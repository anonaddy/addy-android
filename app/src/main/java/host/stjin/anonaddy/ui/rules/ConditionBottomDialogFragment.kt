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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import host.stjin.anonaddy.R
import kotlinx.android.synthetic.main.bottomsheet_rules_condition.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class ConditionBottomDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddConditionBottomDialogListener


    // 1. Defines the listener interface with a method passing back data result.
    interface AddConditionBottomDialogListener {
        fun onAddedCondition(type: String, match: String, values: List<String>)
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
            R.layout.bottomsheet_rules_condition, container,
            false
        )
        listener = activity as AddConditionBottomDialogListener


        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            fillSpinners(root, requireContext())
        }

        root.bs_rule_condition_add_condition_button.setOnClickListener(this)
        //spinnerChangeListener(root, requireContext())
        return root
    }


    private var TYPES: List<String> = listOf()
    private var MATCHES: List<String> = listOf()
    private var MATCHES_NAME: List<String> = listOf()
    private var TYPES_NAME: List<String> = listOf()
    private fun fillSpinners(root: View, context: Context) {
        TYPES = this.resources.getStringArray(R.array.conditions_type).toList()
        MATCHES = this.resources.getStringArray(R.array.conditions_match).toList()
        TYPES_NAME = this.resources.getStringArray(R.array.conditions_match_name).toList()
        MATCHES_NAME = this.resources.getStringArray(R.array.conditions_match_name).toList()

        val domainAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            R.layout.dropdown_menu_popup_item,
            TYPES_NAME
        )
        root.bs_rule_condition_type_mact.setAdapter(domainAdapter)


        val formatAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            R.layout.dropdown_menu_popup_item,
            MATCHES_NAME
        )
        root.bs_rule_condition_match_mact.setAdapter(formatAdapter)
    }

    companion object {
        fun newInstance(): ConditionBottomDialogFragment {
            return ConditionBottomDialogFragment()
        }
    }

    private fun addAlias(root: View, context: Context) {

        if (!TYPES_NAME.contains(root.bs_rule_condition_type_mact.text.toString())) {
            root.bs_rule_condition_type_til.error =
                context.resources.getString(R.string.not_a_valid_condition_type)
            return
        }

        if (!MATCHES_NAME.contains(root.bs_rule_condition_match_mact.text.toString())) {
            root.bs_rule_condition_match_til.error =
                context.resources.getString(R.string.not_a_valid_condition_match)
            return
        }

        // Set error to null if domain and alias is valid
        root.bs_rule_condition_type_til.error = null
        root.bs_rule_condition_match_til.error = null

        root.bs_rule_condition_add_condition_button.isEnabled = false

        val type =
            TYPES[TYPES_NAME.indexOf(root.bs_rule_condition_type_mact.text.toString())]
        val match =
            MATCHES[MATCHES_NAME.indexOf(root.bs_rule_condition_match_mact.text.toString())]

        // Split the textfield to an array (using , as a delimiter)
        val values = root.bs_rule_condition_values_tiet.text.toString().split(",")



        listener.onAddedCondition(type, match, values)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_addalias_alias_add_alias_button) {
                addAlias(requireView(), requireContext())
            }
        }
    }
}