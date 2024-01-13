package host.stjin.anonaddy.ui.rules

import android.os.Bundle
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityRuleSettingsBinding


class RulesSettingsActivity : BaseActivity() {


    private lateinit var binding: ActivityRuleSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRuleSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activityRulesSettingsFcv)
        )

        setupToolbar(
            R.string.manage_rules,
            null,
            binding.activityRulesSettingsToolbar,
            R.drawable.ic_filter
        )


        supportFragmentManager
            .beginTransaction()
            .replace(R.id.activity_rules_settings_fcv, RulesSettingsFragment())
            .commit()


    }
}




