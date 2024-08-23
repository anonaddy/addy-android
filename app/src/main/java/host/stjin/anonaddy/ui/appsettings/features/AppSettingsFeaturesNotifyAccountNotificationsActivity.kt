package host.stjin.anonaddy.ui.appsettings.features

import android.content.Intent
import android.os.Bundle
import android.widget.CompoundButton
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesNotifyAccountNotificationsBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.ui.accountnotifications.AccountNotificationsActivity
import host.stjin.anonaddy_shared.managers.SettingsManager


class AppSettingsFeaturesNotifyAccountNotificationsActivity : BaseActivity() {

    private lateinit var settingsManager: SettingsManager
    private var forceSwitch = false

    private lateinit var binding: ActivityAppSettingsFeaturesNotifyAccountNotificationsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesNotifyAccountNotificationsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activityAppSettingsFeaturesNotifyAccountNotificationsNSVLL)
        )

        settingsManager = SettingsManager(false, this)
        setupToolbar(
            R.string.feature_notify_account_notifications,
            binding.activityAppSettingsFeaturesNotifyAccountNotificationsNSV,
            binding.appsettingsFeaturesNotifyAccountNotificationsToolbar,
            R.drawable.ic_bell
        )

        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
    }

    private fun loadSettings() {
        binding.activityAppSettingsFeaturesNotifyAccountNotificationsSection.setSwitchChecked(
            settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_ACCOUNT_NOTIFICATIONS)
        )
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsFeaturesNotifyAccountNotificationsSection.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed || forceSwitch) {
                    settingsManager.putSettingsBool(SettingsManager.PREFS.NOTIFY_ACCOUNT_NOTIFICATIONS, checked)

                    // Since account notifications should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                    BackgroundWorkerHelper(this@AppSettingsFeaturesNotifyAccountNotificationsActivity).scheduleBackgroundWorker()
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
        binding.activityAppSettingsFeaturesNotifyAccountNotificationsSection.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityAppSettingsFeaturesNotifyAccountNotificationsSection.setSwitchChecked(!binding.activityAppSettingsFeaturesNotifyAccountNotificationsSection.getSwitchChecked())
            }
        })
        binding.activityAppSettingsFeaturesNotifyAccountNotificationsActivity.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsFeaturesNotifyAccountNotificationsActivity, AccountNotificationsActivity::class.java)
                startActivity(intent)
            }
        })


    }


}