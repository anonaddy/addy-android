package host.stjin.anonaddy.ui.home

import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.databinding.FragmentHomeBinding
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.utils.NumberUtils.roundOffDecimal
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class HomeFragment : Fragment() {

    private var networkHelper: NetworkHelper? = null
    private var shouldAnimateRecyclerview: Boolean = true

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

    private fun getDataFromWeb(context: Context) {
        binding.homeStatisticsLL1.visibility = View.VISIBLE
        binding.homeStatisticsRLLottieview.visibility = View.GONE

        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getMostActiveAliases()
            getWebStatistics(context)
        }
    }

    // Update information when coming back, such as aliases and statistics
    override fun onResume() {
        super.onResume()
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
                User.userResource = user
                getStatistics()
            } else {
                val bottomNavView: BottomNavigationView? =
                    activity?.findViewById(R.id.nav_view)
                val snackbar = bottomNavView?.let {
                    Snackbar.make(
                        it,
                        context.resources.getString(R.string.error_obtaining_user) + "\n" + result,
                        Snackbar.LENGTH_SHORT
                    ).apply {
                        anchorView = bottomNavView
                    }
                }
                if (SettingsManager(false, context).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
                    snackbar?.setAction(R.string.logs) {
                        val intent = Intent(context, LogViewerActivity::class.java)
                        startActivity(intent)
                    }
                }
                snackbar?.show()
            }
        }
    }

    private lateinit var aliasAdapter: AliasAdapter
    private suspend fun getMostActiveAliases() {
        binding.homeMostActiveAliasesRecyclerview.apply {

            networkHelper?.getAliases({ list ->

                // Check if there are new aliases since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                if (::aliasAdapter.isInitialized && list == aliasAdapter.getList()) {
                    return@getAliases
                }

                layoutManager = if (context.resources.getBoolean(R.bool.isTablet)) {
                    // set a GridLayoutManager for tablets
                    GridLayoutManager(activity, 2)
                } else {
                    LinearLayoutManager(activity)
                }

                if (shouldAnimateRecyclerview) {
                    shouldAnimateRecyclerview = false
                    val resId: Int = R.anim.layout_animation_fall_down
                    val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                    binding.homeMostActiveAliasesRecyclerview.layoutAnimation = animation
                }

                if (list != null) {
                    if (list.size > 0) {
                        binding.homeNoAliases.visibility = View.GONE
                    } else {
                        binding.homeNoAliases.visibility = View.VISIBLE
                    }

                    // Sort by emails forwarded
                    list.sortByDescending { it.emails_forwarded }

                    // Get the top 5
                    val aliasList = list.take(5)
                    val aliasAdapter = AliasAdapter(aliasList, context)
                    aliasAdapter.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                        override fun onClick(pos: Int) {
                            val intent = Intent(context, ManageAliasActivity::class.java)
                            // Pass data object in the bundle and populate details activity.
                            intent.putExtra("alias_id", aliasList[pos].id)
                            startActivity(intent)
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
                                Snackbar.make(
                                    it,
                                    context.resources.getString(R.string.copied_alias),
                                    Snackbar.LENGTH_SHORT
                                ).apply {
                                    anchorView = bottomNavView
                                }.show()
                            }
                        }

                    })
                    adapter = aliasAdapter
                    binding.homeMostActiveAliasesRecyclerview.hideShimmerAdapter()
                } else {
                    binding.homeStatisticsLL1.visibility = View.GONE
                    binding.homeStatisticsRLLottieview.visibility = View.VISIBLE
                }
            }, activeOnly = true, includeDeleted = false)

        }

    }


    private fun getStatistics() {
        //  / 1024 / 1024 because api returns bytes
        val currMonthlyBandwidth = User.userResource.bandwidth.toDouble() / 1024 / 1024
        val maxMonthlyBandwidth = User.userResource.bandwidth_limit / 1024 / 1024

        setMonthlyBandwidthStatistics(currMonthlyBandwidth, maxMonthlyBandwidth)
        setAliasesStatistics(User.userResource.active_shared_domain_alias_count, User.userResource.active_shared_domain_alias_limit)
        setRecipientStatistics(User.userResource.recipient_count, User.userResource.recipient_limit)
    }

    private fun setAliasesStatistics(count: Int, maxAliases: Int) {
        binding.homeStatisticsAliasesProgress.max = maxAliases * 100
        binding.homeStatisticsAliasesCurrent.text = count.toString()
        binding.homeStatisticsAliasesMax.text = if (maxAliases == 0) "∞" else maxAliases.toString()
        Handler(Looper.getMainLooper()).postDelayed({
            ObjectAnimator.ofInt(
                binding.homeStatisticsAliasesProgress,
                "progress",
                count * 100
            )
                .setDuration(300)
                .start()
        }, 400)
    }

    private fun setMonthlyBandwidthStatistics(
        currMonthlyBandwidth: Double,
        maxMonthlyBandwidth: Int
    ) {
        binding.homeStatisticsMonthlyBandwidthProgress.max =
            if (maxMonthlyBandwidth == 0) 0 else maxMonthlyBandwidth * 100


        binding.homeStatisticsMonthlyBandwidthCurrent.text =
            this.resources.getString(R.string._sMB, roundOffDecimal(currMonthlyBandwidth).toString())


        binding.homeStatisticsMonthlyBandwidthMax.text =
            if (maxMonthlyBandwidth == 0) this.resources.getString(R.string._sMB, "∞") else this.resources.getString(
                R.string._sMB,
                maxMonthlyBandwidth.toString()
            )


        ObjectAnimator.ofInt(
            binding.homeStatisticsMonthlyBandwidthProgress,
            "progress",
            currMonthlyBandwidth.roundToInt() * 100
        )
            .setDuration(300)
            .start()
    }


    private fun setRecipientStatistics(currRecipients: Int, maxRecipient: Int) {
        binding.homeStatisticsRecipientsProgress.max =
            maxRecipient * 100
        binding.homeStatisticsRecipientsCurrent.text = currRecipients.toString()
        binding.homeStatisticsRecipientsMax.text =
            if (maxRecipient == 0) "∞" else maxRecipient.toString()
        ObjectAnimator.ofInt(
            binding.homeStatisticsRecipientsProgress,
            "progress",
            currRecipients * 100
        )
            .setDuration(300)
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}