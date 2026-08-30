package host.stjin.anonaddy.ui.accountnotifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.adapter.AccountNotificationsAdapter
import host.stjin.anonaddy.databinding.FragmentAccountNotificationsBinding
import host.stjin.anonaddy.interfaces.Refreshable
import host.stjin.anonaddy.ui.base.BaseFragment
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.AccountNotifications
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class AccountNotificationsFragment : BaseFragment(),
    AccountNotificationsDetailsBottomDialogFragment.AddAccountNotificationsBottomDialogListener, Refreshable {

    // 1. Properties
    private val notificationsViewModel: AccountNotificationsViewModel by viewModels()

    private var accountNotifications: ArrayList<AccountNotifications>? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var oneTimeRecyclerViewActions: Boolean = true

    private var accountNotificationsDetailsBottomDialogFragment: AccountNotificationsDetailsBottomDialogFragment? = null

    private var _binding: FragmentAccountNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var accountNotificationsAdapter: AccountNotificationsAdapter

    private var isSilentRefresh = false

    // 2. Lifecycle Methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountNotificationsBinding.inflate(inflater, container, false)
        InsetUtils.applyBottomInset(binding.fragmentAccountNotificationsLL1)
        val root = binding.root

        encryptedSettingsManager = ServiceLocator.encryptedSettingsManager

        setAccountNotificationsRecyclerView()
        observeViewModel()
        getDataFromWeb(savedInstanceState)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 3. View Setup
    private fun setAccountNotificationsRecyclerView() {
        accountNotificationsAdapter = AccountNotificationsAdapter()
        accountNotificationsAdapter.setClickListener(object : AccountNotificationsAdapter.ClickListener {
            override fun onClickDetails(pos: Int, view: View) {
                accountNotifications?.getOrNull(pos)?.let {
                    accountNotificationsDetailsBottomDialogFragment = AccountNotificationsDetailsBottomDialogFragment.newInstance(
                        it.created_at,
                        it.title,
                        it.text,
                        it.link_text,
                        it.link
                    )
                    accountNotificationsDetailsBottomDialogFragment!!.show(
                        childFragmentManager,
                        "accountNotificationsDetailsBottomDialogFragment"
                    )
                }
            }
        })

        binding.fragmentAccountNotificationsAllAccountNotificationsRecyclerview.apply {
            adapter = accountNotificationsAdapter
            if (oneTimeRecyclerViewActions) {
                oneTimeRecyclerViewActions = false
                shimmerItemCount =
                    encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT, 2) ?: 2

                layoutManager = LinearLayoutManager(requireContext())

                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation

                showShimmer()
            }
        }
    }

    private fun setAccountNotificationsAdapter(list: ArrayList<AccountNotifications>) {
        binding.fragmentAccountNotificationsAllAccountNotificationsRecyclerview.apply {
            accountNotifications = list

            if (list.isNotEmpty()) {
                binding.fragmentAccountNotificationsNoAccountNotifications.visibility = View.GONE
            } else {
                binding.fragmentAccountNotificationsNoAccountNotifications.visibility = View.VISIBLE
            }

            accountNotificationsAdapter.submitList(list.toList())

            if (!isTablet) {
                fragmentShown()
            }

            binding.animationFragment.stopAnimation()
        }
    }

    // 4. Observers
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                notificationsViewModel.notificationsState.collect { state ->
                    handleUiState(
                        state,
                        shimmer = if (!isSilentRefresh) binding.fragmentAccountNotificationsAllAccountNotificationsRecyclerview else null,
                        progress = if (isSilentRefresh) binding.notificationsProgress else null,
                        titleProgress = if (isSilentRefresh) binding.notificationsTitleProgress else null,
                        errorStringRes = R.string.something_went_wrong_retrieving_account_notifications
                    ) { data ->
                        setAccountNotificationsAdapter(ArrayList(data))
                    }
                }
            }
        }
    }

    // 5. Private Helpers / Public Methods
    fun getDataFromWeb(savedInstanceState: Bundle?, showShimmer: Boolean = true) {
        isSilentRefresh = !showShimmer
        notificationsViewModel.loadNotifications(forceRefresh = (savedInstanceState == null))
    }

    override suspend fun onRefreshData() {
        if (!isAdded) {
            return
        }
        try {
            isSilentRefresh = true
            notificationsViewModel.loadNotifications(forceRefresh = true).join()
        } catch (e: Exception) {
            LoggingHelper(requireContext()).addLog(
                LOGIMPORTANCE.CRITICAL.int,
                "Failed to refresh data, view lifecycle not available. $e",
                "AccountNotificationsFragment",
                null
            )
        }
    }

    override fun onOpenUrl(url: String?) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        startActivity(intent)
    }

    fun fragmentShown() {
        if (::accountNotificationsAdapter.isInitialized) {
            encryptedSettingsManager?.putSettingsInt(
                SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT,
                accountNotificationsAdapter.itemCount
            )
        }
    }

    companion object {
        fun newInstance() = AccountNotificationsFragment()
    }
}
