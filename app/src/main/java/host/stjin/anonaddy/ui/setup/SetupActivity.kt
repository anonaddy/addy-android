package host.stjin.anonaddy.ui.setup

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.PermissionChecker
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivitySetupBinding
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import kotlinx.coroutines.launch
import kotlin.random.Random

class SetupActivity : BaseActivity(), AddApiBottomDialogFragment.AddApiBottomDialogListener,
    BackupPasswordBottomDialogFragment.AddBackupPasswordBottomDialogListener {

    private val addApiBottomDialogFragment: AddApiBottomDialogFragment =
        AddApiBottomDialogFragment.newInstance()

    private lateinit var binding: ActivitySetupBinding
    lateinit var mainHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        val view = binding.root


        // First check if the user has set up the app before, if so just launch SplashActivity
        if (SettingsManager(true, this).getSettingsString(SettingsManager.PREFS.API_KEY) != null) {
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            setContentView(view)
            setButtonClickListeners()
            requestNotificationPermissions()
            mainHandler = Handler(Looper.getMainLooper())

            // Check for verification Urls
            if (intent.action != null) {
                // /deactivate URI's
                val data: Uri? = intent?.data
                if (data.toString().contains("/api/auth/verify")) {
                    val query = data?.query
                    if (query != null) {
                        binding.fragmentSetupInitButtonApi.startAnimation()
                        binding.fragmentSetupInitButtonNew.isEnabled = false
                        binding.fragmentSetupInitButtonRestoreBackup.isEnabled = false

                        lifecycleScope.launch {
                            finishRegistrationVerification(query)
                        }
                    }

                }
            }
        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private var notificationPermissionsResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
    }

    private fun requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (PermissionChecker.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PermissionChecker.PERMISSION_GRANTED) {
                notificationPermissionsResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(updateBackground)
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(updateBackground)
    }

    private val updateBackground = object : Runnable {
        override fun run() {
            binding.activitySetupApiTextview.text = getDummyAPIKey()
            mainHandler.postDelayed(this, Random.nextLong(300, 1500))
        }
    }

    private fun getDummyAPIKey(): StringBuilder {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        val dummyApi = StringBuilder(binding.activitySetupApiTextview.text)
        dummyApi.setCharAt(Random.nextInt(binding.activitySetupApiTextview.length()), chars.random())
        return dummyApi
    }

    private fun setButtonClickListeners() {
        binding.fragmentSetupInitButtonApi.setOnClickListener {

            /**
             * Check if there is a 40 length string in the clipboard (that's most likely the API key)
             */

            val clipboard: ClipboardManager =
                this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipboardData = clipboard.primaryClip
            val item = clipboardData?.getItemAt(0)
            val text = item?.text.toString()

            // Sanctum keys (which has 56char tokens) will trigger the clipboard readout.
            if (text.length == 56) {
                // a 56 length string found. This is most likely the API key
                verifyKeyAndAdd(this, text)
                Toast.makeText(this, resources.getString(R.string.API_key_copied_from_clipboard), Toast.LENGTH_LONG).show()
            } else {
                if (!addApiBottomDialogFragment.isAdded) {
                    addApiBottomDialogFragment.show(
                        supportFragmentManager,
                        "addApiBottomDialogFragment"
                    )
                }
            }
        }

        binding.fragmentSetupInitButtonNew.setOnClickListener {
            val intent = Intent(this, SetupNewActivity::class.java)
            startActivity(intent)
        }

        binding.fragmentSetupInitButtonRestoreBackup.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
            }

            resultLauncher.launch(intent)
        }
    }


    private var resultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                data?.data?.let {
                    val backupPasswordBottomDialogFragment: BackupPasswordBottomDialogFragment =
                        BackupPasswordBottomDialogFragment.newInstance(it)

                    if (!backupPasswordBottomDialogFragment.isAdded) {
                        backupPasswordBottomDialogFragment.show(
                            supportFragmentManager,
                        "backupPasswordBottomDialogFragment"
                    )
                }
            }
        }
    }

    private fun verifyKeyAndAdd(context: Context, apiKey: String, baseUrl: String = AddyIo.API_BASE_URL) {
        binding.fragmentSetupInitButtonNew.isEnabled = false
        binding.fragmentSetupInitButtonRestoreBackup.isEnabled = false

        // Animate the button to progress
        binding.fragmentSetupInitButtonApi.startAnimation()

        lifecycleScope.launch {
            // AddyIo.API_BASE_URL is defaulted to the addy.io instance. If the API key is valid there it was meant to use that instance.
            // If the baseURL/API do not work or match it opens the API screen
            verifyApiKey(context, apiKey, baseUrl)
        }
    }

    private suspend fun verifyApiKey(context: Context, apiKey: String, baseUrl: String) {
        val networkHelper = NetworkHelper(context)
        networkHelper.verifyApiKey(baseUrl, apiKey) { result, error ->
            if (result != null) {
                addKey(baseUrl, apiKey)
            } else {
                binding.fragmentSetupInitButtonNew.isEnabled = true
                binding.fragmentSetupInitButtonRestoreBackup.isEnabled = true

                // Revert the button to normal
                binding.fragmentSetupInitButtonApi.revertAnimation()

                if (!addApiBottomDialogFragment.isAdded) {
                    addApiBottomDialogFragment.show(
                        supportFragmentManager,
                        "addApiBottomDialogFragment"
                    )
                }
            }
        }
    }

    private suspend fun finishRegistrationVerification(query: String) {
        val networkHelper = NetworkHelper(this)
        networkHelper.verifyRegistration({ apiKey, error ->
            if (!apiKey.isNullOrEmpty()) {
                addKey(AddyIo.API_BASE_URL, apiKey)
            } else {
                binding.fragmentSetupInitButtonNew.isEnabled = true
                binding.fragmentSetupInitButtonRestoreBackup.isEnabled = true

                // Revert the button to normal
                binding.fragmentSetupInitButtonApi.revertAnimation()

                MaterialDialogHelper.showMaterialDialog(
                    context = this,
                    title = resources.getString(R.string.registration_register),
                    message = error,
                    icon = R.drawable.ic_key,
                    neutralButtonText = resources.getString(R.string.close)
                ).show()
            }
        }, query = query)
    }

    private fun addKey(baseUrl: String, apiKey: String) {
        val encryptedSettingsManager = SettingsManager(true, this)
        encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.API_KEY, apiKey)
        encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.BASE_URL, baseUrl)
        val intent = Intent(this, SplashActivity::class.java)
        startActivity(intent)
        finish()
    }


    override fun onClickSave(baseUrl: String, apiKey: String) {
        addApiBottomDialogFragment.dismissAllowingStateLoss()
        addKey(baseUrl, apiKey)
    }

    override fun onBackupRestoreCompleted() {
        val intent = Intent(this, SplashActivity::class.java)
        startActivity(intent)
        finish()
    }
}