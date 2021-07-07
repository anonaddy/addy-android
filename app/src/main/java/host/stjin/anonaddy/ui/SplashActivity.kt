package host.stjin.anonaddy.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.*
import host.stjin.anonaddy.databinding.ActivityMainFailedBinding
import host.stjin.anonaddy.databinding.ActivitySplashBinding
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.models.UserResourceExtended
import host.stjin.anonaddy.ui.setup.SetupActivity
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity(), UnsupportedBottomDialogFragment.UnsupportedBottomDialogListener {


    lateinit var networkHelper: NetworkHelper

    private val unsupportedBottomDialogFragment: UnsupportedBottomDialogFragment =
        UnsupportedBottomDialogFragment.newInstance()

    private lateinit var binding: ActivitySplashBinding
    private lateinit var bindingFailed: ActivityMainFailedBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        val view = binding.root

        // Set dark mode on the splashactivity to prevent Main- and later activities from restarting and repeating calls
        checkForDarkModeAndSetFlags()
        setContentView(view)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setInsets()
        val settingsManager = SettingsManager(true, this)

        networkHelper = NetworkHelper(this)
        // Open setup
        if (settingsManager.getSettingsString(SettingsManager.PREFS.API_KEY) == null) {
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
            finish()
            return
        } else {
            lifecycleScope.launch {
                loadDataAndStartApp()
            }
        }
    }

    private suspend fun loadDataAndStartApp() {
        // The default instance at anonaddy.com does NOT return its version
        // However, assume that the creator of AnonAddy keeps the main version up-to-date :P
        // So set the versioncode to 9999 so it will always pass the min version check
        if (AnonAddy.API_BASE_URL == this.resources.getString(R.string.default_base_url)) {
            AnonAddy.VERSIONCODE = 9999
            AnonAddy.VERSIONSTRING = this.resources.getString(R.string.latest)

            lifecycleScope.launch {
                loadUserResourceIntoMemory()
            }
        } else {
            networkHelper.getAnonAddyInstanceVersion { version, error ->
                if (version != null) {
                    AnonAddy.VERSIONCODE = "${version.major}${version.minor}${version.patch}".toInt()
                    AnonAddy.VERSIONSTRING = version.version.toString()
                    //0.7.4 translates to 074 aka 074
                    if (AnonAddy.VERSIONCODE >= 74) {
                        lifecycleScope.launch {
                            loadUserResourceIntoMemory()
                        }
                    } else {
                        if (!unsupportedBottomDialogFragment.isAdded) {
                            unsupportedBottomDialogFragment.show(
                                supportFragmentManager,
                                "unsupportedBottomDialogFragment"
                            )
                        }
                    }
                } else {
                    showErrorScreen(error)
                }
            }
        }
    }

    private fun setInsets() {
        binding.activitySplashProgressbar.doOnApplyWindowInsets { view, insets, padding ->
            // padding contains the original padding values after inflation
            view.updatePadding(
                bottom = padding.bottom + insets.systemWindowInsetBottom
            )
        }
    }


    private suspend fun loadUserResourceIntoMemory() {
        networkHelper.getUserResource { user: UserResource?, error: String? ->
            if (user != null) {
                User.userResource = user
                lifecycleScope.launch {
                    getDefaultRecipientAddress(user.default_recipient_id)
                }
            } else {
                showErrorScreen(error)
            }
        }
    }

    private suspend fun getDefaultRecipientAddress(recipientId: String) {
        networkHelper.getSpecificRecipient({ recipient, error ->
            if (recipient != null) {
                User.userResourceExtended = UserResourceExtended(recipient.email)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                showErrorScreen(error)
            }
        }, recipientId)
    }

    private fun showErrorScreen(error: String?) {
        bindingFailed = ActivityMainFailedBinding.inflate(layoutInflater)
        val view = bindingFailed.root
        setContentView(view)

        bindingFailed.activityMainFailedErrorMessage.text = error
        bindingFailed.activityMainFailedRetryButton.setOnClickListener {
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onClickHowToUpdate() {
        unsupportedBottomDialogFragment.dismiss()
        val url = "https://github.com/anonaddy/anonaddy/blob/master/SELF-HOSTING.md#updating"
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
        finish()
    }

    override fun onClickIgnore() {
        unsupportedBottomDialogFragment.dismiss()
        lifecycleScope.launch {
            loadUserResourceIntoMemory()
        }
    }

}