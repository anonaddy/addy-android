package host.stjin.anonaddy.ui.appsettings.features

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesNotifySubscriptionExpiryBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy_shared.AnonAddy
import host.stjin.anonaddy_shared.AnonAddyForAndroid
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import kotlinx.coroutines.launch
import org.ocpsoft.prettytime.PrettyTime


class AppSettingsFeaturesNotifySubscriptionExpiryActivity : BaseActivity() {

    private lateinit var settingsManager: SettingsManager
    private var forceSwitch = false
    private lateinit var networkHelper: NetworkHelper


    private lateinit var binding: ActivityAppSettingsFeaturesNotifySubscriptionExpiryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesNotifySubscriptionExpiryBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activityAppSettingsFeaturesNotifySubscriptionExpiryNSVLL)
        )

        settingsManager = SettingsManager(false, this)
        networkHelper = NetworkHelper(this)
        setupToolbar(
            R.string.feature_subscription_expiry_notification,
            binding.activityAppSettingsFeaturesNotifySubscriptionExpiryNSV,
            binding.appsettingsFeaturesNotifySubscriptionExpiryToolbar,
            R.drawable.ic_credit_card
        )

        setSubscriptionInfoText((this@AppSettingsFeaturesNotifySubscriptionExpiryActivity.application as AnonAddyForAndroid).userResource) // Set this data right away for visuals
        checkSubscriptionExpiry()
        checkGooglePlayGuidelines()
        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
    }

    private fun checkGooglePlayGuidelines() {
        // Only show the renew button when not-google play version
        // https://support.google.com/googleplay/android-developer/answer/13321562
        if (BuildConfig.FLAVOR == "gplay") {
            binding.activityAppSettingsFeaturesNotifySubscriptionExpiryUpdateSubscription.visibility = View.GONE
        } else {
            binding.activityAppSettingsFeaturesNotifySubscriptionExpiryUpdateSubscription.visibility = View.VISIBLE
        }
    }

    private fun checkSubscriptionExpiry() {
        if (AnonAddy.VERSIONMAJOR == 9999) {
            lifecycleScope.launch {
                networkHelper.getUserResource { user: UserResource?, _: String? ->
                    setSubscriptionInfoText(user)
                }
            }
        } else {
            binding.activityAppSettingsFeaturesNotifySubscriptionExpiryCurrentSubscriptionExpiry.text =
                resources.getString(R.string.subscription_expiry_date_self_hosted)
            binding.activityAppSettingsFeaturesNotifySubscriptionExpirySection.setLayoutEnabled(false)
            binding.activityAppSettingsFeaturesNotifySubscriptionExpiryUpdateSubscription.setLayoutEnabled(false)
            binding.activityAppSettingsFeaturesNotifySubscriptionExpirySection.setDescription(resources.getString(R.string.subscription_expiry_date_self_hosted))
        }
    }

    @SuppressLint("StringFormatInvalid") // Suppress StringFormatInvalid, the gplayless version accepts 2 parameters where the gplay version only accepts 1
    private fun setSubscriptionInfoText(user: UserResource?) {
        if (user != null) {
            when {
                (this@AppSettingsFeaturesNotifySubscriptionExpiryActivity.application as AnonAddyForAndroid).userResource.subscription == null -> {
                    binding.activityAppSettingsFeaturesNotifySubscriptionExpiryCurrentSubscriptionExpiry.text =
                        resources.getString(R.string.subscription_expiry_date_never, AnonAddy.API_BASE_URL)
                }
                (this@AppSettingsFeaturesNotifySubscriptionExpiryActivity.application as AnonAddyForAndroid).userResource.subscription_ends_at != null -> {
                    val expiryDate =
                        DateTimeUtils.turnStringIntoLocalDateTime((this@AppSettingsFeaturesNotifySubscriptionExpiryActivity.application as AnonAddyForAndroid).userResource.subscription_ends_at)

                    val text = PrettyTime().format(expiryDate)
                    binding.activityAppSettingsFeaturesNotifySubscriptionExpiryCurrentSubscriptionExpiry.text =
                        resources.getString(R.string.subscription_expiry_date, text)
                }
                else -> {
                    binding.activityAppSettingsFeaturesNotifySubscriptionExpiryCurrentSubscriptionExpiry.text =
                        resources.getString(R.string.subscription_expiry_date_unknown, AnonAddy.API_BASE_URL)
                }
            }
        } else {
            binding.activityAppSettingsFeaturesNotifySubscriptionExpiryCurrentSubscriptionExpiry.text =
                resources.getString(R.string.subscription_expiry_date_unknown, AnonAddy.API_BASE_URL)
        }
    }

    private fun loadSettings() {
        binding.activityAppSettingsFeaturesNotifySubscriptionExpirySection.setSwitchChecked(
            settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_SUBSCRIPTION_EXPIRY, false)
        )
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsFeaturesNotifySubscriptionExpirySection.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed || forceSwitch) {
                    settingsManager.putSettingsBool(SettingsManager.PREFS.NOTIFY_SUBSCRIPTION_EXPIRY, checked)

                    // Since API token expiry should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                    BackgroundWorkerHelper(this@AppSettingsFeaturesNotifySubscriptionExpiryActivity).scheduleBackgroundWorker()
                }
            }
        })
    }

    // If the user comes back from eg. settings re-check + enable biometricswitch
    override fun onResume() {
        super.onResume()
        loadSettings()
        checkSubscriptionExpiry()
    }


    private fun setOnClickListeners() {
        binding.activityAppSettingsFeaturesNotifySubscriptionExpirySection.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityAppSettingsFeaturesNotifySubscriptionExpirySection.setSwitchChecked(!binding.activityAppSettingsFeaturesNotifySubscriptionExpirySection.getSwitchChecked())
            }
        })

        // This section is only visible in the gplayless version
        binding.activityAppSettingsFeaturesNotifySubscriptionExpiryUpdateSubscription.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val url = "${AnonAddy.API_BASE_URL}/settings/subscription"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })
    }

}