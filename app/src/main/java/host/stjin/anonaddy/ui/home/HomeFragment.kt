package host.stjin.anonaddy.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView.OnScrollChangeListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.FragmentHomeBinding
import host.stjin.anonaddy.interfaces.Refreshable
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.ui.MainViewpagerAdapter
import host.stjin.anonaddy.ui.alias.AliasFragment
import host.stjin.anonaddy.ui.customviews.HomeStatCardView
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.NumberUtils.roundOffDecimal
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


class HomeFragment : Fragment(), Refreshable {

    private var networkHelper: NetworkHelper? = null

    companion object {
        fun newInstance() = HomeFragment()
    }

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        //InsetUtil.applyBottomInset(binding.homeStatisticsLL) Not necessary, MainActivity elevated the viewpager for the fab

        val root = binding.root
        networkHelper = NetworkHelper(requireContext())

        // load values from local to make the app look quick and snappy!
        setOnClickListeners()
        setStatistics()
        setNsvListener()

        // Only run this once, not doing it in onresume as scrolling between the pages might trigger too much
        // API calls, user should swipe to refresh starting from v4.5.0
        getDataFromWeb(savedInstanceState)

        return root
    }



    private val mScrollUpBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            binding.homeStatisticsNSV.post { binding.homeStatisticsNSV.smoothScrollTo(0,0) }
        }
    }

    private fun setNsvListener() {
        binding.homeStatisticsNSV.setOnScrollChangeListener(OnScrollChangeListener { _, _, _, _, _ -> setHasReachedTopOfNsv() })
    }

    private fun setHasReachedTopOfNsv() {
        (activity as MainActivity).hasReachedTopOfNsv = !binding.homeStatisticsNSV.canScrollVertically(-1)
    }

    override fun onPause() {
        super.onPause()
        activity?.unregisterReceiver(mScrollUpBroadcastReceiver)
    }


    fun getDataFromWeb(savedInstanceState: Bundle?) {
        // Get the latest data in the background, and update the values when loaded
        viewLifecycleOwner.lifecycleScope.launch {

            // Check if savedInstanceState is null, or not
            // On activity recreations (orientationchanges, sizing of the app) savedInstanceState will be filled using onSaveInstanceState
            // This way we can instantly set the values without another API call.
            if (savedInstanceState != null) {
                getWebStatistics()
                // (activity?.application as AddyIoApp).userResource is not being cleared upon activity-creation,
                // no need to obtain this from savedInstanceState
                setStatistics()
            } else {
                getWebStatistics()
            }
        }
    }



    // Update information when coming back, such as aliases and statistics
    override fun onResume() {
        super.onResume()
        setHasReachedTopOfNsv()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity?.registerReceiver(mScrollUpBroadcastReceiver, IntentFilter("scroll_up"), Context.RECEIVER_EXPORTED)
        } else {
            activity?.registerReceiver(mScrollUpBroadcastReceiver, IntentFilter("scroll_up"))
        }
    }


    private fun setOnClickListeners() {

        binding.homeStatCardTotalAliases.setOnLayoutClickedListener(object : HomeStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = requireContext().resources.getString(R.string.apply_filter),
                    message = requireContext().resources.getString(R.string.apply_filter_desc),
                    icon = R.drawable.ic_filter,
                    neutralButtonText = requireContext().resources.getString(R.string.cancel),
                    positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                    positiveButtonAction = {
                        val aliasFragment: AliasFragment = ((activity as MainActivity).viewPager.adapter as MainViewpagerAdapter).getFragmentByTag("AliasFragment") as AliasFragment
                        aliasFragment.setFilterAndSortingSettings(
                            AliasSortFilter(
                                onlyActiveAliases = false,
                                onlyDeletedAliases = false,
                                onlyInactiveAliases = false,
                                onlyWatchedAliases = false,
                                sort = null,
                                sortDesc = false,
                                filter = null
                            )
                        )
                        (activity as MainActivity).navigateTo(R.id.navigation_alias)
                    }
                ).show()
            }

        })

        binding.homeStatCardActiveAliases.setOnLayoutClickedListener(object : HomeStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = requireContext().resources.getString(R.string.apply_filter),
                    message = requireContext().resources.getString(R.string.apply_filter_desc),
                    icon = R.drawable.ic_filter,
                    neutralButtonText = requireContext().resources.getString(R.string.cancel),
                    positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                    positiveButtonAction = {
                        val aliasFragment: AliasFragment = ((activity as MainActivity).viewPager.adapter as MainViewpagerAdapter).getFragmentByTag("AliasFragment") as AliasFragment
                        aliasFragment.setFilterAndSortingSettings(
                            AliasSortFilter(
                                onlyActiveAliases = true,
                                onlyDeletedAliases = false,
                                onlyInactiveAliases = false,
                                onlyWatchedAliases = false,
                                sort = null,
                                sortDesc = false,
                                filter = null
                            )
                        )
                        (activity as MainActivity).navigateTo(R.id.navigation_alias)
                    }
                ).show()
            }

        })

        binding.homeStatCardInactiveAliases.setOnLayoutClickedListener(object : HomeStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = requireContext().resources.getString(R.string.apply_filter),
                    message = requireContext().resources.getString(R.string.apply_filter_desc),
                    icon = R.drawable.ic_filter,
                    neutralButtonText = requireContext().resources.getString(R.string.cancel),
                    positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                    positiveButtonAction = {
                        val aliasFragment: AliasFragment = ((activity as MainActivity).viewPager.adapter as MainViewpagerAdapter).getFragmentByTag("AliasFragment") as AliasFragment
                        aliasFragment.setFilterAndSortingSettings(
                            AliasSortFilter(
                                onlyActiveAliases = false,
                                onlyDeletedAliases = false,
                                onlyInactiveAliases = true,
                                onlyWatchedAliases = false,
                                sort = null,
                                sortDesc = false,
                                filter = null
                            )
                        )
                        (activity as MainActivity).navigateTo(R.id.navigation_alias)
                    }
                ).show()
            }

        })


        binding.homeStatCardDeletedAliases.setOnLayoutClickedListener(object : HomeStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = requireContext().resources.getString(R.string.apply_filter),
                    message = requireContext().resources.getString(R.string.apply_filter_desc),
                    icon = R.drawable.ic_filter,
                    neutralButtonText = requireContext().resources.getString(R.string.cancel),
                    positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                    positiveButtonAction = {
                        val aliasFragment: AliasFragment = ((activity as MainActivity).viewPager.adapter as MainViewpagerAdapter).getFragmentByTag("AliasFragment") as AliasFragment
                        aliasFragment.setFilterAndSortingSettings(
                            AliasSortFilter(
                                onlyActiveAliases = false,
                                onlyDeletedAliases = true,
                                onlyInactiveAliases = true,
                                onlyWatchedAliases = false,
                                sort = null,
                                sortDesc = false,
                                filter = null
                            )
                        )
                        (activity as MainActivity).navigateTo(R.id.navigation_alias)
                    }
                ).show()
            }

        })

        binding.homeStatWatchedAliases.setOnLayoutClickedListener(object : HomeStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = requireContext().resources.getString(R.string.apply_filter),
                    message = requireContext().resources.getString(R.string.apply_filter_desc),
                    icon = R.drawable.ic_filter,
                    neutralButtonText = requireContext().resources.getString(R.string.cancel),
                    positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                    positiveButtonAction = {

                        val aliasWatcher = AliasWatcher(requireContext())
                        val aliasesToWatch = aliasWatcher.getAliasesToWatch().toList()
                        binding.homeStatWatchedAliases.setDescription(aliasesToWatch.size.toString())
                        if (aliasesToWatch.isNotEmpty()) {
                            val aliasFragment: AliasFragment = ((activity as MainActivity).viewPager.adapter as MainViewpagerAdapter).getFragmentByTag("AliasFragment") as AliasFragment
                            aliasFragment.setFilterAndSortingSettings(
                                AliasSortFilter(
                                    onlyActiveAliases = false,
                                    onlyDeletedAliases = false,
                                    onlyInactiveAliases = false,
                                    onlyWatchedAliases = true,
                                    sort = null,
                                    sortDesc = false,
                                    filter = null
                                )
                            )
                        }


                        (activity as MainActivity).navigateTo(R.id.navigation_alias)
                    }
                ).show()
            }

        })

        binding.homeStatCardTotalRecipients.setOnLayoutClickedListener(object : HomeStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                (activity as MainActivity).navigateTo(R.id.navigation_recipients)
            }

        })

    }

    private suspend fun getWebStatistics() {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                (activity?.application as AddyIoApp).userResource = user
                setStatistics()
            } else {
                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        requireContext().resources.getString(R.string.error_obtaining_user) + "\n" + result,
                        (activity as MainActivity).findViewById(R.id.main_container),
                        LoggingHelper.LOGFILES.DEFAULT
                    ).show()
                } else {
                    val bottomNavView: BottomNavigationView? =
                        activity?.findViewById(R.id.nav_view)
                    bottomNavView?.let {
                        SnackbarHelper.createSnackbar(
                            requireContext(),
                            requireContext().resources.getString(R.string.error_obtaining_user) + "\n" + result,
                            it,
                            LoggingHelper.LOGFILES.DEFAULT
                        )
                            .apply {
                                anchorView = bottomNavView
                            }.show()
                    }
                }

            }
        }
    }


    private fun setStatistics() {
        //  / 1024 / 1024 because api returns bytes
        val currMonthlyBandwidth = (activity?.application as AddyIoApp).userResource.bandwidth.toDouble() / 1024 / 1024
        val maxMonthlyBandwidth = (activity?.application as AddyIoApp).userResource.bandwidth_limit / 1024 / 1024


        binding.homeStatCardForwarded.setDescription(
            (activity?.application as AddyIoApp).userResource.total_emails_forwarded.toString()
        )

        binding.homeStatCardBlocked.setDescription(
            (activity?.application as AddyIoApp).userResource.total_emails_blocked.toString()
        )

        binding.homeStatCardReplies.setDescription(
            (activity?.application as AddyIoApp).userResource.total_emails_replied.toString()
        )

        binding.homeStatCardSent.setDescription(
            (activity?.application as AddyIoApp).userResource.total_emails_sent.toString()
        )


        // Bandwidth could be unlimited
        val bandwidthText = if (maxMonthlyBandwidth.compareTo(0) == 0) {
            this.resources.getString(R.string.home_bandwidth_text, roundOffDecimal(currMonthlyBandwidth).toString(), "∞")
        } else {
            this.resources.getString(R.string.home_bandwidth_text, roundOffDecimal(currMonthlyBandwidth).toString(), maxMonthlyBandwidth.toString())
        }

        binding.homeStatCardBandwidth.setDescription(bandwidthText)

        if (maxMonthlyBandwidth > 0) {
            binding.homeStatCardBandwidth.setProgress(currMonthlyBandwidth.toFloat() / maxMonthlyBandwidth.toFloat() * 100)
        }


        binding.homeStatCardTotalAliases.setDescription((activity?.application as AddyIoApp).userResource.total_aliases.toString())
        binding.homeStatCardActiveAliases.setDescription((activity?.application as AddyIoApp).userResource.total_active_aliases.toString())
        binding.homeStatCardInactiveAliases.setDescription((activity?.application as AddyIoApp).userResource.total_inactive_aliases.toString())
        binding.homeStatCardDeletedAliases.setDescription((activity?.application as AddyIoApp).userResource.total_deleted_aliases.toString())
        binding.homeStatCardTotalRecipients.setDescription((activity?.application as AddyIoApp).userResource.recipient_count.toString())



        val aliasWatcher = AliasWatcher(requireContext())
        val aliasesToWatch = aliasWatcher.getAliasesToWatch().toList()
        binding.homeStatWatchedAliases.setDescription(aliasesToWatch.size.toString())

        if (aliasesToWatch.isEmpty()) {
            binding.homeStatWatchedAliases.setButtonText(requireContext().resources.getString(R.string.start_watching))
        } else {
            binding.homeStatWatchedAliases.setButtonText(requireContext().resources.getString(R.string.view_watched))
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRefreshData() {
        // The key is to check if the view is created before proceeding.
        // `viewLifecycleOwner` can be used as a proxy for this check.
        if (!isAdded) return

        // Use a try-catch as an ultimate safeguard against rare lifecycle race conditions.
        try {
            // This ensures the coroutine is launched only when the view's lifecycle is active.
            viewLifecycleOwner.lifecycleScope.launch {
                getDataFromWeb(null)
            }
        } catch (e: IllegalStateException) {
            // Log the error if the lifecycle state was somehow invalid despite the check.
            LoggingHelper(requireContext()).addLog(LOGIMPORTANCE.CRITICAL.int, "Failed to refresh data, view lifecycle not available. $e", "HomeFragment", null)
        }
    }
}