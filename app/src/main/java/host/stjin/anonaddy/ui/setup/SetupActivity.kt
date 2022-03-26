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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.PermissionChecker
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivitySetupBinding
import host.stjin.anonaddy.ui.SplashActivity
import host.stjin.anonaddy_shared.AnonAddy
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
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
        setContentView(view)
        drawBehindNavBar(view, bottomViewsToShiftUpUsingPadding = arrayListOf(binding.fragmentSetupInitButtonLl))

        setButtonClickListeners()
        checkForIntents()

        requestNotificationPermissions()

        mainHandler = Handler(Looper.getMainLooper())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private var notificationPermissionsResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
    }

    // TODO replace this with a version
    private fun requestNotificationPermissions() {
        // Check if notification permissions are granted
        if (Build.VERSION.SDK_INT >= 33) {
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

    private fun addIntentApiKeyConfirmation(data: String) {
        val hostname = StringUtils.substringBefore(data, "/setup/")
        val apiKey = StringUtils.substringAfter(data, "/setup/")

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Catalog_MaterialAlertDialog_Centered_FullWidthButtons)
            .setTitle(resources.getString(R.string.setup_app))
            .setIcon(R.drawable.ic_letters_case)
            .setMessage(resources.getString(R.string.setup_intent_message, hostname, apiKey.takeLast(5)))
            .setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->
                finish()
            }
            .setPositiveButton(resources.getString(R.string.setup_app)) { _, _ ->
                // Reset app data in case app is already setup
                //clearAllData() will automatically elevate to encrypt=true
                SettingsManager(false, this).clearAllData()
                verifyKeyAndAdd(this, apiKey, hostname)
                Toast.makeText(this, resources.getString(R.string.API_key_received_from_intent), Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun getDummyAPIKey(): StringBuilder {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        val dummyApi = StringBuilder(binding.activitySetupApiTextview.text)
        dummyApi.setCharAt(Random.nextInt(binding.activitySetupApiTextview.length()), chars.random())
        return dummyApi
    }

    private fun checkForIntents() {
        if (intent.action != null) {
            // /deactivate URI's
            val data: Uri? = intent?.data
            if (data.toString().contains("/setup")) {
                addIntentApiKeyConfirmation(data.toString())
            }
        }
    }

    private fun setButtonClickListeners() {
        binding.fragmentSetupInitButtonApi.setOnClickListener {

            /**
             * Check if there is a 999 length string in the clipboard (that's most likely the API key)
             */

            val clipboard: ClipboardManager =
                this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipboardData = clipboard.primaryClip
            val item = clipboardData?.getItemAt(0)
            val text = item?.text.toString()

            // Most passport keys are 999 or 1024, as there are plans to move to Sanctum (which has 40char tokens) 40 will also trigger the clipboard readout.
            if (text.length == 999 || text.length == 1024 || text.length == 40) {
                // a 999 length string found. This is most likely the API key
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


    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
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

    private fun verifyKeyAndAdd(context: Context, apiKey: String, baseUrl: String = AnonAddy.API_BASE_URL) {
        binding.fragmentSetupInitButtonNew.isEnabled = false

        // Animate the button to progress
        binding.fragmentSetupInitButtonApi.startAnimation()

        lifecycleScope.launch {
            // AnonAddy.API_BASE_URL is defaulted to the anonaddy.com instance. If the API key is valid there it was meant to use that instance.
            // If the baseURL/API do not work or match it opens the API screen
            verifyApiKey(context, apiKey, baseUrl)
        }
    }

    private suspend fun verifyApiKey(context: Context, apiKey: String, baseUrl: String) {
        val networkHelper = NetworkHelper(context)
        networkHelper.verifyApiKey(baseUrl, apiKey) { result ->
            if (result == "200") {
                addKey(baseUrl, apiKey)
            } else {
                Toast.makeText(this, resources.getString(R.string.API_key_invalid), Toast.LENGTH_LONG).show()

                binding.fragmentSetupInitButtonNew.isEnabled = true

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

    private fun addKey(baseUrl: String, apiKey: String) {
        val settingsManager = SettingsManager(true, this)
        settingsManager.putSettingsString(SettingsManager.PREFS.API_KEY, apiKey)
        settingsManager.putSettingsString(SettingsManager.PREFS.BASE_URL, baseUrl)
        val intent = Intent(this, SplashActivity::class.java)
        startActivity(intent)
        finish()
    }


    override fun onClickSave(baseUrl: String, apiKey: String) {
        addApiBottomDialogFragment.dismissAllowingStateLoss()
        addKey(baseUrl, apiKey)
    }

    override fun onClickGetMyKey(baseUrl: String) {
        val url = "$baseUrl/settings"
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
    }

    override fun onBackupRestoreCompleted() {
        val intent = Intent(this, SplashActivity::class.java)
        startActivity(intent)
        finish()
    }
}