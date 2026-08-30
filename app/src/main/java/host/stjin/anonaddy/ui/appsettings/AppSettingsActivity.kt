package host.stjin.anonaddy.ui.appsettings

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.Wearable
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.Updater
import host.stjin.anonaddy.databinding.ActivityAppSettingsBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.appsettings.backup.AppSettingsBackupActivity
import host.stjin.anonaddy.ui.appsettings.features.AppSettingsFeaturesActivity
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.appsettings.update.AppSettingsUpdateActivity
import host.stjin.anonaddy.ui.appsettings.wearos.AppSettingsWearOSActivity
import host.stjin.anonaddy.utils.AnonAddyUtils
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


class AppSettingsActivity : BaseActivity(),
    UIUXInterfaceBottomDialogFragment.AddUIUXInterfaceBottomDialogListener,
    BackgroundServiceIntervalBottomDialogFragment.AddBackgroundServiceIntervalBottomDialogListener,
    PreferredEmailClientBottomDialogFragment.PreferredEmailClientBottomDialogListener {
    private val addUIUXInterfaceBottomDialogFragment: UIUXInterfaceBottomDialogFragment =

        UIUXInterfaceBottomDialogFragment.newInstance()

    private var addBackgroundServiceIntervalBottomDialogFragment: BackgroundServiceIntervalBottomDialogFragment =

        BackgroundServiceIntervalBottomDialogFragment.newInstance()

    private val deleteAccountConfirmationBottomDialogFragment: DeleteAccountConfirmationBottomDialogFragment =

        DeleteAccountConfirmationBottomDialogFragment.newInstance()

    private lateinit var settingsManager: SettingsManager

    private lateinit var encryptedSettingsManager: SettingsManager

    private var forceSwitch = false

    private lateinit var binding: ActivityAppSettingsBinding

    private var shouldEnableBiometric = true

    private var notificationPermissionsResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        when (result) {
            true -> checkPermissions()
            false -> openNotificationSettings()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        InsetUtils.applyBottomInset(binding.activityAppSettingsNSVLL)
        val view = binding.root
        setContentView(view)

        settingsManager = ServiceLocator.settingsManager
        encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
        setupToolbar(
            R.string.settings,
            binding.activityAppSettingsNSV,
            binding.appsettingsToolbar,
            R.drawable.ic_settings
        )

        setVersion()
        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
        setOnBiometricSwitchListeners()

        checkForVariant()

        checkForUpdates()
        checkPermissions()
    }

    // If the user comes back from eg. settings re-check + enable biometricswitch
    override fun onResume() {
        super.onResume()
        setOnBiometricSwitchListeners()
        checkPermissions() // When the user allows permissions through the system settings app, this value needs to be updated when coming back
        loadSettings()
    }

    private fun setOnClickListeners() {
        binding.activityAppSettingsSectionAppTheme.setOnLayoutClickedListener {
            if (!addUIUXInterfaceBottomDialogFragment.isAdded) {
                addUIUXInterfaceBottomDialogFragment.show(
                    supportFragmentManager,
                    "addDarkModeBottomDialogFragment"
                )
            }
        }

        binding.activityAppSettingsSectionPreferredEmailClient.setOnLayoutClickedListener {
            val dialog = PreferredEmailClientBottomDialogFragment()
            dialog.show(
                supportFragmentManager,
                "PreferredEmailClientBottomDialogFragment"
            )
        }

        binding.activityAppSettingsSectionFeatures.setOnLayoutClickedListener {
            val intent = Intent(this@AppSettingsActivity, AppSettingsFeaturesActivity::class.java)
            startActivity(intent)
        }

        binding.activityAppSettingsSectionPrivacy.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityAppSettingsSectionPrivacy.setSwitchChecked(!binding.activityAppSettingsSectionPrivacy.getSwitchChecked())
        }

        binding.activityAppSettingsSectionWearos.setOnLayoutClickedListener {
            val intent = Intent(this@AppSettingsActivity, AppSettingsWearOSActivity::class.java)
            startActivity(intent)
        }

        binding.activityAppSettingsSectionBackgroundService.setOnLayoutClickedListener {
            if (!addBackgroundServiceIntervalBottomDialogFragment.isAdded) {
                addBackgroundServiceIntervalBottomDialogFragment.show(
                    supportFragmentManager,
                    "addBackgroundServiceIntervalBottomDialogFragment"
                )
            }
        }

        binding.activityAppSettingsSectionFaq.setOnLayoutClickedListener {
            val url = "https://addy.io/faq/"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = url.toUri()
            startActivity(i)
        }


        binding.activityAppSettingsSectionHelp.setOnLayoutClickedListener {
            val url = "https://addy.io/help/"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = url.toUri()
            startActivity(i)
        }


        binding.activityAppSettingsSectionGithub.setOnLayoutClickedListener {
            val url = "https://github.com/anonaddy/addy-android"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = url.toUri()
            startActivity(i)
        }


        binding.activityAppSettingsSectionReportIssue.setOnLayoutClickedListener {
            val url = "https://github.com/anonaddy/addy-android/issues/new"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = url.toUri()
            startActivity(i)
        }

        binding.activityAppSettingsStjinLogo.setOnClickListener {
            val url = "https://stjin.host"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = url.toUri()
            startActivity(i)
        }



        binding.activityAppSettingsSectionLogs.setOnLayoutClickedListener {
            val intent = Intent(this@AppSettingsActivity, LogViewerActivity::class.java)
            intent.putExtra("logfile", LoggingHelper.LOGFILES.DEFAULT.filename)
            startActivity(intent)
        }

        binding.activityAppSettingsSectionReset.setOnLayoutClickedListener { resetApp() }

        binding.activityAppSettingsSectionDeleteAccount.setOnLayoutClickedListener {
            if (!deleteAccountConfirmationBottomDialogFragment.isAdded) {
                deleteAccountConfirmationBottomDialogFragment.show(
                    supportFragmentManager,
                    "deleteAccountConfirmationBottomDialogFragment"
                )
            }
        }


        binding.activityAppSettingsSectionUpdater.setOnLayoutClickedListener {
            val intent = Intent(this@AppSettingsActivity, AppSettingsUpdateActivity::class.java)
            startActivity(intent)
        }

        binding.activityAppSettingsSectionBackup.setOnLayoutClickedListener {
            val intent = Intent(this@AppSettingsActivity, AppSettingsBackupActivity::class.java)
            startActivity(intent)
        }

        binding.activityAppSettingsSectionNotificationPermission.setOnLayoutClickedListener { requestNotificationPermissions() }


        binding.activityAppSettingsSectionReview.setOnLayoutClickedListener {
            val url = "https://play.google.com/store/apps/details?id=host.stjin.anonaddy"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = url.toUri()
            this@AppSettingsActivity.startActivity(i)
        }

    }

    override fun onDarkModeOff() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        settingsManager.putSettingsInt(SettingsManager.PREFS.DARK_MODE, 0)
        delegate.applyDayNight()
    }

    override fun onDarkModeOn() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        settingsManager.putSettingsInt(SettingsManager.PREFS.DARK_MODE, 1)
        delegate.applyDayNight()
    }

    override fun onDarkModeAutomatic() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        settingsManager.putSettingsInt(SettingsManager.PREFS.DARK_MODE, -1)
        delegate.applyDayNight()
    }

    override fun onApplyDynamicColors() {
        recreate()
    }

    override fun setInterval(minutes: Int) {
        settingsManager.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL, minutes)

        // Schedule the background worker (this will cancel if already scheduled)
        BackgroundWorkerHelper(this).scheduleBackgroundWorker()
        addBackgroundServiceIntervalBottomDialogFragment.dismissAllowingStateLoss()
    }

    override fun onPreferredEmailClientSelected(packageName: String?, appName: String) {
        binding.activityAppSettingsSectionPreferredEmailClient.setDescription(
            if (packageName.isNullOrEmpty()) resources.getString(R.string.always_ask) else appName
        )
    }

    private fun checkForVariant() {
        if (BuildConfig.FLAVOR == "gplay") {
            binding.activityAppSettingsSectionReview.visibility = View.VISIBLE
        }
    }

    private fun checkPermissions() {
        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Notification permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !notificationManager.areNotificationsEnabled()) {
            binding.activityAppSettingsSectionNotificationPermission.visibility = View.VISIBLE
        } else {
            binding.activityAppSettingsSectionNotificationPermission.visibility = View.GONE
        }
    }

    private fun checkForUpdates() {
        lifecycleScope.launch {
            if (settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_UPDATES)) {
                val updateInfo = Updater.isUpdateAvailable()
                binding.activityAppSettingsSectionUpdater.setSectionAlert(updateInfo.isServerNewer)
                if (updateInfo.isServerNewer) {
                    binding.activityAppSettingsSectionUpdater.setTitle(this@AppSettingsActivity.resources.getString(R.string.new_update_available))
                }
            }
        }
    }

    private fun loadSettings() {
        binding.activityAppSettingsSectionSecurity.setSwitchChecked(encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED))
        binding.activityAppSettingsSectionLogs.setSwitchChecked(settingsManager.getSettingsBool(SettingsManager.PREFS.STORE_LOGS))
        binding.activityAppSettingsSectionPrivacy.setSwitchChecked(encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.PRIVACY_MODE))

        val preferredPackage = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.DEFAULT_EMAIL_CLIENT)
        if (preferredPackage.isNullOrEmpty()) {
            binding.activityAppSettingsSectionPreferredEmailClient.setDescription(resources.getString(R.string.always_ask))
        } else {
            val appName = AnonAddyUtils.getAppNameFromPackage(this, preferredPackage)
            if (appName != null) {
                binding.activityAppSettingsSectionPreferredEmailClient.setDescription(appName)
            } else {
                // If the app was uninstalled, reset to Always ask
                encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.DEFAULT_EMAIL_CLIENT, "")
                binding.activityAppSettingsSectionPreferredEmailClient.setDescription(resources.getString(R.string.always_ask))
            }
        }
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsSectionLogs.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            if (compoundButton.isPressed) {
                settingsManager.putSettingsBool(SettingsManager.PREFS.STORE_LOGS, checked)
            }
        }
        binding.activityAppSettingsSectionPrivacy.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            if (compoundButton.isPressed || forceSwitch) {
                encryptedSettingsManager.putSettingsBool(SettingsManager.PREFS.PRIVACY_MODE, checked)

                if (checked) {
                    // If privacy mode enabled, remove all shortcuts
                    ShortcutManagerCompat.removeAllDynamicShortcuts(this@AppSettingsActivity)
                }

                // Schedule the background worker to update widgets (this will cancel if already scheduled)
                BackgroundWorkerHelper(this@AppSettingsActivity).scheduleBackgroundWorker()

            }
        }
    }

    private fun setOnBiometricSwitchListeners() {
        binding.activityAppSettingsSectionSecurity.setLayoutEnabled(false)

        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                binding.activityAppSettingsSectionSecurity.setDescription(resources.getString(R.string.security_desc))

                binding.activityAppSettingsSectionSecurity.setLayoutEnabled(true)


                binding.activityAppSettingsSectionSecurity.setOnLayoutClickedListener {
                    forceSwitch = true
                    binding.activityAppSettingsSectionSecurity.setSwitchChecked(!binding.activityAppSettingsSectionSecurity.getSwitchChecked())
                }
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                binding.activityAppSettingsSectionSecurity.setDescription(
                    resources.getString(R.string.biometric_error_no_hardware)
                )

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                binding.activityAppSettingsSectionSecurity.setDescription(
                    resources.getString(R.string.biometric_error_hw_unavailable)
                )

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {

                binding.activityAppSettingsSectionSecurity.setDescription(
                    resources.getString(R.string.biometric_error_none_enrolled)
                )

                if (encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED)) {
                    // Biometrics is enabled but there is nothing enrolled.
                    encryptedSettingsManager.putSettingsBool(
                        SettingsManager.PREFS.BIOMETRIC_ENABLED,
                        false
                    )
                    SnackbarHelper.createSnackbar(
                        this,
                        this.resources.getString(R.string.biometric_error_hw_unavailable),
                        binding.activityAppSettingsCL
                    ).show()
                    loadSettings()
                }
            }

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                binding.activityAppSettingsSectionSecurity.setDescription(
                    resources.getString(R.string.biometric_error_hw_unavailable)
                )
            }

            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                binding.activityAppSettingsSectionSecurity.setDescription(
                    resources.getString(R.string.biometric_error_hw_unavailable)
                )
            }

            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                binding.activityAppSettingsSectionSecurity.setDescription(
                    resources.getString(R.string.biometric_error_hw_unavailable)
                )
            }
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)

                    SnackbarHelper.createSnackbar(
                        this@AppSettingsActivity, this@AppSettingsActivity.resources.getString(
                            R.string.authentication_error_s,
                            errString
                        ), binding.activityAppSettingsCL
                    ).show()

                    binding.activityAppSettingsSectionSecurity.setSwitchChecked(!shouldEnableBiometric)
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    binding.activityAppSettingsSectionSecurity.setSwitchChecked(shouldEnableBiometric)
                    encryptedSettingsManager.putSettingsBool(
                        SettingsManager.PREFS.BIOMETRIC_ENABLED,
                        shouldEnableBiometric
                    )
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    SnackbarHelper.createSnackbar(
                        this@AppSettingsActivity,
                        resources.getString(R.string.authentication_failed),
                        binding.activityAppSettingsCL
                    ).show()
                    binding.activityAppSettingsSectionSecurity.setSwitchChecked(!shouldEnableBiometric)
                }
            })


        binding.activityAppSettingsSectionSecurity.setOnSwitchCheckedChangedListener { compoundButton, checked -> // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
            if (compoundButton.isPressed || forceSwitch) {
                forceSwitch = false
                shouldEnableBiometric = checked
                val promptInfo = if (checked) {
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(resources.getString(R.string.enable_biometric_authentication))
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                        .build()
                } else {
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(resources.getString(R.string.disable_biometric_authentication))
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                        .build()
                }

                biometricPrompt.authenticate(promptInfo)
            }
        }
    }

    private fun requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if notification permissions are granted
            if (PermissionChecker.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PermissionChecker.PERMISSION_GRANTED) {
                notificationPermissionsResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            openNotificationSettings()
        }
    }

    private fun openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, this.packageName)
            startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
            }
            startActivity(intent)
        }
    }

    private fun resetApp() {
        MaterialDialogHelper.showMaterialDialog(
            context = this,
            title = resources.getString(R.string.reset_app),
            message = resources.getString(R.string.reset_app_confirmation_desc),
            icon = R.drawable.ic_loader,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.reset_app),
            positiveButtonAction = {

                try {
                    Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
                        if (nodes.any()) {
                            lifecycleScope.launch {
                                resetAppOnAllWearables { _ ->
                                    logoutAndReset()
                                }
                            }
                        } else {
                            logoutAndReset()
                        }
                    }.addOnFailureListener {
                        logoutAndReset()
                    }
                } catch (_: Exception) {
                    // Expected crash, the gplayless version will return null as connectedNodes
                    logoutAndReset()
                }
            }
        ).show()
    }

    private fun logoutAndReset() {

        lifecycleScope.launch {
            val result = ServiceLocator.userRepository.logout()
            if (result is NetworkResult.Success) {
                (getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
            } else {
                MaterialDialogHelper.showMaterialDialog(
                    context = this@AppSettingsActivity,
                    title = resources.getString(R.string.reset_app),
                    message = resources.getString(R.string.reset_app_logout_failure),
                    icon = R.drawable.ic_loader,
                    neutralButtonText = resources.getString(R.string.cancel),
                    positiveButtonText = resources.getString(R.string.reset_app_anyways),
                    positiveButtonAction = {
                        (getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                    }
                ).show()
            }
        }

    }

    private fun resetAppOnAllWearables(callback: (Boolean) -> Unit) {
        val nodeClient = Wearable.getNodeClient(this)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.any()) {
                nodeClient.localNode.addOnSuccessListener { localNode ->
                    for (node in nodes) {
                        Wearable.getMessageClient(this).sendMessage(
                            node.id,
                            "/reset",
                            localNode.displayName.toByteArray()
                        )
                    }
                    callback(true)
                }.addOnFailureListener {
                    callback(false)
                }.addOnCanceledListener {
                    callback(false)
                }
            } else {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }.addOnCanceledListener {
            callback(false)
        }
    }

    private fun setVersion() {
        binding.activityAppSettingsVersion.text = BuildConfig.VERSION_NAME
    }
}
