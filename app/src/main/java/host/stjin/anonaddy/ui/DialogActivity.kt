package host.stjin.anonaddy.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import host.stjin.anonaddy.R
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.ui.anonaddysettings.AnonAddySettingsActivity
import host.stjin.anonaddy.ui.appsettings.AppSettingsActivity
import host.stjin.anonaddy.ui.domains.DomainSettingsActivity
import host.stjin.anonaddy.ui.usernames.UsernamesSettingsActivity
import kotlinx.android.synthetic.main.main_profile_select_dialog.*

class DialogActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_profile_select_dialog)
        val flags: Int =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        window.decorView.systemUiVisibility = flags

        (findViewById<View>(R.id.main_profile_select_dialog_card).parent as View).setOnClickListener { finishAfterTransition() }

        main_profile_select_dialog_card_accountname.text = User.userResource.username
        main_profile_select_dialog_card_subscription.text = resources.getString(R.string.subscription_user, User.userResource.subscription)

        main_profile_select_dialog_app_settings.setOnClickListener {
            val intent = Intent(baseContext, AppSettingsActivity::class.java)
            startActivity(intent)
        }

        main_profile_select_dialog_domain_settings.setOnClickListener {
            val intent = Intent(baseContext, DomainSettingsActivity::class.java)
            startActivity(intent)
        }

        main_profile_select_dialog_username_settings.setOnClickListener {
            val intent = Intent(baseContext, UsernamesSettingsActivity::class.java)
            startActivity(intent)
        }

        main_profile_select_dialog_anonaddy_settings.setOnClickListener {
            val intent = Intent(baseContext, AnonAddySettingsActivity::class.java)
            startActivity(intent)
        }

    }
}