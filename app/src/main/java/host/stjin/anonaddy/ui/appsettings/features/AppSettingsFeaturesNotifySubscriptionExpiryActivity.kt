package host.stjin.anonaddy.ui.appsettings.features

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesNotifySubscriptionExpiryBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.InsetUtil
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.AddyIoApp
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
        InsetUtil.applyBottomInset(binding.activityAppSettingsFeaturesNotifySubscriptionExpiryNSVLL)

        val view = binding.root
        setContentView(view)


        settingsManager = SettingsManager(false, this)
        networkHelper = NetworkHelper(this)
        setupToolbar(
            R.string.feature_subscription_expiry_notification,
            binding.activityAppSettingsFeaturesNotifySubscriptionExpiryNSV,
            binding.appsettingsFeaturesNotifySubscriptionExpiryToolbar,
            R.drawable.ic_credit_card
        )

        setSubscriptionInfoText((this@AppSettingsFeaturesNotifySubscriptionExpiryActivity.application as AddyIoApp).userResource) // Set this data right away for visuals
        checkSubscriptionExpiry()
        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
    }


    private fun checkSubscriptionExpiry() {
            lifecycleScope.launch {
                networkHelper.getUserResource { user: UserResource?, _: String? ->
                    setSubscriptionInfoText(user)
                }
            }
    }

    @SuppressLint("StringFormatInvalid") // Suppress StringFormatInvalid, the gplayless version accepts 2 parameters where the gplay version only accepts 1
    private fun setSubscriptionInfoText(user: UserResource?) {
        if (user != null) {
            when {
                user.subscription == null -> {
                    binding.activityAppSettingsFeaturesNotifySubscriptionExpiryCurrentSubscriptionExpiry.text =
                        resources.getString(R.string.subscription_expiry_date_never, AddyIo.API_BASE_URL)
                }

                user.subscription_ends_at != null -> {
                    val expiryDate =
                        DateTimeUtils.turnStringIntoLocalDateTime(user.subscription_ends_at)

                    val text = PrettyTime().format(expiryDate)
                    binding.activityAppSettingsFeaturesNotifySubscriptionExpiryCurrentSubscriptionExpiry.text =
                        resources.getString(R.string.subscription_expiry_date, text)
                }

                else -> {
                    binding.activityAppSettingsFeaturesNotifySubscriptionExpiryCurrentSubscriptionExpiry.text =
                        resources.getString(R.string.subscription_expiry_date_unknown, AddyIo.API_BASE_URL)
                }
            }
        } else {
            binding.activityAppSettingsFeaturesNotifySubscriptionExpiryCurrentSubscriptionExpiry.text =
                resources.getString(R.string.subscription_expiry_date_unknown, AddyIo.API_BASE_URL)
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
    }

}