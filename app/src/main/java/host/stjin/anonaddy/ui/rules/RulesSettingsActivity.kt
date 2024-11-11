package host.stjin.anonaddy.ui.rules

import android.os.Bundle
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityRuleSettingsBinding


class RulesSettingsActivity : BaseActivity() {


    private lateinit var binding: ActivityRuleSettingsBinding
    private val rulesSettingsFragment = RulesSettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRuleSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.manage_rules,
            null,
            binding.activityRulesSettingsToolbar,
            R.drawable.ic_filter
        )

        setRefreshLayout()

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.activity_rules_settings_fcv, rulesSettingsFragment)
            .commit()


    }

    // This only applies to <sw600Dp devices
    private fun setRefreshLayout() {
        binding.activityRulesSettingsSwiperefresh.setOnRefreshListener {
            binding.activityRulesSettingsSwiperefresh.isRefreshing = true

            rulesSettingsFragment.getDataFromWeb(null) {
                binding.activityRulesSettingsSwiperefresh.isRefreshing = false
            }
        }
    }
}




