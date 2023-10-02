package host.stjin.anonaddy.ui.home

import android.content.*
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
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
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.FragmentHomeBinding
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.utils.NumberUtils.roundOffDecimal
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.ChartData
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch
import org.ocpsoft.prettytime.PrettyTime


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
        getStatistics()
        setSubscriptionText()
        setNsvListener()

        // Only run this once, not doing it in onresume as scrolling between the pages might trigger too much
        // API calls, user should swipe to refresh starting from v4.5.0
        getDataFromWeb(requireContext(), savedInstanceState)

        return root
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


    fun getDataFromWeb(context: Context, savedInstanceState: Bundle?) {
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
                getStatistics()
                setSubscriptionText()
            } else {
                getChartData()
                getWebStatistics(context)
            }
        }
    }

    private suspend fun getChartData() {
        networkHelper?.getChartData { chartData: ChartData?, result: String? ->
            if (chartData != null) {
                setChartData(chartData)
            } else {
                if ((activity as MainActivity).resources.getBoolean(R.bool.isTablet)) {
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


        val chartEntryModel = if (BuildConfig.DEBUG) {
            val random = java.util.Random()
            val forwardedData = entriesOf(
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20)
            )
            val repliesData = entriesOf(
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20)
            )
            val sendsData = entriesOf(
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20)
            )
            entryModelOf(
                forwardedData, repliesData, sendsData
            )
        } else {
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
            entryModelOf(
                forwardedData, repliesData, sendsData
            )
        }


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
        //
    }

    private suspend fun getWebStatistics(context: Context) {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                (activity?.application as AddyIoApp).userResource = user
                getStatistics()
                setSubscriptionText()
            } else {


                if ((activity as MainActivity).resources.getBoolean(R.bool.isTablet)) {
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


    private fun setSubscriptionText() {

        when {
            (activity?.application as AddyIoApp).userResource.subscription == null -> {
                binding.homeStatCardSubscription.visibility = View.GONE
            }

            (activity?.application as AddyIoApp).userResource.subscription_ends_at != null -> {
                binding.homeStatCardSubscription.visibility = View.VISIBLE
                binding.homeStatCardSubscription.setTitle(
                    resources.getString(
                        R.string.subscription_user,
                        (activity?.application as AddyIoApp).userResource.subscription
                    )
                )
                binding.homeStatCardSubscription.setDescription(
                    resources.getString(
                        R.string.subscription_user_until,
                        (activity?.application as AddyIoApp).userResource.subscription,
                        DateTimeUtils.turnStringIntoLocalString(
                            (activity?.application as AddyIoApp).userResource.subscription_ends_at,
                            DateTimeUtils.DATETIMEUTILS.DATE
                        )
                    )
                )
            }

            else -> {
                binding.homeStatCardSubscription.visibility = View.VISIBLE
                binding.homeStatCardSubscription.setTitle(
                    resources.getString(
                        R.string.subscription_user,
                        (activity?.application as AddyIoApp).userResource.subscription
                    )
                )
                binding.homeStatCardSubscription.setDescription(
                    resources.getString(R.string.subscription_user, (activity?.application as AddyIoApp).userResource.subscription)
                )
            }
        }
    }


    private fun getStatistics() {
        //  / 1024 / 1024 because api returns bytes
        val currMonthlyBandwidth = (activity?.application as AddyIoApp).userResource.bandwidth.toDouble() / 1024 / 1024
        val maxMonthlyBandwidth = (activity?.application as AddyIoApp).userResource.bandwidth_limit / 1024 / 1024

        binding.homeStatCardSharedDomainAliases.setDescription(
            this.resources.getString(
                R.string.d_slash_d,
                (activity?.application as AddyIoApp).userResource.active_shared_domain_alias_count,
                (activity?.application as AddyIoApp).userResource.active_shared_domain_alias_limit
            )
        )
        binding.homeStatCardRecipients.setDescription(
            this.resources.getString(
                R.string.d_slash_d,
                (activity?.application as AddyIoApp).userResource.recipient_count,
                (activity?.application as AddyIoApp).userResource.recipient_limit
            )
        )
        binding.homeStatCardDomains.setDescription(
            this.resources.getString(
                R.string.d_slash_d,
                (activity?.application as AddyIoApp).userResource.active_domain_count,
                (activity?.application as AddyIoApp).userResource.active_domain_count
            )
        )
        binding.homeStatCardUsernames.setDescription(
            this.resources.getString(
                R.string.d_slash_d,
                (activity?.application as AddyIoApp).userResource.username_count,
                (activity?.application as AddyIoApp).userResource.username_limit
            )
        )
        binding.homeStatCardRules.setDescription(
            this.resources.getString(
                R.string.d_slash_d,
                (activity?.application as AddyIoApp).userResource.active_rule_count,
                (activity?.application as AddyIoApp).userResource.active_rule_limit
            )
        )

        // Bandwidth could be unlimited
        val bandwidthText = if (maxMonthlyBandwidth == 0) {
            this.resources.getString(R.string.home_bandwidth_text, roundOffDecimal(currMonthlyBandwidth).toString(), "∞")
        } else {
            this.resources.getString(R.string.home_bandwidth_text, currMonthlyBandwidth.toString(), maxMonthlyBandwidth.toString())
        }

        binding.homeStatCardBandwidth.setDescription(bandwidthText)

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}