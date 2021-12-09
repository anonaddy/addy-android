package host.stjin.anonaddy.ui.home

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import host.stjin.anonaddy.AnonAddyForAndroid
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.databinding.FragmentHomeBinding
import host.stjin.anonaddy.models.Aliases
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.utils.LoggingHelper
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.NumberUtils.roundOffDecimal
import host.stjin.anonaddy.utils.SnackbarHelper
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class HomeFragment : Fragment() {

    private var networkHelper: NetworkHelper? = null
    private var OneTimeRecyclerViewActions: Boolean = true

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

        // Called on OnResume(), prevent double calls
        //getDataFromWeb(root, requireContext())

        return root
    }


    private val mScrollUpBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            binding.homeStatisticsNSV.post { binding.homeStatisticsNSV.fullScroll(ScrollView.FOCUS_UP) }
        }
    }


    override fun onPause() {
        super.onPause()
        activity?.unregisterReceiver(mScrollUpBroadcastReceiver)
    }


    private fun getDataFromWeb(context: Context) {
        binding.homeStatisticsLL1.visibility = View.VISIBLE
        binding.homeStatisticsRLLottieview.visibility = View.GONE

        // Get the latest data in the background, and update the values when loaded
        viewLifecycleOwner.lifecycleScope.launch {
            getMostActiveAliases()
            getWebStatistics(context)
            // Set forceUpdate to false (if it was true) to prevent the lists from reloading every oneresume
            forceUpdate = false
        }
    }

    // Update information when coming back, such as aliases and statistics
    override fun onResume() {
        super.onResume()
        activity?.registerReceiver(mScrollUpBroadcastReceiver, IntentFilter("scroll_up"))
        getDataFromWeb(requireContext())
    }

    private fun setOnClickListeners() {
        binding.homeMostActiveAliasesViewMore.setOnClickListener {
            (activity as MainActivity).switchFragments(R.id.navigation_alias)
        }
    }

    private suspend fun getWebStatistics(context: Context) {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                (activity?.application as AnonAddyForAndroid).userResource = user
                getStatistics()
            } else {
                val bottomNavView: BottomNavigationView? =
                    activity?.findViewById(R.id.nav_view)
                bottomNavView?.let {
                    SnackbarHelper.createSnackbar(
                        context,
                        context.resources.getString(R.string.error_obtaining_user) + "\n" + result,
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

    // This value is there to force updating the alias recyclerview in case "Watch alias" has been enabled.
    private var forceUpdate = false

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            if (data != null) {
                if (data.getBooleanExtra("should_update", false)) {
                    forceUpdate = true
                }
            }
        }
    }

    private lateinit var aliasAdapter: AliasAdapter
    private var previousList: ArrayList<Aliases> = arrayListOf()
    private suspend fun getMostActiveAliases() {
        binding.homeMostActiveAliasesRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false

                shimmerLayoutManager = if (this.resources.getBoolean(R.bool.isTablet)) {
                    // set a GridLayoutManager for tablets
                    GridLayoutManager(activity, 2)
                } else {
                    LinearLayoutManager(activity)
                }

                layoutManager = if (this.resources.getBoolean(R.bool.isTablet)) {
                    // set a GridLayoutManager for tablets
                    GridLayoutManager(activity, 2)
                } else {
                    LinearLayoutManager(activity)
                }

                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation

                showShimmer()
            }

            // TODO This needs to have a filter to get the most active aliases. Due to pagination this won't work properly anymore
            networkHelper?.getAliases({ list ->

                // Check if there are new aliases since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                // Unless forceUpdate is true. If forceupdate is true, always update
                if (::aliasAdapter.isInitialized && list?.data == previousList && !forceUpdate) {
                    return@getAliases
                }


                if (list != null) {
                    previousList.clear()
                    previousList.addAll(list.data)

                    if (list.data.size > 0) {
                        binding.homeNoAliases.visibility = View.GONE
                    } else {
                        binding.homeNoAliases.visibility = View.VISIBLE
                    }

                    // Sort by emails forwarded
                    list.data.sortByDescending { it.emails_forwarded }

                    // Get the top 5
                    val aliasList = list.data.take(5)
                    aliasAdapter = AliasAdapter(aliasList, context)
                    aliasAdapter.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                        override fun onClick(pos: Int) {
                            val intent = Intent(context, ManageAliasActivity::class.java)
                            // Pass data object in the bundle and populate details activity.
                            intent.putExtra("alias_id", aliasList[pos].id)
                            resultLauncher.launch(intent)
                        }

                        override fun onClickCopy(pos: Int, aView: View) {
                            val clipboard: ClipboardManager =
                                context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            val aliasEmailAddress = aliasList[pos].email
                            val clip = ClipData.newPlainText("alias", aliasEmailAddress)
                            clipboard.setPrimaryClip(clip)

                            val bottomNavView: BottomNavigationView? =
                                activity?.findViewById(R.id.nav_view)
                            bottomNavView?.let {
                                SnackbarHelper.createSnackbar(context, context.resources.getString(R.string.copied_alias), it).apply {
                                    anchorView = bottomNavView
                                }.show()
                            }
                        }

                    })
                    adapter = aliasAdapter
                } else {
                    binding.homeStatisticsLL1.visibility = View.GONE
                    binding.homeStatisticsRLLottieview.visibility = View.VISIBLE
                }
                hideShimmer()
            }, activeOnly = true, includeDeleted = false)

        }

    }


    private fun getStatistics() {
        //  / 1024 / 1024 because api returns bytes
        val currMonthlyBandwidth = (activity?.application as AnonAddyForAndroid).userResource.bandwidth.toDouble() / 1024 / 1024
        val maxMonthlyBandwidth = (activity?.application as AnonAddyForAndroid).userResource.bandwidth_limit / 1024 / 1024

        setMonthlyBandwidthStatistics(currMonthlyBandwidth, maxMonthlyBandwidth)
        setAliasesStatistics(
            (activity?.application as AnonAddyForAndroid).userResource.active_shared_domain_alias_count,
            (activity?.application as AnonAddyForAndroid).userResource.active_shared_domain_alias_limit
        )
        setRecipientStatistics(
            (activity?.application as AnonAddyForAndroid).userResource.recipient_count,
            (activity?.application as AnonAddyForAndroid).userResource.recipient_limit
        )
    }


    private val STATISTICS_ANIMATION_DURATION = 500L
    private fun setAliasesStatistics(count: Int, maxAliases: Int) {
        binding.homeStatisticsAliasesProgress.max = maxAliases * 100
        binding.homeStatisticsAliasesMax.text = if (maxAliases == 0) "∞" else maxAliases.toString()

        try {
            startNumberCountAnimation(binding.homeStatisticsAliasesCurrent, count, "/")
        } catch (e: Exception) {
            binding.homeStatisticsAliasesCurrent.text = "$count /"
        }

        Handler(Looper.getMainLooper()).postDelayed({
            ObjectAnimator.ofInt(
                binding.homeStatisticsAliasesProgress,
                "progress",
                count * 100
            )
                .setDuration(STATISTICS_ANIMATION_DURATION)
                .start()
        }, 400)

    }

    @SuppressLint("SetTextI18n")
    private fun startNumberCountAnimation(textView: TextView, count: Int, suffix: String? = null) {
        if (textView.text != "$count$suffix") {
            val animator = ValueAnimator.ofInt(textView.text.toString().substringBefore(" ").toInt(), count)
            animator.duration = STATISTICS_ANIMATION_DURATION
            animator.addUpdateListener { animation -> textView.text = animation.animatedValue.toString() + " " + suffix }
            animator.start()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startBandwidthCountAnimation(textView: TextView, count: Float, suffix: String? = null) {
        if (textView.text != "$count$suffix") {
            val animator = ValueAnimator.ofFloat(textView.text.toString().substringBefore("MB ").toFloat(), count)
            animator.duration = STATISTICS_ANIMATION_DURATION
            animator.addUpdateListener { animation ->
                textView.text = roundOffDecimal(animation.animatedValue.toString().toDouble()).toString() + "MB " + suffix
            }
            animator.start()
        }
    }

    private fun setMonthlyBandwidthStatistics(
        currMonthlyBandwidth: Double,
        maxMonthlyBandwidth: Int
    ) {
        binding.homeStatisticsMonthlyBandwidthProgress.max =
            if (maxMonthlyBandwidth == 0) 0 else maxMonthlyBandwidth * 100

        binding.homeStatisticsMonthlyBandwidthMax.text =
            if (maxMonthlyBandwidth == 0) this.resources.getString(R.string._sMB, "∞") else this.resources.getString(
                R.string._sMB,
                maxMonthlyBandwidth.toString()
            )


        try {
            startBandwidthCountAnimation(binding.homeStatisticsMonthlyBandwidthCurrent, roundOffDecimal(currMonthlyBandwidth), "/")
        } catch (e: Exception) {
            val currentCount = this.resources.getString(R.string._sMB, roundOffDecimal(currMonthlyBandwidth).toString())
            binding.homeStatisticsMonthlyBandwidthCurrent.text = "$currentCount /"
        }


        ObjectAnimator.ofInt(
            binding.homeStatisticsMonthlyBandwidthProgress,
            "progress",
            currMonthlyBandwidth.roundToInt() * 100
        )
            .setDuration(STATISTICS_ANIMATION_DURATION)
            .start()
    }


    private fun setRecipientStatistics(currRecipients: Int, maxRecipient: Int) {
        binding.homeStatisticsRecipientsProgress.max =
            maxRecipient * 100

        binding.homeStatisticsRecipientsMax.text =
            if (maxRecipient == 0) "∞" else maxRecipient.toString()

        try {
            startNumberCountAnimation(binding.homeStatisticsRecipientsCurrent, currRecipients, "/")
        } catch (e: Exception) {
            binding.homeStatisticsRecipientsCurrent.text = "$currRecipients /"
        }

        ObjectAnimator.ofInt(
            binding.homeStatisticsRecipientsProgress,
            "progress",
            currRecipients * 100
        )
            .setDuration(STATISTICS_ANIMATION_DURATION)
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}