package host.stjin.anonaddy.ui.appsettings.features
import host.stjin.anonaddy_shared.utils.GsonTools

import android.app.NotificationManager
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.Wearable
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesNotifyApiTokenExpiryBinding
import host.stjin.anonaddy.notifications.NotificationHelper
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.setup.AddApiBottomDialogFragment
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy.utils.WearOSHelper
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.ApiTokenDetails
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.repositories.UserRepository
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch
import org.ocpsoft.prettytime.PrettyTime


class AppSettingsFeaturesNotifyApiTokenExpiryActivity : BaseActivity(), AddApiBottomDialogFragment.AddApiBottomDialogListener {
    private lateinit var settingsManager: SettingsManager

    private var forceSwitch = false

    private lateinit var userRepository: UserRepository

    private lateinit var binding: ActivityAppSettingsFeaturesNotifyApiTokenExpiryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesNotifyApiTokenExpiryBinding.inflate(layoutInflater)
        InsetUtils.applyBottomInset(binding.activityAppSettingsFeaturesNotifyApiTokenExpiryNSVLL)

        val view = binding.root
        setContentView(view)

        settingsManager = ServiceLocator.settingsManager
        userRepository = ServiceLocator.userRepository
        setupToolbar(
            R.string.feature_api_token_expiry_notification,
            binding.activityAppSettingsFeaturesNotifyApiTokenExpiryNSV,
            binding.appsettingsFeaturesNotifyApiTokenExpiryToolbar,
            R.drawable.ic_letters_case
        )

        checkTokenExpiry()
        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
    }

    // If the user comes back from eg. settings re-check + enable biometricswitch
    override fun onResume() {
        super.onResume()
        loadSettings()
        checkTokenExpiry()
    }

    private fun setOnClickListeners() {
        binding.activityAppSettingsFeaturesNotifyApiTokenExpirySection.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityAppSettingsFeaturesNotifyApiTokenExpirySection.setSwitchChecked(!binding.activityAppSettingsFeaturesNotifyApiTokenExpirySection.getSwitchChecked())
        }
        binding.activityAppSettingsFeaturesNotifyApiTokenExpiryChangeToken.setOnLayoutClickedListener {
            val addApiBottomDialogFragment = AddApiBottomDialogFragment.newInstance(AddyIo.API_BASE_URL)
            if (!addApiBottomDialogFragment.isAdded) {
                addApiBottomDialogFragment.show(
                    supportFragmentManager,
                    "addApiBottomDialogFragment"
                )
            }
        }
    }

    override fun onClickSave(baseUrl: String, apiKey: String) {
        (supportFragmentManager.findFragmentByTag("addApiBottomDialogFragment") as? AddApiBottomDialogFragment)?.dismissAllowingStateLoss()
        updateKey(apiKey)
        checkTokenExpiry()

        // Send the new configuration to all the connected Wear devices
        try {
            Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
                for (node in nodes) {
                    val configuration = GsonTools.gson.toJson(WearOSHelper.createWearOSConfiguration())
                    Wearable.getMessageClient(this).sendMessage(
                        node.id,
                        "/setup",
                        configuration.toByteArray()
                    )
                }

            }
        } catch (ex: Exception) {
            // WearAPI not available, not sending anything to nodes
            LoggingHelper(this).addLog(LOGIMPORTANCE.WARNING.int, ex.toString(), "AppSettingsFeaturesNotifyApiTokenExpiryActivity;onClickSave", null)
        }
    }

    private fun checkTokenExpiry() {
        lifecycleScope.launch {
            when (val result = userRepository.getApiTokenDetails()) {
                is NetworkResult.Success -> setApiInfoText(result.data)
                is NetworkResult.Error -> setApiInfoText(null)
            }
        }
    }

    private fun setApiInfoText(apiTokenDetails: ApiTokenDetails?) {
        if (apiTokenDetails != null) {
            if (apiTokenDetails.expires_at != null) {
                val expiryDate = DateTimeUtils.convertStringToLocalTimeZoneDate(apiTokenDetails.expires_at) // Get the expiry date
                val text = PrettyTime().format(expiryDate)
                binding.activityAppSettingsFeaturesNotifyApiTokenExpiryCurrentTokenExpiry.text =
                    this@AppSettingsFeaturesNotifyApiTokenExpiryActivity.resources.getString(
                        R.string.current_api_token_expiry_date,
                        apiTokenDetails.name,
                        text
                    )
            } else {
                binding.activityAppSettingsFeaturesNotifyApiTokenExpiryCurrentTokenExpiry.text =
                    this@AppSettingsFeaturesNotifyApiTokenExpiryActivity.resources.getString(
                        R.string.current_api_token_expiry_date_never,
                        apiTokenDetails.name,
                        AddyIo.API_BASE_URL
                    )
            }
        } else {
            binding.activityAppSettingsFeaturesNotifyApiTokenExpiryCurrentTokenExpiry.text =
                this@AppSettingsFeaturesNotifyApiTokenExpiryActivity.resources.getString(
                    R.string.current_api_token_expiry_date_unknown,
                    AddyIo.API_BASE_URL
                )
        }
    }

    private fun loadSettings() {
        binding.activityAppSettingsFeaturesNotifyApiTokenExpirySection.setSwitchChecked(
            settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_API_TOKEN_EXPIRY, true)
        )
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsFeaturesNotifyApiTokenExpirySection.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            if (compoundButton.isPressed || forceSwitch) {
                settingsManager.putSettingsBool(SettingsManager.PREFS.NOTIFY_API_TOKEN_EXPIRY, checked)

                // Since API token expiry should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(this@AppSettingsFeaturesNotifyApiTokenExpiryActivity).scheduleBackgroundWorker()
            }
        }
    }

    private fun updateKey(apiKey: String) {
        val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
        encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.API_KEY, apiKey)
        SnackbarHelper.createSnackbar(
            this,
            this.resources.getString(R.string.api_key_updated),
            binding.activityAppSettingsFeaturesNotifyApiTokenExpiryCL
        ).show()

        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationHelper.API_KEY_EXPIRE_NOTIFICATION_ID)
    }
}
