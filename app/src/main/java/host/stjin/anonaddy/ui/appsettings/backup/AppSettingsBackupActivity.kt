package host.stjin.anonaddy.ui.appsettings.backup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.CompoundButton
import androidx.activity.result.contract.ActivityResultContracts
import host.stjin.anonaddy.*
import host.stjin.anonaddy.databinding.ActivityAppSettingsBackupBinding
import host.stjin.anonaddy.models.LOGIMPORTANCE
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.service.BackupHelper
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.LoggingHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import org.ocpsoft.prettytime.PrettyTime
import java.util.*


class AppSettingsBackupActivity : BaseActivity(),
    BackupSetPasswordBottomDialogFragment.AddBackupPasswordBottomDialogListener {

    private val backupSetPasswordBottomDialogFragment: BackupSetPasswordBottomDialogFragment =
        BackupSetPasswordBottomDialogFragment.newInstance()

    private var forceSwitch = false
    private lateinit var settingsManager: SettingsManager
    private lateinit var encryptedSettingsManager: SettingsManager
    private lateinit var binding: ActivityAppSettingsBackupBinding
    private lateinit var backupHelper: BackupHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsBackupBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        settingsManager = SettingsManager(false, this)
        encryptedSettingsManager = SettingsManager(true, this)
        backupHelper = BackupHelper(this)
        setupToolbar(binding.appsettingsBackupToolbar.customToolbarOneHandedMaterialtoolbar, R.string.anonaddy_backup)

        // loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
    }

    private fun figureOutLastBackup() {
        val lastBackup = backupHelper.getLatestBackupDate()

        if (lastBackup == null) {
            binding.activityAppSettingsBackupSectionBackupNow.setDescription(
                this.resources.getString(
                    R.string.last_backup_desc,
                    this.resources.getString(R.string.never)
                )
            )
        } else {
            binding.activityAppSettingsBackupSectionBackupNow.setDescription(
                this.resources.getString(
                    R.string.last_backup_desc,
                    PrettyTime().format(Date(lastBackup))
                )
            )
        }
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsBackupSectionPeriodicBackups.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    forceSwitch = false
                    settingsManager.putSettingsBool(SettingsManager.PREFS.PERIODIC_BACKUPS, checked)

                    // Schedule the background worker (this will cancel if already scheduled)
                    BackgroundWorkerHelper(this@AppSettingsBackupActivity).scheduleBackgroundWorker()
                }
            }
        })
    }


    private fun loadSettings() {
        // Nothing to load
        binding.activityAppSettingsBackupSectionPeriodicBackups.setSwitchChecked(settingsManager.getSettingsBool(SettingsManager.PREFS.PERIODIC_BACKUPS))
        figureOutLastBackup()
        checkIfBackupLocationIsAccessible()
    }

    private fun checkIfBackupLocationIsAccessible() {
        binding.activityAppSettingsBackupSectionBackupLocation.setSectionAlert(!backupHelper.isBackupLocationAccessible())
        binding.activityAppSettingsBackupSectionBackupNow.setLayoutEnabled(backupHelper.isBackupLocationAccessible())
        if (!backupHelper.isBackupLocationAccessible()) {
            binding.activityAppSettingsBackupSectionBackupLocation.setDescription(this.resources.getString(R.string.backup_location_not_accessible))
        } else {
            binding.activityAppSettingsBackupSectionBackupLocation.setDescription(this.resources.getString(R.string.backup_location_desc))
        }
    }


    // If the user comes back from eg. settings re-check + enable biometricswitch
    override fun onResume() {
        super.onResume()
        loadSettings()
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            // Very important. Take persistable Uri permissions to make sure we can access this place later
            val sourceTreeUri: Uri = result.data?.data!!
            applicationContext.contentResolver
                .takePersistableUriPermission(sourceTreeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            result?.data?.also { uri ->
                uri.data?.toString()?.let {
                    settingsManager.putSettingsString(SettingsManager.PREFS.BACKUPS_LOCATION, it)
                    SnackbarHelper.createSnackbar(this, this.resources.getString(R.string.backup_location_set), binding.appsettingsBackupCL)
                        .show()
                    LoggingHelper(this, LoggingHelper.LOGFILES.BACKUP_LOGS).addLog(
                        LOGIMPORTANCE.WARNING.int,
                        this.resources.getString(R.string.log_backup_location_changed),
                        "resultLauncher",
                        null
                    )
                }
                // Perform operations on the document using its URI.
            }
        }
    }

    fun openDirectory() {
        // Choose a directory using the system's file picker.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
        resultLauncher.launch(intent)
    }


    private fun setOnClickListeners() {
        binding.activityAppSettingsBackupSectionPeriodicBackups.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityAppSettingsBackupSectionPeriodicBackups.setSwitchChecked(!binding.activityAppSettingsBackupSectionPeriodicBackups.getSwitchChecked())
                // Schedule the background worker (this will cancel if already scheduled)
                BackgroundWorkerHelper(this@AppSettingsBackupActivity).scheduleBackgroundWorker()

            }
        })

        binding.activityAppSettingsBackupSectionBackupNow.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (backupHelper.createBackup()) {
                    SnackbarHelper.createSnackbar(
                        this@AppSettingsBackupActivity,
                        this@AppSettingsBackupActivity.resources.getString(R.string.backup_completed),
                        binding.appsettingsBackupCL
                    ).show()
                    figureOutLastBackup()
                } else {
                    SnackbarHelper.createSnackbar(
                        this@AppSettingsBackupActivity,
                        this@AppSettingsBackupActivity.resources.getString(R.string.backup_failed),
                        binding.appsettingsBackupCL, LoggingHelper.LOGFILES.BACKUP_LOGS
                    ).show()
                }
            }
        })

        binding.activityAppSettingsBackupSectionBackupLocation.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                openDirectory()
            }
        })

        binding.activityAppSettingsBackupSectionBackupPassword.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!backupSetPasswordBottomDialogFragment.isAdded) {
                    backupSetPasswordBottomDialogFragment.show(
                        supportFragmentManager,
                        "backupSetPasswordBottomDialogFragment"
                    )
                }

            }
        })

        binding.activityAppSettingsBackupSectionBackupLog.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsBackupActivity, LogViewerActivity::class.java)
                intent.putExtra("logfile", LoggingHelper.LOGFILES.BACKUP_LOGS.filename)
                startActivity(intent)
            }
        })
    }

    override fun onSaved() {
        backupSetPasswordBottomDialogFragment.dismiss()
        SnackbarHelper.createSnackbar(
            this@AppSettingsBackupActivity,
            this@AppSettingsBackupActivity.resources.getString(R.string.backup_password_set),
            binding.appsettingsBackupCL
        ).show()
        LoggingHelper(this, LoggingHelper.LOGFILES.BACKUP_LOGS).addLog(
            LOGIMPORTANCE.WARNING.int,
            this.resources.getString(R.string.log_backup_password_changed),
            "onSaved",
            null
        )
    }


}