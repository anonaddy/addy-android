package host.stjin.anonaddy.ui.appsettings.features

import android.os.Bundle
import android.widget.CompoundButton
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesNotifyFailedDeliveriesBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.customviews.SectionView


class AppSettingsFeaturesNotifyFailedDeliveriesActivity : BaseActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var encryptedSettingsManager: SettingsManager
    private var forceSwitch = false

    private lateinit var binding: ActivityAppSettingsFeaturesNotifyFailedDeliveriesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesNotifyFailedDeliveriesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        settingsManager = SettingsManager(false, this)
        encryptedSettingsManager = SettingsManager(true, this)
        setupToolbar(
            binding.appsettingsFeaturesNotifyFailedDeliveriesToolbar.customToolbarOneHandedMaterialtoolbar,
            R.string.feature_notify_failed_deliveries
        )

        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
    }

    private fun loadSettings() {
        binding.activityAppSettingsFeaturesNotifyFailedDeliveriesSection.setSwitchChecked(
            settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_FAILED_DELIVERIES)
        )
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsFeaturesNotifyFailedDeliveriesSection.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed || forceSwitch) {
                    settingsManager.putSettingsBool(SettingsManager.PREFS.NOTIFY_FAILED_DELIVERIES, checked)

                    // Since failed deliveries should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                    BackgroundWorkerHelper(this@AppSettingsFeaturesNotifyFailedDeliveriesActivity).scheduleBackgroundWorker()
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
        binding.activityAppSettingsFeaturesNotifyFailedDeliveriesSection.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityAppSettingsFeaturesNotifyFailedDeliveriesSection.setSwitchChecked(!binding.activityAppSettingsFeaturesNotifyFailedDeliveriesSection.getSwitchChecked())
            }
        })
    }


}