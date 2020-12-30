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
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.databinding.FragmentAliasBinding
import host.stjin.anonaddy.models.Aliases
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.utils.GsonTools
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

    private var _binding: FragmentAliasBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAliasBinding.inflate(inflater, container, false)
        val root = binding.root

        settingsManager = SettingsManager(true, requireContext())
        networkHelper = NetworkHelper(requireContext())


        // Load values from local to make the app look quick and snappy!
        setStatisticsFromLocal(requireContext())
        setOnClickListeners()
        setOnScrollViewListener()

        // Called on OnResume() as well, call this in onCreateView so the viewpager can serve loaded fragments
        getDataFromWeb()

        return root
    }


    private fun setOnScrollViewListener() {

        binding.aliasScrollview.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->

            val scrollViewHeight: Double = (v.getChildAt(0).bottom - v.height).toDouble()
            val getScrollY: Double = scrollY.toDouble()
            val scrollPosition = getScrollY / scrollViewHeight * 100.0
            //Log.i("scrollview", "scroll Percent Y: " + scrollPosition.toInt())
            val percentage = scrollPosition.toInt()

            if (percentage in 6..100) { // If between 6 and 100, show the fab
                binding.aliasFragmentAddAliasFab.show()
            } else if (percentage in 0..5) { // If between 0 and 5, hide the fab
                binding.aliasFragmentAddAliasFab.hide()
            }
        })


    }

    private fun getDataFromWeb() {
        binding.aliasListLL1.visibility = View.VISIBLE
        binding.aliasStatisticsRLLottieview.visibility = View.GONE

        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllAliasesAndSetStatistics()
            getAllDeletedAliases()
        }
    }

    // Update list of aliases when coming back
    override fun onResume() {
        super.onResume()
        getDataFromWeb()
    }

    private fun setOnClickListeners() {
        binding.aliasStatisticsDismiss.setOnClickListener {
            binding.aliasStatisticsLL.visibility = View.GONE
        }

        binding.aliasAddAlias.setOnClickListener {
            if (!addAliasBottomDialogFragment.isAdded) {
                addAliasBottomDialogFragment.show(
                    childFragmentManager,
                    "addAliasBottomDialogFragment"
                )
            }
        }

        binding.aliasFragmentAddAliasFab.setOnClickListener {
            if (!addAliasBottomDialogFragment.isAdded) {
                addAliasBottomDialogFragment.show(
                    childFragmentManager,
                    "addAliasBottomDialogFragment"
                )
            }
        }

        binding.aliasShowDeletedAliasToggleLL.setOnClickListener {
            if (binding.aliasDeletedAliasesRecyclerview.visibility == View.GONE) {
                binding.aliasDeletedAliasesRecyclerview.visibility = View.VISIBLE
                binding.aliasShowDeletedAliasToggle.setImageResource(R.drawable.ic_menu_up_outline)
            } else {
                binding.aliasDeletedAliasesRecyclerview.visibility = View.GONE
                binding.aliasShowDeletedAliasToggle.setImageResource(R.drawable.ic_menu_down_outline)
            }
        }
    }

    private suspend fun getAllAliasesAndSetStatistics() {
        binding.aliasAllAliasesRecyclerview.apply {

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
                    setAliasesStatistics(context, forwarded, blocked, replied, sent)



                    if (list.size > 0) {
                        binding.aliasNoAliases.visibility = View.GONE
                    } else {
                        binding.aliasNoAliases.visibility = View.VISIBLE
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
                            startActivity(intent)
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
                    binding.aliasListLL1.visibility = View.GONE
                    binding.aliasStatisticsRLLottieview.visibility = View.VISIBLE
                }
            }, activeOnly = false, includeDeleted = false)
        }

    }


    private suspend fun getAllDeletedAliases() {
        binding.aliasDeletedAliasesRecyclerview.apply {

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
                        binding.aliasNoDeletedAliases.visibility = View.GONE
                        for (alias in list) {
                            if (alias.deleted_at != null) {
                                onlyDeletedList.add(alias)
                            }
                        }
                    } else {
                        binding.aliasNoDeletedAliases.visibility = View.VISIBLE
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
                            startActivity(intent)
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
                    binding.aliasListLL1.visibility = View.GONE
                    binding.aliasStatisticsRLLottieview.visibility = View.VISIBLE
                }
            }, activeOnly = false, includeDeleted = true)
        }

    }

    /*
    Only gets called in onCreate, so when coming back later the number won't just jump back to the old value
     */
    private fun setStatisticsFromLocal(context: Context) {
        val statCurrentEmailsForwardedTotalCount = 0
        val statCurrentEmailsBlockedTotalCount = 0
        val statCurrentEmailsRepliedTotalCount = 0
        val statCurrentEmailsSentTotalCount = 0

        // Get aliasList
        val aliasesJson = settingsManager?.getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DATA_ALIASES)
        val aliasesList = aliasesJson?.let { GsonTools.jsonToAliasObject(context, it) }

        // Count the stats from the cache
        if (aliasesList != null) {
            for (alias in aliasesList) {
                statCurrentEmailsForwardedTotalCount + alias.emails_forwarded
                statCurrentEmailsBlockedTotalCount + alias.emails_blocked
                statCurrentEmailsRepliedTotalCount + alias.emails_replied
                statCurrentEmailsSentTotalCount + alias.emails_sent
            }
        }

        setAliasesStatistics(
            context,
            statCurrentEmailsForwardedTotalCount,
            statCurrentEmailsBlockedTotalCount,
            statCurrentEmailsRepliedTotalCount,
            statCurrentEmailsSentTotalCount
        )
    }

    private fun setAliasesStatistics(
        context: Context,
        forwarded: Int,
        blocked: Int,
        replied: Int,
        sent: Int
    ) {
        binding.aliasRepliedSentStatsTextview.text =
            context.resources.getString(R.string.replied_replied_sent_stat, replied, sent)
        binding.aliasForwardedBlockedStatsTextview.text =
            context.resources.getString(
                R.string.replied_forwarded_blocked_stat,
                forwarded,
                blocked
            )
    }

    override fun onAdded() {
        addAliasBottomDialogFragment.dismiss()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}