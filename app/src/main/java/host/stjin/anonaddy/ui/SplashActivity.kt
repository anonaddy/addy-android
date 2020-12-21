package host.stjin.anonaddy.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import host.stjin.anonaddy.*
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.models.UserResourceExtended
import host.stjin.anonaddy.ui.setup.SetupActivity
import kotlinx.android.synthetic.main.activity_main_failed.*
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity(), UnsupportedBottomDialogFragment.UnsupportedBottomDialogListener {


    lateinit var networkHelper: NetworkHelper

    private val unsupportedBottomDialogFragment: UnsupportedBottomDialogFragment =
        UnsupportedBottomDialogFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set dark mode on the splashactivity to prevent Main- and later activities from restarting and repeating calls
        checkForDarkModeAndSetFlags()
        setContentView(R.layout.activity_splash)

        window.decorView.systemUiVisibility =
                // Tells the system that the window wishes the content to
                // be laid out at the most extreme scenario. See the docs for
                // more information on the specifics
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    // Tells the system that the window wishes the content to
                    // be laid out as if the navigation bar was hidden
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

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
            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
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

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                loadUserResourceIntoMemory()
            }
        } else {
            networkHelper.getAnonAddyInstanceVersion { version, error ->
                if (version != null) {
                    AnonAddy.VERSIONCODE = "${version.major}${version.minor}${version.patch}".toInt()
                    AnonAddy.VERSIONSTRING = version.version.toString()
                    //0.6.0 translates to 060 aka 60
                    if (AnonAddy.VERSIONCODE > 60) {
                        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
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
        activity_splash_progressbar.doOnApplyWindowInsets { view, insets, padding ->
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
                GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
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
        setContentView(R.layout.activity_main_failed)
        activity_main_failed_error_message.text = error
        activity_main_failed_retry_button.setOnClickListener {
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
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            loadUserResourceIntoMemory()
        }
    }

}