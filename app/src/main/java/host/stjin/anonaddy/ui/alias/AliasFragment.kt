package host.stjin.anonaddy.ui.alias

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.core.widget.NestedScrollView
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
import host.stjin.anonaddy.models.Aliases
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import kotlinx.android.synthetic.main.fragment_alias.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class AliasFragment : Fragment(), AddAliasBottomDialogFragment.AddAliasBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var shouldAnimateRecyclerview: Boolean = true


    companion object {
        fun newInstance() = AliasFragment()
    }

    private val addAliasBottomDialogFragment: AddAliasBottomDialogFragment =
        AddAliasBottomDialogFragment.newInstance()

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
        val root = inflater.inflate(R.layout.fragment_alias, container, false)
        settingsManager = SettingsManager(true, requireContext())
        networkHelper = NetworkHelper(requireContext())


        // Load values from local to make the app look quick and snappy!
        setStatisticsFromLocal(root, requireContext())
        setOnClickListeners(root)
        setOnScrollViewListener(root)

        // Called on OnResume() as well, call this in onCreateView so the viewpager can serve loaded fragments
        getDataFromWeb(root)

        return root
    }


    private fun setOnScrollViewListener(root: View) {

        root.alias_scrollview?.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, oldScrollY ->

            val scrollViewHeight: Double = (v.getChildAt(0).bottom - v.height).toDouble()
            val getScrollY: Double = scrollY.toDouble()
            val scrollPosition = getScrollY / scrollViewHeight * 100.0
            //Log.i("scrollview", "scroll Percent Y: " + scrollPosition.toInt())
            val percentage = scrollPosition.toInt()

            if (percentage in 6..100) { // If between 6 and 100, show the fab
                root.alias_fragment_add_alias_fab.show()
            } else if (percentage in 0..5) { // If between 0 and 5, hide the fab
                root.alias_fragment_add_alias_fab.hide()
            }
        })


    }

    private fun getDataFromWeb(root: View) {
        root.alias_list_LL1.visibility = View.VISIBLE
        root.alias_statistics_RL_lottieview.visibility = View.GONE

        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllAliasesAndSetStatistics(root)
            getAllDeletedAliases(root)
        }
    }

    // Update list of aliases when coming back
    override fun onResume() {
        super.onResume()
        getDataFromWeb(requireView())
    }

    private fun setOnClickListeners(root: View) {
        root.alias_statistics_dismiss.setOnClickListener {
            root.alias_statistics_LL.visibility = View.GONE
        }

        root.alias_add_alias.setOnClickListener {
            if (!addAliasBottomDialogFragment.isAdded) {
                addAliasBottomDialogFragment.show(
                    childFragmentManager,
                    "addAliasBottomDialogFragment"
                )
            }
        }

        root.alias_fragment_add_alias_fab.setOnClickListener {
            if (!addAliasBottomDialogFragment.isAdded) {
                addAliasBottomDialogFragment.show(
                    childFragmentManager,
                    "addAliasBottomDialogFragment"
                )
            }
        }

        root.alias_show_deleted_alias_toggle_LL.setOnClickListener {
            if (root.alias_deleted_aliases_recyclerview.visibility == View.GONE) {
                root.alias_deleted_aliases_recyclerview.visibility = View.VISIBLE
                root.alias_show_deleted_alias_toggle.setImageResource(R.drawable.ic_menu_up_outline)
            } else {
                root.alias_deleted_aliases_recyclerview.visibility = View.GONE
                root.alias_show_deleted_alias_toggle.setImageResource(R.drawable.ic_menu_down_outline)
            }
        }
    }

    private suspend fun getAllAliasesAndSetStatistics(root: View) {
        root.alias_all_aliases_recyclerview.apply {

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
                layoutAnimation = animation
            }


            networkHelper?.getAliases({ list ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                /**
                 * Count the totals for the aliases statistics
                 * Done here because otherwise would need to get the aliases twice from the web
                 */

                if (list != null) {
                    var forwarded = 0
                    var blocked = 0
                    var replied = 0
                    var sent = 0

                    for (alias in list) {
                        forwarded += alias.emails_forwarded
                        blocked += alias.emails_blocked
                        replied += alias.emails_replied
                        sent += alias.emails_sent
                    }

                    // Set the actual statistics
                    setAliasesStatistics(root, context, forwarded, blocked, replied, sent)



                    if (list.size > 0) {
                        root.alias_no_aliases.visibility = View.GONE
                    } else {
                        root.alias_no_aliases.visibility = View.VISIBLE
                    }


                    /**
                     * ALIAS LIST
                     */
                    val aliasAdapter = AliasAdapter(list, true)
                    aliasAdapter.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                        override fun onClick(pos: Int, aView: View) {
                            val intent = Intent(context, ManageAliasActivity::class.java)
                            // Pass data object in the bundle and populate details activity.
                            intent.putExtra("alias_id", list[pos].id)
                            intent.putExtra("alias_forward_count", list[pos].emails_forwarded)
                            intent.putExtra("alias_replied_sent_count", list[pos].emails_replied)

                            val options: ActivityOptionsCompat =
                                ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    requireActivity(),
                                    aView,
                                    list[pos].id
                                )

                            startActivity(intent, options.toBundle())
                        }

                        override fun onClickCopy(pos: Int, aView: View) {
                            val clipboard: ClipboardManager =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val aliasEmailAddress = list[pos].email
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
                    hideShimmerAdapter()
                } else {
                    root.alias_list_LL1.visibility = View.GONE
                    root.alias_statistics_RL_lottieview.visibility = View.VISIBLE
                }
            }, activeOnly = false, includeDeleted = false)
        }

    }


    private suspend fun getAllDeletedAliases(root: View) {
        root.alias_deleted_aliases_recyclerview.apply {

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
                layoutAnimation = animation
            }


            networkHelper?.getAliases({ list ->

                if (list != null) {

                    /**
                     * Seperate the deleted and non-deleted aliases
                     */


                    val onlyDeletedList: ArrayList<Aliases> = arrayListOf()

                    if (list.size > 0) {
                        root.alias_no_deleted_aliases.visibility = View.GONE
                        for (alias in list) {
                            if (alias.deleted_at != null) {
                                onlyDeletedList.add(alias)
                            }
                        }
                    } else {
                        root.alias_no_deleted_aliases.visibility = View.VISIBLE
                    }


                    /**
                     * ALIAS LIST
                     */
                    val aliasAdapter = AliasAdapter(onlyDeletedList, true)
                    aliasAdapter.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                        override fun onClick(pos: Int, aView: View) {
                            val intent = Intent(context, ManageAliasActivity::class.java)
                            // Pass data object in the bundle and populate details activity.
                            intent.putExtra("alias_id", onlyDeletedList[pos].id)
                            intent.putExtra("alias_forward_count", onlyDeletedList[pos].emails_forwarded)
                            intent.putExtra("alias_replied_sent_count", onlyDeletedList[pos].emails_replied)

                            val options: ActivityOptionsCompat =
                                ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    requireActivity(),
                                    aView,
                                    onlyDeletedList[pos].id
                                )

                            startActivity(intent, options.toBundle())
                        }

                        override fun onClickCopy(pos: Int, aView: View) {
                            val clipboard: ClipboardManager =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val aliasEmailAddress = onlyDeletedList[pos].email
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
                    hideShimmerAdapter()
                } else {
                    root.alias_list_LL1.visibility = View.GONE
                    root.alias_statistics_RL_lottieview.visibility = View.VISIBLE
                }
            }, activeOnly = false, includeDeleted = true)
        }

    }

    private fun setStatisticsFromLocal(root: View, context: Context) {
        var statCurrentEmailsForwardedTotalCount = 0
        var statCurrentEmailsBlockedTotalCount = 0
        var statCurrentEmailsRepliedTotalCount = 0
        var statCurrentEmailsSentTotalCount = 0


        // These settings are related to AnonAddy's service. Thus encrypted
        settingsManager?.getSettingsInt(SettingsManager.PREFS.STAT_CURRENT_EMAILS_FORWARDED_TOTAL_COUNT)?.let {
            statCurrentEmailsForwardedTotalCount = it
        }

        settingsManager?.getSettingsInt(SettingsManager.PREFS.STAT_CURRENT_EMAILS_BLOCKED_TOTAL_COUNT)?.let {
            statCurrentEmailsBlockedTotalCount = it
        }

        settingsManager?.getSettingsInt(SettingsManager.PREFS.STAT_CURRENT_EMAILS_REPLIED_TOTAL_COUNT)?.let {
            statCurrentEmailsRepliedTotalCount = it
        }

        settingsManager?.getSettingsInt(SettingsManager.PREFS.STAT_CURRENT_EMAILS_SENT_TOTAL_COUNT)?.let {
            statCurrentEmailsSentTotalCount = it
        }

        root.alias_replied_sent_stats_textview.text =
            context.resources.getString(R.string.replied_replied_sent_stat, statCurrentEmailsRepliedTotalCount, statCurrentEmailsSentTotalCount)
        root.alias_forwarded_blocked_stats_textview.text =
            context.resources.getString(
                R.string.replied_forwarded_blocked_stat,
                statCurrentEmailsForwardedTotalCount,
                statCurrentEmailsBlockedTotalCount
            )


    }

    private fun setAliasesStatistics(
        root: View,
        context: Context,
        forwarded: Int,
        blocked: Int,
        replied: Int,
        sent: Int
    ) {
        settingsManager?.putSettingsInt(SettingsManager.PREFS.STAT_CURRENT_EMAILS_FORWARDED_TOTAL_COUNT, forwarded)
        settingsManager?.putSettingsInt(SettingsManager.PREFS.STAT_CURRENT_EMAILS_BLOCKED_TOTAL_COUNT, blocked)
        settingsManager?.putSettingsInt(SettingsManager.PREFS.STAT_CURRENT_EMAILS_REPLIED_TOTAL_COUNT, replied)
        settingsManager?.putSettingsInt(SettingsManager.PREFS.STAT_CURRENT_EMAILS_SENT_TOTAL_COUNT, sent)

        root.alias_replied_sent_stats_textview.text =
            context.resources.getString(R.string.replied_replied_sent_stat, replied, sent)
        root.alias_forwarded_blocked_stats_textview.text =
            context.resources.getString(
                R.string.replied_forwarded_blocked_stat,
                forwarded,
                blocked
            )

    }

    override fun onAdded() {
        addAliasBottomDialogFragment.dismiss()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb(requireView())
    }


}