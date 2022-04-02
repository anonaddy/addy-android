package host.stjin.anonaddy.ui.appsettings.features

import android.os.Bundle
import android.widget.CompoundButton
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesManageMultipleAliasesBinding
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy_shared.managers.SettingsManager


class AppSettingsFeaturesManageMultipleAliasesActivity : BaseActivity() {

    private lateinit var settingsManager: SettingsManager
    private var forceSwitch = false

    private lateinit var binding: ActivityAppSettingsFeaturesManageMultipleAliasesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesManageMultipleAliasesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activityAppSettingsFeaturesLongpressNSVLL)
        )

        settingsManager = SettingsManager(false, this)
        setupToolbar(
            R.string.feature_longpress,
            binding.activityAppSettingsFeaturesLongpressNSV,
            binding.appsettingsFeaturesLongpressToolbar,
            R.drawable.ic_hand_click
        )

        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
    }

    private fun loadSettings() {
        binding.activityAppSettingsFeaturesLongpressSectionLongpressSheet.setSwitchChecked(
            settingsManager.getSettingsBool(SettingsManager.PREFS.MANAGE_MULTIPLE_ALIASES, default = true)
        )
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsFeaturesLongpressSectionLongpressSheet.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed || forceSwitch) {
                    binding.activityAppSettingsFeaturesLongpressSectionLongpressSheet.setSectionAlert(true)
                    binding.activityAppSettingsFeaturesLongpressSectionLongpressSheet.setDescription(
                        this@AppSettingsFeaturesManageMultipleAliasesActivity.resources.getString(
                            R.string.restart_app_required
                        )
                    )

                    settingsManager.putSettingsBool(SettingsManager.PREFS.MANAGE_MULTIPLE_ALIASES, checked)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
    }


    private fun setOnClickListeners() {
        binding.activityAppSettingsFeaturesLongpressSectionLongpressSheet.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityAppSettingsFeaturesLongpressSectionLongpressSheet.setSwitchChecked(!binding.activityAppSettingsFeaturesLongpressSectionLongpressSheet.getSwitchChecked())
            }
        })
    }

}