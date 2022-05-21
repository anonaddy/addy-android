package host.stjin.anonaddy.ui.rules

import android.app.Dialog
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetRulesActionBinding
import host.stjin.anonaddy_shared.models.Action


class ActionBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {


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

        checkForArguments(requireContext())


        if (VERSION.SDK_INT >= VERSION_CODES.R) {
            setIMEAnimation(binding.bsRuleActionRoot)
        }

        return root
    }

    private fun checkForArguments(context: Context) {
        // Check if there arguments (to be filled from the Create Rule Activity)
        if ((arguments?.size() ?: 0) > 0) {
            arguments?.getInt(CreateRuleActivity.ARGUMENTS.ACTION_EDIT_INDEX.argument)?.let {
                actionEditIndex = it
            }
            arguments?.getSerializable(CreateRuleActivity.ARGUMENTS.ACTION_EDIT.argument)?.let {
                actionEditObject = it as? Action
            }


            val typeText =
                TYPES_NAME[TYPES.indexOf(actionEditObject?.type)]
            binding.bsRuleActionTypeMact.setText(typeText, false)
            binding.bsRuleActionValuesTiet.setText(actionEditObject?.value)



            checkIfTypeRequiresValueField(context)
            binding.bsRuleActionTypeTil.error = null
        }

    }

    /*
    Check if the type spinner matches any of the value-type type or spinner-type type
     */

    private fun spinnerChangeListener(context: Context) {
        binding.bsRuleActionTypeMact.setOnItemClickListener { _, _, _, _ ->
            checkIfTypeRequiresValueField(context)
            binding.bsRuleActionTypeTil.error = null
        }
    }


    private fun checkIfTypeRequiresValueField(context: Context) {
        // If the type is set to set banner location show the spinner and hide the value field
        when {
            binding.bsRuleActionTypeMact.text.toString() == context.resources.getString(R.string.set_the_banner_information_location_to) -> {
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.VISIBLE
                binding.bsRuleActionValuesTil.visibility = View.GONE
            }
            // If the type is set to block email hide both
            binding.bsRuleActionTypeMact.text.toString() == context.resources.getString(R.string.block_the_email) -> {
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.GONE
            }
            // If the type is set to turn off PGP hide both
            binding.bsRuleActionTypeMact.text.toString() == context.resources.getString(R.string.turn_PGP_encryption_off) -> {
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.GONE
            }
            else -> {
                binding.bsRuleActionValuesSpinnerBannerLocationTil.visibility = View.GONE
                binding.bsRuleActionValuesTil.visibility = View.VISIBLE
            }
        }
    }


    private var TYPES: List<String> = listOf()
    private var VALUE_BANNER_LOCATION: List<String> = listOf()
    private var VALUE_BANNER_LOCATION_NAME: List<String> = listOf()
    private var TYPES_NAME: List<String> = listOf()
    private fun fillSpinners(context: Context) {
        TYPES = this.resources.getStringArray(R.array.actions_type).toList()
        TYPES_NAME = this.resources.getStringArray(R.array.actions_type_name).toList()
        VALUE_BANNER_LOCATION = this.resources.getStringArray(R.array.actions_type_bannerlocation_options).toList()
        VALUE_BANNER_LOCATION_NAME = this.resources.getStringArray(R.array.actions_type_bannerlocation_options_name).toList()

        val domainAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            R.layout.dropdown_menu_popup_item,
            TYPES_NAME
        )
        binding.bsRuleActionTypeMact.setAdapter(domainAdapter)


        val formatAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            R.layout.dropdown_menu_popup_item,
            VALUE_BANNER_LOCATION_NAME
        )
        binding.bsRuleActionValuesSpinnerBannerLocationMact.setAdapter(formatAdapter)
    }

    companion object {
        fun newInstance(): ActionBottomDialogFragment {
            return ActionBottomDialogFragment()
        }
    }

    private fun addAction(context: Context) {

        if (!TYPES_NAME.contains(binding.bsRuleActionTypeMact.text.toString())) {
            binding.bsRuleActionTypeTil.error =
                context.resources.getString(R.string.not_a_valid_action_type)
            return
        }

        if (!VALUE_BANNER_LOCATION_NAME.contains(binding.bsRuleActionValuesSpinnerBannerLocationMact.text.toString())) {
            binding.bsRuleActionValuesSpinnerBannerLocationTil.error =
                context.resources.getString(R.string.not_a_valid_banner_location)
            return
        }


        // Set error to null if domain and alias is valid
        binding.bsRuleActionTypeTil.error = null
        binding.bsRuleActionValuesSpinnerBannerLocationTil.error = null

        binding.bsRuleActionAddActionButton.isEnabled = false

        val type =
            TYPES[TYPES_NAME.indexOf(binding.bsRuleActionTypeMact.text.toString())]

        /*
        GET VALUES
         */

        when {
            // If the type is set to set banner information location get the value from the spinner
            binding.bsRuleActionTypeMact.text.toString() == context.resources.getString(R.string.set_the_banner_information_location_to) -> {
                val bannerLocation =
                    VALUE_BANNER_LOCATION[VALUE_BANNER_LOCATION_NAME.indexOf(binding.bsRuleActionValuesSpinnerBannerLocationMact.text.toString())]

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