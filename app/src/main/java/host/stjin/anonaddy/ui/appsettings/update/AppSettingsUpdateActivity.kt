package host.stjin.anonaddy.ui.appsettings.update

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.*
import host.stjin.anonaddy.databinding.ActivityAppSettingsUpdateBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.YDGooglePlayUtils
import kotlinx.coroutines.launch


class AppSettingsUpdateActivity : BaseActivity() {

    private val addChangelogBottomDialogFragment: ChangelogBottomDialogFragment =
        ChangelogBottomDialogFragment.newInstance()

    private var forceSwitch = false
    private lateinit var settingsManager: SettingsManager
    private lateinit var binding: ActivityAppSettingsUpdateBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsUpdateBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(view, binding.appsettingsUpdateNSVLL)

        settingsManager = SettingsManager(false, this)

        setupToolbar(
            binding.appsettingsUpdateToolbar.customToolbarOneHandedMaterialtoolbar,
            R.string.anonaddy_updater,
            binding.appsettingsUpdateToolbar.customToolbarOneHandedImage,
            R.drawable.ic_settings_update
        )

        setVersionAndChannel()
        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
        checkForUpdates()

    }

    private fun checkForUpdates() {
        lifecycleScope.launch {
            Updater.isUpdateAvailable({ updateAvailable: Boolean, latestVersion: String? ->
                if (updateAvailable) {
                    binding.activityAppSettingsUpdateSectionDownload.setTitle(this@AppSettingsUpdateActivity.resources.getString(R.string.new_update_available))
                    binding.activityAppSettingsUpdateSectionDownload.setDescription(
                        this@AppSettingsUpdateActivity.resources.getString(
                            R.string.new_update_available_version,
                            BuildConfig.VERSION_NAME.substringBefore("|").trim(),
                            latestVersion
                        )
                    )
                } else {
                    binding.activityAppSettingsUpdateSectionDownload.setTitle(this@AppSettingsUpdateActivity.resources.getString(R.string.no_new_update_available))
                    binding.activityAppSettingsUpdateSectionDownload.setDescription(this@AppSettingsUpdateActivity.resources.getString(R.string.no_new_update_available_desc))
                }
                binding.activityAppSettingsUpdateSectionDownload.setSectionAlert(updateAvailable)
            }, this@AppSettingsUpdateActivity)
        }
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsUpdateSectionNotify.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    forceSwitch = false
                    settingsManager.putSettingsBool(SettingsManager.PREFS.NOTIFY_UPDATES, checked)

                    // Schedule the background worker (this will cancel if already scheduled)
                    BackgroundWorkerHelper(this@AppSettingsUpdateActivity).scheduleBackgroundWorker()
                }
            }
        })
    }

    private fun setVersionAndChannel() {
        val installerPackageName = YDGooglePlayUtils.getInstallerPackageName(this)
        val channel = if (installerPackageName != null) {
            YDGooglePlayUtils.getInstallerPackageName(this)?.let { YDGooglePlayUtils.getInstallerApplicationName(this, it) }
        } else {
            this.resources.getString(R.string.sideloaded)
        }
        binding.activityAppSettingsUpdateVersionChannel.text =
            this.resources.getString(R.string.version_channel_info, BuildConfig.VERSION_NAME, channel)

        if (YDGooglePlayUtils.isInstalledViaFDroid(this)) {
            binding.activityAppSettingsUpdateVersionChannel.text =
                "${binding.activityAppSettingsUpdateVersionChannel.text}\n\n${this.resources.getString(R.string.version_channel_fdroid_info)}"
        }

    }

    private fun loadSettings() {
        // Nothing to load
        binding.activityAppSettingsUpdateSectionNotify.setSwitchChecked(settingsManager.getSettingsBool(SettingsManager.PREFS.NOTIFY_UPDATES))
    }


    // If the user comes back from eg. settings re-check + enable biometricswitch
    override fun onResume() {
        super.onResume()
        loadSettings()
    }


    private fun setOnClickListeners() {
        binding.activityAppSettingsUpdateSectionNotify.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityAppSettingsUpdateSectionNotify.setSwitchChecked(!binding.activityAppSettingsUpdateSectionNotify.getSwitchChecked())
            }
        })

        binding.activityAppSettingsUpdateSectionChangelog.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!addChangelogBottomDialogFragment.isAdded) {
                    addChangelogBottomDialogFragment.show(
                        supportFragmentManager,
                        "addChangelogBottomDialogFragment"
                    )
                }
            }
        })

        binding.activityAppSettingsUpdateSectionDownload.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                downloadUpdate()
            }
        })

        binding.activityAppSettingsUpdateSectionPreviousChangelog.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val url = "https://gitlab.com/Stjin/anonaddy-android/-/blob/master/CHANGELOG.md"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })
    }

    private fun downloadUpdate() {
        val url = Updater.figureOutDownloadUrl(this)
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
    }


}