package host.stjin.anonaddy.ui.setup

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.ui.SplashActivity
import kotlinx.android.synthetic.main.activity_setup.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SetupActivity : BaseActivity(), AddApiBottomDialogFragment.AddApiBottomDialogListener {

    private val addApiBottomDialogFragment: AddApiBottomDialogFragment =
        AddApiBottomDialogFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
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
    }

    private fun setButtonClickListeners() {
        fragment_setup_init_button_api.setOnClickListener {

            /**
             * Check if there is a 999 length string in the clipboard (that's most likely the API key)
             */

            val clipboard: ClipboardManager? =
                baseContext?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipboardData = clipboard?.primaryClip
            val item = clipboardData?.getItemAt(0)
            val text = item?.text.toString()

            if (text.length == 999) {
                // a 999 length string found. This is most likely the API key
                verifyKeyAndAdd(baseContext, text)
            } else {
                if (!addApiBottomDialogFragment.isAdded) {
                    addApiBottomDialogFragment.show(
                        supportFragmentManager,
                        "addApiBottomDialogFragment"
                    )
                }
            }
        }

        fragment_setup_init_button_new.setOnClickListener {
            val intent = Intent(this, SetupNewActivity::class.java)
            startActivity(intent)
        }
    }

    private fun verifyKeyAndAdd(context: Context, apiKey: String) {
        fragment_setup_init_button_api.isEnabled = false
        fragment_setup_init_button_new.isEnabled = false
        fragment_setup_apikey_get_progressbar.visibility = View.VISIBLE

        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            verifyApiKey(context, apiKey)
        }
    }

    private suspend fun verifyApiKey(context: Context, apiKey: String) {
        val networkHelper = NetworkHelper(context)
        networkHelper.verifyApiKey(apiKey) { result ->
            if (result == "200") {
                addKey(apiKey)
            } else {
                fragment_setup_init_button_api.isEnabled = true
                fragment_setup_init_button_new.isEnabled = true
                fragment_setup_apikey_get_progressbar.visibility = View.INVISIBLE
                if (!addApiBottomDialogFragment.isAdded) {
                    addApiBottomDialogFragment.show(
                        supportFragmentManager,
                        "addApiBottomDialogFragment"
                    )
                }
            }
        }
    }

    private fun addKey(apiKey: String) {
        val settingsManager = SettingsManager(true, baseContext)
        settingsManager.putSettingsString(SettingsManager.PREFS.API_KEY, apiKey)
        val intent = Intent(baseContext, SplashActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setInsets() {
        fragment_setup_init_button_ll.doOnApplyWindowInsets { view, insets, padding ->
            // padding contains the original padding values after inflation
            view.updatePadding(
                bottom = padding.bottom + insets.systemWindowInsetBottom
            )
        }

        fragment_setup_hi_there.doOnApplyWindowInsets { view, insets, padding ->
            // padding contains the original padding values after inflation
            view.updatePadding(
                top = padding.top + insets.systemWindowInsetTop
            )
        }
    }

    override fun onClickSave(inputText: String?) {
        addApiBottomDialogFragment.dismiss()
        inputText?.let { addKey(it) }
    }

    override fun onClickGetMyKey() {
        val url = "https://app.anonaddy.com/settings"
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
    }

}