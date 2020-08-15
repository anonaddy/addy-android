package host.stjin.anonaddy.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.appsettings.AppSettingsActivity
import kotlinx.android.synthetic.main.main_profile_select_dialog.*

class DialogActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_profile_select_dialog)
        val flags: Int =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        window.decorView.systemUiVisibility = flags

        (findViewById<View>(R.id.main_profile_select_dialog_card).parent as View).setOnClickListener { finishAfterTransition() }

        main_profile_select_dialog_app_settings.setOnClickListener {
            val intent = Intent(baseContext, AppSettingsActivity::class.java)
            startActivity(intent)
        }

    }
}