package host.stjin.anonaddy.ui.appsettings.features

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.ComponentUtils.getComponentState
import host.stjin.anonaddy.utils.ComponentUtils.setComponentState
import host.stjin.anonaddy.utils.InsetUtil
import host.stjin.anonaddy.utils.WebIntentManager
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.managers.SettingsManager.PREFS


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
        InsetUtil.applyBottomInset(binding.activityAppSettingsFeaturesSectionsNSVLL)

        val view = binding.root
        setContentView(view)


        settingsManager = SettingsManager(false, this)
        encryptedSettingsManager = SettingsManager(true, this)

        setupToolbar(
            R.string.features_and_integrations,
            binding.activityAppSettingsFeaturesSectionsNSV,
            binding.appsettingsFeaturesToolbar,
            R.drawable.ic_features_integrations_banner
        )

        loadSettings()
        checkForSelfHostedInstance()
        setOnClickListeners()
        setOnSwitchListeners()
    }

    private fun checkForSelfHostedInstance() {
        // Hide the switch on Subscription Expiry Notification Card when user is using self-hosted instance
        if (AddyIo.isUsingHostedInstance){
            binding.activityAppSettingsFeaturesSectionSubscriptionExpiryNotification.visibility = View.VISIBLE
            binding.activityAppSettingsFeaturesSectionNotifyAccountNotificationsSheet.visibility = View.VISIBLE
        } else {
            binding.activityAppSettingsFeaturesSectionSubscriptionExpiryNotification.visibility = View.GONE
            binding.activityAppSettingsFeaturesSectionNotifyAccountNotificationsSheet.visibility = View.GONE
        }


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
            settingsManager.getSettingsBool(PREFS.NOTIFY_FAILED_DELIVERIES)
        )

        binding.activityAppSettingsFeaturesSectionNotifyAccountNotificationsSheet.setSwitchChecked(
            settingsManager.getSettingsBool(PREFS.NOTIFY_ACCOUNT_NOTIFICATIONS)
        )

        binding.activityAppSettingsFeaturesSectionManageMultipleAliasesSheet.setSwitchChecked(
            settingsManager.getSettingsBool(PREFS.MANAGE_MULTIPLE_ALIASES, true)
        )

        binding.activityAppSettingsFeaturesSectionApiTokenExpiryNotification.setSwitchChecked(
            settingsManager.getSettingsBool(PREFS.NOTIFY_API_TOKEN_EXPIRY, true)
        )

        binding.activityAppSettingsFeaturesSectionCertificateExpiryNotification.setSwitchChecked(
            settingsManager.getSettingsBool(PREFS.NOTIFY_CERTIFICATE_EXPIRY, false)
        )

        val alias = encryptedSettingsManager.getSettingsString(PREFS.CERTIFICATE_ALIAS)
        binding.activityAppSettingsFeaturesSectionCertificateExpiryNotification.showSwitch(
            alias != null
        )

        binding.activityAppSettingsFeaturesSectionDomainErrorNotification.setSwitchChecked(
            settingsManager.getSettingsBool(PREFS.NOTIFY_DOMAIN_ERROR, false)
        )

        binding.activityAppSettingsFeaturesSectionSubscriptionExpiryNotification.setSwitchChecked(
            settingsManager.getSettingsBool(PREFS.NOTIFY_SUBSCRIPTION_EXPIRY, false)
        )

        binding.activityAppSettingsFeaturesSectionWebintentSheet.setSwitchChecked(
            WebIntentManager(this).isCurrentDomainAssociated()
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
                    settingsManager.putSettingsBool(PREFS.NOTIFY_FAILED_DELIVERIES, checked)

                    // Since failed deliveries should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                    BackgroundWorkerHelper(this@AppSettingsFeaturesActivity).scheduleBackgroundWorker()
                }
            }
        })

        binding.activityAppSettingsFeaturesSectionNotifyAccountNotificationsSheet.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed) {
                    settingsManager.putSettingsBool(PREFS.NOTIFY_ACCOUNT_NOTIFICATIONS, checked)

                    // Since account notifications should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                    BackgroundWorkerHelper(this@AppSettingsFeaturesActivity).scheduleBackgroundWorker()
                }
            }
        })

        binding.activityAppSettingsFeaturesSectionManageMultipleAliasesSheet.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed) {
                    binding.activityAppSettingsFeaturesSectionManageMultipleAliasesSheet.setSectionAlert(true)
                    binding.activityAppSettingsFeaturesSectionManageMultipleAliasesSheet.setDescription(
                        this@AppSettingsFeaturesActivity.resources.getString(
                            R.string.restart_app_required
                        )
                    )

                    settingsManager.putSettingsBool(PREFS.MANAGE_MULTIPLE_ALIASES, checked)
                }
            }
        })

        binding.activityAppSettingsFeaturesSectionApiTokenExpiryNotification.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed) {
                    settingsManager.putSettingsBool(PREFS.NOTIFY_API_TOKEN_EXPIRY, checked)

                    // Since api token check should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                    BackgroundWorkerHelper(this@AppSettingsFeaturesActivity).scheduleBackgroundWorker()
                }
            }
        })

        binding.activityAppSettingsFeaturesSectionCertificateExpiryNotification.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed) {
                    settingsManager.putSettingsBool(PREFS.NOTIFY_CERTIFICATE_EXPIRY, checked)

                    // Since api token check should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                    BackgroundWorkerHelper(this@AppSettingsFeaturesActivity).scheduleBackgroundWorker()
                }
            }
        })

        binding.activityAppSettingsFeaturesSectionDomainErrorNotification.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed) {
                    settingsManager.putSettingsBool(PREFS.NOTIFY_DOMAIN_ERROR, checked)

                    // Since api token check should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                    BackgroundWorkerHelper(this@AppSettingsFeaturesActivity).scheduleBackgroundWorker()
                }
            }
        })

        binding.activityAppSettingsFeaturesSectionSubscriptionExpiryNotification.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed) {
                    settingsManager.putSettingsBool(PREFS.NOTIFY_SUBSCRIPTION_EXPIRY, checked)

                    // Since api token check should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                    BackgroundWorkerHelper(this@AppSettingsFeaturesActivity).scheduleBackgroundWorker()
                }
            }
        })

        binding.activityAppSettingsFeaturesSectionWebintentSheet.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed) {
                    WebIntentManager(this@AppSettingsFeaturesActivity).requestSupportedLinks(checked)
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

        binding.activityAppSettingsFeaturesSectionNotifyAccountNotificationsSheet.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesNotifyAccountNotificationsActivity::class.java)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsFeaturesSectionNotifyAccountNotificationsSheet.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesNotifyAccountNotificationsActivity::class.java)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsFeaturesSectionManageMultipleAliasesSheet.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesManageMultipleAliasesActivity::class.java)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsFeaturesSectionApiTokenExpiryNotification.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesNotifyApiTokenExpiryActivity::class.java)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsFeaturesSectionCertificateExpiryNotification.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesNotifyCertificateExpiryActivity::class.java)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsFeaturesSectionDomainErrorNotification.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesNotifyDomainErrorActivity::class.java)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsFeaturesSectionSubscriptionExpiryNotification.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesNotifySubscriptionExpiryActivity::class.java)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsFeaturesSectionWebintentSheet.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesWebIntentResolutionActivity::class.java)
                startActivity(intent)
            }
        })
    }


}