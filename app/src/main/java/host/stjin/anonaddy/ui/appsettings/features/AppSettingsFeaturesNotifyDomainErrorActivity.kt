package host.stjin.anonaddy.ui.appsettings.features

import android.os.Bundle
import android.widget.CompoundButton
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesNotifyDomainErrorBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager


class AppSettingsFeaturesNotifyDomainErrorActivity : BaseActivity() {

    private lateinit var settingsManager: SettingsManager
    private var forceSwitch = false
    private lateinit var networkHelper: NetworkHelper


    private lateinit var binding: ActivityAppSettingsFeaturesNotifyDomainErrorBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesNotifyDomainErrorBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activityAppSettingsFeaturesNotifyDomainErrorNSVLL)
        )

        settingsManager = SettingsManager(false, this)
        networkHelper = NetworkHelper(this)
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

    private fun loadSettings() {
        binding.activityAppSettingsFeaturesNotifyDomainErrorSection.setSwitchChecked(
            settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_DOMAIN_ERROR, false)
        )
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsFeaturesNotifyDomainErrorSection.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed || forceSwitch) {
                    settingsManager.putSettingsBool(SettingsManager.PREFS.NOTIFY_DOMAIN_ERROR, checked)

                    // Since API token expiry should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                    BackgroundWorkerHelper(this@AppSettingsFeaturesNotifyDomainErrorActivity).scheduleBackgroundWorker()
                }
            }
        })
    }

    // If the user comes back from eg. settings re-check + enable biometricswitch
    override fun onResume() {
        super.onResume()
        loadSettings()
    }


    private fun setOnClickListeners() {
        binding.activityAppSettingsFeaturesNotifyDomainErrorSection.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityAppSettingsFeaturesNotifyDomainErrorSection.setSwitchChecked(!binding.activityAppSettingsFeaturesNotifyDomainErrorSection.getSwitchChecked())
            }
        })
    }

}