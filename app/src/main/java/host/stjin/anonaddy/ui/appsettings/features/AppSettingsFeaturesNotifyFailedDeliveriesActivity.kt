package host.stjin.anonaddy.ui.appsettings.features

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesNotifyFailedDeliveriesBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.ui.faileddeliveries.FailedDeliveriesActivity
import host.stjin.anonaddy.utils.InsetUtil
import host.stjin.anonaddy_shared.managers.SettingsManager

class AppSettingsFeaturesNotifyFailedDeliveriesActivity : BaseActivity() {

    private lateinit var settingsManager: SettingsManager
    private var forceSwitch = false

    private lateinit var binding: ActivityAppSettingsFeaturesNotifyFailedDeliveriesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesNotifyFailedDeliveriesBinding.inflate(layoutInflater)
        InsetUtil.applyBottomInset(binding.activityAppSettingsFeaturesNotifyFailedDeliveriesNSVLL)

        val view = binding.root
        setContentView(view)

        settingsManager = SettingsManager(false, this)
        setupToolbar(
            R.string.feature_notify_failed_deliveries,
            binding.activityAppSettingsFeaturesNotifyFailedDeliveriesNSV,
            binding.appsettingsFeaturesNotifyFailedDeliveriesToolbar,
            R.drawable.ic_mail_error
        )

        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
    }

    private fun loadSettings() {
        val notifyFailedDeliveries = settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_FAILED_DELIVERIES)
        binding.activityAppSettingsFeaturesNotifyFailedDeliveriesSection.setSwitchChecked(notifyFailedDeliveries)
        
        if (notifyFailedDeliveries) {
            binding.activityAppSettingsFeaturesNotifyFailedDeliveriesTypeSection.visibility = View.VISIBLE
        } else {
            binding.activityAppSettingsFeaturesNotifyFailedDeliveriesTypeSection.visibility = View.GONE
        }

        val type = settingsManager.getSettingsString(SettingsManager.PREFS.NOTIFY_FAILED_DELIVERIES_TYPE) ?: "all"
        binding.activityAppSettingsFeaturesNotifyFailedDeliveriesTypeSection.setDescription(
            when (type) {
                "inbound" -> getString(R.string.inbound)
                "outbound" -> getString(R.string.outbound)
                else -> getString(R.string.all)
            }
        )
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsFeaturesNotifyFailedDeliveriesSection.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed || forceSwitch) {
                    settingsManager.putSettingsBool(SettingsManager.PREFS.NOTIFY_FAILED_DELIVERIES, checked)
                    
                    if (checked) {
                        binding.activityAppSettingsFeaturesNotifyFailedDeliveriesTypeSection.visibility = View.VISIBLE
                    } else {
                        binding.activityAppSettingsFeaturesNotifyFailedDeliveriesTypeSection.visibility = View.GONE
                    }

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
        binding.activityAppSettingsFeaturesNotifyFailedDeliveriesTypeSection.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val types = arrayOf("all", "inbound", "outbound")
                val typeNames = arrayOf(getString(R.string.all), getString(R.string.inbound), getString(R.string.outbound))
                val currentType = settingsManager.getSettingsString(SettingsManager.PREFS.NOTIFY_FAILED_DELIVERIES_TYPE) ?: "all"
                val checkedItem = types.indexOf(currentType).takeIf { it != -1 } ?: 0

                MaterialAlertDialogBuilder(this@AppSettingsFeaturesNotifyFailedDeliveriesActivity)
                    .setTitle(R.string.type)
                    .setSingleChoiceItems(typeNames, checkedItem) { dialog, which ->
                        settingsManager.putSettingsString(SettingsManager.PREFS.NOTIFY_FAILED_DELIVERIES_TYPE, types[which])
                        loadSettings()
                        BackgroundWorkerHelper(this@AppSettingsFeaturesNotifyFailedDeliveriesActivity).scheduleBackgroundWorker()
                        dialog.dismiss()
                    }
                    .show()
            }
        })
        binding.activityAppSettingsFeaturesNotifyFailedDeliveriesActivity.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsFeaturesNotifyFailedDeliveriesActivity, FailedDeliveriesActivity::class.java)
                startActivity(intent)
            }
        })
    }
}