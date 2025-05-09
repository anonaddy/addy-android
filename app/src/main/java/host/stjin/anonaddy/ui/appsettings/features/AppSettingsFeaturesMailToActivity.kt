package host.stjin.anonaddy.ui.appsettings.features

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.CompoundButton
import android.widget.Toast
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesMailtoBinding
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.ComponentUtils.getComponentState
import host.stjin.anonaddy.utils.ComponentUtils.setComponentState
import host.stjin.anonaddy.utils.InsetUtil
import host.stjin.anonaddy_shared.managers.SettingsManager


class AppSettingsFeaturesMailToActivity : BaseActivity() {

    private lateinit var settingsManager: SettingsManager
    private var forceSwitch = false

    private lateinit var binding: ActivityAppSettingsFeaturesMailtoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesMailtoBinding.inflate(layoutInflater)
        InsetUtil.applyBottomInset(binding.activityAppSettingsFeaturesMailtoNSVLL)

        val view = binding.root
        setContentView(view)


        settingsManager = SettingsManager(false, this)
        setupToolbar(
            R.string.integration_mailto_alias,
            binding.activityAppSettingsFeaturesMailtoNSV,
            binding.appsettingsFeaturesMailtoToolbar,
            R.drawable.ic_dots_circle_horizontal
        )

        loadSettings()
        setOnClickListeners()
        setOnLongClickListeners()
        setOnSwitchListeners()
    }

    private fun loadSettings() {
        binding.activityAppSettingsFeaturesMailtoSectionMailtoSheet.setSwitchChecked(
            getComponentState(
                this,
                BuildConfig.APPLICATION_ID,
                AppSettingsFeaturesActivity.COMPONENTS.MAILTO.componentClassName
            )
        )
        binding.activityAppSettingsFeaturesMailtoSectionMailtoSheetSuggestions.setSwitchChecked(
            settingsManager.getSettingsBool(SettingsManager.PREFS.MAILTO_ACTIVITY_SHOW_SUGGESTIONS)
        )
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsFeaturesMailtoSectionMailtoSheet.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed || forceSwitch) {
                    setComponentState(
                        this@AppSettingsFeaturesMailToActivity,
                        BuildConfig.APPLICATION_ID,
                        AppSettingsFeaturesActivity.COMPONENTS.MAILTO.componentClassName,
                        checked
                    )
                }
            }
        })

        binding.activityAppSettingsFeaturesMailtoSectionMailtoSheetSuggestions.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed || forceSwitch) {
                    forceSwitch = false
                    settingsManager.putSettingsBool(SettingsManager.PREFS.MAILTO_ACTIVITY_SHOW_SUGGESTIONS, checked)
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
        binding.activityAppSettingsFeaturesMailtoSectionMailtoSheet.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityAppSettingsFeaturesMailtoSectionMailtoSheet.setSwitchChecked(!binding.activityAppSettingsFeaturesMailtoSectionMailtoSheet.getSwitchChecked())
            }
        })

        binding.activityAppSettingsFeaturesMailtoSectionMailtoSheetSuggestions.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityAppSettingsFeaturesMailtoSectionMailtoSheetSuggestions.setSwitchChecked(!binding.activityAppSettingsFeaturesMailtoSectionMailtoSheetSuggestions.getSwitchChecked())
            }
        })
    }

    private fun setOnLongClickListeners() {
        binding.activityAppSettingsFeaturesMailtoImage.setOnLongClickListener {
            binding.activityAppSettingsFeaturesMailtoImage.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            Toast.makeText(
                this@AppSettingsFeaturesMailToActivity,
                this@AppSettingsFeaturesMailToActivity.resources.getString(R.string.just_like_that), Toast.LENGTH_SHORT
            ).show()
            false
        }
    }


}