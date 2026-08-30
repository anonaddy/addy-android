package host.stjin.anonaddy.ui.appsettings.features

import android.content.Intent
import android.os.Bundle
import android.view.View
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.utils.ComponentUtils.getComponentState
import host.stjin.anonaddy.utils.ComponentUtils.setComponentState
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy.utils.WebIntentHelper
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.managers.SettingsManager.PREFS


class AppSettingsFeaturesActivity : BaseActivity() {
    private lateinit var settingsManager: SettingsManager

    private lateinit var encryptedSettingsManager: SettingsManager

    private lateinit var binding: ActivityAppSettingsFeaturesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesBinding.inflate(layoutInflater)
        InsetUtils.applyBottomInset(binding.activityAppSettingsFeaturesSectionsNSVLL)

        val view = binding.root
        setContentView(view)


        settingsManager = ServiceLocator.settingsManager
        encryptedSettingsManager = ServiceLocator.encryptedSettingsManager

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

    // If the user comes back from eg. settings re-check + enable biometricswitch
    override fun onResume() {
        super.onResume()
        loadSettings()
    }

    private fun setOnClickListeners() {
        binding.activityAppSettingsFeaturesSectionMailtoSheet.setOnLayoutClickedListener {
            val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesMailToActivity::class.java)
            startActivity(intent)
        }

        binding.activityAppSettingsFeaturesSectionWatchAliasSheet.setOnLayoutClickedListener {
            val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesWatchAliasActivity::class.java)
            startActivity(intent)
        }

        binding.activityAppSettingsFeaturesSectionNotifyFailedDeliveriesSheet.setOnLayoutClickedListener {
            val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesNotifyFailedDeliveriesActivity::class.java)
            startActivity(intent)
        }

        binding.activityAppSettingsFeaturesSectionNotifyAccountNotificationsSheet.setOnLayoutClickedListener {
            val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesNotifyAccountNotificationsActivity::class.java)
            startActivity(intent)
        }

        binding.activityAppSettingsFeaturesSectionApiTokenExpiryNotification.setOnLayoutClickedListener {
            val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesNotifyApiTokenExpiryActivity::class.java)
            startActivity(intent)
        }

        binding.activityAppSettingsFeaturesSectionCertificateExpiryNotification.setOnLayoutClickedListener {
            val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesNotifyCertificateExpiryActivity::class.java)
            startActivity(intent)
        }

        binding.activityAppSettingsFeaturesSectionDomainErrorNotification.setOnLayoutClickedListener {
            val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesNotifyDomainErrorActivity::class.java)
            startActivity(intent)
        }

        binding.activityAppSettingsFeaturesSectionSubscriptionExpiryNotification.setOnLayoutClickedListener {
            val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesNotifySubscriptionExpiryActivity::class.java)
            startActivity(intent)
        }

        binding.activityAppSettingsFeaturesSectionWebintentSheet.setOnLayoutClickedListener {
            val intent = Intent(this@AppSettingsFeaturesActivity, AppSettingsFeaturesWebIntentResolutionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkForSelfHostedInstance() {
        // Hide the switch on Subscription Expiry Notification Card when user is using self-hosted instance
        if (AddyIo.isUsingHostedInstance) {
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
            WebIntentHelper(this).isCurrentDomainAssociated()
        )
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsFeaturesSectionMailtoSheet.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            if (compoundButton.isPressed) {
                setComponentState(this@AppSettingsFeaturesActivity, BuildConfig.APPLICATION_ID, COMPONENTS.MAILTO.componentClassName, checked)
            }
        }


        binding.activityAppSettingsFeaturesSectionNotifyFailedDeliveriesSheet.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            if (compoundButton.isPressed) {
                settingsManager.putSettingsBool(PREFS.NOTIFY_FAILED_DELIVERIES, checked)

                // Since failed deliveries should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(this@AppSettingsFeaturesActivity).scheduleBackgroundWorker()
            }
        }

        binding.activityAppSettingsFeaturesSectionNotifyAccountNotificationsSheet.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            if (compoundButton.isPressed) {
                settingsManager.putSettingsBool(PREFS.NOTIFY_ACCOUNT_NOTIFICATIONS, checked)

                // Since account notifications should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(this@AppSettingsFeaturesActivity).scheduleBackgroundWorker()
            }
        }

        binding.activityAppSettingsFeaturesSectionApiTokenExpiryNotification.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            if (compoundButton.isPressed) {
                settingsManager.putSettingsBool(PREFS.NOTIFY_API_TOKEN_EXPIRY, checked)

                // Since api token check should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(this@AppSettingsFeaturesActivity).scheduleBackgroundWorker()
            }
        }

        binding.activityAppSettingsFeaturesSectionCertificateExpiryNotification.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            if (compoundButton.isPressed) {
                settingsManager.putSettingsBool(PREFS.NOTIFY_CERTIFICATE_EXPIRY, checked)

                // Since api token check should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(this@AppSettingsFeaturesActivity).scheduleBackgroundWorker()
            }
        }

        binding.activityAppSettingsFeaturesSectionDomainErrorNotification.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            if (compoundButton.isPressed) {
                settingsManager.putSettingsBool(PREFS.NOTIFY_DOMAIN_ERROR, checked)

                // Since api token check should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(this@AppSettingsFeaturesActivity).scheduleBackgroundWorker()
            }
        }

        binding.activityAppSettingsFeaturesSectionSubscriptionExpiryNotification.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            if (compoundButton.isPressed) {
                settingsManager.putSettingsBool(PREFS.NOTIFY_SUBSCRIPTION_EXPIRY, checked)

                // Since api token check should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(this@AppSettingsFeaturesActivity).scheduleBackgroundWorker()
            }
        }

        binding.activityAppSettingsFeaturesSectionWebintentSheet.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            if (compoundButton.isPressed) {
                WebIntentHelper(this@AppSettingsFeaturesActivity).requestSupportedLinks(checked)
            }
        }
    }

    enum class COMPONENTS(val componentClassName: String) {
        MAILTO("host.stjin.anonaddy.ui.intent.IntentContextMenuAliasActivity")
    }
}
