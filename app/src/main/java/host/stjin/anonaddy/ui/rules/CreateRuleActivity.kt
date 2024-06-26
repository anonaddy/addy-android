package host.stjin.anonaddy.ui.rules

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityRulesCreateBinding
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.Action
import host.stjin.anonaddy_shared.models.Condition
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.models.Rules
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


class CreateRuleActivity : BaseActivity(), ConditionBottomDialogFragment.AddConditionBottomDialogListener,
    ActionBottomDialogFragment.AddActionBottomDialogListener {

    enum class ARGUMENTS(val argument: String) {
        ACTION_EDIT_INDEX("action_edit_index"),
        ACTION_EDIT("action_edit"),
        CONDITION_EDIT_INDEX("condition_edit_index"),
        CONDITION_EDIT("condition_edit")
    }

    lateinit var networkHelper: NetworkHelper
    private var shouldRefreshOnFinish = false

    private var ruleId: String? = null
    private lateinit var rules: Rules
    private lateinit var recipients: ArrayList<Recipients>

    private var conditionBottomDialogFragment: ConditionBottomDialogFragment =
        ConditionBottomDialogFragment.newInstance(null, null)

    private var actionBottomDialogFragment: ActionBottomDialogFragment =
        ActionBottomDialogFragment.newInstance(arrayListOf(), null, null)

    private lateinit var binding: ActivityRulesCreateBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRulesCreateBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activityRulesCreateLL1)
        )

        setupToolbar(
            R.string.creating_a_rule,
            binding.activityRulesCreateRLNSV,
            binding.activityRulesToolbar
        )


        networkHelper = NetworkHelper(this)


        // Check if there is an instance to restore (in case of rotations or folding)
        val json = savedInstanceState?.getString("rules")
        val recipientsJson = savedInstanceState?.getString("recipients")
        if (json?.isNotEmpty() == true && recipientsJson?.isNotEmpty() == true) {
            val gson = Gson()
            rules = gson.fromJson(json, Rules::class.java)

            val recipientsType = object : TypeToken<ArrayList<Recipients>>() {}.type
            val recipientsList = gson.fromJson<ArrayList<Recipients>>(recipientsJson, recipientsType)
            recipients = recipientsList


            this.ruleId = savedInstanceState.getString("rule_id")
            setPage()
        } else {
            val b = intent.extras
            val gson = Gson()


            // Get recipients from the parent activity (this can be null in case of a searchActivity link)
            // In the case of nullOrEmpty, get the recipients first, then load the Rule
            val recipientsStringFromBundle = b?.getString("recipients")
            if (recipientsStringFromBundle.isNullOrEmpty()) {
                lifecycleScope.launch {
                    getAllRecipients(b)
                }
            } else {
                val recipientsType = object : TypeToken<ArrayList<Recipients>>() {}.type
                val recipientsList = gson.fromJson<ArrayList<Recipients>>(recipientsStringFromBundle, recipientsType)
                recipients = recipientsList
                loadRule(b)
            }

        }


    }

    private fun loadRule(b: Bundle?){
        val ruleId = b?.getString("rule_id")
        if (ruleId == null) {
            // No ruleID, generate an empty rule
            generateEmptyRule()
            setPage()
        } else {
            // ruleID, generate the rule
            this.ruleId = ruleId
            getRule()
        }
    }

    private suspend fun getAllRecipients(b: Bundle?) {
        val networkHelper = NetworkHelper(this)

        networkHelper.getRecipients({ result, error ->
            if (result != null) {
                lifecycleScope.launch {
                    recipients = result
                    loadRule(b)
                }
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_obtaining_recipients) + "\n" + error,
                    binding.activityRulesCreateCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()

                // Show error animations
                binding.activityRulesCreateLL1.visibility = View.GONE
                binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
            }
        }, false)
    }

    override fun finish() {
        val resultIntent = Intent()
        resultIntent.putExtra("shouldRefresh", shouldRefreshOnFinish)
        setResult(RESULT_OK, resultIntent)
        super.finish()
    }

    private fun generateEmptyRule() {
        val rule = Rules(
            actions = arrayListOf(
                Action(
                    type = "subject",
                    value = "SPAM"
                ),
                Action(
                    type = "block",
                    value = "true"
                )
            ),
            active = true,
            conditions = arrayListOf(
                Condition(
                    match = "is exactly",
                    type = "sender",
                    values = listOf(
                        "will@addy.io",
                        "no-reply@addy.io"
                    )
                ),
                Condition(
                    match = "contains",
                    type = "subject",
                    values = listOf(
                        "newsletter",
                        "subscription"
                    )
                )
            ),
            created_at = "",
            id = "",
            name = "First Rule",
            operator = "AND",
            order = 0,
            updated_at = "",
            user_id = "",
            forwards = true,
            replies = true,
            sends = true
        )
        rules = rule
    }

    private fun getRule() {
        // Get the rule
        lifecycleScope.launch {
            ruleId?.let { getRuleInfo(it) }
        }
    }



    private suspend fun getRuleInfo(id: String) {
        networkHelper.getSpecificRule({ list, error ->
            if (list != null) {
                /**
                 *  CREATE RULE
                 */
                rules = list
                setPage()
            } else {

                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_obtaining_rule) + "\n" + error,
                    binding.activityRulesCreateCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()

                // Show error animations
                binding.activityRulesCreateLL1.visibility = View.GONE
                binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
            }
        }, id)
    }


    @SuppressLint("CutPasteId")
    private fun setPage() {
        val inflater = LayoutInflater.from(this)

        // First remove all the views from the condition and action layouts
        binding.activityRulesCreateLLConditions.removeAllViews()
        binding.activityRulesCreateLLActions.removeAllViews()


        // Set name
        binding.activityRulesCreateRuleNameTiet.setText(rules.name)

        // Set apply rules
        binding.activityRulesCreateRuleRunChipForwards.isChecked = rules.forwards
        binding.activityRulesCreateRuleRunChipSends.isChecked = rules.sends
        binding.activityRulesCreateRuleRunChipReplies.isChecked = rules.replies

        binding.activityRulesCreateRuleRunChipForwards.setOnCheckedChangeListener { _, isChecked ->
            rules.forwards = isChecked
        }
        binding.activityRulesCreateRuleRunChipSends.setOnCheckedChangeListener { _, isChecked ->
            rules.sends = isChecked
        }
        binding.activityRulesCreateRuleRunChipReplies.setOnCheckedChangeListener { _, isChecked ->
            rules.replies = isChecked
        }


        /**
         * AND/OR
         */

        if (rules.operator == "AND") {
            binding.rulesViewAndOrANDButton.isChecked = true
        } else {
            binding.rulesViewAndOrORButton.isChecked = true
        }


        /**
         * CONDITIONS
         */

        var firstCondition = true
        // For every condition, add a condition view and append an id to all subviews
        for ((conditionNumber, condition) in rules.conditions.withIndex()) {

            // If this is NOT the first condition, add a AND/OR before the condition
            if (firstCondition) {
                firstCondition = false
            } else {
                val inflatedLayout: View = inflater.inflate(R.layout.rules_action, binding.activityRulesCreateLLConditions as ViewGroup?, false)
                binding.activityRulesCreateLLConditions.addView(inflatedLayout)
            }

            /**
             * CONDITION
             */

            val inflatedLayout: View =
                inflater.inflate(R.layout.rules_view_condition_action, binding.activityRulesCreateLLConditions as ViewGroup?, false)
            val title = inflatedLayout.findViewById<TextView>(R.id.rules_view_condition_action_title)
            val deleteCondition = inflatedLayout.findViewById<MaterialButton>(R.id.rules_view_condition_action_close)
            val cardView = inflatedLayout.findViewById<CardView>(R.id.rules_view_condition_CV)


            val typeText =
                this.resources.getStringArray(R.array.conditions_type_name)[this.resources.getStringArray(R.array.conditions_type)
                    .indexOf(condition.type)]

            val matchText =
                this.resources.getStringArray(R.array.conditions_match_name)[this.resources.getStringArray(R.array.conditions_match)
                    .indexOf(condition.match)]

            title.text = this.resources.getString(R.string.rule_if_, "`${typeText}` ${matchText}...")

            val subtitle = inflatedLayout.findViewById<TextView>(R.id.rules_view_condition_action_subtitle)
            // Loop through all the values
            var values = ""
            var firstValue = true
            for (value in condition.values) {
                if (firstValue) {
                    values += value
                    firstValue = false
                } else {
                    values += ", $value"
                }
            }
            subtitle.text = values


            deleteCondition.setOnClickListener {
                rules.conditions.removeAt(conditionNumber)
                setPage()
            }

            cardView.setOnClickListener {
                if (!conditionBottomDialogFragment.isAdded) {
                    // Reset the variable to remove the arguments that could be sent with the previous edit button
                    conditionBottomDialogFragment = ConditionBottomDialogFragment.newInstance(conditionNumber, rules.conditions[conditionNumber])
                    conditionBottomDialogFragment.show(
                        supportFragmentManager,
                        "conditionBottomDialogFragment"
                    )
                }
            }

            binding.activityRulesCreateLLConditions.addView(inflatedLayout)
        }

        val inflatedAddConditionLayout: View =
            inflater.inflate(R.layout.rules_view_condition_action_add, binding.activityRulesCreateLLConditions as ViewGroup?, false)
        inflatedAddConditionLayout.findViewById<MaterialButton>(R.id.rules_view_condition_action_add).setOnClickListener {
            if (!conditionBottomDialogFragment.isAdded) {
                // Remove the arguments that could be sent with the edit button
                conditionBottomDialogFragment = ConditionBottomDialogFragment.newInstance(null, null)
                conditionBottomDialogFragment.show(
                    supportFragmentManager,
                    "conditionBottomDialogFragment"
                )
            }
        }
        binding.activityRulesCreateLLConditions.addView(inflatedAddConditionLayout)

        /**
         * ACTIONS
         */

        var firstActions = true
        // For every condition, add a condition view and append an id to all subviews
        for ((actionNumber, action) in rules.actions.withIndex()) {

            // If this is NOT the first condition, add a AND/OR before the condition
            if (firstActions) {
                firstActions = false
            } else {
                val inflatedLayout: View = inflater.inflate(R.layout.rules_action, binding.activityRulesCreateLLActions as ViewGroup?, false)
                binding.activityRulesCreateLLActions.addView(inflatedLayout)
            }


            val inflatedLayout: View =
                inflater.inflate(R.layout.rules_view_condition_action, binding.activityRulesCreateLLActions as ViewGroup?, false)
            val title = inflatedLayout.findViewById<TextView>(R.id.rules_view_condition_action_title)
            val deleteAction = inflatedLayout.findViewById<MaterialButton>(R.id.rules_view_condition_action_close)
            val cardView = inflatedLayout.findViewById<MaterialCardView>(R.id.rules_view_condition_CV)


            val typeText =
                this.resources.getStringArray(R.array.actions_type_name)[this.resources.getStringArray(R.array.actions_type).indexOf(action.type)]
            title.text = this.resources.getString(R.string.rule_then_, "`${typeText}`")

            val subtitle = inflatedLayout.findViewById<TextView>(R.id.rules_view_condition_action_subtitle)



            // If forward_to type resolve the recipient
            if (action.type == "forwardTo"){
                val recipient = recipients.first { it.id == action.value }
                subtitle.text = recipient.email
            } else {
                subtitle.text = action.value
            }



            deleteAction.setOnClickListener {
                rules.actions.removeAt(actionNumber)
                setPage()
            }

            cardView.setOnClickListener {
                if (!actionBottomDialogFragment.isAdded) {
                    // Reset the variable to remove the arguments that could be sent with the previous edit button
                    actionBottomDialogFragment = ActionBottomDialogFragment.newInstance(recipients, actionNumber, rules.actions[actionNumber])
                    actionBottomDialogFragment.show(
                        supportFragmentManager,
                        "actionBottomDialogFragment"
                    )
                }
            }


            binding.activityRulesCreateLLActions.addView(inflatedLayout)
        }

        val inflatedAddActionLayout: View =
            inflater.inflate(R.layout.rules_view_condition_action_add, binding.activityRulesCreateLLConditions as ViewGroup?, false)
        inflatedAddActionLayout.findViewById<MaterialButton>(R.id.rules_view_condition_action_add).setOnClickListener {
            if (!actionBottomDialogFragment.isAdded) {
                // Reset the variable to remove the arguments that could be sent with the edit button
                actionBottomDialogFragment = ActionBottomDialogFragment.newInstance(recipients, null, null)
                actionBottomDialogFragment.show(
                    supportFragmentManager,
                    "actionBottomDialogFragment"
                )
            }
        }
        binding.activityRulesCreateLLActions.addView(inflatedAddActionLayout)


        binding.animationFragment.stopAnimation()
        binding.activityRulesCreateRLNSV.animate().alpha(1.0f)
        setOnClickListeners()
        setOnChangeListeners()
    }

    private fun setOnChangeListeners() {
        binding.activityRulesCreateRuleNameTiet.addTextChangedListener {
            rules.name = binding.activityRulesCreateRuleNameTiet.text.toString()
        }
    }

    private fun setOnClickListeners() {
        toolbarSetAction(binding.activityRulesToolbar, R.drawable.ic_check) {
            // Update title
            binding.activityRulesToolbar.customToolbarOneHandedActionProgressbar.visibility = View.VISIBLE

            if (ruleId != null) {
                // Update the rule
                lifecycleScope.launch {
                    networkHelper.updateRule({ result ->
                        when (result) {
                            "200" -> {
                                shouldRefreshOnFinish = true
                                finish()
                            }
                            else -> {
                                binding.activityRulesToolbar.customToolbarOneHandedActionProgressbar.visibility = View.INVISIBLE
                                SnackbarHelper.createSnackbar(
                                    this@CreateRuleActivity,
                                    resources.getString(R.string.error_creating_rule) + "\n" + result,
                                    binding.activityRulesCreateCL,
                                    LoggingHelper.LOGFILES.DEFAULT
                                ).show()
                            }
                        }
                    }, ruleId!!, rules)
                }
            } else {
                // Post the rule
                lifecycleScope.launch {
                    networkHelper.createRule({ rule, error ->
                        if (rule != null) {
                            shouldRefreshOnFinish = true
                            finish()
                        } else {
                            binding.activityRulesToolbar.customToolbarOneHandedActionProgressbar.visibility = View.INVISIBLE
                            SnackbarHelper.createSnackbar(
                                this@CreateRuleActivity,
                                resources.getString(R.string.error_creating_rule) + "\n" + error,
                                binding.activityRulesCreateCL,
                                LoggingHelper.LOGFILES.DEFAULT
                            ).show()
                        }
                    }, rules)
                }
            }
        }


        binding.rulesViewAndOrANDButton.setOnClickListener {
            rules.operator = "AND"
        }
        binding.rulesViewAndOrORButton.setOnClickListener {
            rules.operator = "OR"
        }
    }


    // Condition
    override fun onAddedCondition(conditionEditIndex: Int?, type: String, match: String, values: List<String>) {
        conditionBottomDialogFragment.dismissAllowingStateLoss()

        val condition = Condition(
            type = type,
            match = match,
            values = values
        )

        // Edit index is not empty, thus are editing a condition, replace the condition at index
        if (conditionEditIndex != null) {
            rules.conditions[conditionEditIndex] = condition
        } else {
            rules.conditions.add(condition)
        }

        setPage()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val gson = Gson()
        val json = gson.toJson(rules)
        val recipientsJson = gson.toJson(recipients)
        outState.putSerializable("rules", json)
        outState.putString("rule_id", this.ruleId)
        outState.putString("recipients", recipientsJson)
    }

    // Actions
    override fun onAddedAction(actionEditIndex: Int?, type: String, value: String) {
        actionBottomDialogFragment.dismissAllowingStateLoss()
        val action = Action(
            type = type,
            value = value
        )
        // Edit index is not empty, thus are editing an action, replace the action at index
        if (actionEditIndex != null) {
            rules.actions[actionEditIndex] = action
        } else {
            rules.actions.add(action)
        }

        setPage()
    }

    override fun onAddedAction(actionEditIndex: Int?, type: String, value: Boolean) {
        actionBottomDialogFragment.dismissAllowingStateLoss()
        val action = Action(
            type = type,
            value = value.toString()
        )
        // Edit index is not empty, thus are editing an action, replace the action at index
        if (actionEditIndex != null) {
            rules.actions[actionEditIndex] = action
        } else {
            rules.actions.add(action)
        }

        setPage()
    }
}