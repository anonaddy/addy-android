package host.stjin.anonaddy.ui.appsettings.features

import android.os.Bundle
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesWatchAliasBinding


class AppSettingsFeaturesWatchAliasActivity : BaseActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var encryptedSettingsManager: SettingsManager

    private lateinit var binding: ActivityAppSettingsFeaturesWatchAliasBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesWatchAliasBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.appsettingsFeaturesWatchAliasNSVLL)
        )

        settingsManager = SettingsManager(false, this)
        encryptedSettingsManager = SettingsManager(true, this)
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