package host.stjin.anonaddy.ui.rules

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    lateinit var networkHelper: NetworkHelper

    private var ruleId: String? = null
    private lateinit var rules: Rules

    private val conditionBottomDialogFragment: ConditionBottomDialogFragment =
        ConditionBottomDialogFragment.newInstance()

    private val actionBottomDialogFragment: ActionBottomDialogFragment =
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
            actions = listOf(
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
            conditions = listOf(
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


    private fun setPage() {
        val inflater = LayoutInflater.from(this)

        // Set name
        activity_rules_create_rule_name_tiet.setText(rules.name)

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

                activity_rules_create_LL_conditions.addView(inflatedLayout)
            }


            val inflatedLayout: View = inflater.inflate(R.layout.rules_view_condition_rule, activity_rules_create_LL_conditions as ViewGroup?, false)
            val title = inflatedLayout.findViewById<TextView>(R.id.rules_view_condition_rule_title)
            title.id = conditionNumber
            title.text = this.resources.getString(R.string.rule_if_, "`${condition.type}` ${condition.match}...")

            val subtitle = inflatedLayout.findViewById<TextView>(R.id.rules_view_condition_rule_subtitle)
            subtitle.id = conditionNumber

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

                activity_rules_create_LL_actions.addView(inflatedLayout)
            }


            val inflatedLayout: View = inflater.inflate(R.layout.rules_view_condition_rule, activity_rules_create_LL_actions as ViewGroup?, false)
            val title = inflatedLayout.findViewById<TextView>(R.id.rules_view_condition_rule_title)
            title.id = actionNumber
            title.text = this.resources.getString(R.string.rule_the_, "`${action.type}`")

            val subtitle = inflatedLayout.findViewById<TextView>(R.id.rules_view_condition_rule_subtitle)
            subtitle.id = actionNumber
            subtitle.text = action.value
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
                conditionBottomDialogFragment.show(
                    supportFragmentManager,
                    "conditionBottomDialogFragment"
                )
            }
        }

        activity_rules_create_add_action.setOnClickListener {
            if (!actionBottomDialogFragment.isAdded) {
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
    override fun onAddedCondition(type: String, match: String, values: List<String>) {
        //TODO check if this is a new or a edited condition
        rules.conditions
    }


    // Actions
    override fun onAddedAction(type: String, values: String) {
        //TODO check if this is a new or a edited condition

    }

    override fun onAddedAction(type: String, values: Boolean) {
        //TODO check if this is a new or a edited condition

    }
}