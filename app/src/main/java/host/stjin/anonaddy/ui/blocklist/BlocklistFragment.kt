package host.stjin.anonaddy.ui.blocklist
import host.stjin.anonaddy_shared.utils.GsonTools

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.BlocklistAdapter
import host.stjin.anonaddy.adapter.SearchAdapter
import host.stjin.anonaddy.databinding.FragmentManageBlocklistBinding
import host.stjin.anonaddy.interfaces.Refreshable
import host.stjin.anonaddy.ui.base.BaseFragment
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.BlocklistEntries
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.NewBlocklistEntry
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class BlocklistFragment : BaseFragment(), AddBlocklistBottomDialogFragment.AddBlocklistBottomDialogListener, Refreshable {

    // 1. Properties
    private val blocklistViewModel: BlocklistViewModel by viewModels()

    private var blocklistEntries: PaginatedResponse<BlocklistEntries>? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var oneTimeRecyclerViewActions: Boolean = true

    private var blocklistAddBottomDialogFragment: AddBlocklistBottomDialogFragment? = null

    private var _binding: FragmentManageBlocklistBinding? = null
    private val binding get() = _binding!!

    private lateinit var blocklistAdapter: BlocklistAdapter
    private lateinit var searchAdapter: SearchAdapter
    private var recentSearchesList: ArrayList<String> = arrayListOf()

    private lateinit var deleteBlocklistSnackbar: Snackbar

    private var isSilentRefresh = false

    // 2. Lifecycle Methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageBlocklistBinding.inflate(inflater, container, false)
        InsetUtils.applyBottomInset(binding.fragmentBlocklistLL1)
        val root = binding.root

        encryptedSettingsManager = ServiceLocator.encryptedSettingsManager

        setBlocklistRecyclerView()
        setSearchRecyclerView()
        observeViewModel()
        getDataFromWeb(savedInstanceState)
        setOnClickListeners()

        return root
    }

    // 4. Observers
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    blocklistViewModel.blocklistState.collect { state ->
                        handleUiState(
                            state,
                            shimmer = if (!isSilentRefresh) binding.fragmentBlocklistAllBlocklistRecyclerview else null,
                            progress = if (isSilentRefresh && !blocklistViewModel.isLoadingMore.value) binding.blocklistProgress else null,
                            titleProgress = if (isSilentRefresh && !blocklistViewModel.isLoadingMore.value) binding.blocklistTitleProgress else null,
                            errorStringRes = R.string.something_went_wrong_retrieving_blocklist_entries,
                            unavailableView = binding.fragmentContentUnavailable.root,
                            contentView = binding.fragmentBlocklistContentLL
                        ) { data ->
                            setOnNestedScrollViewListener(true)
                            setBlocklistAdapter(data)
                        }
                        if (state is UiState.Error) {
                            setOnNestedScrollViewListener(true)
                        }
                    }
                }
                launch {
                    blocklistViewModel.isLoadingMore.collect { loadingMore ->
                        binding.blocklistProgress.visibility = if (loadingMore) View.VISIBLE else View.GONE
                        binding.blocklistTitleProgress.visibility = if (loadingMore) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::deleteBlocklistSnackbar.isInitialized && deleteBlocklistSnackbar.isShown) {
            deleteBlocklistSnackbar.dismiss()
        }
        _binding = null
    }

    // 3. View Setup
    private fun setOnClickListeners() {
        binding.blocklistSearchView.editText.addTextChangedListener { text ->
            val searchText = text?.toString()?.trim()
            if (searchText.isNullOrEmpty()) {
                binding.blocklistSearchBar.setText(null)
                getDataFromWeb(null)
            }
        }

        binding.blocklistSearchView.addTransitionListener { _, _, newState ->
            if (newState == com.google.android.material.search.SearchView.TransitionState.HIDDEN) {
                val searchText = binding.blocklistSearchView.text.toString().trim()
                if (searchText.isEmpty()) {
                    binding.blocklistSearchBar.setText(null)
                    getDataFromWeb(null)
                }
            }
        }

        binding.blocklistSearchView.editText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event?.action == android.view.KeyEvent.ACTION_DOWN &&
                        event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)
            ) {
                val inputMethodManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.blocklistSearchView.windowToken, 0)

                binding.blocklistSearchBar.setText(binding.blocklistSearchView.text)
                binding.blocklistSearchView.hide()

                val searchText = binding.blocklistSearchView.text.toString().trim()
                saveRecentSearch(searchText)
                getDataFromWeb(null)
                true
            } else {
                false
            }
        }

        binding.fragmentBlocklistAddBlocklistEntryButton.setOnClickListener {
            blocklistAddBottomDialogFragment = AddBlocklistBottomDialogFragment()
            blocklistAddBottomDialogFragment!!.show(
                childFragmentManager,
                "blocklistAddBottomDialogFragment"
            )
        }
    }

    private fun setBlocklistRecyclerView() {
        blocklistAdapter = BlocklistAdapter()
        blocklistAdapter.setClickListener(object : BlocklistAdapter.ClickListener {
            override fun onClickDelete(pos: Int, view: View, id: String) {
                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = resources.getString(R.string.remove_from_blocklist),
                    message = resources.getString(R.string.remove_from_blocklist_desc),
                    icon = R.drawable.ic_trash,
                    neutralButtonText = resources.getString(R.string.cancel),
                    positiveButtonText = resources.getString(R.string.remove),
                    positiveButtonAction = {
                        deleteBlocklistSnackbar = SnackbarHelper.createSnackbar(
                            requireContext(),
                            requireContext().resources.getString(R.string.deleting_blocklist_entry),
                            getSnackbarContainer(),
                            length = Snackbar.LENGTH_INDEFINITE
                        )
                        deleteBlocklistSnackbar.show()

                        viewLifecycleOwner.lifecycleScope.launch {
                            deleteBlocklistEntryHttpRequest(id)
                        }
                    }
                ).show()
            }
        })

        binding.fragmentBlocklistAllBlocklistRecyclerview.apply {
            adapter = blocklistAdapter
            if (oneTimeRecyclerViewActions) {
                oneTimeRecyclerViewActions = false

                shimmerItemCount =
                    encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_BLOCKLIST_ENTRIES_COUNT, 2) ?: 2
                shimmerLayoutManager = GridLayoutManager(requireContext(), ScreenSizeUtils.calculateNoOfColumns(context))
                layoutManager = GridLayoutManager(requireContext(), ScreenSizeUtils.calculateNoOfColumns(context))

                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation

                showShimmer()

                binding.fragmentBlocklistChipgroup.setOnCheckedStateChangeListener { _, checkedIds ->
                    if (checkedIds.isNotEmpty()) {
                        loadBlocklistEntries(forceReload = true, showShimmer = false)
                    }
                }
            }
        }
    }

    private fun setBlocklistAdapter(list: PaginatedResponse<BlocklistEntries>) {
        binding.blocklistCount.apply {
            list.meta?.total?.let { total ->
                if (total > 0) {
                    text = String.format(java.util.Locale.getDefault(), "%d", total)
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            } ?: run { visibility = View.GONE }
        }

        binding.fragmentBlocklistAllBlocklistRecyclerview.apply {
            blocklistEntries = list
            val data = list.data

            if (data.isNotEmpty()) {
                binding.fragmentBlocklistNoBlocklist.visibility = View.GONE
            } else {
                binding.fragmentBlocklistNoBlocklist.visibility = View.VISIBLE
            }

            blocklistAdapter.submitList(data.toList())

            if (!isTablet) {
                fragmentShown()
            }

            binding.animationFragment.stopAnimation()
        }
    }

    private fun loadRecentSearches() {
        val json = encryptedSettingsManager?.getSettingsString(SettingsManager.PREFS.RECENT_SEARCHES_BLOCKLIST)
        if (json != null) {
            val type = object : TypeToken<ArrayList<String>>() {}.type
            recentSearchesList = GsonTools.gson.fromJson(json, type) ?: arrayListOf()
        }
    }

    private fun updateRecentSearchesVisibility() {
        binding.blocklistRecentSearchesLayout.visibility = if (recentSearchesList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun saveRecentSearch(search: String) {
        if (search.isEmpty()) return
        recentSearchesList.remove(search)
        recentSearchesList.add(0, search)
        if (recentSearchesList.size > 25) {
            recentSearchesList.removeAt(25)
        }
        val json = GsonTools.gson.toJson(recentSearchesList)
        encryptedSettingsManager?.putSettingsString(SettingsManager.PREFS.RECENT_SEARCHES_BLOCKLIST, json)
        searchAdapter.submitList(recentSearchesList.toList())
        updateRecentSearchesVisibility()
    }

    private fun setSearchRecyclerView() {
        binding.blocklistSearchView.setupWithSearchBar(binding.blocklistSearchBar)
        loadRecentSearches()
        updateRecentSearchesVisibility()
        searchAdapter = SearchAdapter(recentSearchesList)
        binding.blocklistSearchViewRecyclerview.adapter = searchAdapter

        searchAdapter.setClickListener(object : SearchAdapter.ClickListener {
            override fun onClickSearchResult(pos: Int, aView: View) {
                val currentList = recentSearchesList
                if (pos in currentList.indices) {
                    val search = currentList[pos]
                    binding.blocklistSearchBar.setText(search)
                binding.blocklistSearchView.setText(search)
                binding.blocklistSearchView.hide()

                getDataFromWeb(null)
                }
            }
        })

        binding.blocklistClearRecentSearches.setOnClickListener {
            recentSearchesList.clear()
            encryptedSettingsManager?.removeSetting(SettingsManager.PREFS.RECENT_SEARCHES_BLOCKLIST)
            searchAdapter.submitList(emptyList())
            updateRecentSearchesVisibility()
        }
    }

    private fun setOnNestedScrollViewListener(set: Boolean) {
        if (set) {
            binding.fragmentBlocklistNSV.setOnScrollChangeListener(androidx.core.widget.NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                val threshold = 10 // or some small number to account for rounding errors
                if (scrollY + v.measuredHeight + threshold >= v.getChildAt(0).measuredHeight) {
                    // Consider this as being at the bottom
                    getDataFromWeb(null, isLoadMore = true)
                }
                updateHasReachedTopOfNsv(binding.fragmentBlocklistNSV)
            })
        } else {
            binding.fragmentBlocklistNSV.setOnScrollChangeListener(null as androidx.core.widget.NestedScrollView.OnScrollChangeListener?)
        }
    }

    // 5. Private Helpers / Public Methods
    fun getDataFromWeb(savedInstanceState: Bundle?, isLoadMore: Boolean = false, showShimmer: Boolean = true): kotlinx.coroutines.Job? {
        isSilentRefresh = !showShimmer
        if (isLoadMore) {
            val current = blocklistViewModel.currentData
            if (current == null || (current.meta?.current_page ?: 0) >= (current.meta?.last_page ?: 0)) {
                return null
            }
            if (blocklistViewModel.isLoadingMore.value) {
                return null
            }
            isSilentRefresh = true
            setOnNestedScrollViewListener(set = false)
            return loadBlocklistEntries(forceReload = false, showShimmer = false)
        } else {
            setOnNestedScrollViewListener(set = false)
            return loadBlocklistEntries(forceReload = (savedInstanceState == null), showShimmer = showShimmer)
        }
    }

    private fun getSelectedFilter(): String? {
        return when (binding.fragmentBlocklistChipgroup.checkedChipId) {
            R.id.fragment_blocklist_chip_domain -> "domain"
            R.id.fragment_blocklist_chip_email -> "email"
            else -> null
        }
    }

    private fun loadBlocklistEntries(forceReload: Boolean = false, showShimmer: Boolean = true): kotlinx.coroutines.Job {
        isSilentRefresh = !showShimmer
        if (getSelectedFilter() == null) {
            binding.fragmentBlocklistAllBlocklistTitle.text = getString(R.string.blocklist_entries)
        } else {
            binding.fragmentBlocklistAllBlocklistTitle.text = getString(R.string.blocklist_entries_filtered)
        }

        val searchText = binding.blocklistSearchBar.text.toString().trim()
        val search = if (searchText.isEmpty()) null else searchText.lowercase(java.util.Locale.getDefault())

        if (forceReload) {
            blocklistEntries = null
            if (showShimmer) {
                binding.fragmentBlocklistAllBlocklistRecyclerview.showShimmer()
            }
        }

        val currentData = blocklistViewModel.currentData
        return blocklistViewModel.loadBlocklist(
            filter = getSelectedFilter(),
            search = search,
            forceRefresh = forceReload,
            isLoadMore = !forceReload && currentData != null
        )
    }

    private fun fragmentShown() {
        if (::blocklistAdapter.isInitialized) {
            encryptedSettingsManager?.putSettingsInt(
                SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_BLOCKLIST_ENTRIES_COUNT,
                blocklistAdapter.itemCount
            )
        }
    }

    private suspend fun deleteBlocklistEntryHttpRequest(id: String) {
        val result = blocklistViewModel.deleteBlocklistEntry(id)
        if (result is NetworkResult.Success && result.data == "204") {
            deleteBlocklistSnackbar.dismiss()

            val index = blocklistEntries?.data?.indexOfFirst { it.id == id } ?: -1
            if (index != -1) {
                blocklistEntries?.data?.removeAt(index)
                blocklistAdapter.submitList(blocklistEntries?.data?.toList() ?: emptyList())

                if (blocklistEntries?.data?.isEmpty() == true) {
                    binding.fragmentBlocklistNoBlocklist.visibility = View.VISIBLE
                }
                fragmentShown()
            } else {
                getDataFromWeb(null)
            }
        } else {
            val error = result.errorOrNull() ?: ""
            showError(error, R.string.error_deleting_blocklist_entry)
        }
    }

    override suspend fun onRefreshData() {
        if (!isAdded) {
            return
        }
        try {
            isSilentRefresh = true
            loadBlocklistEntries(forceReload = true, showShimmer = false).join()
        } catch (e: Exception) {
            LoggingHelper(requireContext()).addLog(
                LOGIMPORTANCE.CRITICAL.int,
                "Failed to refresh data, view lifecycle not available. $e",
                "BlocklistFragment",
                null
            )
        }
    }

    override fun onAddedBlocklistEntry(newBlocklistEntry: NewBlocklistEntry) {
        blocklistAddBottomDialogFragment?.dismissAllowingStateLoss()
        getDataFromWeb(null, showShimmer = false)
    }

    companion object {
        fun newInstance() = BlocklistFragment()
    }
}
