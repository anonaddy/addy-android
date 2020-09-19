package host.stjin.anonaddy.ui.home

import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.utils.NumberUtils.roundOffDecimal
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class HomeFragment : Fragment() {

    private var networkHelper: NetworkHelper? = null
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
        networkHelper = NetworkHelper(requireContext())

        // We load values from local to make the app look quick and snappy!
        setOnClickListeners(root)

        getStatistics(root)
        // Called on OnResume()
        // getDataFromWeb(root, requireContext())

        return root
    }

    private fun getDataFromWeb(root: View, context: Context) {
        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getMostActiveAliases(root)
            getWebStatistics(root, context)
        }
    }

    override fun onResume() {
        super.onResume()
        getDataFromWeb(requireView(), requireContext())
    }

    private fun setOnClickListeners(root: View) {
        root.home_statistics_dismiss.setOnClickListener {
            root.home_statistics_LL.visibility = View.GONE
        }

        root.home_most_active_aliases_view_more.setOnClickListener {
            (activity as MainActivity).switchFragments(R.id.navigation_alias)
        }
    }

    private suspend fun getWebStatistics(root: View, context: Context) {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                User.userResource = user
                getStatistics(root)
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

                    // Sort by emails forwarded
                    list.sortByDescending { it.emails_forwarded }

                    // Get the top 5
                    val aliasList = list.take(5)
                    val aliasAdapter = AliasAdapter(aliasList, false)
                    aliasAdapter.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                        override fun onClick(pos: Int, aView: View) {
                            val intent = Intent(context, ManageAliasActivity::class.java)
                            // Pass data object in the bundle and populate details activity.
                            intent.putExtra("alias_id", aliasList[pos].id)
                            intent.putExtra("alias_forward_count", aliasList[pos].emails_forwarded)
                            intent.putExtra("alias_replied_sent_count", aliasList[pos].emails_replied)

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
                } else {
                    root.home_statistics_LL1.visibility = View.GONE
                    root.home_statistics_RL_lottieview.visibility = View.VISIBLE
                }
            }, activeOnly = true, includeDeleted = false)

        }

    }


    private fun getStatistics(root: View) {
        //  / 1024 / 1024 because api returns bytes
        val currMonthlyBandwidth = User.userResource.bandwidth.toDouble() / 1024 / 1024
        val maxMonthlyBandwidth = User.userResource.bandwidth_limit / 1024 / 1024

        setMonthlyBandwidthStatistics(root, currMonthlyBandwidth, maxMonthlyBandwidth)
        setAliasesStatistics(root, User.userResource.active_shared_domain_alias_count, User.userResource.active_shared_domain_alias_limit)
        setRecipientStatistics(root, User.userResource.recipient_count, User.userResource.recipient_limit)
    }

    private fun setAliasesStatistics(root: View, count: Int, maxAliases: Int) {
        root.home_statistics_aliases_progress.max = maxAliases * 100
        root.home_statistics_aliases_current.text = count.toString()
        root.home_statistics_aliases_max.text = if (maxAliases == 0) "∞" else maxAliases.toString()
        Handler().postDelayed({
            ObjectAnimator.ofInt(
                root.home_statistics_aliases_progress,
                "progress",
                count * 100
            )
                .setDuration(300)
                .start()
        }, 400)
    }

    private fun setMonthlyBandwidthStatistics(
        root: View,
        currMonthlyBandwidth: Double,
        maxMonthlyBandwidth: Int
    ) {
        root.home_statistics_monthly_bandwidth_progress.max =
            if (maxMonthlyBandwidth == 0) 0 else maxMonthlyBandwidth * 100


        root.home_statistics_monthly_bandwidth_current.text = "${roundOffDecimal(currMonthlyBandwidth)}MB"


        root.home_statistics_monthly_bandwidth_max.text =
            "${if (maxMonthlyBandwidth == 0) "∞" else maxMonthlyBandwidth.toString()}MB"


        ObjectAnimator.ofInt(
            root.home_statistics_monthly_bandwidth_progress,
            "progress",
            currMonthlyBandwidth.roundToInt() * 100
        )
            .setDuration(300)
            .start()
    }


    private fun setRecipientStatistics(root: View, currRecipients: Int, maxRecipient: Int) {
        root.home_statistics_recipients_progress.max =
            maxRecipient * 100
        root.home_statistics_recipients_current.text = currRecipients.toString()
        root.home_statistics_recipients_max.text =
            if (maxRecipient == 0) "∞" else maxRecipient.toString()
        ObjectAnimator.ofInt(
            root.home_statistics_recipients_progress,
            "progress",
            currRecipients * 100
        )
            .setDuration(300)
            .start()
    }
}