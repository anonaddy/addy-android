package host.stjin.anonaddy.ui.accountnotifications

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.AccountNotificationsAdapter
import host.stjin.anonaddy.databinding.FragmentAccountNotificationsBinding
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.AccountNotifications
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class AccountNotificationsFragment : Fragment(), AccountNotificationsDetailsBottomDialogFragment.AddAccountNotificationsBottomDialogListener {

    private var accountNotifications: ArrayList<AccountNotifications>? = null
    private var networkHelper: NetworkHelper? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true

    private var accountNotificationsDetailsBottomDialogFragment: AccountNotificationsDetailsBottomDialogFragment? = null


    companion object {
        fun newInstance() = AccountNotificationsFragment()
    }


    private var _binding: FragmentAccountNotificationsBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountNotificationsBinding.inflate(inflater, container, false)
        val root = binding.root

        encryptedSettingsManager = SettingsManager(true, requireContext())
        networkHelper = NetworkHelper(requireContext())

        setAccountNotificationsRecyclerView()
        getDataFromWeb(savedInstanceState)

        return root
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        val json = gson.toJson(accountNotifications)
        outState.putString("accountNotifications", json)
    }


    private fun getDataFromWeb(savedInstanceState: Bundle?) {
        // Get the latest data in the background, and update the values when loaded
        lifecycleScope.launch {
            if (savedInstanceState != null) {

                val accountNotificationsJson = savedInstanceState.getString("accountNotifications")
                if (accountNotificationsJson!!.isNotEmpty() && accountNotificationsJson != "null") {
                    val gson = Gson()

                    val myType = object : TypeToken<ArrayList<AccountNotifications>>() {}.type
                    val list = gson.fromJson<ArrayList<AccountNotifications>>(accountNotificationsJson, myType)
                    setAccountNotificationsAdapter(list)
                } else {
                    // accountNotificationsJson could be null when an embedded activity is opened instantly
                    getAllAccountNotificationsAndSetRecyclerview()
                }

            } else {
                getAllAccountNotificationsAndSetRecyclerview()
            }

        }
    }


    private fun setAccountNotificationsRecyclerView() {
        binding.fragmentAccountNotificationsAllAccountNotificationsRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false
                shimmerItemCount =
                    encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT, 2) ?: 2
                shimmerLayoutManager = GridLayoutManager(requireContext(), ScreenSizeUtils.calculateNoOfColumns(context))
                layoutManager = GridLayoutManager(requireContext(), ScreenSizeUtils.calculateNoOfColumns(context))

                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation

                showShimmer()
            }
        }
    }

    private lateinit var accountNotificationsAdapter: AccountNotificationsAdapter
    private suspend fun getAllAccountNotificationsAndSetRecyclerview() {
        binding.fragmentAccountNotificationsAllAccountNotificationsRecyclerview.apply {
            networkHelper?.getAllAccountNotifications { list, error ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                // Check if there are new account notifications since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                if (::accountNotificationsAdapter.isInitialized && list == accountNotificationsAdapter.getList()) {
                    return@getAllAccountNotifications
                }

                if (list != null) {
                    setAccountNotificationsAdapter(list)
                } else {
                        if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                            SnackbarHelper.createSnackbar(
                                requireContext(),
                                requireContext().resources.getString(R.string.something_went_wrong_retrieving_account_notifications) + "\n" + error,
                                (activity as MainActivity).findViewById(R.id.main_container),
                                LoggingHelper.LOGFILES.DEFAULT
                            ).show()
                        } else {
                            SnackbarHelper.createSnackbar(
                                requireContext(),
                                requireContext().resources.getString(R.string.something_went_wrong_retrieving_account_notifications) + "\n" + error,
                                (activity as AccountNotificationsActivity).findViewById(R.id.activity_account_notifications_settings_CL),
                                LoggingHelper.LOGFILES.DEFAULT
                            ).show()
                        }

                        // Show error animations
                        binding.fragmentAccountNotificationsLL1.visibility = View.GONE
                        binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)



                }
                hideShimmer()
            }

        }

    }

    private fun fragmentShown() {
        if (::accountNotificationsAdapter.isInitialized) {
            // Set the count of account notifications so that the shimmerview looks better next time AND so that we can use it for the backgroundservice AND mark this a read for the badge
            encryptedSettingsManager?.putSettingsInt(
                SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT,
                accountNotificationsAdapter.itemCount
            )
        }
    }


    private fun setAccountNotificationsAdapter(list: ArrayList<AccountNotifications>) {
        binding.fragmentAccountNotificationsAllAccountNotificationsRecyclerview.apply {
            accountNotifications = list
            if (list.size > 0) {
                binding.fragmentAccountNotificationsNoAccountNotifications.visibility = View.GONE
            } else {
                binding.fragmentAccountNotificationsNoAccountNotifications.visibility = View.VISIBLE
            }


            accountNotificationsAdapter = AccountNotificationsAdapter(list)
            accountNotificationsAdapter.setClickListener(object : AccountNotificationsAdapter.ClickListener {

                override fun onClickDetails(pos: Int, aView: View) {
                    accountNotificationsDetailsBottomDialogFragment = AccountNotificationsDetailsBottomDialogFragment(
                        list[pos].created_at,
                        list[pos].title,
                        list[pos].text,
                        list[pos].link_text,
                        list[pos].link
                    )
                    accountNotificationsDetailsBottomDialogFragment!!.show(
                        childFragmentManager,
                        "accountNotificationsDetailsBottomDialogFragment"
                    )
                }

            })
            adapter = accountNotificationsAdapter


            // Since this activity is always in foreground (no fragments in the MainActivity, always update the cache data
            fragmentShown()


            binding.animationFragment.stopAnimation()
            //binding.activityAccountNotificationsNSV.animate().alpha(1.0f) -> Do not animate as there is a shimmerview
        }
    }


    override fun onOpenUrl(url: String) {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)

        accountNotificationsDetailsBottomDialogFragment?.dismissAllowingStateLoss()
    }
}