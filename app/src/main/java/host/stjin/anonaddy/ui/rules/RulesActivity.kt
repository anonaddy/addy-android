package host.stjin.anonaddy.ui.rules

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityRuleSettingsBinding
import kotlinx.coroutines.launch

class RulesActivity : BaseActivity() {
    private lateinit var binding: ActivityRuleSettingsBinding

    private fun getFragment(): RulesFragment? {
        return supportFragmentManager.findFragmentById(R.id.activity_rules_settings_fcv) as? RulesFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRuleSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.rules,
            null,
            binding.activityRulesSettingsToolbar,
            R.drawable.ic_filter
        )

        setRefreshLayout()
        setPage()
    }

    // This only applies to <sw600Dp devices
    private fun setRefreshLayout() {
        binding.activityRulesSettingsSwiperefresh.setOnRefreshListener {
            lifecycleScope.launch {
                getFragment()?.onRefreshData()
                binding.activityRulesSettingsSwiperefresh.isRefreshing = false
            }
        }
    }

    private fun setPage() {
        requireAuthentication {
            if (supportFragmentManager.findFragmentById(R.id.activity_rules_settings_fcv) == null) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.activity_rules_settings_fcv, RulesFragment.newInstance())
                    .commit()
            }
        }
    }
}
