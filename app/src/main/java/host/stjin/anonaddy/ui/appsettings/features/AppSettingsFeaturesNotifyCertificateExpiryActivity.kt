package host.stjin.anonaddy.ui.appsettings.features

import android.os.Bundle
import android.security.KeyChain
import android.security.KeyChainAliasCallback
import android.view.View
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAppSettingsFeaturesNotifyCertificateExpiryBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.InsetUtil
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.managers.SettingsManager.PREFS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ocpsoft.prettytime.PrettyTime


class AppSettingsFeaturesNotifyCertificateExpiryActivity : BaseActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var encryptedSettingsManager: SettingsManager
    private var forceSwitch = false
    private lateinit var networkHelper: NetworkHelper



    private lateinit var binding: ActivityAppSettingsFeaturesNotifyCertificateExpiryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsFeaturesNotifyCertificateExpiryBinding.inflate(layoutInflater)
        InsetUtil.applyBottomInset(binding.activityAppSettingsFeaturesNotifyCertificateExpiryNSVLL)

        val view = binding.root
        setContentView(view)

        settingsManager = SettingsManager(false, this)
        encryptedSettingsManager = SettingsManager(true, this)
        networkHelper = NetworkHelper(this)
        setupToolbar(
            R.string.feature_api_token_expiry_notification,
            binding.activityAppSettingsFeaturesNotifyCertificateExpiryNSV,
            binding.appsettingsFeaturesNotifyCertificateExpiryToolbar,
            R.drawable.ic_letters_case
        )

        checkCertificateExpiry()
        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
    }

    private fun checkCertificateExpiry() {
        val alias = encryptedSettingsManager.getSettingsString(PREFS.CERTIFICATE_ALIAS)
        if (alias != null) {
            binding.activityAppSettingsFeaturesNotifyCertificateExpiryCurrentCertificateExpiry.visibility = View.VISIBLE

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val chain = KeyChain.getCertificateChain(this@AppSettingsFeaturesNotifyCertificateExpiryActivity, alias)
                    val expiryDate = chain?.firstOrNull()?.notAfter
                    val text = PrettyTime().format(expiryDate)

                    withContext(Dispatchers.Main) {
                        binding.activityAppSettingsFeaturesNotifyCertificateExpiryCurrentCertificateExpiry.text =
                            this@AppSettingsFeaturesNotifyCertificateExpiryActivity.resources.getString(
                                R.string.certificate_expiry_date,
                                alias,
                                text
                            )
                    }
                }

            }

        } else {
            binding.activityAppSettingsFeaturesNotifyCertificateExpiryCurrentCertificateExpiry.visibility = View.GONE
        }
    }


    private fun loadSettings() {
        binding.activityAppSettingsFeaturesNotifyCertificateExpirySection.setSwitchChecked(
            settingsManager.getSettingsBool(PREFS.NOTIFY_CERTIFICATE_EXPIRY, true)
        )


        val alias = encryptedSettingsManager.getSettingsString(PREFS.CERTIFICATE_ALIAS)
        binding.activityAppSettingsFeaturesNotifyCertificateExpirySection.setLayoutEnabled(
            alias != null
        )
        binding.activityAppSettingsFeaturesNotifyCertificateExpiryRemoveCertificate.setLayoutEnabled(
            alias != null
        )
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsFeaturesNotifyCertificateExpirySection.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed || forceSwitch) {
                    settingsManager.putSettingsBool(PREFS.NOTIFY_CERTIFICATE_EXPIRY, checked)

                    // Since certificate expiry should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                    BackgroundWorkerHelper(this@AppSettingsFeaturesNotifyCertificateExpiryActivity).scheduleBackgroundWorker()
                }
            }
        })
    }

    // If the user comes back from eg. settings re-check + enable biometricswitch
    override fun onResume() {
        super.onResume()
        loadSettings()
        checkCertificateExpiry()
    }


    private fun setOnClickListeners() {
        binding.activityAppSettingsFeaturesNotifyCertificateExpirySection.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityAppSettingsFeaturesNotifyCertificateExpirySection.setSwitchChecked(!binding.activityAppSettingsFeaturesNotifyCertificateExpirySection.getSwitchChecked())
            }
        })
        binding.activityAppSettingsFeaturesNotifyCertificateExpiryChangeCertificate.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                selectCertificate()
            }
        })
        binding.activityAppSettingsFeaturesNotifyCertificateExpiryRemoveCertificate.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                deleteCertificatePrompt()
            }
        })
    }

    private fun deleteCertificatePrompt() {
        MaterialDialogHelper.showMaterialDialog(
            context = this,
            title = resources.getString(R.string.remove_certificate),
            message = resources.getString(R.string.remove_certificate_desc_confirm),
            icon = R.drawable.ic_certificate,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.remove),
            positiveButtonAction = {
                encryptedSettingsManager.removeSetting(PREFS.CERTIFICATE_ALIAS)
                settingsManager.putSettingsBool(PREFS.NOTIFY_CERTIFICATE_EXPIRY, false) // Disable by default when a certificate has been deleted

                // No need to call the backgroundHelper, it will see this is not necessary anymore next run. And we don't want to trigger API calls
                // immediately after removing the certificate, there might be a chance the user did this on accident or wants to select another one.

                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.certificate_removed),
                    binding.activityAppSettingsFeaturesNotifyCertificateExpiryCL,
                ).show()

                loadSettings()
                checkCertificateExpiry()
            }
        ).show()
    }

    private fun selectCertificate() {
        KeyChain.choosePrivateKeyAlias(this, object : KeyChainAliasCallback {
            override fun alias(alias: String?) {
                // If user denies access to the selected certificate
                if (alias == null) {
                    return
                }

                encryptedSettingsManager.putSettingsString(PREFS.CERTIFICATE_ALIAS, alias)
                settingsManager.putSettingsBool(PREFS.NOTIFY_CERTIFICATE_EXPIRY, true) // Enable by default when a certificate has been selected

                // Since certificate expiry should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(this@AppSettingsFeaturesNotifyCertificateExpiryActivity).scheduleBackgroundWorker()

                SnackbarHelper.createSnackbar(
                    this@AppSettingsFeaturesNotifyCertificateExpiryActivity,
                    this@AppSettingsFeaturesNotifyCertificateExpiryActivity.resources.getString(R.string.certificate_updated),
                    binding.activityAppSettingsFeaturesNotifyCertificateExpiryCL,
                ).show()

                loadSettings()
                checkCertificateExpiry()
            }
        }, null, null, null, null)
    }

}