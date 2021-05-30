package host.stjin.anonaddy.ui

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.DisplayCutout
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import host.stjin.anonaddy.AnonAddy
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.MainProfileSelectDialogBinding
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.ui.appsettings.AppSettingsActivity
import host.stjin.anonaddy.ui.domains.DomainSettingsActivity
import host.stjin.anonaddy.ui.rules.RulesSettingsActivity
import host.stjin.anonaddy.ui.usernames.UsernamesSettingsActivity
import host.stjin.anonaddy.utils.DateTimeUtils
import host.stjin.anonaddy.utils.NumberUtils
import host.stjin.anonaddy.utils.ThemeUtils
import java.util.*
import kotlin.math.roundToInt


class DialogActivity : Activity() {
    private lateinit var binding: MainProfileSelectDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainProfileSelectDialogBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        WindowCompat.setDecorFitsSystemWindows(window, false)


        if (this.resources.getBoolean(R.bool.isTablet)) {
            (findViewById<View>(R.id.main_profile_select_dialog_card).parent as View).setOnClickListener { finishAfterTransition() }
        } else {
            window.statusBarColor = Color.TRANSPARENT
        }

        setMonthlyBandwidthStatistics()
        setInfo()
        setOnClickListeners()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !this.resources.getBoolean(R.bool.isTablet)) {
            val displayCutout: DisplayCutout? = window.decorView.rootWindowInsets.displayCutout
            val newLayoutParams = binding.mainProfileSelectDialogTitle?.layoutParams as ViewGroup.MarginLayoutParams
            if (displayCutout != null) {
                newLayoutParams.setMargins(0, displayCutout.safeInsetTop + 24, 0, 0)
            }
            binding.mainProfileSelectDialogTitle!!.layoutParams = newLayoutParams
        }
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

        binding.mainProfileSelectDialogClose?.setOnClickListener {
            finish()
        }

        binding.mainProfileSelectDialogAnonaddySettings.setOnClickListener {
            val url = "${AnonAddy.API_BASE_URL}/settings"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.nothing, R.anim.bottom_down)
    }

    override fun onBackPressed() {
        if (this.resources.getBoolean(R.bool.isTablet)) {
            finishAfterTransition()
        } else {
            finish()
        }
    }

    private fun setMonthlyBandwidthStatistics() {
        val currMonthlyBandwidth = User.userResource.bandwidth.toDouble() / 1024 / 1024
        val maxMonthlyBandwidth = User.userResource.bandwidth_limit / 1024 / 1024

        binding.mainProfileSelectDialogStatisticsMonthlyBandwidthProgress.max =
            if (maxMonthlyBandwidth == 0) 0 else maxMonthlyBandwidth * 100


        binding.mainProfileSelectDialogStatisticsMonthlyBandwidthLeftText.text =
            if (maxMonthlyBandwidth == 0) this.resources.getString(R.string._sMB_remaining_this_month, "∞") else
                this.resources.getString(
                    R.string._sMB_remaining_this_month,
                    (NumberUtils.roundOffDecimal(maxMonthlyBandwidth.toDouble()) - NumberUtils.roundOffDecimal(currMonthlyBandwidth)).toString()
                )


        ObjectAnimator.ofInt(
            binding.mainProfileSelectDialogStatisticsMonthlyBandwidthProgress,
            "progress",
            currMonthlyBandwidth.roundToInt() * 100
        )
            .setDuration(300)
            .start()
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