package host.stjin.anonaddy.ui.appsettings.features

import android.os.Bundle
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesWatchAliasBinding
import host.stjin.anonaddy.utils.InsetUtil
import host.stjin.anonaddy_shared.managers.SettingsManager


class AppSettingsFeaturesWatchAliasActivity : BaseActivity() {

    private lateinit var settingsManager: SettingsManager

    private lateinit var binding: ActivityAppSettingsFeaturesWatchAliasBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesWatchAliasBinding.inflate(layoutInflater)
        InsetUtil.applyBottomInset(binding.appsettingsFeaturesWatchAliasNSVLL)

        val view = binding.root
        setContentView(view)

        settingsManager = SettingsManager(false, this)
        setupToolbar(
            R.string.watch_alias,
            binding.appsettingsFeaturesWatchAliasNSV,
            binding.appsettingsFeaturesWatchAliasToolbar,
            R.drawable.ic_watch_alias
        )

        loadSettings()
        setOnClickListeners()
    }

    private fun loadSettings() {
        // Nothing to load
    }


    // If the user comes back from eg. settings re-check + enable biometricswitch
    override fun onResume() {
        super.onResume()
        loadSettings()
    }


    private fun setOnClickListeners() {
        // Nothing to click
    }


}