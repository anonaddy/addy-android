package host.stjin.anonaddy

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.ui.setup.SetupActivity
import kotlinx.android.synthetic.main.activity_main_failed.*
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Open setup
        if (settingsManager.getSettingsString("API_KEY") == null) {
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
            finish()
            return
        } else {
            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                loadUserResourceIntoMemory()
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
        val networkHelper = NetworkHelper(this)

        networkHelper.getUserResource { user ->
            if (user != null) {
                User.userResource = user
                val intent = Intent(baseContext, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                setContentView(R.layout.activity_main_failed)
                activity_main_failed_retry_button.setOnClickListener {
                    val intent = Intent(baseContext, SplashActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }

    }

}