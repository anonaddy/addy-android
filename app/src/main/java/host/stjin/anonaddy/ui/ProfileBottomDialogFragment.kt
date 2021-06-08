package host.stjin.anonaddy.ui

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.AnonAddy
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetProfileBinding
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


class ProfileBottomDialogFragment : BaseBottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetProfileBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetProfileBinding.inflate(inflater, container, false)
        // get the views and attach the listener
        val root = binding.root


        setMonthlyBandwidthStatistics()
        setInfo()
        setOnClickListeners()

        return root

    }

    private fun setOnClickListeners() {
        binding.mainProfileSelectDialogAppSettings.setOnClickListener {
            val intent = Intent(activity, AppSettingsActivity::class.java)
            startActivity(intent)
        }

        binding.mainProfileSelectDialogDomainSettings.setOnClickListener {
            val intent = Intent(activity, DomainSettingsActivity::class.java)
            startActivity(intent)
        }

        binding.mainProfileSelectDialogRules.setOnClickListener {
            val intent = Intent(activity, RulesSettingsActivity::class.java)
            startActivity(intent)
        }

        binding.mainProfileSelectDialogUsernameSettings.setOnClickListener {
            val intent = Intent(activity, UsernamesSettingsActivity::class.java)
            startActivity(intent)
        }

        binding.mainProfileSelectDialogAnonaddySettings.setOnClickListener {
            val url = "${AnonAddy.API_BASE_URL}/settings"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
    }

    private fun setMonthlyBandwidthStatistics() {
        val currMonthlyBandwidth = User.userResource.bandwidth.toDouble() / 1024 / 1024
        val maxMonthlyBandwidth = User.userResource.bandwidth_limit / 1024 / 1024

        binding.mainProfileSelectDialogStatisticsMonthlyBandwidthProgress.max =
            if (maxMonthlyBandwidth == 0) 0 else maxMonthlyBandwidth * 100



        binding.mainProfileSelectDialogStatisticsMonthlyBandwidthLeftText.text =
            when {
                currMonthlyBandwidth > maxMonthlyBandwidth -> this.resources.getString(R.string.exceeded_bandwidth_limit)
                maxMonthlyBandwidth == 0 -> this.resources.getString(
                    R.string._sMB_remaining_this_month,
                    "∞"
                )
                else -> this.resources.getString(
                    R.string._sMB_remaining_this_month,
                    (NumberUtils.roundOffDecimal(maxMonthlyBandwidth.toDouble()) - NumberUtils.roundOffDecimal(currMonthlyBandwidth)).toString()
                )
            }


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
                ThemeUtils.getDeviceAccentColor(requireContext()),
                BlendMode.SRC_OVER
            )
        } else {
            binding.mainProfileSelectDialogUsernameInitials.background.setColorFilter(
                ThemeUtils.getDeviceAccentColor(requireContext()),
                PorterDuff.Mode.SRC_OVER
            )
        }

        binding.mainProfileSelectDialogAnonaddyVersion.text =
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


    companion object {
        fun newInstance(): ProfileBottomDialogFragment {
            return ProfileBottomDialogFragment()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}