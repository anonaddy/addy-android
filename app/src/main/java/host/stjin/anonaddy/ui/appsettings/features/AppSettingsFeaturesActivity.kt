package host.stjin.anonaddy.ui.appsettings.features

import android.content.Intent
import android.os.Bundle
import android.widget.CompoundButton
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.ComponentUtils.getComponentState
import host.stjin.anonaddy.utils.ComponentUtils.setComponentState


class AppSettingsFeaturesActivity : BaseActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var encryptedSettingsManager: SettingsManager

    enum class COMPONENTS(val componentClassName: String) {
        MAILTO("host.stjin.anonaddy.ui.intent.IntentContextMenuAliasActivity")
    }

    private lateinit var binding: ActivityAppSettingsFeaturesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        settingsManager = SettingsManager(false, this)
        encryptedSettingsManager = SettingsManager(true, this)
        setupToolbar(binding.appsettingsFeaturesToolbar.customToolbarOneHandedMaterialtoolbar, R.string.features_and_integrations)
        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
    }

    private fun loadSettings() {
        binding.activityAppSettingsFeaturesSectionMailtoSheet.setSwitchChecked(
            getComponentState(
                this,
                BuildConfig.APPLICATION_ID,
                COMPONENTS.MAILTO.componentClassName
            )
        )

        binding.activityAppSettingsFeaturesSectionNotifyFailedDeliveriesSheet.setSwitchChecked(
            settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_FAILED_DELIVERIES)
        )
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsFeaturesSectionMailtoSheet.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed) {
                    setComponentState(this@AppSettingsFeaturesActivity, BuildConfig.APPLICATION_ID, COMPONENTS.MAILTO.componentClassName, checked)
                }
            }
        })


        binding.activityAppSettingsFeaturesSectionNotifyFailedDeliveriesSheet.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed) {
                    settingsManager.putSettingsBool(SettingsManager.PREFS.NOTIFY_FAILED_DELIVERIES, checked)

                    // Since failed deliveries should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                    BackgroundWorkerHelper(this@AppSettingsFeaturesActivity).scheduleBackgroundWorker()
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
        binding.activityAppSettingsFeaturesSectionMailtoSheet.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesMailToActivity::class.java)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsFeaturesSectionWatchAliasSheet.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesWatchAliasActivity::class.java)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsFeaturesSectionNotifyFailedDeliveriesSheet.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesNotifyFailedDeliveriesActivity::class.java)
                startActivity(intent)
            }
        })
    }


}