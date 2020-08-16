package host.stjin.anonaddy.ui.home

import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import host.stjin.anonaddy.MainActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class HomeFragment : Fragment() {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var shouldAnimateRecyclerview: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialFadeThrough()
        enterTransition = MaterialFadeThrough()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val context = this.context
        settingsManager = SettingsManager(true, requireContext())
        networkHelper = NetworkHelper(requireContext())

        // We load values from local to make the app look quick and snappy!
        setStatisticsFromLocal(root)
        setOnClickListeners(root)

        getDataFromWeb(root)

        return root
    }

    private fun getDataFromWeb(root: View) {
        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getMostActiveAliases(root)
            getStatisticsFromWeb(root)
        }
    }

    override fun onResume() {
        super.onResume()
        getDataFromWeb(requireView())
    }

    private fun setOnClickListeners(root: View) {
        root.home_statistics_dismiss.setOnClickListener {
            root.home_statistics_LL.visibility = View.GONE
        }

        root.home_most_active_aliases_view_more.setOnClickListener {
            (activity as MainActivity).switchFragments(R.id.navigation_alias)
        }
    }

    private suspend fun getMostActiveAliases(root: View) {
        root.home_most_active_aliases_recyclerview.apply {

            if (itemDecorationCount > 0) {
                addItemDecoration(
                    DividerItemDecoration(
                        this.context,
                        (layoutManager as LinearLayoutManager).orientation
                    )
                )
            }
            // set a LinearLayoutManager to handle Android
            // RecyclerView behavior
            layoutManager = LinearLayoutManager(activity)
            // set the custom adapter to the RecyclerView

            if (shouldAnimateRecyclerview) {
                shouldAnimateRecyclerview = false
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                root.home_most_active_aliases_recyclerview.layoutAnimation = animation
            }

            networkHelper?.getAliases({ list ->

                if (list != null) {
                    if (list.size > 0) {
                        root.home_no_aliases.visibility = View.GONE
                    } else {
                        root.home_no_aliases.visibility = View.VISIBLE
                    }
                }

                // Sort by emails forwarded
                list?.sortByDescending { it.emails_forwarded }

                // Get the top 5
                val aliasList = list?.take(5)
                val aliasAdapter = aliasList?.let { AliasAdapter(it, false) }
                aliasAdapter?.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                    override fun onClick(pos: Int, aView: View) {
                        val intent = Intent(context, ManageAliasActivity::class.java)
                        // Pass data object in the bundle and populate details activity.
                        intent.putExtra("alias_id", aliasList[pos].id)
                        intent.putExtra("alias_email", aliasList[pos].email)
                        intent.putExtra("alias_deleted", aliasList[pos].deleted_at)
                        intent.putExtra("alias_forward_count", aliasList[pos].emails_forwarded)
                        intent.putExtra("aliasRepliedSentCount", aliasList[pos].emails_replied)

                        val options: ActivityOptionsCompat =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                requireActivity(),
                                aView,
                                aliasList[pos].id
                            )

                        startActivity(intent, options.toBundle())
                    }

                    override fun onClickCopy(pos: Int, aView: View) {
                        val clipboard: ClipboardManager? =
                            context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val aliasEmailAddress = aliasList[pos].email
                        val clip = ClipData.newPlainText("alias", aliasEmailAddress)
                        clipboard?.setPrimaryClip(clip)

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
                root.home_most_active_aliases_recyclerview.hideShimmerAdapter()
            }, activeOnly = true, includeDeleted = false)

        }

    }

    private fun setStatisticsFromLocal(root: View) {
        // These settings are related to AnonAddy's service. Thus encrypted
        settingsManager?.getSettingsFloat("stat_current_monthly_bandwidth")?.let {
            setMonthlyBandwidthStatistics(
                root,
                it, settingsManager?.getSettingsFloat("stat_max_monthly_bandwidth")!!
            )
        }

        settingsManager?.getSettingsInt("stat_current_recipients_count")?.let {
            setRecipientStatistics(
                root,
                it, settingsManager?.getSettingsInt("stat_max_recipients_count")!!
            )
        }
        settingsManager?.getSettingsInt("stat_current_aliases_count")?.let {
            setAliasesStatistics(
                root,
                it, settingsManager?.getSettingsInt("stat_max_aliases_count")!!
            )
        }
    }

    private suspend fun getStatisticsFromWeb(root: View) {
        val currMonthlyBandwidth = 10.2f
        val maxMonthlyBandwidth = 50f

        setMonthlyBandwidthStatistics(root, currMonthlyBandwidth, maxMonthlyBandwidth)
        settingsManager?.putSettingsFloat(
            "stat_current_monthly_bandwidth",
            currMonthlyBandwidth
        )
        settingsManager?.putSettingsFloat(
            "stat_max_monthly_bandwidth",
            maxMonthlyBandwidth
        )

        // ================

        networkHelper?.getRecipientCount { count ->
            if (count != null) {
                val maxRecipient = 20
                setRecipientStatistics(root, count, maxRecipient)
                settingsManager?.putSettingsInt("stat_current_recipients_count", count)
                settingsManager?.putSettingsInt("stat_max_recipients_count", maxRecipient)
            }
        }


        // ================
        networkHelper?.getAliasesCount({ count ->
            if (count != null) {
                val maxAliases = -1
                setAliasesStatistics(root, count, maxAliases)
                settingsManager?.putSettingsInt("stat_current_aliases_count", count)
                settingsManager?.putSettingsInt("stat_max_aliases_count", maxAliases)
            }
        })


    }

    private fun setAliasesStatistics(root: View, count: Int, maxAliases: Int) {
        root.home_statistics_aliases_progress.max = if (maxAliases == -1) 0 else maxAliases * 100
        root.home_statistics_aliases_current.text = count.toString()
        root.home_statistics_aliases_max.text = if (maxAliases == -1) "∞" else maxAliases.toString()
        Handler().postDelayed({
            ObjectAnimator.ofInt(
                root.home_statistics_aliases_progress,
                "progress",
                if (count == -1) 0 else count * 100
            )
                .setDuration(300)
                .start()
        }, 400)
    }

    private fun setMonthlyBandwidthStatistics(
        root: View,
        currMonthlyBandwidth: Float,
        maxMonthlyBandwidth: Float
    ) {
        root.home_statistics_monthly_bandwidth_progress.max =
            if (maxMonthlyBandwidth.roundToInt() == -1) 0 else maxMonthlyBandwidth.roundToInt() * 100
        root.home_statistics_monthly_bandwidth_current.text = "${currMonthlyBandwidth}MB"
        root.home_statistics_monthly_bandwidth_max.text =
            "${if (maxMonthlyBandwidth == -1f) "∞" else maxMonthlyBandwidth.roundToInt()
                .toString()}MB"


        ObjectAnimator.ofInt(
            root.home_statistics_monthly_bandwidth_progress,
            "progress",
            if (currMonthlyBandwidth == -1f) 0 else currMonthlyBandwidth.roundToInt() * 100
        )
            .setDuration(300)
            .start()
    }

    private fun setRecipientStatistics(root: View, currRecipients: Int, maxRecipient: Int) {
        root.home_statistics_recipients_progress.max =
            if (maxRecipient == -1) 0 else maxRecipient * 100
        root.home_statistics_recipients_current.text = currRecipients.toString()
        root.home_statistics_recipients_max.text =
            if (maxRecipient == -1) "∞" else maxRecipient.toString()
        ObjectAnimator.ofInt(
            root.home_statistics_recipients_progress,
            "progress",
            if (currRecipients == -1) 0 else currRecipients * 100
        )
            .setDuration(300)
            .start()
    }
}