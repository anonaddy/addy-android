package host.stjin.anonaddy.ui.alias

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import host.stjin.anonaddy.AnonAddyForAndroid
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.databinding.FragmentAliasBinding
import host.stjin.anonaddy.models.AliasSortFilter
import host.stjin.anonaddy.models.Aliases
import host.stjin.anonaddy.models.AliasesArray
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.utils.GsonTools
import host.stjin.anonaddy.utils.LoggingHelper
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.SnackbarHelper
import kotlinx.coroutines.launch


class AliasFragment : Fragment(), AddAliasBottomDialogFragment.AddAliasBottomDialogListener,
    FilterOptionsAliasBottomDialogFragment.AddFilterOptionsAliasBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true


    // Default filter
    private val defaultAliasSortFilter: AliasSortFilter = AliasSortFilter(
        onlyActiveAliases = false,
        onlyInactiveAliases = false,
        includeDeleted = false,
        onlyWatchedAliases = false,
        sort = null,
        sortDesc = false,
        filter = null
    )

    private var aliasSortFilter: AliasSortFilter = defaultAliasSortFilter.copy()

    companion object {
        fun newInstance() = AliasFragment()
    }

    private val addAliasBottomDialogFragment: AddAliasBottomDialogFragment =
        AddAliasBottomDialogFragment.newInstance()

    private lateinit var filterOptionsAliasBottomDialogFragment: FilterOptionsAliasBottomDialogFragment

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

        loadFilter()
        setOnClickListeners()
        setOnNestedScrollViewListener(true)
        getDataFromWeb()

        return root
    }

    private fun loadFilter() {
        val settingsManager = SettingsManager(false, requireContext())
        val aliasSortFilterJson = settingsManager.getSettingsString(SettingsManager.PREFS.ALIAS_SORT_FILTER)
        val aliasSortFilterObject = aliasSortFilterJson?.let { GsonTools.jsonToAliasSortFilterObject(requireContext(), it) }

        if (aliasSortFilterObject != null) {
            this.aliasSortFilter = aliasSortFilterObject
        }

        if (defaultAliasSortFilter != aliasSortFilter) {
            // Filter is active, let user know
            binding.aliasSortList.text = binding.aliasSortList.context.resources.getString(R.string.filter_active)
        } else {
            binding.aliasSortList.text = binding.aliasSortList.context.resources.getString(R.string.filter)
        }

        filterOptionsAliasBottomDialogFragment = FilterOptionsAliasBottomDialogFragment.newInstance(aliasSortFilter)

    }

    private fun setOnNestedScrollViewListener(set: Boolean) {
        if (set) {
            binding.fragmentAliasNsv.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                if (-scrollY == v.measuredHeight - v.getChildAt(0).measuredHeight) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        // Bottom of NSV reached. Time to load more data (if available)
                        getAliasesAndAddThemToList()
                    }
                }
            })
        } else {
            binding.fragmentAliasNsv.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
        }
    }

    private val mScrollUpBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            binding.fragmentAliasNsv.post { binding.fragmentAliasNsv.fullScroll(ScrollView.FOCUS_UP) }
        }
    }


    override fun onPause() {
        super.onPause()
        activity?.unregisterReceiver(mScrollUpBroadcastReceiver)
    }


    private fun getDataFromWeb() {
        binding.aliasNoAliases.visibility = View.GONE

        // Get the latest data in the background, and update the values when loaded
        viewLifecycleOwner.lifecycleScope.launch {
            getUserResource(requireContext())
            binding.aliasAllAliasesRecyclerview.showShimmer()
            getAliasesAndAddThemToList(forceReload = true)
        }


    }

    private suspend fun getUserResource(context: Context) {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                (activity?.application as AnonAddyForAndroid).userResource = user
                setAliasesStatistics(
                    context,
                    user.total_emails_forwarded,
                    user.total_emails_blocked,
                    user.total_emails_replied,
                    user.total_emails_sent
                )
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


    // Decided to not load aliases when coming back to hold back on performance issues
    override fun onResume() {
        super.onResume()
        activity?.registerReceiver(mScrollUpBroadcastReceiver, IntentFilter("scroll_up"))
    }

    private fun setOnClickListeners() {
        binding.aliasAddAliasFab.setOnClickListener {
            if (!addAliasBottomDialogFragment.isAdded) {
                addAliasBottomDialogFragment.show(
                    childFragmentManager,
                    "addAliasBottomDialogFragment"
                )
            }
        }

        binding.aliasAddAlias.setOnClickListener {
            if (!addAliasBottomDialogFragment.isAdded) {
                addAliasBottomDialogFragment.show(
                    childFragmentManager,
                    "addAliasBottomDialogFragment"
                )
            }
        }

        binding.aliasSortList.setOnClickListener {

            if (!filterOptionsAliasBottomDialogFragment.isAdded) {
                filterOptionsAliasBottomDialogFragment.show(
                    childFragmentManager,
                    "filterOptionsAliasBottomDialogFragment"
                )
            }
        }
    }


    private suspend fun getAliasesAndAddThemToList(forceReload: Boolean = false) {

        if (forceReload) {
            aliasList = null
        }
        // Only obtain data and do a network call whenever there is actually more information on the API side to obtain
        // If alias == null
        // OR
        // If the page we're currently on is LOWER than the last page
        if (aliasList == null || aliasList?.meta?.current_page ?: 0 < aliasList?.meta?.last_page ?: 0) {
            binding.aliasProgress.visibility = View.VISIBLE

            // When loading data disable the scrollviewlistener to prevent double loading
            setOnNestedScrollViewListener(set = false)

            // When the user reached page 5, offer to use search instead
            if (aliasList?.meta?.current_page == 5) {
                showSearchHintSnackbar()
            }

            networkHelper?.getAliases(
                { list: AliasesArray?, result: String? ->
                    if (list != null) {
                        if (aliasList == null || forceReload) {
                            // If aliasList is empty, assign it
                            aliasList = list

                            // Make sure to have the aliases in the array be filtered, aliases are being filtered on API level
                            // But some filters can only be applied locally
                            aliasList!!.data = getFilteredAliasList(list.data)
                        } else {
                            // If aliasList is not empty, set the meta and links and append the retrieved aliases to the list (as pagination is being used)
                            aliasList?.meta = list.meta
                            aliasList?.links = list.links
                            val filteredAliases = getFilteredAliasList(list.data)
                            aliasList?.data?.addAll(filteredAliases)


                            // If there are 0 new items in this page but there are more pages, continue searching to the next page
                            if (filteredAliases.size == 0 && aliasList?.meta?.current_page ?: 0 < aliasList?.meta?.last_page ?: 0) {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    getAliasesAndAddThemToList()
                                }
                                return@getAliases
                            }

                            // Get the totalsize of the adapteritems
                            val totalSize = aliasAdapter?.itemCount ?: 0
                            // Tell the adapter there is new data (from the original size to the added items)
                            binding.aliasAllAliasesRecyclerview.post { aliasAdapter?.notifyItemRangeInserted(totalSize, filteredAliases.size - 1) }


                        }


                        // TODO fix workaround?
                        // WORKAROUND #0001 START
                        /*
                        Situation: An alias on the 2nd page of the API is being watched or inactive.
                        The aliasfragment shows only 2 results (as the first page only contains 2 inactive/watched aliases)
                        Due to the 2 results the NSV is not scrollable thus this method will not be called again which results in missing results.

                        Solution: As long as there are less than 100 items (which is the default number of aliases to obtain as seen in getAllAliases)
                                  Keep loading results as long as there are more pages
                     */

                        if (aliasList?.data?.size ?: 0 < 100 && aliasList?.meta?.current_page ?: 0 < aliasList?.meta?.last_page ?: 0) {
                            viewLifecycleOwner.lifecycleScope.launch {
                                getAliasesAndAddThemToList()
                            }
                        }

                        // WORKAROUND END


                        // If the list is empty, set noAliasVisibility to visible
                        if (aliasList!!.data.size == 0) {
                            // Set to GONE in getDataFromWeb
                            binding.aliasNoAliases.visibility = View.VISIBLE
                        } else {
                            binding.aliasNoAliases.visibility = View.GONE
                        }

                        // Okay we got data, init the recyclerview
                        // If we do a force reload the recyclerview also needs to be re-init to show shimmerview as well as clear the adapter
                        initRecyclerview(forceReload)

                    } else {
                        // Data could not be loaded
                        val bottomNavView: BottomNavigationView? =
                            activity?.findViewById(R.id.nav_view)
                        bottomNavView?.let {
                            SnackbarHelper.createSnackbar(
                                requireContext(),
                                requireContext().resources.getString(R.string.error_obtaining_aliases) + "\n" + result,
                                it,
                                LoggingHelper.LOGFILES.DEFAULT
                            )
                                .apply {
                                    anchorView = bottomNavView
                                }.show()
                        }
                    }

                    binding.aliasAllAliasesRecyclerview.hideShimmer()
                    binding.aliasProgress.visibility = View.GONE

                    // Enable scrollviewlistener again
                    setOnNestedScrollViewListener(set = true)
                    // Size 100 is being used for above WORKAROUND #0001
                },
                aliasSortFilter = aliasSortFilter,
                page = (aliasList?.meta?.current_page ?: 0) + 1,
                size = 100
            )
        }
    }


    private fun showSearchHintSnackbar() {
        hideFabForSnackBarTime()
        val bottomNavView: BottomNavigationView? =
            activity?.findViewById(R.id.nav_view)
        bottomNavView?.let {
            val snackbar = Snackbar.make(
                it,
                requireContext().resources.getString(R.string.alias_global_search_hint),
                Snackbar.LENGTH_SHORT
            )
            snackbar.setAction(R.string.search) {
                (activity as MainActivity).openSearch()
            }
            snackbar.anchorView = bottomNavView
            snackbar.show()
        }
    }

    private var aliasAdapter: AliasAdapter? = null
    private var aliasList: AliasesArray? = null

    private fun initRecyclerview(forceReload: Boolean) {
        binding.aliasAllAliasesRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false
                shimmerItemCount = 100
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
            }

            if (forceReload) {
                aliasAdapter = AliasAdapter(aliasList!!.data, context)
                aliasAdapter!!.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                    override fun onClick(pos: Int) {
                        val intent = Intent(context, ManageAliasActivity::class.java)
                        // Pass data object in the bundle and populate details activity.
                        intent.putExtra("alias_id", aliasList!!.data[pos].id)
                        resultLauncher.launch(intent)
                    }

                    override fun onClickCopy(pos: Int, aView: View) {
                        val clipboard: ClipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val aliasEmailAddress = aliasList!!.data[pos].email
                        val clip = ClipData.newPlainText("alias", aliasEmailAddress)
                        clipboard.setPrimaryClip(clip)


                        hideFabForSnackBarTime()
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
            }
        }

    }

    private fun getFilteredAliasList(aliasesList: ArrayList<Aliases>): ArrayList<Aliases> {
        if (aliasSortFilter.onlyWatchedAliases) {
            val aliasesToWatch = context?.let { AliasWatcher(it).getAliasesToWatch() }
            if (aliasesToWatch != null) {
                aliasesList.removeAll { alias -> alias.id !in aliasesToWatch }
            }
        }
        return aliasesList
    }

    private fun hideFabForSnackBarTime() {
        binding.aliasAddAliasFab.hide()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.aliasAddAliasFab.show()
        }, 3500)
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            if (data != null) {
                if (data.getBooleanExtra("should_update", false)) {
                    getDataFromWeb()
                }
            }
        }
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
        addAliasBottomDialogFragment.dismissAllowingStateLoss()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb()
    }

    override fun onCancel() {
        // Nothing
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun setFilterAndSortingSettings(aliasSortFilter: AliasSortFilter) {
        this.aliasSortFilter = aliasSortFilter

        val settingsManager = SettingsManager(false, requireContext())

        // Turn the list into a json object
        val data = Gson().toJson(aliasSortFilter)
        // Store a copy of the just received data locally
        settingsManager.putSettingsString(SettingsManager.PREFS.ALIAS_SORT_FILTER, data)


        filterOptionsAliasBottomDialogFragment.dismissAllowingStateLoss()
        getDataFromWeb()
    }

    override fun onDismiss() {
        loadFilter()
    }

}