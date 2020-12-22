package host.stjin.anonaddy.ui.appsettings

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.customviews.SectionView
import kotlinx.android.synthetic.main.activity_app_settings.*
import kotlinx.android.synthetic.main.anonaddy_custom_dialog.view.*

class AppSettingsActivity : BaseActivity(),
    DarkModeBottomDialogFragment.AddDarkmodeBottomDialogListener,
    BackgroundServiceIntervalBottomDialogFragment.AddBackgroundServiceIntervalBottomDialogListener {

    private val addDarkModeBottomDialogFragment: DarkModeBottomDialogFragment =
        DarkModeBottomDialogFragment.newInstance()

    private val addChangelogBottomDialogFragment: ChangelogBottomDialogFragment =
        ChangelogBottomDialogFragment.newInstance()

    private val addBackgroundServiceIntervalBottomDialogFragment: BackgroundServiceIntervalBottomDialogFragment =
        BackgroundServiceIntervalBottomDialogFragment.newInstance()


    private lateinit var settingsManager: SettingsManager
    private lateinit var encryptedSettingsManager: SettingsManager
    private var forceSwitch = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)
        settingsManager = SettingsManager(false, this)
        encryptedSettingsManager = SettingsManager(true, this)
        setupToolbar(appsettings_toolbar)
        setVersion()
        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
        setOnBiometricSwitchListeners()
    }

    private fun loadSettings() {
        activity_app_settings_section_security.setSwitchChecked(encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED))
        activity_app_settings_section_logs.setSwitchChecked(settingsManager.getSettingsBool(SettingsManager.PREFS.STORE_LOGS))
    }

    private fun setOnSwitchListeners() {
        activity_app_settings_section_logs.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
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
        activity_app_settings_section_security.setLayoutEnabled(false)

        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                activity_app_settings_section_security.setDescription(resources.getString(R.string.security_desc))

                activity_app_settings_section_security.setLayoutEnabled(true)


                activity_app_settings_section_security.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
                    override fun onClick() {
                        forceSwitch = true
                        activity_app_settings_section_security.setSwitchChecked(!activity_app_settings_section_security.getSwitchChecked())
                    }
                })
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                activity_app_settings_section_security.setDescription(
                    resources.getString(R.string.biometric_error_no_hardware)
                )
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                activity_app_settings_section_security.setDescription(
                    resources.getString(R.string.biometric_error_hw_unavailable)
                )
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {

                activity_app_settings_section_security.setDescription(
                    resources.getString(R.string.biometric_error_none_enrolled)
                )

                if (encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED)) {
                    // Biometrics is enabled but there is nothing enrolled.
                    encryptedSettingsManager.putSettingsBool(
                        SettingsManager.PREFS.BIOMETRIC_ENABLED,
                        false
                    )
                    Snackbar.make(
                        findViewById(R.id.activity_app_settings_LL),
                        resources.getString(
                            R.string.biometric_error_hw_unavailable
                        ),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    loadSettings()
                }
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
                    Snackbar.make(
                        findViewById(R.id.activity_app_settings_LL),
                        this@AppSettingsActivity.resources.getString(
                            R.string.authentication_error_s,
                            errString
                        ),
                        Snackbar.LENGTH_SHORT
                    ).show()

                    activity_app_settings_section_security.setSwitchChecked(!shouldEnableBiometric)
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    activity_app_settings_section_security.setSwitchChecked(shouldEnableBiometric)
                    encryptedSettingsManager.putSettingsBool(
                        SettingsManager.PREFS.BIOMETRIC_ENABLED,
                        shouldEnableBiometric
                    )
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Snackbar.make(
                        findViewById(R.id.activity_app_settings_LL),
                        resources.getString(R.string.authentication_failed),
                        Snackbar.LENGTH_SHORT
                    ).show()

                    activity_app_settings_section_security.setSwitchChecked(!shouldEnableBiometric)
                }
            })


        activity_app_settings_section_security.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    forceSwitch = false
                    shouldEnableBiometric = checked
                    val promptInfo = if (checked) {
                        BiometricPrompt.PromptInfo.Builder()
                            .setTitle(resources.getString(R.string.enable_biometric_authentication))
                            .setDeviceCredentialAllowed(true)
                            .build()
                    } else {
                        BiometricPrompt.PromptInfo.Builder()
                            .setTitle(resources.getString(R.string.disable_biometric_authentication))
                            .setDeviceCredentialAllowed(true)
                            .build()
                    }

                    biometricPrompt.authenticate(promptInfo)
                }
            }
        })
    }

    private fun setOnClickListeners() {
        activity_app_settings_section_app_theme.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!addDarkModeBottomDialogFragment.isAdded) {
                    addDarkModeBottomDialogFragment.show(
                        supportFragmentManager,
                        "addDarkModeBottomDialogFragment"
                    )
                }
            }
        })

        activity_app_settings_section_changelog.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!addChangelogBottomDialogFragment.isAdded) {
                    addChangelogBottomDialogFragment.show(
                        supportFragmentManager,
                        "addChangelogBottomDialogFragment"
                    )
                }
            }
        })

        activity_app_settings_section_background_service.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!addBackgroundServiceIntervalBottomDialogFragment.isAdded) {
                    addBackgroundServiceIntervalBottomDialogFragment.show(
                        supportFragmentManager,
                        "addBackgroundServiceIntervalBottomDialogFragment"
                    )
                }
            }
        })

        activity_app_settings_section_faq.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val url = "https://anonaddy.com/faq/"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })


        activity_app_settings_section_help.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val url = "https://anonaddy.com/help/"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })


        activity_app_settings_section_gitlab.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val url = "https://gitlab.com/Stjin/anonaddy-android"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })


        activity_app_settings_section_report_issue.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val url = "https://gitlab.com/Stjin/anonaddy-android/-/issues/new"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })




        activity_app_settings_section_logs.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsActivity, LogViewerActivity::class.java)
                startActivity(intent)
            }
        })

        activity_app_settings_section_reset.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                resetApp()
            }
        })
    }

    private fun resetApp() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        // set the custom layout
        val customLayout =
            layoutInflater.inflate(R.layout.anonaddy_custom_dialog, null)
        builder.setView(customLayout)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        customLayout.dialog_title.text = resources.getString(R.string.reset_app)
        customLayout.dialog_text.text =
            resources.getString(R.string.reset_app_confirmation_desc)
        customLayout.dialog_positive_button.text =
            resources.getString(R.string.reset_app)
        customLayout.dialog_positive_button.setOnClickListener {
            (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
        }
        customLayout.dialog_negative_button.setOnClickListener {
            dialog.dismiss()
        }
        // create and show the alert dialog
        dialog.show()
    }


    private fun setVersion() {
        activity_app_settings_version.text = BuildConfig.VERSION_NAME
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

    override fun setInterval(minutes: Int) {
        settingsManager.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL, minutes)

        // Schedule the background worker (this will cancel if already scheduled)
        BackgroundWorkerHelper(this).scheduleBackgroundWorker()
        addBackgroundServiceIntervalBottomDialogFragment.dismiss()
    }


}