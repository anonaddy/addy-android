package host.stjin.anonaddy.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager

class DialogActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_profile_select_dialog)
        val flags: Int =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        window.decorView.systemUiVisibility = flags

        (findViewById<View>(R.id.main_profile_select_dialog_card).parent as View).setOnClickListener { v: View? -> finishAfterTransition() }
        findViewById<View>(R.id.main_profile_select_dialog_card).setOnClickListener {
            val settingsManager = SettingsManager(true, this)
            settingsManager.clearSettings()
        }
    }
}