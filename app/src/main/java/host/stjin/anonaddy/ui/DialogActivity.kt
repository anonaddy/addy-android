package host.stjin.anonaddy.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import host.stjin.anonaddy.AnonAddy
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.MainProfileSelectDialogBinding
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.ui.anonaddysettings.AnonAddySettingsActivity
import host.stjin.anonaddy.ui.appsettings.AppSettingsActivity
import host.stjin.anonaddy.ui.domains.DomainSettingsActivity
import host.stjin.anonaddy.ui.rules.RulesSettingsActivity
import host.stjin.anonaddy.ui.usernames.UsernamesSettingsActivity
import host.stjin.anonaddy.utils.DateTimeUtils
import host.stjin.anonaddy.utils.ThemeUtils
import java.util.*


class DialogActivity : Activity() {
    private lateinit var binding: MainProfileSelectDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainProfileSelectDialogBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        WindowCompat.setDecorFitsSystemWindows(window, false)


        (findViewById<View>(R.id.main_profile_select_dialog_card).parent as View).setOnClickListener { finishAfterTransition() }

        setInfo()
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding.mainProfileSelectDialogAppSettings.setOnClickListener {
            val intent = Intent(this, AppSettingsActivity::class.java)
            startActivity(intent)
        }

        binding.mainProfileSelectDialogDomainSettings.setOnClickListener {
            val intent = Intent(this, DomainSettingsActivity::class.java)
            startActivity(intent)
        }

        binding.mainProfileSelectDialogRules.setOnClickListener {
            val intent = Intent(this, RulesSettingsActivity::class.java)
            startActivity(intent)
        }

        binding.mainProfileSelectDialogUsernameSettings.setOnClickListener {
            val intent = Intent(this, UsernamesSettingsActivity::class.java)
            startActivity(intent)
        }

        binding.mainProfileSelectDialogAnonaddySettings.setOnClickListener {
            val intent = Intent(this, AnonAddySettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setInfo() {
        val usernameInitials = User.userResource.username.take(2).uppercase(Locale.getDefault())
        binding.mainProfileSelectDialogUsernameInitials.text = usernameInitials

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.mainProfileSelectDialogUsernameInitials.background.colorFilter = BlendModeColorFilter(
                ThemeUtils.getDeviceAccentColor(this),
                BlendMode.SRC_IN
            )
        } else {
            binding.mainProfileSelectDialogUsernameInitials.background.setColorFilter(ThemeUtils.getDeviceAccentColor(this), PorterDuff.Mode.SRC_ATOP)
        }



        binding.mainProfileSelectDialogAnonaddySettingsDesc.text =
            if (AnonAddy.VERSIONCODE == 9999) this.resources.getString(R.string.hosted_instance) else this.resources.getString(
                R.string.self_hosted_instance_s,
                AnonAddy.VERSIONSTRING
            )
        binding.mainProfileSelectDialogCardAccountname.text = User.userResource.username

        if (User.userResource.subscription_ends_at != null) {
            binding.mainProfileSelectDialogCardSubscription.text = resources.getString(
                R.string.subscription_user_until,
                User.userResource.subscription,
                DateTimeUtils.turnStringIntoLocalString(User.userResource.subscription_ends_at, DateTimeUtils.DATETIMEUTILS.DATE)
            )
        } else {
            binding.mainProfileSelectDialogCardSubscription.text = resources.getString(R.string.subscription_user, User.userResource.subscription)
        }


        binding.mainProfileSelectDialogAppSettingsDesc.text = resources.getString(R.string.version_s, BuildConfig.VERSION_NAME)

    }
}