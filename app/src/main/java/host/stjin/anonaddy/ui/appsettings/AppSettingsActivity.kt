package host.stjin.anonaddy.ui.appsettings

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.*
import host.stjin.anonaddy.databinding.ActivityAppSettingsBinding
import host.stjin.anonaddy.databinding.AnonaddyCustomDialogBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.appsettings.backup.AppSettingsBackupActivity
import host.stjin.anonaddy.ui.appsettings.features.AppSettingsFeaturesActivity
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.appsettings.update.AppSettingsUpdateActivity
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.AttributeHelper
import host.stjin.anonaddy.utils.LoggingHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import kotlinx.coroutines.launch

class AppSettingsActivity : BaseActivity(),
    AppearanceBottomDialogFragment.AddAppearanceBottomDialogListener,
    BackgroundServiceIntervalBottomDialogFragment.AddBackgroundServiceIntervalBottomDialogListener {

    private val addAppearanceBottomDialogFragment: AppearanceBottomDialogFragment =
        AppearanceBottomDialogFragment.newInstance()

    private var addBackgroundServiceIntervalBottomDialogFragment: BackgroundServiceIntervalBottomDialogFragment =
        BackgroundServiceIntervalBottomDialogFragment.newInstance()


    private lateinit var settingsManager: SettingsManager
    private lateinit var encryptedSettingsManager: SettingsManager
    private var forceSwitch = false

    private lateinit var binding: ActivityAppSettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(view, binding.activityAppSettingsNSVLL)

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

        checkForUpdates()
    }

    private fun checkForUpdates() {
        lifecycleScope.launch {
            Updater.isUpdateAvailable({ updateAvailable: Boolean, _: String?, _: Boolean ->
                binding.activityAppSettingsSectionUpdater.setSectionAlert(updateAvailable)
                if (updateAvailable) {
                    binding.activityAppSettingsSectionUpdater.setTitle(this@AppSettingsActivity.resources.getString(R.string.new_update_available))
                }
            }, this@AppSettingsActivity)
        }
    }

    private fun loadSettings() {
        binding.activityAppSettingsSectionSecurity.setSwitchChecked(encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED))
        binding.activityAppSettingsSectionLogs.setSwitchChecked(settingsManager.getSettingsBool(SettingsManager.PREFS.STORE_LOGS))
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsSectionLogs.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed) {
                    settingsManager.putSettingsBool(SettingsManager.PREFS.STORE_LOGS, checked)
                }
            }
        })
    }

    // If the user comes back from eg. settings re-check + enable biometricswitch
    override fun onResume() {
        super.onResume()
        setOnBiometricSwitchListeners()
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
                val url = "https://anonaddy.com/faq/"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })


        binding.activityAppSettingsSectionHelp.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val url = "https://anonaddy.com/help/"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })


        binding.activityAppSettingsSectionGitlab.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val url = "https://gitlab.com/Stjin/anonaddy-android"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })


        binding.activityAppSettingsSectionReportIssue.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val url = "https://gitlab.com/Stjin/anonaddy-android/-/issues/new"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })


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

    }

    private fun resetApp() {
        // create an alert builder
        val anonaddyCustomDialogBinding = AnonaddyCustomDialogBinding.inflate(LayoutInflater.from(this), null, false)
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(anonaddyCustomDialogBinding.root)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        anonaddyCustomDialogBinding.dialogTitle.text = resources.getString(R.string.reset_app)
        anonaddyCustomDialogBinding.dialogText.text =
            resources.getString(R.string.reset_app_confirmation_desc)
        anonaddyCustomDialogBinding.dialogPositiveButton.text =
            resources.getString(R.string.reset_app)
        anonaddyCustomDialogBinding.dialogPositiveButton.drawableBackground.setColorFilter(
            AttributeHelper.getValueByAttr(this, R.attr.colorError),
            PorterDuff.Mode.SRC_ATOP
        )
        anonaddyCustomDialogBinding.dialogPositiveButton.setTextColor(AttributeHelper.getValueByAttr(this, R.attr.colorOnError))
        anonaddyCustomDialogBinding.dialogPositiveButton.spinningBarColor = AttributeHelper.getValueByAttr(this, R.attr.colorOnError)

        anonaddyCustomDialogBinding.dialogPositiveButton.setOnClickListener {
            (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
        }
        anonaddyCustomDialogBinding.dialogNegativeButton.setOnClickListener {
            dialog.dismiss()
        }
        // create and show the alert dialog
        dialog.show()
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
        addBackgroundServiceIntervalBottomDialogFragment.dismiss()
    }


}