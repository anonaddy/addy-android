package host.stjin.anonaddy.ui.appsettings.features

import android.os.Bundle
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesWebintentResolutionBinding
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy.utils.WebIntentHelper
import host.stjin.anonaddy_shared.managers.SettingsManager


class AppSettingsFeaturesWebIntentResolutionActivity : BaseActivity() {
    private lateinit var settingsManager: SettingsManager

    private var forceSwitch = false

    private lateinit var binding: ActivityAppSettingsFeaturesWebintentResolutionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesWebintentResolutionBinding.inflate(layoutInflater)
        InsetUtils.applyBottomInset(binding.appsettingsFeaturesWebintentResolutionNSVLL)

        val view = binding.root
        setContentView(view)

        settingsManager = ServiceLocator.settingsManager
        setupToolbar(
            R.string.integration_webintent_resolution,
            binding.appsettingsFeaturesWebintentResolutionNSV,
            binding.appsettingsFeaturesWebintentResolutionToolbar,
            R.drawable.ic_external_link
        )

        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
    }

    // If the user comes back from eg. settings re-check + enable biometricswitch
    override fun onResume() {
        super.onResume()
        loadSettings()
    }

    private fun setOnClickListeners() {
        binding.activityAppSettingsFeaturesWebintentResolutionSectionWebintentResolutionSheet.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityAppSettingsFeaturesWebintentResolutionSectionWebintentResolutionSheet.setSwitchChecked(!binding.activityAppSettingsFeaturesWebintentResolutionSectionWebintentResolutionSheet.getSwitchChecked())
        }
    }

    private fun loadSettings() {
        binding.activityAppSettingsFeaturesWebintentResolutionSectionWebintentResolutionSheet.setSwitchChecked(
            WebIntentHelper(this).isCurrentDomainAssociated()
        )
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsFeaturesWebintentResolutionSectionWebintentResolutionSheet.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            if (compoundButton.isPressed || forceSwitch) {
                forceSwitch = false
                WebIntentHelper(this@AppSettingsFeaturesWebIntentResolutionActivity).requestSupportedLinks(checked)
            }
        }
    }
}
