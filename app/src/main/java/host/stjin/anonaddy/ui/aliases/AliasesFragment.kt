package host.stjin.anonaddy.ui.aliases

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.adapter.SearchAdapter
import host.stjin.anonaddy.databinding.FragmentAliasesBinding
import host.stjin.anonaddy.interfaces.Refreshable
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.ui.aliases.manage.ManageAliasActivity
import host.stjin.anonaddy.ui.base.BaseFragment
import host.stjin.anonaddy.ui.base.SharedScrollViewModel
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.ReviewHelper
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy_shared.utils.GsonTools
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

@Suppress("SimplifyBooleanWithConstants")
class AliasesFragment : BaseFragment(), AddAliasBottomDialogFragment.AddAliasBottomDialogListener,
    FilterOptionsAliasBottomDialogFragment.AddFilterOptionsAliasBottomDialogListener,
    AliasMultipleSelectionBottomDialogFragment.AddAliasMultipleSelectionBottomDialogListener, Refreshable {

    // 1. Properties
    private val aliasesViewModel: AliasesViewModel by viewModels()
    private val sharedFilterViewModel: SharedFilterViewModel by activityViewModels()
    private val sharedScrollViewModel: SharedScrollViewModel by activityViewModels()
    private var settingsManager: SettingsManager? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var oneTimeRecyclerViewActions: Boolean = true

    // Default filter
    private val defaultAliasSortFilter: AliasSortFilter = AliasSortFilter(
        onlyActiveAliases = false,
        onlyDeletedAliases = false,
        onlyInactiveAliases = false,
        onlyWatchedAliases = false,
        onlyPinnedAliases = false,
        sort = null,
        sortDesc = false,
        filter = null
    )

    private var aliasSortFilter: AliasSortFilter = defaultAliasSortFilter.copy()

    private val addAliasBottomDialogFragment: AddAliasBottomDialogFragment =
        AddAliasBottomDialogFragment.newInstance()

    private lateinit var filterOptionsAliasBottomDialogFragment: FilterOptionsAliasBottomDialogFragment

    private lateinit var aliasMultipleSelectionBottomDialogFragment: AliasMultipleSelectionBottomDialogFragment

    private var _binding: FragmentAliasesBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            if (data?.getBooleanExtra("shouldRefresh", false) == true) {
                getDataFromWeb(null, showShimmer = false)
            }
        }
    }

    private var isUpdatingChips = false
    private var isSilentRefresh = false

    private var aliasAdapter: AliasAdapter? = null
    private var aliasList: PaginatedResponse<Aliases>? = null
    private var aliasSelectionSnackbar: Snackbar? = null
    private lateinit var searchAdapter: SearchAdapter
    private var recentSearchesList: ArrayList<String> = arrayListOf()

    // 2. Lifecycle Methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAliasesBinding.inflate(inflater, container, false)
        //InsetUtils.applyBottomInset(binding.aliasListLL1) Not necessary, MainActivity elevated the viewpager for the fab

        val root = binding.root

        settingsManager = ServiceLocator.settingsManager
        encryptedSettingsManager = ServiceLocator.encryptedSettingsManager

        initShimmerRecyclerView()
        loadFilter()
        setOnClickListeners()
        setOnNestedScrollViewListener(true)
        setAliasesRecyclerView()
        setSearchRecyclerView()

        observeViewModel()
        getDataFromWeb(savedInstanceState)

        return root
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    aliasesViewModel.aliasesState.collect { state ->
                        handleUiState(
                            state,
                            shimmer = if (!isSilentRefresh) binding.aliasAllAliasesRecyclerview else null,
                            progress = if (isSilentRefresh && !aliasesViewModel.isLoadingMore.value) binding.aliasProgress else null,
                            titleProgress = if (isSilentRefresh && !aliasesViewModel.isLoadingMore.value) binding.aliasTitleProgress else null,
                            errorStringRes = R.string.error_obtaining_aliases
                        ) { data ->
                            aliasList = data
                            setAliasesAdapter(requireContext(), data)
                            setOnNestedScrollViewListener(set = true)
                        }
                        if (state is UiState.Error) {
                            setOnNestedScrollViewListener(set = true)
                        }
                    }
                }
                launch {
                    aliasesViewModel.isLoadingMore.collect { loadingMore ->
                        binding.aliasProgress.visibility = if (loadingMore) View.VISIBLE else View.GONE
                        binding.aliasTitleProgress.visibility = if (loadingMore) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    sharedFilterViewModel.filterEvents.collect { filter ->
                        setFilterAndSortingSettings(filter)
                    }
                }
                launch {
                    sharedScrollViewModel.scrollEvents.collect {
                        _binding?.fragmentAliasesNsv?.smoothScrollTo(0, 0)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setHasReachedTopOfNsv()
        aliasAdapter?.updateWatchedAliases()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 3. View Setup
    private fun initShimmerRecyclerView() {
        // Set the item margindecoration before the shimmer is being shown, so that the shimmerviews have the exact margins
        // as the list items
        binding.aliasAllAliasesRecyclerview.addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))
    }

    private fun updateChipSelection(filter: AliasSortFilter) {
        isUpdatingChips = true
        val filterWithoutSearch = filter.copy(filter = null)
        when {
            filterWithoutSearch.label != null -> binding.aliasChipgroup.check(R.id.alias_chip_custom)
            filterWithoutSearch.onlyPinnedAliases -> binding.aliasChipgroup.check(R.id.alias_chip_pinned)
            filterWithoutSearch.onlyActiveAliases -> binding.aliasChipgroup.check(R.id.alias_chip_active)
            filterWithoutSearch.onlyInactiveAliases -> binding.aliasChipgroup.check(R.id.alias_chip_inactive)
            filterWithoutSearch.onlyDeletedAliases -> binding.aliasChipgroup.check(R.id.alias_chip_deleted)
            filterWithoutSearch.onlyWatchedAliases -> binding.aliasChipgroup.check(R.id.alias_chip_watched)
            filterWithoutSearch == defaultAliasSortFilter -> binding.aliasChipgroup.check(R.id.alias_chip_all)
            else -> binding.aliasChipgroup.check(R.id.alias_chip_custom)
        }
        isUpdatingChips = false
    }

    private fun loadFilter() {
        val aliasSortFilterJson = settingsManager?.getSettingsString(SettingsManager.PREFS.ALIAS_SORT_FILTER)
        val aliasSortFilterObject = aliasSortFilterJson?.let { GsonTools.jsonToAliasSortFilterObject(requireContext(), it) }

        if (aliasSortFilterObject != null) {
            this.aliasSortFilter = aliasSortFilterObject
        }

        val searchText = binding.aliasSearchBar.text.toString().trim()
        this.aliasSortFilter.filter = if (searchText.isEmpty()) null else searchText.lowercase(java.util.Locale.getDefault())

        updateChipSelection(this.aliasSortFilter)

        filterOptionsAliasBottomDialogFragment = FilterOptionsAliasBottomDialogFragment.newInstance(aliasSortFilter)
    }

    private fun setOnNestedScrollViewListener(set: Boolean) {
        if (set) {
            binding.fragmentAliasesNsv.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                val threshold = 10 // or some small number to account for rounding errors
                if (scrollY + v.measuredHeight + threshold >= v.getChildAt(0).measuredHeight) {
                    // Consider this as being at the bottom
                    getDataFromWeb(null, isLoadMore = true)
                }
                setHasReachedTopOfNsv()
            })
        } else {
            binding.fragmentAliasesNsv.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
        }
    }

    private fun setOnClickListeners() {
        binding.aliasSearchView.editText.addTextChangedListener { text ->
            val searchText = text?.toString()?.trim()
            if (searchText.isNullOrEmpty() && aliasSortFilter.filter != null) {
                aliasSortFilter.filter = null
                binding.aliasSearchBar.setText(null)
                getDataFromWeb(null)
            }
        }

        binding.aliasSearchView.addTransitionListener { _, _, newState ->
            if (newState == com.google.android.material.search.SearchView.TransitionState.HIDDEN) {
                val searchText = binding.aliasSearchView.text.toString().trim()
                if (searchText.isEmpty() && aliasSortFilter.filter != null) {
                    aliasSortFilter.filter = null
                    binding.aliasSearchBar.setText(null)
                    getDataFromWeb(null)
                }
            }
        }

        binding.aliasSearchView.editText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event?.action == android.view.KeyEvent.ACTION_DOWN &&
                        event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)
            ) {
                val inputMethodManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.aliasSearchView.windowToken, 0)

                binding.aliasSearchBar.setText(binding.aliasSearchView.text)
                binding.aliasSearchView.hide()

                val searchText = binding.aliasSearchView.text.toString().trim()
                saveRecentSearch(searchText)
                aliasSortFilter.filter = if (searchText.isEmpty()) null else searchText.lowercase(java.util.Locale.getDefault())
                getDataFromWeb(null)
                true
            } else {
                false
            }
        }

        binding.aliasChipgroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (!isUpdatingChips && checkedIds.isNotEmpty()) {
                val checkedId = checkedIds.first()
                val newFilter = defaultAliasSortFilter.copy()
                newFilter.filter = aliasSortFilter.filter // Preserve the search text
                when (checkedId) {
                    R.id.alias_chip_pinned -> newFilter.onlyPinnedAliases = true
                    R.id.alias_chip_active -> newFilter.onlyActiveAliases = true
                    R.id.alias_chip_inactive -> newFilter.onlyInactiveAliases = true
                    R.id.alias_chip_deleted -> newFilter.onlyDeletedAliases = true
                    R.id.alias_chip_watched -> newFilter.onlyWatchedAliases = true
                }

                if (checkedId != R.id.alias_chip_custom) {
                    setFilterAndSortingSettings(newFilter)
                }
            }
        }

        binding.aliasChipCustom.setOnClickListener {
            if (!filterOptionsAliasBottomDialogFragment.isAdded) {
                filterOptionsAliasBottomDialogFragment.show(
                    childFragmentManager,
                    "filterOptionsAliasBottomDialogFragment"
                )
            }
        }

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

    }

    private fun setAliasesAdapter(context: Context, list: PaginatedResponse<Aliases>) {
        binding.aliasCount.apply {
            list.meta?.total?.let { total ->
                if (total > 0) {
                    text = String.format(java.util.Locale.getDefault(), "%d", total)
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            } ?: run { visibility = View.GONE }
        }

        binding.aliasAllAliasesRecyclerview.apply {
            aliasList = list

            // If the list is empty, set noAliasVisibility to visible
            if (aliasList!!.data.isNotEmpty()) {
                binding.aliasNoAliases.visibility = View.GONE
            } else {
                binding.aliasNoAliases.visibility = View.VISIBLE
            }

            if (defaultAliasSortFilter != aliasSortFilter) {
                binding.aliasHeader.text = this@AliasesFragment.resources.getString(R.string.aliases_filtered)
            } else {
                binding.aliasHeader.text = this@AliasesFragment.resources.getString(R.string.aliases)
            }

            // Hide snackbar
            hideSnackBar()

            if (aliasAdapter == null) {
                aliasAdapter = AliasAdapter(
                    aliasList!!.data,
                    context
                )
                aliasAdapter!!.setClickOnAliasClickListener(object : AliasAdapter.AliasInterface {
                    override fun onClick(pos: Int) {
                        val currentList = aliasAdapter?.currentList ?: aliasList!!.data
                        if (pos in currentList.indices) {
                            val intent = Intent(context, ManageAliasActivity::class.java)
                            intent.putExtra("alias_id", currentList[pos].id)
                            resultLauncher.launch(intent)
                        }
                    }

                    override fun onClickCopy(pos: Int, view: View) {
                        val currentList = aliasAdapter?.currentList ?: aliasList!!.data
                        if (pos in currentList.indices) {
                            val clipboard: ClipboardManager =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val aliasEmailAddress = currentList[pos].email
                            val clip = ClipData.newPlainText("alias", aliasEmailAddress)
                            clipboard.setPrimaryClip(clip)

                            hideFabForSnackBarTime()

                            showError(null, R.string.copied_alias, null, null)
                        }
                    }

                    override fun onSelectionMode(selectionMode: Boolean, selectedAliases: ArrayList<Aliases>) {
                        if (selectionMode) {
                            binding.aliasAddAliasFab.hide()

                            if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                                aliasSelectionSnackbar = SnackbarHelper.createSnackbar(
                                    context,
                                    context.resources.getQuantityString(R.plurals.multiple_alias_selected, selectedAliases.count(), selectedAliases.count()),
                                    (activity as? MainActivity)?.findViewById(R.id.main_container) ?: requireView(),
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
                                }

                                aliasSelectionSnackbar?.show()
                            } else {
                                val bottomNavView: BottomNavigationView? =
                                    activity?.findViewById(R.id.nav_view)
                                bottomNavView?.let {
                                    aliasSelectionSnackbar = SnackbarHelper.createSnackbar(
                                        context,
                                        context.resources.getQuantityString(R.plurals.multiple_alias_selected, selectedAliases.count(), selectedAliases.count()),
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
                binding.aliasAllAliasesRecyclerview.adapter = aliasAdapter
            } else {
                aliasAdapter?.submitList(aliasList!!.data.toList())
            }
            binding.aliasProgress.visibility = View.GONE
            binding.aliasAllAliasesRecyclerview.hideShimmer()
            // Enable scrollviewlistener again
            setOnNestedScrollViewListener(set = true)
        }
    }

    private fun loadRecentSearches() {
        val json = encryptedSettingsManager?.getSettingsString(SettingsManager.PREFS.RECENT_SEARCHES_ALIASES)
        if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<ArrayList<String>>() {}.type
            recentSearchesList = GsonTools.gson.fromJson(json, type) ?: arrayListOf()
        }
    }

    private fun updateRecentSearchesVisibility() {
        binding.aliasRecentSearchesLayout.visibility = if (recentSearchesList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun saveRecentSearch(search: String) {
        if (search.isEmpty()) return
        recentSearchesList.remove(search)
        recentSearchesList.add(0, search)
        // Keep max 25 recent searches
        if (recentSearchesList.size > 25) {
            recentSearchesList.removeAt(25)
        }
        val json = GsonTools.gson.toJson(recentSearchesList)
        encryptedSettingsManager?.putSettingsString(SettingsManager.PREFS.RECENT_SEARCHES_ALIASES, json)
        searchAdapter.submitList(recentSearchesList.toList())
        updateRecentSearchesVisibility()
    }

    private fun setSearchRecyclerView() {
        binding.aliasSearchView.setupWithSearchBar(binding.aliasSearchBar)
        loadRecentSearches()
        updateRecentSearchesVisibility()
        searchAdapter = SearchAdapter(recentSearchesList)
        binding.aliasSearchViewRecyclerview.adapter = searchAdapter

        searchAdapter.setClickListener(object : SearchAdapter.ClickListener {
            override fun onClickSearchResult(pos: Int, aView: View) {
                val currentList = recentSearchesList
                if (pos in currentList.indices) {
                    val search = currentList[pos]
                    binding.aliasSearchBar.setText(search)
                    binding.aliasSearchView.setText(search)
                    binding.aliasSearchView.hide()

                    aliasSortFilter.filter = search.lowercase(java.util.Locale.getDefault())
                    getDataFromWeb(null)
                }
            }
        })

        binding.aliasClearRecentSearches.setOnClickListener {
            recentSearchesList.clear()
            encryptedSettingsManager?.removeSetting(SettingsManager.PREFS.RECENT_SEARCHES_ALIASES)
            searchAdapter.submitList(emptyList())
            updateRecentSearchesVisibility()
        }
    }

    private fun setAliasesRecyclerView() {
        binding.aliasAllAliasesRecyclerview.apply {
            if (oneTimeRecyclerViewActions) {
                oneTimeRecyclerViewActions = false
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

    // 4. Observers (None)

    // 5. Private Helpers / Public Methods
    private fun setHasReachedTopOfNsv() {
        updateHasReachedTopOfNsv(binding.fragmentAliasesNsv)
    }

    fun getDataFromWeb(savedInstanceState: Bundle?, isLoadMore: Boolean = false, showShimmer: Boolean = true): kotlinx.coroutines.Job? {
        isSilentRefresh = !showShimmer
        if (isLoadMore) {
            val current = aliasesViewModel.currentData
            if (current == null || (current.meta?.current_page ?: 0) >= (current.meta?.last_page ?: 0)) {
                return null
            }
            if (aliasesViewModel.isLoadingMore.value) {
                return null
            }
            isSilentRefresh = true
            setOnNestedScrollViewListener(set = false)
            if (current.meta?.current_page == 3) {
                showSearchHintSnackbar()
            }
            return aliasesViewModel.loadAliases(aliasSortFilter, forceRefresh = false, isLoadMore = true)
        } else {
            setOnNestedScrollViewListener(set = false)
            return aliasesViewModel.loadAliases(aliasSortFilter, forceRefresh = (savedInstanceState == null), isLoadMore = false)
        }
    }

    private fun showSearchHintSnackbar() {
        hideFabForSnackBarTime()
        showError(null, R.string.alias_search_hint, null, null)
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

    override fun onAdded() {
        addAliasBottomDialogFragment.dismissAllowingStateLoss()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb(null, showShimmer = false)

        if (BuildConfig.FLAVOR == "gplay") {
            // User has successfully created an alias, this is usually a sign of a satisfied user, let's ask the user to review the app only after the app has been opened at least 10 times
            if ((settingsManager?.getSettingsInt(SettingsManager.PREFS.TIMES_THE_APP_HAS_BEEN_OPENED) ?: 0) >= 10) {
                activity?.let { ReviewHelper().launchReviewFlow(it) }
            }

        }
    }

    override fun onCancel() {
        // Nothing
    }

    override fun setFilterAndSortingSettings(aliasSortFilter: AliasSortFilter) {
        this.aliasSortFilter = aliasSortFilter
        // Turn the list into a json object
        val filterToSave = aliasSortFilter.copy(filter = null)
        val data = GsonTools.gson.toJson(filterToSave)
        // Store a copy of the just received data locally
        settingsManager?.putSettingsString(SettingsManager.PREFS.ALIAS_SORT_FILTER, data)


        if (filterOptionsAliasBottomDialogFragment.isAdded) {
            // Could not be added because this is called from homeFragment on sw600dp
            filterOptionsAliasBottomDialogFragment.dismissAllowingStateLoss()
        }

        loadFilter()
        getDataFromWeb(null, showShimmer = false)
    }

    override fun onDismiss() {
        loadFilter()
    }

    override fun onWatchedAliasesChanged() {
        aliasAdapter?.updateWatchedAliases()
    }

    override fun onCloseMultipleSelectionBottomDialogFragment(shouldRefreshData: Boolean) {
        aliasMultipleSelectionBottomDialogFragment.dismissAllowingStateLoss()
        aliasAdapter?.updateWatchedAliases()

        if (shouldRefreshData) {
            aliasAdapter?.unselectAliases()
            hideSnackBar()
            getDataFromWeb(null, showShimmer = false)
        } else {
            // Show snackbar again
            aliasSelectionSnackbar?.show()
        }
    }

    override fun onCancelMultipleSelectionBottomDialogFragment(shouldRefreshData: Boolean) {
        aliasMultipleSelectionBottomDialogFragment.dismissAllowingStateLoss()
        aliasAdapter?.unselectAliases()
        aliasAdapter?.updateWatchedAliases()
        hideSnackBar()
        if (shouldRefreshData) {
            getDataFromWeb(null, showShimmer = false)
        }
    }

    override suspend fun onRefreshData() {
        // The key is to check if the view is created before proceeding.
        // `viewLifecycleOwner` can be used as a proxy for this check.
        if (!isAdded) {
            return
        }

        // Use a try-catch as an ultimate safeguard against rare lifecycle race conditions.
        try {
            isSilentRefresh = true
            getDataFromWeb(null, showShimmer = false)?.join()
        } catch (e: Exception) {
            // Log the error if the lifecycle state was somehow invalid despite the check.
            LoggingHelper(requireContext()).addLog(
                LOGIMPORTANCE.CRITICAL.int,
                "Failed to refresh data, view lifecycle not available. $e",
                "AliasesFragment",
                null
            )
        }
    }

    // 6. Companion Object
    companion object {
        fun newInstance() = AliasesFragment()
    }
}
