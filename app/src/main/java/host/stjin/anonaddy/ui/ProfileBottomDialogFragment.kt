package host.stjin.anonaddy.ui

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetProfileBinding
import host.stjin.anonaddy.ui.appsettings.AppSettingsActivity
import host.stjin.anonaddy.ui.domains.DomainSettingsActivity
import host.stjin.anonaddy.ui.rules.RulesSettingsActivity
import host.stjin.anonaddy.ui.usernames.UsernamesSettingsActivity
import host.stjin.anonaddy.utils.AttributeHelper
import host.stjin.anonaddy.utils.NumberUtils
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import org.ocpsoft.prettytime.PrettyTime
import java.util.Locale
import kotlin.math.roundToInt


class ProfileBottomDialogFragment : BaseBottomSheetDialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    var updateAvailable: Boolean = false
    var permissionsRequired: Boolean = false
    private var _binding: BottomsheetProfileBinding? = null
    private var altSubscriptionTextShown = false

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

    override fun onResume() {
        super.onResume()

        // When this view comes into the screen, set the update text
        // The lower the check-method
        checkForUpdates()
        checkForPermissions()
        tintSettingsIcon()

    }

    private fun checkForPermissions() {
        if (permissionsRequired) {
            binding.mainProfileSelectDialogAppSettingsDesc.text =
                resources.getString(R.string.permissions_required)
        }
    }

    private fun checkForUpdates() {
        // The main activity tells the dialog if an update is available
        if (updateAvailable) {
            binding.mainProfileSelectDialogAppSettingsDesc.text =
                resources.getString(R.string.version_s_update_available, BuildConfig.VERSION_NAME)
        }

    }

    private fun tintSettingsIcon() {
        if (updateAvailable || permissionsRequired) {
            ImageViewCompat.setImageTintList(
                binding.mainProfileSelectDialogAppSettingsIcon,
                context?.let { ContextCompat.getColorStateList(it, R.color.softRed) }
            )
        } else {
            ImageViewCompat.setImageTintList(
                binding.mainProfileSelectDialogAppSettingsIcon,
                context?.let { ColorStateList.valueOf(AttributeHelper.getValueByAttr(it, R.attr.colorControlNormal)) }
            )
            binding.mainProfileSelectDialogAppSettingsDesc.text = resources.getString(R.string.version_s, BuildConfig.VERSION_NAME)
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("updateAvailable", updateAvailable)
        outState.putBoolean("permissionsRequired", permissionsRequired)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            updateAvailable = savedInstanceState.getBoolean("updateAvailable")
            permissionsRequired = savedInstanceState.getBoolean("permissionsRequired")
        }
    }

    private fun setOnClickListeners() {

        binding.mainProfileSelectDialogCardSubscription.setOnClickListener {
            if (altSubscriptionTextShown) {
                setSubscriptionText()
            } else {
                setSubscriptionTextAlt()
            }
        }

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
            val url = "${AddyIo.API_BASE_URL}/settings"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
    }

    private fun setMonthlyBandwidthStatistics() {
        val currMonthlyBandwidth = (activity?.application as AddyIoApp).userResource.bandwidth.toDouble() / 1024 / 1024
        val maxMonthlyBandwidth = (activity?.application as AddyIoApp).userResource.bandwidth_limit / 1024 / 1024

        binding.mainProfileSelectDialogStatisticsMonthlyBandwidthProgress.max =
            if (maxMonthlyBandwidth.compareTo(0) == 0) 0 else (maxMonthlyBandwidth * 100).toInt()



        binding.mainProfileSelectDialogStatisticsMonthlyBandwidthLeftText.text =
            when {
                maxMonthlyBandwidth.compareTo(0) == 0 -> this.resources.getString(
                    R.string._sMB_remaining_this_month,
                    "∞"
                )
                currMonthlyBandwidth > maxMonthlyBandwidth -> this.resources.getString(R.string.exceeded_bandwidth_limit)
                else -> this.resources.getString(
                    R.string._sMB_remaining_this_month,
                    (NumberUtils.roundOffDecimal(maxMonthlyBandwidth.toDouble()) - NumberUtils.roundOffDecimal(currMonthlyBandwidth)).toString()
                )
            }

        if (maxMonthlyBandwidth.compareTo(0) == 0) {
            Handler(Looper.getMainLooper()).postDelayed({
                binding.mainProfileSelectDialogStatisticsMonthlyBandwidthProgressShimmer.startShimmer()
            }, 500)
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
        val usernameInitials = (activity?.application as AddyIoApp).userResource.username.take(2).uppercase(Locale.getDefault())
        binding.mainProfileSelectDialogUsernameInitials.text = usernameInitials

        binding.mainProfileSelectDialogAnonaddyVersion.text =
            if (AddyIo.VERSIONMAJOR == 9999) this.resources.getString(R.string.hosted_instance) else this.resources.getString(
                R.string.self_hosted_instance_s,
                AddyIo.VERSIONSTRING
            )

        binding.mainProfileSelectDialogCardAccountname.text = (activity?.application as AddyIoApp).userResource.username

        setSubscriptionText()

        binding.mainProfileSelectDialogAppSettingsDesc.text = resources.getString(R.string.version_s, BuildConfig.VERSION_NAME)
    }

    private fun setSubscriptionText() {
        altSubscriptionTextShown = false

        when {
            (activity?.application as AddyIoApp).userResource.subscription == null -> {
                binding.mainProfileSelectDialogCardSubscription.visibility = View.GONE
            }

            (activity?.application as AddyIoApp).userResource.subscription_ends_at != null -> {
                binding.mainProfileSelectDialogCardSubscription.visibility = View.VISIBLE
                binding.mainProfileSelectDialogCardSubscription.text = resources.getString(
                    R.string.subscription_user_until,
                    (activity?.application as AddyIoApp).userResource.subscription,
                    DateTimeUtils.turnStringIntoLocalString(
                        (activity?.application as AddyIoApp).userResource.subscription_ends_at,
                        DateTimeUtils.DatetimeFormat.DATE
                    )
                )
            }
            else -> {
                binding.mainProfileSelectDialogCardSubscription.visibility = View.VISIBLE
                binding.mainProfileSelectDialogCardSubscription.text =
                    resources.getString(R.string.subscription_user, (activity?.application as AddyIoApp).userResource.subscription)
            }
        }
    }

    private fun setSubscriptionTextAlt() {
        altSubscriptionTextShown = true

        when {
            (activity?.application as AddyIoApp).userResource.subscription == null -> {
                binding.mainProfileSelectDialogCardSubscription.visibility = View.GONE
            }

            (activity?.application as AddyIoApp).userResource.subscription_ends_at != null -> {
                binding.mainProfileSelectDialogCardSubscription.visibility = View.VISIBLE
                val expiryDate =
                    DateTimeUtils.turnStringIntoLocalDateTime((activity?.application as AddyIoApp).userResource.subscription_ends_at)
                val text = PrettyTime().format(expiryDate)
                binding.mainProfileSelectDialogCardSubscription.text = resources.getString(R.string.subscription_expiry_date, text)
            }

            else -> {
                binding.mainProfileSelectDialogCardSubscription.visibility = View.VISIBLE
                binding.mainProfileSelectDialogCardSubscription.text =
                    resources.getString(R.string.subscription_user, (activity?.application as AddyIoApp).userResource.subscription)
            }
        }
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