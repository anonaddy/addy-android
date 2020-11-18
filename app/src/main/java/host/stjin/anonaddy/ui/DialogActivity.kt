package host.stjin.anonaddy.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import host.stjin.anonaddy.AnonAddy
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.ui.anonaddysettings.AnonAddySettingsActivity
import host.stjin.anonaddy.ui.appsettings.AppSettingsActivity
import host.stjin.anonaddy.ui.domains.DomainSettingsActivity
import host.stjin.anonaddy.ui.rules.RulesSettingsActivity
import host.stjin.anonaddy.ui.usernames.UsernamesSettingsActivity
import kotlinx.android.synthetic.main.main_profile_select_dialog.*

class DialogActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_profile_select_dialog)

        window.decorView.systemUiVisibility =
                // Tells the system that the window wishes the content to
                // be laid out at the most extreme scenario. See the docs for
                // more information on the specifics
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    // Tells the system that the window wishes the content to
                    // be laid out as if the navigation bar was hidden
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        (findViewById<View>(R.id.main_profile_select_dialog_card).parent as View).setOnClickListener { finishAfterTransition() }

        setInfo()
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        main_profile_select_dialog_app_settings.setOnClickListener {
            val intent = Intent(this, AppSettingsActivity::class.java)
            startActivity(intent)
        }

        main_profile_select_dialog_domain_settings.setOnClickListener {
            val intent = Intent(this, DomainSettingsActivity::class.java)
            startActivity(intent)
        }

        main_profile_select_dialog_rules.setOnClickListener {
            val intent = Intent(this, RulesSettingsActivity::class.java)
            startActivity(intent)
        }

        main_profile_select_dialog_username_settings.setOnClickListener {
            val intent = Intent(this, UsernamesSettingsActivity::class.java)
            startActivity(intent)
        }

        main_profile_select_dialog_anonaddy_settings.setOnClickListener {
            val intent = Intent(this, AnonAddySettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setInfo() {
        main_profile_select_dialog_instance_type.text =
            if (AnonAddy.VERSIONCODE == 9999) this.resources.getString(R.string.hosted_instance) else this.resources.getString(
                R.string.self_hosted_instance_s,
                AnonAddy.VERSIONSTRING
            )
        main_profile_select_dialog_card_accountname.text = User.userResource.username
        main_profile_select_dialog_card_subscription.text = resources.getString(R.string.subscription_user, User.userResource.subscription)
        main_profile_select_dialog_app_settings_desc.text = resources.getString(R.string.version_s, BuildConfig.VERSION_NAME)
    }
}