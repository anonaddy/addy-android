package host.stjin.anonaddy.ui.appsettings.update

import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.Updater
import host.stjin.anonaddy.databinding.ActivityAppSettingsUpdateBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy.utils.GooglePlayUtils
import host.stjin.anonaddy_shared.managers.SettingsManager
import kotlinx.coroutines.launch


class AppSettingsUpdateActivity : BaseActivity() {
    private var checkedForUpdates: Boolean = false

    private val addChangelogBottomDialogFragment: ChangelogBottomDialogFragment =

        ChangelogBottomDialogFragment.newInstance()

    private var forceSwitch = false

    private lateinit var settingsManager: SettingsManager

    private lateinit var binding: ActivityAppSettingsUpdateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsUpdateBinding.inflate(layoutInflater)
        InsetUtils.applyBottomInset(binding.appsettingsUpdateNSVLL)

        val view = binding.root
        setContentView(view)

        settingsManager = ServiceLocator.settingsManager

        setupToolbar(
            R.string.addyio_updater,
            binding.appsettingsUpdateNSV,
            binding.appsettingsUpdateToolbar,
            R.drawable.ic_settings_update
        )

        setVersionAndChannel()
        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
        checkForUpdates()

    }

    // If the user comes back from eg. settings re-check + enable biometricswitch
    override fun onResume() {
        super.onResume()
        loadSettings()
    }

    private fun setOnClickListeners() {
        binding.activityAppSettingsUpdateSectionNotify.setOnLayoutClickedListener {
            forceSwitch = true
            binding.activityAppSettingsUpdateSectionNotify.setSwitchChecked(!binding.activityAppSettingsUpdateSectionNotify.getSwitchChecked())
        }

        binding.activityAppSettingsUpdateSectionChangelog.setOnLayoutClickedListener {
            if (!addChangelogBottomDialogFragment.isAdded) {
                addChangelogBottomDialogFragment.show(
                    supportFragmentManager,
                    "addChangelogBottomDialogFragment"
                )
            }
        }

        binding.activityAppSettingsUpdateSectionDownload.setOnLayoutClickedListener {
            if (checkedForUpdates) {
                downloadUpdate()
            } else {
                checkForUpdates(forceCheck = true)
            }
        }

        binding.activityAppSettingsUpdateSectionPreviousChangelog.setOnLayoutClickedListener {
            val url = "https://github.com/anonaddy/addy-android/blob/master/CHANGELOG.md"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = url.toUri()
            startActivity(i)
        }
    }

    private fun checkForUpdates(forceCheck: Boolean = false) {
        val settingsManager = ServiceLocator.settingsManager
        if (settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_UPDATES) || forceCheck) {
            binding.activityAppSettingsUpdateSectionDownload.setTitle(this.resources.getString(R.string.obtaining_information))
            lifecycleScope.launch {
                val updateInfo = Updater.isUpdateAvailable()
                checkedForUpdates = true

                if (updateInfo.error == null) {
                    when {
                        updateInfo.isServerNewer -> {
                            binding.activityAppSettingsUpdateSectionDownload.setTitle(this@AppSettingsUpdateActivity.resources.getString(R.string.new_update_available))
                            binding.activityAppSettingsUpdateSectionDownload.setDescription(
                                this@AppSettingsUpdateActivity.resources.getString(
                                    R.string.new_update_available_version,
                                    BuildConfig.VERSION_NAME,
                                    updateInfo.serverVersion
                                )
                            )
                        }

                        updateInfo.isAppNewer -> {
                            binding.activityAppSettingsUpdateSectionDownload.setTitle(this@AppSettingsUpdateActivity.resources.getString(R.string.greetings_time_traveller))
                            binding.activityAppSettingsUpdateSectionDownload.setDescription(this@AppSettingsUpdateActivity.resources.getString(R.string.greetings_time_traveller_desc))
                            binding.activityAppSettingsUpdateSectionDownload.setImageResourceIcons(R.drawable.ic_infinity, null)
                        }

                        else -> {
                            binding.activityAppSettingsUpdateSectionDownload.setTitle(this@AppSettingsUpdateActivity.resources.getString(R.string.no_new_update_available))
                            binding.activityAppSettingsUpdateSectionDownload.setDescription(this@AppSettingsUpdateActivity.resources.getString(R.string.no_new_update_available_desc))
                        }
                    }
                    binding.activityAppSettingsUpdateSectionDownload.setSectionAlert(updateInfo.isServerNewer)
                } else {
                    binding.activityAppSettingsUpdateSectionDownload.setTitle(this@AppSettingsUpdateActivity.resources.getString(R.string.could_not_check_for_updates))

                    SnackbarHelper.createSnackbar(
                        this@AppSettingsUpdateActivity,
                        this@AppSettingsUpdateActivity.resources.getString(R.string.could_not_check_for_updates),
                        binding.appsettingsUpdateCL
                    ).show()
                }
            }
        }
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsUpdateSectionNotify.setOnSwitchCheckedChangedListener { compoundButton, checked ->
            // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
            if (compoundButton.isPressed || forceSwitch) {
                forceSwitch = false
                settingsManager.putSettingsBool(SettingsManager.PREFS.NOTIFY_UPDATES, checked)

                // Schedule the background worker (this will cancel if already scheduled)
                BackgroundWorkerHelper(this@AppSettingsUpdateActivity).scheduleBackgroundWorker()
            }
        }
    }

    private fun setVersionAndChannel() {
        val installerPackageName = GooglePlayUtils.getInstallerPackageName(this)
        val channel = if (installerPackageName != null) {
            GooglePlayUtils.getInstallerApplicationName(this, installerPackageName)
        } else {
            this.resources.getString(R.string.sideloaded)
        }
        val baseInfo = this.resources.getString(R.string.version_channel_info, BuildConfig.VERSION_NAME, channel)

        if (GooglePlayUtils.isInstalledViaFDroid(this)) {
            val fdroidInfo = this.resources.getString(R.string.version_channel_fdroid_info)
            binding.activityAppSettingsUpdateVersionChannel.text = String.format(java.util.Locale.getDefault(), "%s\n\n%s", baseInfo, fdroidInfo)
        } else {
            binding.activityAppSettingsUpdateVersionChannel.text = baseInfo
        }
    }

    private fun loadSettings() {
        // Nothing to load
        binding.activityAppSettingsUpdateSectionNotify.setSwitchChecked(settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_UPDATES))
    }

    private fun downloadUpdate() {
        val url = Updater.figureOutDownloadUrl(this)
        val i = Intent(Intent.ACTION_VIEW)
        i.data = url.toUri()
        startActivity(i)
    }
}
