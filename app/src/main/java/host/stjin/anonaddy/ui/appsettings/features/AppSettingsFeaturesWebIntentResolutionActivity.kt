package host.stjin.anonaddy.ui.appsettings.features

import android.os.Bundle
import android.widget.CompoundButton
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesWebintentResolutionBinding
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.WebIntentManager


class AppSettingsFeaturesWebIntentResolutionActivity : BaseActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var encryptedSettingsManager: SettingsManager
    private var forceSwitch = false

    private lateinit var binding: ActivityAppSettingsFeaturesWebintentResolutionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesWebintentResolutionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(view, binding.appsettingsFeaturesWebintentResolutionNSVLL)

        settingsManager = SettingsManager(false, this)
        encryptedSettingsManager = SettingsManager(true, this)
        setupToolbar(
            binding.appsettingsFeaturesWebintentResolutionToolbar.customToolbarOneHandedMaterialtoolbar,
            R.string.integration_webintent_resolution,
            binding.appsettingsFeaturesWebintentResolutionToolbar.customToolbarOneHandedImage,
            R.drawable.ic_external_link
        )

        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
    }

    private fun loadSettings() {
        binding.activityAppSettingsFeaturesWebintentResolutionSectionWebintentResolutionSheet.setSwitchChecked(
            WebIntentManager(this).isCurrentDomainAssociated()
        )
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsFeaturesWebintentResolutionSectionWebintentResolutionSheet.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed || forceSwitch) {
                    forceSwitch = false
                    WebIntentManager(this@AppSettingsFeaturesWebIntentResolutionActivity).requestSupportedLinks(checked)
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
        binding.activityAppSettingsFeaturesWebintentResolutionSectionWebintentResolutionSheet.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityAppSettingsFeaturesWebintentResolutionSectionWebintentResolutionSheet.setSwitchChecked(!binding.activityAppSettingsFeaturesWebintentResolutionSectionWebintentResolutionSheet.getSwitchChecked())
            }
        })
    }

}