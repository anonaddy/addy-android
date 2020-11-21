package host.stjin.anonaddy.ui.rules

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.models.Action
import host.stjin.anonaddy.models.Condition
import host.stjin.anonaddy.models.Rules
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import kotlinx.android.synthetic.main.activity_rules_create.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class CreateRuleActivity : BaseActivity(), ConditionBottomDialogFragment.AddConditionBottomDialogListener,
    ActionBottomDialogFragment.AddActionBottomDialogListener {

    enum class ARGUMENTS(val argument: String) {
        ACTION_EDIT_INDEX("action_edit_index"),
        ACTION_EDIT("action_edit"),
        CONDITION_EDIT_INDEX("condition_edit_index"),
        CONDITION_EDIT("condition_edit"),
    }

    lateinit var networkHelper: NetworkHelper

    private var ruleId: String? = null
    private lateinit var rules: Rules

    private var conditionBottomDialogFragment: ConditionBottomDialogFragment =
        ConditionBottomDialogFragment.newInstance()

    private var actionBottomDialogFragment: ActionBottomDialogFragment =
        ActionBottomDialogFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rules_create)
        setupToolbar(activity_rules_create_toolbar)


        networkHelper = NetworkHelper(this)


        val b = intent.extras
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
                    listOf(
                        "will@anonaddy.com",
                        "no-reply@anonaddy.com"
                    )
                ),
                Condition(
                    match = "contains",
                    type = "subject",
                    listOf(
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
            user_id = ""
        )
        rules = rule
    }

    private fun getRule() {
        activity_rules_create_RL_lottieview.visibility = View.GONE

        // Get the rule
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            ruleId?.let { getRuleInfo(it) }
        }
    }

    private suspend fun getRuleInfo(id: String) {
        networkHelper.getSpecificRule({ list ->
            if (list != null) {
                /**
                 *  CREATE RULE
                 */
                rules = list
                setPage()
            } else {
                activity_rules_create_RL_progressbar.visibility = View.GONE
                activity_rules_create_LL1.visibility = View.GONE

                // Show no internet animations
                activity_rules_create_RL_lottieview.visibility = View.VISIBLE
            }
        }, id)
    }


    @SuppressLint("CutPasteId")
    private fun setPage() {
        val inflater = LayoutInflater.from(this)

        // First remove all the views from the condition and action layouts
        activity_rules_create_LL_conditions.removeAllViews()
        activity_rules_create_LL_actions.removeAllViews()


        // Set name
        activity_rules_create_rule_name_tiet.setText(rules.name)

        /**
         * CONDITIONS
         */

        var firstCondition = true
        // For every condition, add a condition view and append an id to all subviews
        for ((conditionNumber, condition) in rules.conditions.withIndex()) {

            /**
             * AND/OR
             */
            // If this is NOT the first condition, add a AND/OR before the condition
            if (firstCondition) {
                firstCondition = false
            } else {
                val inflatedLayout: View = inflater.inflate(R.layout.rules_view_and_or, activity_rules_create_LL_conditions as ViewGroup?, false)
                //val materialButtonToggleGroup = inflatedLayout.findViewById<MaterialButtonToggleGroup>(R.id.rules_view_and_or_AND_mbtg)
                val andButton = inflatedLayout.findViewById<MaterialButton>(R.id.rules_view_and_or_AND_button)
                val orButton = inflatedLayout.findViewById<MaterialButton>(R.id.rules_view_and_or_OR_button)

                if (rules.operator == "AND") {
                    andButton.isChecked = true
                    orButton.isChecked = false
                } else {
                    andButton.isChecked = false
                    orButton.isChecked = true
                }


                andButton.setOnClickListener {
                    rules.operator = "AND"
                    setPage()
                }
                orButton.setOnClickListener {
                    rules.operator = "OR"
                    setPage()
                }

                activity_rules_create_LL_conditions.addView(inflatedLayout)
            }

            /**
             * CONDITION
             */

            val inflatedLayout: View =
                inflater.inflate(R.layout.rules_view_condition_action, activity_rules_create_LL_conditions as ViewGroup?, false)
            val title = inflatedLayout.findViewById<TextView>(R.id.rules_view_condition_action_title)
            val deleteCondition = inflatedLayout.findViewById<ImageView>(R.id.rules_view_condition_action_close)
            val editCondition = inflatedLayout.findViewById<ImageView>(R.id.rules_view_condition_action_edit)


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

            editCondition.setOnClickListener {
                if (!conditionBottomDialogFragment.isAdded) {
                    // Reset the variable to remove the arguments that could be sent with the previous edit button
                    conditionBottomDialogFragment = ConditionBottomDialogFragment.newInstance()
                    conditionBottomDialogFragment.arguments = Bundle().apply {
                        putSerializable(ARGUMENTS.CONDITION_EDIT.argument, rules.conditions[conditionNumber])
                        putInt(ARGUMENTS.CONDITION_EDIT_INDEX.argument, conditionNumber)
                    }
                    conditionBottomDialogFragment.show(
                        supportFragmentManager,
                        "conditionBottomDialogFragment"
                    )
                }
            }

            activity_rules_create_LL_conditions.addView(inflatedLayout)
        }

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
                val inflatedLayout: View = inflater.inflate(R.layout.rules_view_and_or, activity_rules_create_LL_actions as ViewGroup?, false)
                //val materialButtonToggleGroup = inflatedLayout.findViewById<MaterialButtonToggleGroup>(R.id.rules_view_and_or_AND_mbtg)
                val andButton = inflatedLayout.findViewById<MaterialButton>(R.id.rules_view_and_or_AND_button)
                val orButton = inflatedLayout.findViewById<MaterialButton>(R.id.rules_view_and_or_OR_button)
                orButton.visibility = View.GONE
                // Actions are al-ways AND
                andButton.isChecked = true
                orButton.isChecked = false

                andButton.setOnClickListener {
                    rules.operator = "AND"
                    setPage()
                }
                orButton.setOnClickListener {
                    rules.operator = "OR"
                    setPage()
                }

                activity_rules_create_LL_actions.addView(inflatedLayout)
            }


            val inflatedLayout: View = inflater.inflate(R.layout.rules_view_condition_action, activity_rules_create_LL_actions as ViewGroup?, false)
            val title = inflatedLayout.findViewById<TextView>(R.id.rules_view_condition_action_title)
            val deleteAction = inflatedLayout.findViewById<ImageView>(R.id.rules_view_condition_action_close)
            val editAction = inflatedLayout.findViewById<ImageView>(R.id.rules_view_condition_action_edit)


            val typeText =
                this.resources.getStringArray(R.array.actions_type_name)[this.resources.getStringArray(R.array.actions_type).indexOf(action.type)]
            title.text = this.resources.getString(R.string.rule_then_, "`${typeText}`")

            val subtitle = inflatedLayout.findViewById<TextView>(R.id.rules_view_condition_action_subtitle)
            subtitle.text = action.value


            deleteAction.setOnClickListener {
                rules.actions.removeAt(actionNumber)
                setPage()
            }

            editAction.setOnClickListener {
                if (!actionBottomDialogFragment.isAdded) {
                    // Reset the variable to remove the arguments that could be sent with the previous edit button
                    actionBottomDialogFragment = ActionBottomDialogFragment.newInstance()
                    actionBottomDialogFragment.arguments = Bundle().apply {
                        putSerializable(ARGUMENTS.ACTION_EDIT.argument, rules.actions[actionNumber])
                        putInt(ARGUMENTS.ACTION_EDIT_INDEX.argument, actionNumber)
                    }
                    actionBottomDialogFragment.show(
                        supportFragmentManager,
                        "actionBottomDialogFragment"
                    )
                }
            }


            activity_rules_create_LL_actions.addView(inflatedLayout)
        }


        activity_rules_create_RL_progressbar.visibility = View.GONE
        activity_rules_create_LL1.visibility = View.VISIBLE

        setOnClickListeners()
        setOnChangeListeners()
    }

    private fun setOnChangeListeners() {
        activity_rules_create_rule_name_tiet.addTextChangedListener {
            rules.name = activity_rules_create_rule_name_tiet.text.toString()
        }
    }

    private fun setOnClickListeners() {
        activity_rules_create_check.setOnClickListener {
            // Update title
            activity_rules_create_progressbar.visibility = View.VISIBLE

            if (ruleId != null) {
                // Update the rule
                GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                    networkHelper.updateRule(ruleId!!, rules) { result ->
                        when (result) {
                            "200" -> {
                                finish()
                            }
                            else -> {
                                activity_rules_create_progressbar.visibility = View.INVISIBLE

                                val snackbar =
                                    Snackbar.make(
                                        activity_rules_create_LL, resources.getString(R.string.error_creating_rule) + "\n" + result,
                                        Snackbar.LENGTH_SHORT
                                    )

                                if (SettingsManager(false, this@CreateRuleActivity).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
                                    snackbar.setAction(R.string.logs) {
                                        val intent = Intent(this@CreateRuleActivity, LogViewerActivity::class.java)
                                        startActivity(intent)
                                    }
                                }
                                snackbar.show()
                            }
                        }
                    }
                }
            } else {
                // Post the rule
                GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                    networkHelper.createRule(rules) { result ->
                        when (result) {
                            "201" -> {
                                finish()
                            }
                            else -> {
                                activity_rules_create_progressbar.visibility = View.INVISIBLE

                                val snackbar =
                                    Snackbar.make(
                                        activity_rules_create_LL, resources.getString(R.string.error_creating_rule) + "\n" + result,
                                        Snackbar.LENGTH_SHORT
                                    )

                                if (SettingsManager(false, this@CreateRuleActivity).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
                                    snackbar.setAction(R.string.logs) {
                                        val intent = Intent(this@CreateRuleActivity, LogViewerActivity::class.java)
                                        startActivity(intent)
                                    }
                                }
                                snackbar.show()
                            }
                        }
                    }
                }
            }
        }

        activity_rules_create_add_condition.setOnClickListener {
            if (!conditionBottomDialogFragment.isAdded) {
                // Remove the arguments that could be sent with the edit button
                conditionBottomDialogFragment = ConditionBottomDialogFragment.newInstance()
                conditionBottomDialogFragment.show(
                    supportFragmentManager,
                    "conditionBottomDialogFragment"
                )
            }
        }

        activity_rules_create_add_action.setOnClickListener {
            if (!actionBottomDialogFragment.isAdded) {
                // Reset the variable to remove the arguments that could be sent with the edit button
                actionBottomDialogFragment = ActionBottomDialogFragment.newInstance()
                actionBottomDialogFragment.show(
                    supportFragmentManager,
                    "actionBottomDialogFragment"
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        supportFinishAfterTransition()
    }

    // Condition
    override fun onAddedCondition(conditionEditIndex: Int?, type: String, match: String, values: List<String>) {
        conditionBottomDialogFragment.dismiss()

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


    // Actions
    override fun onAddedAction(actionEditIndex: Int?, type: String, value: String) {
        actionBottomDialogFragment.dismiss()
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
        actionBottomDialogFragment.dismiss()
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