package host.stjin.anonaddy.ui

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.widget.ImageViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetProfileBinding
import host.stjin.anonaddy.ui.appsettings.AppSettingsActivity
import host.stjin.anonaddy.ui.blocklist.BlocklistActivity
import host.stjin.anonaddy.ui.domains.DomainsActivity
import host.stjin.anonaddy.ui.labels.LabelsActivity
import host.stjin.anonaddy.ui.rules.RulesActivity
import host.stjin.anonaddy.ui.usernames.UsernamesActivity
import host.stjin.anonaddy.utils.AttributeHelper
import host.stjin.anonaddy.utils.ReviewHelper
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import java.util.Locale


class ProfileBottomDialogFragment : BaseBottomSheetDialogFragment() {
    private var updateAvailable: Boolean = false

    private var permissionsRequired: Boolean = false

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            if (data?.getBooleanExtra("hasNewSubscription", false) == true) {
                setInfo()
                (activity as? MainActivity)?.refreshAllData()

                // User has switched or purchased a subscription, this is usually a sign of a satisfied user, let's ask the user to review the app
                activity?.let { ReviewHelper().launchReviewFlow(it) }
            }
        }
    }

    private var _binding: BottomsheetProfileBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            updateAvailable = it.getBoolean(ARG_UPDATE_AVAILABLE)
            permissionsRequired = it.getBoolean(ARG_PERMISSIONS_REQUIRED)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetProfileBinding.inflate(inflater, container, false)
        // get the views and attach the listener
        val root = binding.root

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
        checkForHostedInstance()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("updateAvailable", updateAvailable)
        outState.putBoolean("permissionsRequired", permissionsRequired)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setOnClickListeners() {

        binding.mainProfileSelectDialogAppSettings.setOnClickListener {
            val intent = Intent(activity, AppSettingsActivity::class.java)
            startActivity(intent)
        }

        binding.mainProfileSelectDialogDomainSettings.setOnClickListener {
            val intent = Intent(activity, DomainsActivity::class.java)
            startActivity(intent)
        }

        binding.mainProfileSelectDialogRules.setOnClickListener {
            val intent = Intent(activity, RulesActivity::class.java)
            startActivity(intent)
        }

        binding.mainProfileSelectDialogUsernameSettings.setOnClickListener {
            val intent = Intent(activity, UsernamesActivity::class.java)
            startActivity(intent)
        }

        binding.mainProfileSelectDialogBlocklistSettings.setOnClickListener {
            val intent = Intent(activity, BlocklistActivity::class.java)
            startActivity(intent)
        }

        binding.mainProfileSelectDialogLabelsSettings.setOnClickListener {
            val intent = Intent(activity, LabelsActivity::class.java)
            startActivity(intent)
        }

        binding.mainProfileSelectDialogAnonaddySettings.setOnClickListener {
            val url = "${AddyIo.API_BASE_URL}/settings"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = url.toUri()
            startActivity(i)
        }

        binding.mainProfileSelectDialogManageSubscription.setOnClickListener {
            if (BuildConfig.FLAVOR == "gplay") {
                val intent = Intent(activity, ManageSubscriptionActivity::class.java)
                resultLauncher.launch(intent)
            } else {
                val url = "${AddyIo.API_BASE_URL}/settings/subscription"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = url.toUri()
                startActivity(i)
            }

        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            updateAvailable = savedInstanceState.getBoolean("updateAvailable")
            permissionsRequired = savedInstanceState.getBoolean("permissionsRequired")
        }
    }

    private fun checkForHostedInstance() {
        if (AddyIo.isUsingHostedInstance) {
            binding.mainProfileSelectDialogManageSubscription.visibility = View.VISIBLE
        } else {
            binding.mainProfileSelectDialogManageSubscription.visibility = View.GONE
        }
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

    private fun setInfo() {
        val userResource = (activity?.application as? AddyIoApp)?.userResourceOrNull
        val usernameInitials = userResource?.username?.take(2)?.uppercase(Locale.getDefault()) ?: ""
        binding.mainProfileSelectDialogUsernameInitials.text = usernameInitials

        binding.mainProfileSelectDialogAnonaddyVersion.text =
            if (AddyIo.isUsingHostedInstance) this.resources.getString(R.string.hosted_instance) else this.resources.getString(
                R.string.self_hosted_instance_s,
                AddyIo.VERSIONSTRING
            )

        binding.mainProfileSelectDialogCardAccountname.text = userResource?.username ?: ""

        setSubscriptionText()

        binding.mainProfileSelectDialogAppSettingsDesc.text = resources.getString(R.string.version_s, BuildConfig.VERSION_NAME)
    }

    private fun setSubscriptionText() {
        val userResource = (activity?.application as? AddyIoApp)?.userResourceOrNull
        if (userResource?.subscription != null) {
            binding.mainProfileSelectDialogCardLL.visibility = View.VISIBLE
            binding.mainProfileSelectDialogCardSubscription.text =
                resources.getString(R.string.subscription_user, userResource.subscription)

            if (userResource.family_plan_role != null) {
                binding.mainProfileSelectDialogCardFamilyIcon.visibility = View.VISIBLE
            } else {
                binding.mainProfileSelectDialogCardFamilyIcon.visibility = View.GONE
            }
        } else {
            binding.mainProfileSelectDialogCardLL.visibility = View.GONE
        }

        if (userResource?.subscription_ends_at != null) {
            binding.mainProfileSelectDialogCardSubscriptionUntil.visibility = View.VISIBLE
            binding.mainProfileSelectDialogCardSubscriptionUntil.text =
                resources.getString(
                    R.string.subscription_user_until, DateTimeUtils.convertStringToLocalTimeZoneString(
                        userResource.subscription_ends_at,
                        DateTimeUtils.DatetimeFormat.DATE
                    )
                )
        } else {
            binding.mainProfileSelectDialogCardSubscriptionUntil.visibility = View.GONE
        }
    }

    companion object {
        private const val ARG_UPDATE_AVAILABLE = "update_available"
        private const val ARG_PERMISSIONS_REQUIRED = "permissions_required"

        fun newInstance(updateAvailable: Boolean = false, permissionsRequired: Boolean = false): ProfileBottomDialogFragment {
            return ProfileBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_UPDATE_AVAILABLE, updateAvailable)
                    putBoolean(ARG_PERMISSIONS_REQUIRED, permissionsRequired)
                }
            }
        }
    }
}
