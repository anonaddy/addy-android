package host.stjin.anonaddy.ui.setup

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.updatePadding
import host.stjin.anonaddy.*
import host.stjin.anonaddy.databinding.ActivitySetupBinding
import host.stjin.anonaddy.ui.SplashActivity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils

class SetupActivity : BaseActivity(), AddApiBottomDialogFragment.AddApiBottomDialogListener {

    private val addApiBottomDialogFragment: AddApiBottomDialogFragment =
        AddApiBottomDialogFragment.newInstance()

    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        window.decorView.systemUiVisibility =
                // Tells the system that the window wishes the content to
                // be laid out at the most extreme scenario. See the docs for
                // more information on the specifics
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    // Tells the system that the window wishes the content to
                    // be laid out as if the navigation bar was hidden
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION


        setInsets()
        setButtonClickListeners()
        checkForIntents()
    }

    private fun checkForIntents() {
        if (intent.action != null) {
            // /deactivate URI's
            val data: Uri? = intent?.data
            if (data.toString().contains("/setup")) {
                // Reset app data in case app is already setup
                //clearAllData() will automatically elevate to encrypt=true
                SettingsManager(false, this).clearAllData()

                val hostname = StringUtils.substringBefore(data.toString(), "/setup/")
                val apiKey = StringUtils.substringAfter(data.toString(), "/setup/")
                verifyKeyAndAdd(this, apiKey, hostname)
                Toast.makeText(this, resources.getString(R.string.API_key_received_from_intent), Toast.LENGTH_LONG).show()
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

            if (text.length == 999) {
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
    }

    private fun verifyKeyAndAdd(context: Context, apiKey: String, baseUrl: String = AnonAddy.API_BASE_URL) {
        binding.fragmentSetupInitButtonApi.isEnabled = false
        binding.fragmentSetupInitButtonNew.isEnabled = false
        binding.fragmentSetupApikeyGetProgressbar.visibility = View.VISIBLE

        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
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

                binding.fragmentSetupInitButtonApi.isEnabled = true
                binding.fragmentSetupInitButtonNew.isEnabled = true
                binding.fragmentSetupApikeyGetProgressbar.visibility = View.INVISIBLE
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

    private fun setInsets() {
        binding.fragmentSetupInitButtonLl.doOnApplyWindowInsets { view, insets, padding ->
            // padding contains the original padding values after inflation
            view.updatePadding(
                bottom = padding.bottom + insets.systemWindowInsetBottom
            )
        }

        binding.fragmentSetupHiThere.doOnApplyWindowInsets { view, insets, padding ->
            // padding contains the original padding values after inflation
            view.updatePadding(
                top = padding.top + insets.systemWindowInsetTop
            )
        }
    }

    override fun onClickSave(baseUrl: String, apiKey: String) {
        addApiBottomDialogFragment.dismiss()
        addKey(baseUrl, apiKey)
    }

    override fun onClickGetMyKey(baseUrl: String) {
        val url = "$baseUrl/settings"
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
    }
}