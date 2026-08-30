package host.stjin.anonaddy.ui.appsettings.features

import android.os.Bundle
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesNotifyDomainErrorBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy_shared.managers.SettingsManager


class AppSettingsFeaturesNotifyDomainErrorActivity : BaseActivity() {
    private lateinit var settingsManager: SettingsManager

    private var forceSwitch = false

    private lateinit var binding: ActivityAppSettingsFeaturesNotifyDomainErrorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesNotifyDomainErrorBinding.inflate(layoutInflater)
        InsetUtils.applyBottomInset(binding.activityAppSettingsFeaturesNotifyDomainErrorNSVLL)

        val view = binding.root
        setContentView(view)

        settingsManager = ServiceLocator.settingsManager
        setupToolbar(
            R.string.feature_domain_error_notification,
            binding.activityAppSettingsFeaturesNotifyDomainErrorNSV,
            binding.appsettingsFeaturesNotifyDomainErrorToolbar,
            R.drawable.ic_dns_alert
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
        binding.activityAppSettingsFeaturesNotifyDomainErrorSection.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityAppSettingsFeaturesNotifyDomainErrorSection.setSwitchChecked(!binding.activityAppSettingsFeaturesNotifyDomainErrorSection.getSwitchChecked())
        }
    }

    private fun loadSettings() {
        binding.activityAppSettingsFeaturesNotifyDomainErrorSection.setSwitchChecked(
            settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_DOMAIN_ERROR, false)
        )
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsFeaturesNotifyDomainErrorSection.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            if (compoundButton.isPressed || forceSwitch) {
                settingsManager.putSettingsBool(SettingsManager.PREFS.NOTIFY_DOMAIN_ERROR, checked)

                // Since API token expiry should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(this@AppSettingsFeaturesNotifyDomainErrorActivity).scheduleBackgroundWorker()
            }
        }
    }
}
