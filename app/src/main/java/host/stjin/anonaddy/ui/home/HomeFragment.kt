package host.stjin.anonaddy.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView.OnScrollChangeListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.patrykandpatrick.vico.core.entry.entriesOf
import com.patrykandpatrick.vico.core.entry.entryModelOf
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.FragmentHomeBinding
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.ui.alias.AliasFragment
import host.stjin.anonaddy.ui.customviews.DashboardStatCardView
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.NumberUtils.roundOffDecimal
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.ChartData
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


class HomeFragment : Fragment() {

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
        val root = binding.root
        networkHelper = NetworkHelper(requireContext())

        // load values from local to make the app look quick and snappy!
        setOnClickListeners()
        setStatistics()
        setNsvListener()

        setGridLayout()


        Handler(Looper.getMainLooper()).postDelayed({
            // Only run this once, not doing it in onresume as scrolling between the pages might trigger too much
            // API calls, user should swipe to refresh starting from v4.5.0
            getDataFromWeb(savedInstanceState)
        }, 5000)

        return root
    }

    private fun setGridLayout() {
        binding.homeStatsGridlayout.columnCount = ScreenSizeUtils.calculateNoOfColumns(requireContext())
    }


    private var chartData: ChartData? = null
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        val json = gson.toJson(chartData)
        outState.putString("chartData", json)
    }


    private val mScrollUpBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            binding.homeStatisticsNSV.post { binding.homeStatisticsNSV.fullScroll(ScrollView.FOCUS_UP) }
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
                val chartData = savedInstanceState.getString("chartData")
                if (chartData!!.isNotEmpty()) {
                    val gson = Gson()
                    val data: ChartData = gson.fromJson(chartData, ChartData::class.java)
                    setChartData(data)
                }

                // (activity?.application as AddyIoApp).userResource is not being cleared upon activity-creation,
                // no need to obtain this from savedInstanceState
                setStatistics()
            } else {
                getChartData()
                getWebStatistics()
            }
        }
    }

    private suspend fun getChartData() {
        networkHelper?.getChartData { chartData: ChartData?, result: String? ->
            if (chartData != null) {
                setChartData(chartData)
            } else {
                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        requireContext().resources.getString(R.string.error_obtaining_chart_data) + "\n" + result,
                        (activity as MainActivity).findViewById(R.id.main_container),
                        LoggingHelper.LOGFILES.DEFAULT
                    ).show()
                } else {
                    val bottomNavView: BottomNavigationView? =
                        activity?.findViewById(R.id.nav_view)
                    bottomNavView?.let {
                        SnackbarHelper.createSnackbar(
                            requireContext(),
                            requireContext().resources.getString(R.string.error_obtaining_chart_data) + "\n" + result,
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

    private fun setChartData(data: ChartData) {
        chartData = data
        //val numberStr = data.forwardsData.map { FloatEntry(0,it.toFloat()) }


        val forwardedData = entriesOf(
            data.forwardsData[0],
            data.forwardsData[1],
            data.forwardsData[2],
            data.forwardsData[3],
            data.forwardsData[4],
            data.forwardsData[5],
            data.forwardsData[6]
        )
        val repliesData = entriesOf(
            data.repliesData[0],
            data.repliesData[1],
            data.repliesData[2],
            data.repliesData[3],
            data.repliesData[4],
            data.repliesData[5],
            data.repliesData[6]
        )
        val sendsData = entriesOf(
            data.sendsData[0],
            data.sendsData[1],
            data.sendsData[2],
            data.sendsData[3],
            data.sendsData[4],
            data.sendsData[5],
            data.sendsData[6]
        )
        val chartEntryModel = entryModelOf(
            forwardedData, repliesData, sendsData
        )



        binding.homeChartView1.setModel(chartEntryModel)
        binding.homeChartView1.setModel(chartEntryModel)
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
        binding.homeStatCardSharedDomainAliases.setOnLayoutClickedListener(object : DashboardStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                (activity as MainActivity).navigateTo(R.id.navigation_alias)
            }
        })

        binding.homeStatCardRecipients.setOnLayoutClickedListener(object : DashboardStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                (activity as MainActivity).navigateTo(R.id.navigation_recipients)
            }
        })

        binding.homeStatCardUsernames.setOnLayoutClickedListener(object : DashboardStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                (activity as MainActivity).navigateTo(R.id.navigation_usernames)
            }
        })

        binding.homeStatCardDomains.setOnLayoutClickedListener(object : DashboardStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                (activity as MainActivity).navigateTo(R.id.navigation_domains)
            }
        })

        binding.homeStatCardRules.setOnLayoutClickedListener(object : DashboardStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                (activity as MainActivity).navigateTo(R.id.navigation_rules)
            }
        })

        binding.homeStatCardBandwidth.setOnLayoutClickedListener(object : DashboardStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                val url = AddyIo.API_BASE_URL
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        })


        binding.homeStatCardTotalAliases.setOnLayoutClickedListener(object : DashboardStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = requireContext().resources.getString(R.string.apply_filter),
                    message = requireContext().resources.getString(R.string.apply_filter_desc),
                    icon = R.drawable.ic_filter,
                    neutralButtonText = requireContext().resources.getString(R.string.cancel),
                    positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                    positiveButtonAction = {
                        val aliasFragment: AliasFragment = (activity as MainActivity).supportFragmentManager.fragments[1] as AliasFragment
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

        binding.homeStatCardActiveAliases.setOnLayoutClickedListener(object : DashboardStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = requireContext().resources.getString(R.string.apply_filter),
                    message = requireContext().resources.getString(R.string.apply_filter_desc),
                    icon = R.drawable.ic_filter,
                    neutralButtonText = requireContext().resources.getString(R.string.cancel),
                    positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                    positiveButtonAction = {
                        val aliasFragment: AliasFragment = (activity as MainActivity).supportFragmentManager.fragments[1] as AliasFragment
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

        binding.homeStatCardInactiveAliases.setOnLayoutClickedListener(object : DashboardStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = requireContext().resources.getString(R.string.apply_filter),
                    message = requireContext().resources.getString(R.string.apply_filter_desc),
                    icon = R.drawable.ic_filter,
                    neutralButtonText = requireContext().resources.getString(R.string.cancel),
                    positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                    positiveButtonAction = {
                        val aliasFragment: AliasFragment = (activity as MainActivity).supportFragmentManager.fragments[1] as AliasFragment
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


        binding.homeStatCardDeletedAliases.setOnLayoutClickedListener(object : DashboardStatCardView.OnLayoutClickedListener {
            override fun onClick() {
                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = requireContext().resources.getString(R.string.apply_filter),
                    message = requireContext().resources.getString(R.string.apply_filter_desc),
                    icon = R.drawable.ic_filter,
                    neutralButtonText = requireContext().resources.getString(R.string.cancel),
                    positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                    positiveButtonAction = {
                        val aliasFragment: AliasFragment = (activity as MainActivity).supportFragmentManager.fragments[1] as AliasFragment
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

        binding.homeStatWatchedAliases.setOnLayoutClickedListener(object : DashboardStatCardView.OnLayoutClickedListener {
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
                            val aliasFragment: AliasFragment = (activity as MainActivity).supportFragmentManager.fragments[1] as AliasFragment
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


        val activeSharedDomainAliasLimitText = if ((activity?.application as AddyIoApp).userResource.active_shared_domain_alias_limit == 0) {
            "∞"
        } else {
            (activity?.application as AddyIoApp).userResource.active_shared_domain_alias_limit.toString()
        }
        binding.homeStatCardSharedDomainAliases.setDescription(
            this.resources.getString(
                R.string.d_slash_s,
                (activity?.application as AddyIoApp).userResource.active_shared_domain_alias_count,
                activeSharedDomainAliasLimitText
            )
        )
        if ((activity?.application as AddyIoApp).userResource.active_shared_domain_alias_limit > 0) {
            binding.homeStatCardSharedDomainAliases.setProgress((activity?.application as AddyIoApp).userResource.active_shared_domain_alias_count.toFloat() / (activity?.application as AddyIoApp).userResource.active_shared_domain_alias_limit.toFloat() * 100)
        }

        val recipientsLimitText = if ((activity?.application as AddyIoApp).userResource.recipient_limit == 0) {
            "∞"
        } else {
            (activity?.application as AddyIoApp).userResource.recipient_limit.toString()
        }
        binding.homeStatCardRecipients.setDescription(
            this.resources.getString(
                R.string.d_slash_s,
                (activity?.application as AddyIoApp).userResource.recipient_count,
                recipientsLimitText
            )
        )
        if ((activity?.application as AddyIoApp).userResource.recipient_limit > 0) {
            binding.homeStatCardRecipients.setProgress((activity?.application as AddyIoApp).userResource.recipient_count.toFloat() / (activity?.application as AddyIoApp).userResource.recipient_limit.toFloat() * 100)
        }


        val domainsLimitText = if ((activity?.application as AddyIoApp).userResource.active_domain_limit == 0) {
            "∞"
        } else {
            (activity?.application as AddyIoApp).userResource.active_domain_limit.toString()
        }
        binding.homeStatCardDomains.setDescription(
            this.resources.getString(
                R.string.d_slash_s,
                (activity?.application as AddyIoApp).userResource.active_domain_count,
                domainsLimitText
            )
        )
        if ((activity?.application as AddyIoApp).userResource.active_domain_limit > 0) {
            binding.homeStatCardDomains.setProgress((activity?.application as AddyIoApp).userResource.active_domain_count.toFloat() / (activity?.application as AddyIoApp).userResource.active_domain_limit.toFloat() * 100)
        }


        val usernamesLimitText = if ((activity?.application as AddyIoApp).userResource.username_limit == 0) {
            "∞"
        } else {
            (activity?.application as AddyIoApp).userResource.username_limit.toString()
        }
        binding.homeStatCardUsernames.setDescription(
            this.resources.getString(
                R.string.d_slash_s,
                (activity?.application as AddyIoApp).userResource.username_count,
                usernamesLimitText
            )
        )
        if ((activity?.application as AddyIoApp).userResource.username_limit > 0) {
            binding.homeStatCardUsernames.setProgress((activity?.application as AddyIoApp).userResource.username_count.toFloat() / (activity?.application as AddyIoApp).userResource.username_limit.toFloat() * 100)
        }

        val rulesLimitText = if ((activity?.application as AddyIoApp).userResource.active_rule_limit == 0) {
            "∞"
        } else {
            (activity?.application as AddyIoApp).userResource.active_rule_limit.toString()
        }
        binding.homeStatCardRules.setDescription(
            this.resources.getString(
                R.string.d_slash_s,
                (activity?.application as AddyIoApp).userResource.active_rule_count,
                rulesLimitText
            )
        )
        if ((activity?.application as AddyIoApp).userResource.active_rule_limit > 0) {
            binding.homeStatCardRules.setProgress((activity?.application as AddyIoApp).userResource.active_rule_count.toFloat() / (activity?.application as AddyIoApp).userResource.active_rule_limit.toFloat() * 100)
        }


        // Bandwidth could be unlimited
        val bandwidthText = if (maxMonthlyBandwidth == 0) {
            this.resources.getString(R.string.home_bandwidth_text, roundOffDecimal(currMonthlyBandwidth).toString(), "∞")
        } else {
            this.resources.getString(R.string.home_bandwidth_text, roundOffDecimal(currMonthlyBandwidth).toString(), maxMonthlyBandwidth.toString())
        }

        binding.homeStatCardBandwidth.setDescription(bandwidthText)

        if (maxMonthlyBandwidth > 0) {
            binding.homeStatCardBandwidth.setProgress(currMonthlyBandwidth.toFloat() / maxMonthlyBandwidth.toFloat() * 100)
        }


        // TODO take the totals and fill in the alias stats

        binding.homeStatCardTotalAliases.setDescription((activity?.application as AddyIoApp).userResource.total_aliases.toString())
        binding.homeStatCardActiveAliases.setDescription((activity?.application as AddyIoApp).userResource.total_active_aliases.toString())
        binding.homeStatCardInactiveAliases.setDescription((activity?.application as AddyIoApp).userResource.total_inactive_aliases.toString())
        binding.homeStatCardDeletedAliases.setDescription((activity?.application as AddyIoApp).userResource.total_deleted_aliases.toString())


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
}