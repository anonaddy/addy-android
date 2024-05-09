package host.stjin.anonaddy.ui.alias

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.databinding.FragmentAliasBinding
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.AliasesArray
import host.stjin.anonaddy_shared.models.BulkAliasesArray
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.utils.GsonTools
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


class AliasFragment : Fragment(), AddAliasBottomDialogFragment.AddAliasBottomDialogListener,
    FilterOptionsAliasBottomDialogFragment.AddFilterOptionsAliasBottomDialogListener,
    AliasMultipleSelectionBottomDialogFragment.AddAliasMultipleSelectionBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true


    // Default filter
    private val defaultAliasSortFilter: AliasSortFilter = AliasSortFilter(
        onlyActiveAliases = false,
        onlyDeletedAliases = false,
        onlyInactiveAliases = false,
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

    private lateinit var aliasMultipleSelectionBottomDialogFragment: AliasMultipleSelectionBottomDialogFragment

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

        settingsManager = SettingsManager(false, requireContext())
        networkHelper = NetworkHelper(requireContext())


        initShimmerRecyclerView()
        loadFilter()
        setOnClickListeners()
        setOnNestedScrollViewListener(true)
        setAliasesRecyclerView()


        getDataFromWeb(requireContext(), savedInstanceState)

        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        val json = gson.toJson(aliasList)
        outState.putString("aliasesList", json)
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            if (data?.getBooleanExtra("shouldRefresh", false) == true) {
                getDataFromWeb(requireContext(), null)
            }
        }
    }

    private fun initShimmerRecyclerView() {
        // Set the item margindecoration before the shimmer is being shown, so that the shimmerviews have the exact margins
        // as the list items
        binding.aliasAllAliasesRecyclerview.addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))
    }

    private fun loadFilter() {
        val aliasSortFilterJson = settingsManager?.getSettingsString(SettingsManager.PREFS.ALIAS_SORT_FILTER)
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

    private fun setHasReachedTopOfNsv() {
        (activity as MainActivity).hasReachedTopOfNsv = !binding.fragmentAliasNsv.canScrollVertically(-1)
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
                setHasReachedTopOfNsv()
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


    fun getDataFromWeb(context: Context, savedInstanceState: Bundle?) {
        // Get the latest data in the background, and update the values when loaded
        lifecycleScope.launch {
            if (savedInstanceState != null) {
                setAliasesStatistics(
                    context,
                    (activity?.application as AddyIoApp).userResource.total_emails_forwarded,
                    (activity?.application as AddyIoApp).userResource.total_emails_blocked,
                    (activity?.application as AddyIoApp).userResource.total_emails_replied,
                    (activity?.application as AddyIoApp).userResource.total_emails_sent
                )

                val aliasesJson = savedInstanceState.getString("aliasesList")
                if (aliasesJson!!.isNotEmpty() && aliasesJson != "null") {
                    val gson = Gson()
                    val list: AliasesArray = gson.fromJson(aliasesJson, AliasesArray::class.java)
                    setAliasesAdapter(requireContext(), list, true)
                    // need to force reload in order to init the adapter (which has been reset due to the recreation of the activity
                } else {
                    getUserResource(requireContext())
                    getAliasesAndAddThemToList(forceReload = true)
                }

            } else {
                getUserResource(requireContext())
                getAliasesAndAddThemToList(forceReload = true)
            }
        }


    }

    private suspend fun getUserResource(context: Context) {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                (activity?.application as AddyIoApp).userResource = user
                setAliasesStatistics(
                    context,
                    user.total_emails_forwarded,
                    user.total_emails_blocked,
                    user.total_emails_replied,
                    user.total_emails_sent
                )
            } else {
                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        context,
                        context.resources.getString(R.string.error_obtaining_user) + "\n" + result,
                        (activity as MainActivity).findViewById(R.id.main_container),
                        LoggingHelper.LOGFILES.DEFAULT
                    ).show()
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
    }


    // Decided to not load aliases when coming back to hold back on performance issues
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
        // If aliasList == null
        // OR
        // If the page we're currently on is LOWER than the last page
        if (aliasList == null || (aliasList?.meta?.current_page ?: 0) < (aliasList?.meta?.last_page ?: 0)) {
            binding.aliasProgress.visibility = View.VISIBLE

            // When loading data disable the scrollviewlistener to prevent double loading
            setOnNestedScrollViewListener(set = false)

            // When the user reached page 3, offer to use search instead
            if (aliasList?.meta?.current_page == 3) {
                showSearchHintSnackbar()
            }

            /**
             * CHECK IF WATCHED ONLY IS TRUE
             * If true simply bulk-obtain all the watched aliases
             */

            if (aliasSortFilter.onlyWatchedAliases) {

                val aliasWatcher = AliasWatcher(requireContext())
                val aliasesToWatch = aliasWatcher.getAliasesToWatch().toList()
                if (aliasesToWatch.isNotEmpty()) {
                    networkHelper?.bulkGetAlias(
                        { list: BulkAliasesArray?, result: String? ->
                            if (list != null) {
                                val aliasesArray = AliasesArray(list.data, links = null, meta = null)
                                setAliasesAdapter(requireContext(), aliasesArray, forceReload)
                            } else {
                                // Data could not be loaded
                                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                                    SnackbarHelper.createSnackbar(
                                        requireContext(),
                                        requireContext().resources.getString(R.string.error_obtaining_aliases) + "\n" + result,
                                        (activity as MainActivity).findViewById(R.id.main_container),
                                        LoggingHelper.LOGFILES.DEFAULT
                                    ).show()
                                } else {
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

                            }
                        }, aliasesToWatch
                    )
                } else {
                    val aliasesArray = AliasesArray(arrayListOf(), links = null, meta = null)
                    setAliasesAdapter(requireContext(), aliasesArray, forceReload)
                }
            } else {
                networkHelper?.getAliases(
                    { list: AliasesArray?, result: String? ->
                        if (list != null) {
                            setAliasesAdapter(requireContext(), list, forceReload)
                        } else {
                            // Data could not be loaded
                            if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                                SnackbarHelper.createSnackbar(
                                    requireContext(),
                                    requireContext().resources.getString(R.string.error_obtaining_aliases) + "\n" + result,
                                    (activity as MainActivity).findViewById(R.id.main_container),
                                    LoggingHelper.LOGFILES.DEFAULT
                                ).show()
                            } else {
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
                        }
                    },
                    aliasSortFilter = aliasSortFilter,
                    page = (aliasList?.meta?.current_page ?: 0) + 1,
                    size = 25 // Get only 25 aliases for performance
                )
            }


        }
    }

    private fun setAliasesAdapter(context: Context, list: AliasesArray, forceReload: Boolean) {
        binding.aliasAllAliasesRecyclerview.apply {
            if (aliasList == null || forceReload) {
                // If aliasList is empty, assign it
                aliasList = list
            } else {
                // If aliasList is not empty, set the meta and links and append the retrieved aliases to the list (as pagination is being used)
                aliasList?.meta = list.meta
                aliasList?.links = list.links
                aliasList?.data?.addAll(list.data)


                // If there are 0 new items in this page but there are more pages, continue searching to the next page

                // TODO Is this ever the case? Why would the API return nothing but still have more pages?
                if (list.data.size == 0 && (aliasList?.meta?.current_page ?: 0) < (aliasList?.meta?.last_page ?: 0)) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        getAliasesAndAddThemToList()
                    }
                    return
                }

                // Get the totalsize of the adapteritems
                val totalSize = aliasAdapter?.itemCount ?: 0
                // Tell the adapter there is new data (from the original size to the added items)
                binding.aliasAllAliasesRecyclerview.post { aliasAdapter?.notifyItemRangeInserted(totalSize, list.data.size - 1) }
            }

            // If the list is empty, set noAliasVisibility to visible
            if (aliasList!!.data.size > 0) {
                binding.aliasNoAliases.visibility = View.GONE
            } else {
                binding.aliasNoAliases.visibility = View.VISIBLE
            }

            // Hide snackbar
            hideSnackBar()

            aliasAdapter = AliasAdapter(
                aliasList!!.data,
                context,
                supportMultipleSelection = settingsManager?.getSettingsBool(SettingsManager.PREFS.MANAGE_MULTIPLE_ALIASES, default = true) ?: true
            )
            aliasAdapter!!.setClickOnAliasClickListener(object : AliasAdapter.AliasInterface {
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

                    if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                        SnackbarHelper.createSnackbar(
                            context,
                            context.resources.getString(R.string.copied_alias),
                            (activity as MainActivity).findViewById(R.id.main_container)
                        ).show()
                    } else {
                        val bottomNavView: BottomNavigationView? =
                            activity?.findViewById(R.id.nav_view)
                        bottomNavView?.let {
                            SnackbarHelper.createSnackbar(context, context.resources.getString(R.string.copied_alias), it).apply {
                                anchorView = bottomNavView
                            }.show()
                        }
                    }
                }

                override fun onSelectionMode(selectionMode: Boolean, selectedAliases: ArrayList<Aliases>) {
                    // If multipleSelection is supported, long pressing an alias will trigger this method

                    if (selectionMode) {
                        binding.aliasAddAliasFab.hide()

                        if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                            SnackbarHelper.createSnackbar(
                                context,
                                context.resources.getString(R.string.multiple_alias_selected, selectedAliases.count()),
                                (activity as MainActivity).findViewById(R.id.main_container),
                                length = Snackbar.LENGTH_INDEFINITE,
                                allowSwipeDismiss = false
                            ).setAction(R.string.actions) {
                                aliasMultipleSelectionBottomDialogFragment =
                                    AliasMultipleSelectionBottomDialogFragment.newInstance(selectedAliases)
                                if (!aliasMultipleSelectionBottomDialogFragment.isAdded) {
                                    aliasMultipleSelectionBottomDialogFragment.show(
                                        childFragmentManager,
                                        "aliasMultipleSelectionBottomDialogFragment"
                                    )
                                }
                            }.show()
                        } else {
                            val bottomNavView: BottomNavigationView? =
                                activity?.findViewById(R.id.nav_view)
                            bottomNavView?.let {
                                aliasSelectionSnackbar = SnackbarHelper.createSnackbar(
                                    context,
                                    context.resources.getString(R.string.multiple_alias_selected, selectedAliases.count()),
                                    it,
                                    length = Snackbar.LENGTH_INDEFINITE,
                                    allowSwipeDismiss = false
                                ).apply {
                                    anchorView = bottomNavView
                                }.setAction(R.string.actions) {
                                    aliasMultipleSelectionBottomDialogFragment =
                                        AliasMultipleSelectionBottomDialogFragment.newInstance(selectedAliases)
                                    if (!aliasMultipleSelectionBottomDialogFragment.isAdded) {
                                        aliasMultipleSelectionBottomDialogFragment.show(
                                            childFragmentManager,
                                            "aliasMultipleSelectionBottomDialogFragment"
                                        )
                                    }
                                }
                                aliasSelectionSnackbar?.show()
                            }
                        }
                    } else {
                        hideSnackBar()
                    }

                }

            })
            binding.aliasProgress.visibility = View.GONE
            hideShimmer()
            // Enable scrollviewlistener again
            setOnNestedScrollViewListener(set = true)

            adapter = aliasAdapter
        }
    }


    private fun showSearchHintSnackbar() {
        hideFabForSnackBarTime()


        if (requireContext().resources.getBoolean(R.bool.isTablet)) {
            val snackbar = SnackbarHelper.createSnackbar(
                requireContext(),
                requireContext().resources.getString(R.string.alias_global_search_hint),
                (activity as MainActivity).findViewById(R.id.main_container),
                LoggingHelper.LOGFILES.DEFAULT
            )
            snackbar.setAction(R.string.search) {
                (activity as MainActivity).openSearch()
            }
            snackbar.show()
        } else {
            val bottomNavView: BottomNavigationView? =
                activity?.findViewById(R.id.nav_view)
            bottomNavView?.let {
                val snackbar = SnackbarHelper.createSnackbar(
                    requireContext(),
                    requireContext().resources.getString(R.string.alias_global_search_hint),
                    it,
                    LoggingHelper.LOGFILES.DEFAULT
                )
                snackbar.setAction(R.string.search) {
                    (activity as MainActivity).openSearch()
                }
                snackbar.anchorView = bottomNavView
                snackbar.show()
            }
        }


    }

    private var aliasAdapter: AliasAdapter? = null
    private var aliasList: AliasesArray? = null
    var aliasSelectionSnackbar: Snackbar? = null

    private fun setAliasesRecyclerView() {
        binding.aliasAllAliasesRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false
                shimmerItemCount = 100
                shimmerLayoutManager = GridLayoutManager(activity, ScreenSizeUtils.calculateNoOfColumns(requireContext()))
                layoutManager = GridLayoutManager(activity, ScreenSizeUtils.calculateNoOfColumns(requireContext()))

                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation
            }

            showShimmer()
        }

    }


    private fun hideSnackBar() {
        binding.aliasAddAliasFab.show()
        aliasSelectionSnackbar?.dismiss()
    }


    private fun hideFabForSnackBarTime() {
        binding.aliasAddAliasFab.hide()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.aliasAddAliasFab.show()
        }, 3500)
    }

    private fun setAliasesStatistics(
        context: Context,
        forwarded: Int,
        blocked: Int,
        replied: Int,
        sent: Int
    ) {
        binding.aliasStats2.setDescription(
            context.resources.getString(R.string.replied_replied_sent_stat, replied, sent)
        )
        binding.aliasStats1.setDescription(
            context.resources.getString(
                R.string.replied_forwarded_blocked_stat,
                forwarded,
                blocked
            )
        )
    }

    override fun onAdded() {
        addAliasBottomDialogFragment.dismissAllowingStateLoss()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb(requireContext(), null)
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
        // Turn the list into a json object
        val data = Gson().toJson(aliasSortFilter)
        // Store a copy of the just received data locally
        settingsManager?.putSettingsString(SettingsManager.PREFS.ALIAS_SORT_FILTER, data)


        if (filterOptionsAliasBottomDialogFragment.isAdded) {
            // Could not be added because this is called from homeFragment on sw600dp
            filterOptionsAliasBottomDialogFragment.dismissAllowingStateLoss()
        }

        loadFilter()
        getDataFromWeb(requireContext(), null)
    }

    override fun onDismiss() {
        loadFilter()
    }

    override fun onCloseMultipleSelectionBottomDialogFragment(shouldRefreshData: Boolean) {
        aliasMultipleSelectionBottomDialogFragment.dismissAllowingStateLoss()

        if (shouldRefreshData) {
            // Automatically unselects data
            getDataFromWeb(requireContext(), null)
        } else {
            // Show snackbar again
            aliasSelectionSnackbar?.show()
        }
    }

    override fun onCancelMultipleSelectionBottomDialogFragment(shouldRefreshData: Boolean) {
        aliasMultipleSelectionBottomDialogFragment.dismissAllowingStateLoss()
        if (shouldRefreshData) {
            // Automatically unselects data
            getDataFromWeb(requireContext(), null)
        } else {
            aliasAdapter?.unselectAliases()
            hideSnackBar()
        }

    }

}