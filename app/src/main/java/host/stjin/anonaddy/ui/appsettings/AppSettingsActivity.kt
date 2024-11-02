package host.stjin.anonaddy.ui.appsettings

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.CompoundButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.Wearable
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.Updater
import host.stjin.anonaddy.databinding.ActivityAppSettingsBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.appsettings.backup.AppSettingsBackupActivity
import host.stjin.anonaddy.ui.appsettings.features.AppSettingsFeaturesActivity
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.appsettings.update.AppSettingsUpdateActivity
import host.stjin.anonaddy.ui.appsettings.wearos.AppSettingsWearOSActivity
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.InsetUtil
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.ReviewHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


class AppSettingsActivity : BaseActivity(),
    AppearanceBottomDialogFragment.AddAppearanceBottomDialogListener,
    BackgroundServiceIntervalBottomDialogFragment.AddBackgroundServiceIntervalBottomDialogListener {

    private val addAppearanceBottomDialogFragment: AppearanceBottomDialogFragment =
        AppearanceBottomDialogFragment.newInstance()

    private var addBackgroundServiceIntervalBottomDialogFragment: BackgroundServiceIntervalBottomDialogFragment =
        BackgroundServiceIntervalBottomDialogFragment.newInstance()

    private val deleteAccountConfirmationBottomSheetDialog: DeleteAccountConfirmationBottomSheetDialog =
        DeleteAccountConfirmationBottomSheetDialog.newInstance()

    private lateinit var settingsManager: SettingsManager
    private lateinit var encryptedSettingsManager: SettingsManager
    private var forceSwitch = false

    private lateinit var binding: ActivityAppSettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        InsetUtil.applyBottomInset(binding.activityAppSettingsNSVLL)
        val view = binding.root
        setContentView(view)

        settingsManager = SettingsManager(false, this)
        encryptedSettingsManager = SettingsManager(true, this)
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

    private fun checkForVariant() {
        if (BuildConfig.FLAVOR == "gplay") {
            binding.activityAppSettingsSectionReview.visibility = View.VISIBLE
        }
    }


    private fun checkPermissions() {
        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Notification permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !notificationManager.areNotificationsEnabled()) {
            binding.activityAppSettingsSectionNotificationPermission.visibility = View.VISIBLE
        } else {
            binding.activityAppSettingsSectionNotificationPermission.visibility = View.GONE
        }
    }

    private fun checkForUpdates() {
        lifecycleScope.launch {
            val settingsManager = SettingsManager(false, this@AppSettingsActivity)
            if (settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_UPDATES)) {
                Updater.isUpdateAvailable({ updateAvailable: Boolean, _: String?, _: Boolean, _: String? ->
                    binding.activityAppSettingsSectionUpdater.setSectionAlert(updateAvailable)
                    if (updateAvailable) {
                        binding.activityAppSettingsSectionUpdater.setTitle(this@AppSettingsActivity.resources.getString(R.string.new_update_available))
                    }
                }, this@AppSettingsActivity)
            }
        }
    }

    private fun loadSettings() {
        binding.activityAppSettingsSectionSecurity.setSwitchChecked(encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED))
        binding.activityAppSettingsSectionLogs.setSwitchChecked(settingsManager.getSettingsBool(SettingsManager.PREFS.STORE_LOGS))
        binding.activityAppSettingsSectionPrivacy.setSwitchChecked(encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.PRIVACY_MODE))
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsSectionLogs.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed) {
                    settingsManager.putSettingsBool(SettingsManager.PREFS.STORE_LOGS, checked)
                }
            }
        })
        binding.activityAppSettingsSectionPrivacy.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
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
        })
    }

    // If the user comes back from eg. settings re-check + enable biometricswitch
    override fun onResume() {
        super.onResume()
        setOnBiometricSwitchListeners()
        checkPermissions() // When the user allows permissions through the system settings app, this value needs to be updated when coming back
        loadSettings()
    }


    private var shouldEnableBiometric = true
    private fun setOnBiometricSwitchListeners() {
        binding.activityAppSettingsSectionSecurity.setLayoutEnabled(false)

        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                binding.activityAppSettingsSectionSecurity.setDescription(resources.getString(R.string.security_desc))

                binding.activityAppSettingsSectionSecurity.setLayoutEnabled(true)


                binding.activityAppSettingsSectionSecurity.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
                    override fun onClick() {
                        forceSwitch = true
                        binding.activityAppSettingsSectionSecurity.setSwitchChecked(!binding.activityAppSettingsSectionSecurity.getSwitchChecked())
                    }
                })
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
        val biometricPrompt = BiometricPrompt(this, executor,
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


        binding.activityAppSettingsSectionSecurity.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
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
        })
    }

    private fun setOnClickListeners() {
        binding.activityAppSettingsSectionAppTheme.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!addAppearanceBottomDialogFragment.isAdded) {
                    addAppearanceBottomDialogFragment.show(
                        supportFragmentManager,
                        "addDarkModeBottomDialogFragment"
                    )
                }
            }
        })

        binding.activityAppSettingsSectionFeatures.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsActivity, AppSettingsFeaturesActivity::class.java)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsSectionPrivacy.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityAppSettingsSectionPrivacy.setSwitchChecked(!binding.activityAppSettingsSectionPrivacy.getSwitchChecked())
            }
        })

        binding.activityAppSettingsSectionWearos.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsActivity, AppSettingsWearOSActivity::class.java)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsSectionBackgroundService.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!addBackgroundServiceIntervalBottomDialogFragment.isAdded) {
                    addBackgroundServiceIntervalBottomDialogFragment.show(
                        supportFragmentManager,
                        "addBackgroundServiceIntervalBottomDialogFragment"
                    )
                }
            }
        })

        binding.activityAppSettingsSectionFaq.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val url = "https://addy.io/faq/"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })


        binding.activityAppSettingsSectionHelp.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val url = "https://addy.io/help/"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })


        binding.activityAppSettingsSectionGithub.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val url = "https://github.com/anonaddy/addy-android"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })


        binding.activityAppSettingsSectionReportIssue.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val url = "https://github.com/anonaddy/addy-android/issues/new"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })

        binding.activityAppSettingsStjinLogo.setOnClickListener {
            val url = "https://stjin.host"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }



        binding.activityAppSettingsSectionLogs.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsActivity, LogViewerActivity::class.java)
                intent.putExtra("logfile", LoggingHelper.LOGFILES.DEFAULT.filename)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsSectionReset.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                resetApp()
            }
        })

        binding.activityAppSettingsSectionDeleteAccount.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!deleteAccountConfirmationBottomSheetDialog.isAdded) {
                    deleteAccountConfirmationBottomSheetDialog.show(
                        supportFragmentManager,
                        "deleteAccountConfirmationBottomSheetDialog"
                    )
                }
            }

        })


        binding.activityAppSettingsSectionUpdater.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsActivity, AppSettingsUpdateActivity::class.java)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsSectionBackup.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsActivity, AppSettingsBackupActivity::class.java)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsSectionNotificationPermission.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            @RequiresApi(33)
            override fun onClick() {
                requestNotificationPermissions()
            }

        })


         binding.activityAppSettingsSectionReview.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                ReviewHelper().launchReviewFlow(this@AppSettingsActivity)
            }
        })



    }


    @RequiresApi(Build.VERSION_CODES.O)
    private var notificationPermissionsResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        when (result) {
            true -> checkPermissions()
            false -> {
                val intent: Intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, this.packageName)
                startActivity(intent)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermissions() {
        // Check if notification permissions are granted
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PermissionChecker.PERMISSION_GRANTED) {
            notificationPermissionsResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                } catch (e: NullPointerException) {
                    // Expected crash, the gplayless version will return null as connectedNodes
                    logoutAndReset()
                }
            }
        ).show()
    }

    private fun logoutAndReset(){

        lifecycleScope.launch {
            NetworkHelper(this@AppSettingsActivity).logout { result: String? ->
                if (result == "204") {
                    (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                } else {
                    MaterialDialogHelper.showMaterialDialog(
                        context = this@AppSettingsActivity,
                        title = resources.getString(R.string.reset_app),
                        message = resources.getString(R.string.reset_app_logout_failure),
                        icon = R.drawable.ic_loader,
                        neutralButtonText = resources.getString(R.string.cancel),
                        positiveButtonText = resources.getString(R.string.reset_app_anyways),
                        positiveButtonAction = {
                            (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                        }
                    ).show()
                }
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
        binding.activityAppSettingsSectionAppTheme.setDescription(this.resources.getString(R.string.restart_app_required))
        binding.activityAppSettingsSectionAppTheme.setSectionAlert(true)
    }


    override fun setInterval(minutes: Int) {
        settingsManager.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL, minutes)

        // Schedule the background worker (this will cancel if already scheduled)
        BackgroundWorkerHelper(this).scheduleBackgroundWorker()
        addBackgroundServiceIntervalBottomDialogFragment.dismissAllowingStateLoss()
    }


}