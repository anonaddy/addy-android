package host.stjin.anonaddy.ui.domains

import android.os.Bundle
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityDomainSettingsBinding

class DomainSettingsActivity : BaseActivity() {


    private lateinit var binding: ActivityDomainSettingsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDomainSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activityDomainSettingsFcv)
        )

        setupToolbar(
            R.string.manage_domains,
            null,
            binding.activityDomainSettingsToolbar,
            R.drawable.ic_world
        )


        supportFragmentManager
            .beginTransaction()
            .replace(R.id.activity_domain_settings_fcv, DomainSettingsFragment())
            .commit()


    }

}