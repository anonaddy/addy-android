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
import host.stjin.anonaddy.models.Action
import kotlinx.android.synthetic.main.bottomsheet_rules_action.view.*


class ActionBottomDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddActionBottomDialogListener
    private var actionEditIndex: Int? = null
    private var actionEditObject: Action? = null


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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // get the views and attach the listener
        val root = inflater.inflate(
            R.layout.bottomsheet_rules_action, container,
            false
        )
        listener = activity as AddActionBottomDialogListener


        fillSpinners(root, requireContext())
        root.bs_rule_action_add_action_button.setOnClickListener(this)
        spinnerChangeListener(root, requireContext())

        checkForArguments(root, requireContext())
        return root
    }

    private fun checkForArguments(root: View, context: Context) {
        // Check if there arguments (to be filled from the Create Rule Activity)
        if (arguments?.size() ?: 0 > 0) {
            arguments?.getInt(CreateRuleActivity.ARGUMENTS.ACTION_EDIT_INDEX.argument)?.let {
                actionEditIndex = it
            }
            arguments?.getSerializable(CreateRuleActivity.ARGUMENTS.ACTION_EDIT.argument)?.let {
                actionEditObject = it as? Action
            }


            val typeText =
                TYPES_NAME[TYPES.indexOf(actionEditObject?.type)]
            root.bs_rule_action_type_mact.setText(typeText, false)
            root.bs_rule_action_values_tiet.setText(actionEditObject?.value)



            checkIfTypeRequiresValueField(root, context)
            root.bs_rule_action_type_til.error = null
        }

    }

    /*
    Check if the type spinner matches any of the value-type type or spinner-type type
     */

    private fun spinnerChangeListener(root: View, context: Context) {
        root.bs_rule_action_type_mact.setOnItemClickListener { _, _, _, _ ->
            checkIfTypeRequiresValueField(root, context)
            root.bs_rule_action_type_til.error = null
        }
    }


    private fun checkIfTypeRequiresValueField(root: View, context: Context) {
        // If the type is set to set banner location show the spinner and hide the value field
        when {
            root.bs_rule_action_type_mact.text.toString() == context.resources.getString(R.string.set_the_banner_information_location_to) -> {
                root.bs_rule_action_values_spinner_banner_location_til.visibility = View.VISIBLE
                root.bs_rule_action_values_til.visibility = View.GONE
            }
            // If the type is set to block email hide both
            root.bs_rule_action_type_mact.text.toString() == context.resources.getString(R.string.block_the_email) -> {
                root.bs_rule_action_values_spinner_banner_location_til.visibility = View.GONE
                root.bs_rule_action_values_til.visibility = View.GONE
            }
            // If the type is set to turn off PGP hide both
            root.bs_rule_action_type_mact.text.toString() == context.resources.getString(R.string.turn_PGP_encryption_off) -> {
                root.bs_rule_action_values_spinner_banner_location_til.visibility = View.GONE
                root.bs_rule_action_values_til.visibility = View.GONE
            }
            else -> {
                root.bs_rule_action_values_spinner_banner_location_til.visibility = View.GONE
                root.bs_rule_action_values_til.visibility = View.VISIBLE
            }
        }
    }


    private var TYPES: List<String> = listOf()
    private var VALUE_BANNER_LOCATION: List<String> = listOf()
    private var VALUE_BANNER_LOCATION_NAME: List<String> = listOf()
    private var TYPES_NAME: List<String> = listOf()
    private fun fillSpinners(root: View, context: Context) {
        TYPES = this.resources.getStringArray(R.array.actions_type).toList()
        TYPES_NAME = this.resources.getStringArray(R.array.actions_type_name).toList()
        VALUE_BANNER_LOCATION = this.resources.getStringArray(R.array.actions_type_bannerlocation_options).toList()
        VALUE_BANNER_LOCATION_NAME = this.resources.getStringArray(R.array.actions_type_bannerlocation_options_name).toList()

        val domainAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            R.layout.dropdown_menu_popup_item,
            TYPES_NAME
        )
        root.bs_rule_action_type_mact.setAdapter(domainAdapter)


        val formatAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            R.layout.dropdown_menu_popup_item,
            VALUE_BANNER_LOCATION_NAME
        )
        root.bs_rule_action_values_spinner_banner_location_mact.setAdapter(formatAdapter)
    }

    companion object {
        fun newInstance(): ActionBottomDialogFragment {
            return ActionBottomDialogFragment()
        }
    }

    private fun addAction(root: View, context: Context) {

        if (!TYPES_NAME.contains(root.bs_rule_action_type_mact.text.toString())) {
            root.bs_rule_action_type_til.error =
                context.resources.getString(R.string.not_a_valid_action_type)
            return
        }

        if (!VALUE_BANNER_LOCATION_NAME.contains(root.bs_rule_action_values_spinner_banner_location_mact.text.toString())) {
            root.bs_rule_action_values_spinner_banner_location_til.error =
                context.resources.getString(R.string.not_a_valid_banner_location)
            return
        }


        // Set error to null if domain and alias is valid
        root.bs_rule_action_type_til.error = null
        root.bs_rule_action_values_spinner_banner_location_til.error = null

        root.bs_rule_action_add_action_button.isEnabled = false

        val type =
            TYPES[TYPES_NAME.indexOf(root.bs_rule_action_type_mact.text.toString())]

        /*
        GET VALUES
         */

        when {
            // If the type is set to set banner information location get the value from the spinner
            root.bs_rule_action_type_mact.text.toString() == context.resources.getString(R.string.set_the_banner_information_location_to) -> {
                val banner_location =
                    VALUE_BANNER_LOCATION[VALUE_BANNER_LOCATION_NAME.indexOf(root.bs_rule_action_values_spinner_banner_location_mact.text.toString())]

                listener.onAddedAction(actionEditIndex, type, banner_location)
            }

            // If the type is set to block email send a true
            root.bs_rule_action_type_mact.text.toString() == context.resources.getString(R.string.block_the_email) -> {
                listener.onAddedAction(actionEditIndex, type, true)
            }
            // If the type is set to turn off PGP send a true
            root.bs_rule_action_type_mact.text.toString() == context.resources.getString(R.string.turn_PGP_encryption_off) -> {
                listener.onAddedAction(actionEditIndex, type, true)
            }
            else -> {
                // Else just get the textfield value
                val value = root.bs_rule_action_values_tiet.text.toString()
                listener.onAddedAction(actionEditIndex, type, value)
            }
        }


    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_rule_action_add_action_button) {
                addAction(requireView(), requireContext())
            }
        }
    }
}