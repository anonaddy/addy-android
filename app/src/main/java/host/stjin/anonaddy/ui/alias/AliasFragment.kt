package host.stjin.anonaddy.ui.alias

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.android.synthetic.main.fragment_alias.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AliasFragment : Fragment(), AddAliasBottomDialogFragment.AddAliasBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null

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
        val context = this.context
        if (context != null) {
            settingsManager = SettingsManager(true, context)
            networkHelper = NetworkHelper(context)

            // We load values from local to make the app look quick and snappy!
            setStatisticsFromLocal(root, context)
            setOnClickListeners(root)

            // Get the latest data in the background, and update the values when loaded
            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                getAllAliasesAndSetStatistics(root)
            }

        }
        return root
    }

    private fun setOnClickListeners(root: View) {
        root.alias_statistics_dismiss.setOnClickListener {
            root.alias_statistics_LL.visibility = View.GONE
        }

        root.alias_add_alias.setOnClickListener {
            addAliasBottomDialogFragment.show(
                childFragmentManager,
                "addAliasBottomDialogFragment"
            )
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

            networkHelper?.getAliases({ list ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                /**
                 * Count the totals for the aliases statistics
                 * Done here because otherwise we would need to get the aliases twice from the web
                 */

                var forwarded = 0
                var blocked = 0
                var replied = 0
                var sent = 0
                if (list != null) {
                    for (alias in list) {
                        forwarded += alias.emails_forwarded
                        blocked += alias.emails_blocked
                        replied += alias.emails_replied
                        sent += alias.emails_sent
                    }
                }
                // Set the actual statistics
                setAliasesStatistics(root, context, forwarded, blocked, replied, sent)

                /**
                 * Seperate the deleted and non-deleted aliases
                 */


                val nonDeletedList: ArrayList<Aliases> = arrayListOf()
                val onlyDeletedList: ArrayList<Aliases> = arrayListOf()

                if (list != null) {
                    for (alias in list) {
                        if (alias.deleted_at == null) {
                            nonDeletedList.add(alias)
                        } else {
                            onlyDeletedList.add(alias)
                        }
                    }
                }

                val finalList = nonDeletedList + onlyDeletedList
                val aliasAdapter = AliasAdapter(finalList, true)
                aliasAdapter.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                    override fun onClick(pos: Int, aView: View) {
                        TODO("Not yet implemented")
                    }

                    override fun onClickCopy(pos: Int, aView: View) {
                        val clipboard: ClipboardManager? =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val aliasEmailAddress = finalList[pos].email
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
                root.alias_all_aliases_recyclerview.hideShimmerAdapter()
            }, activeOnly = false, includeDeleted = true)

        }

    }

    private fun setStatisticsFromLocal(root: View, context: Context) {
        var statCurrentEmailsForwardedTotalCount = 0
        var statCurrentEmailsBlockedTotalCount = 0
        var statCurrentEmailsRepliedTotalCount = 0
        var statCurrentEmailsSentTotalCount = 0


        // These settings are related to AnonAddy's service. Thus encrypted
        settingsManager?.getSettingsInt("stat_current_emails_forwarded_total_count")?.let {
            statCurrentEmailsForwardedTotalCount = it
        }

        settingsManager?.getSettingsInt("stat_current_emails_blocked_total_count")?.let {
            statCurrentEmailsBlockedTotalCount = it
        }

        settingsManager?.getSettingsInt("stat_current_emails_replied_total_count")?.let {
            statCurrentEmailsRepliedTotalCount = it
        }

        settingsManager?.getSettingsInt("stat_current_emails_sent_total_count")?.let {
            statCurrentEmailsSentTotalCount = it
        }

        setAliasesStatistics(
            root,
            context,
            statCurrentEmailsForwardedTotalCount,
            statCurrentEmailsBlockedTotalCount,
            statCurrentEmailsRepliedTotalCount,
            statCurrentEmailsSentTotalCount
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
        settingsManager?.putSettingsInt("stat_current_emails_forwarded_total_count", forwarded)
        settingsManager?.putSettingsInt("stat_current_emails_blocked_total_count", blocked)
        settingsManager?.putSettingsInt("stat_current_emails_replied_total_count", replied)
        settingsManager?.putSettingsInt("stat_current_emails_sent_total_count", sent)

        root.alias_replied_sent_stats_textview.text =
            context.resources.getString(R.string.replied_replied_sent_stat, replied, sent)
        root.alias_forwarded_blocked_stats_textview.text =
            context.resources.getString(R.string.replied_forwarded_blocked_stat, forwarded, blocked)

    }

    override fun onAdded() {
        addAliasBottomDialogFragment.dismiss()
        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllAliasesAndSetStatistics(requireView())
        }
    }


}