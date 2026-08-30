package host.stjin.anonaddy.ui.faileddeliveries

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.adapter.FailedDeliveryAdapter
import host.stjin.anonaddy.databinding.FragmentFailedDeliveriesBinding
import host.stjin.anonaddy.interfaces.Refreshable
import host.stjin.anonaddy.ui.base.BaseFragment
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy_shared.models.FailedDeliveries
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.PaginatedResponse
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class FailedDeliveriesFragment : BaseFragment(), FailedDeliveryDetailsBottomDialogFragment.AddFailedDeliveryBottomDialogListener, Refreshable {

    // 1. Properties
    private val failedDeliveriesViewModel: FailedDeliveriesViewModel by viewModels()

    private var failedDeliveriesList: PaginatedResponse<FailedDeliveries>? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var oneTimeRecyclerViewActions: Boolean = true

    private var failedDeliveryDetailsBottomDialogFragment: FailedDeliveryDetailsBottomDialogFragment? = null

    private var _binding: FragmentFailedDeliveriesBinding? = null
    private val binding get() = _binding!!

    private lateinit var failedDeliveriesAdapter: FailedDeliveryAdapter

    private var isSilentRefresh = false

    // 2. Lifecycle Methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFailedDeliveriesBinding.inflate(inflater, container, false)
        InsetUtils.applyBottomInset(binding.fragmentFailedDeliveriesLL1)
        val root = binding.root

        encryptedSettingsManager = ServiceLocator.encryptedSettingsManager

        setFailedDeliveriesRecyclerView()
        setOnNestedScrollViewListener(true)
        observeViewModel()
        getDataFromWeb(savedInstanceState)

        return root
    }

    // 4. Observers
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    failedDeliveriesViewModel.failedDeliveriesState.collect { state ->
                        handleUiState(
                            state,
                            shimmer = if (!isSilentRefresh) binding.fragmentFailedDeliveriesAllFailedDeliveriesRecyclerview else null,
                            progress = if (isSilentRefresh && !failedDeliveriesViewModel.isLoadingMore.value) binding.failedDeliveriesProgress else null,
                            titleProgress = if (isSilentRefresh && !failedDeliveriesViewModel.isLoadingMore.value) binding.failedDeliveriesTitleProgress else null,
                            errorStringRes = R.string.error_obtaining_failed_deliveries,
                            unavailableView = binding.fragmentContentUnavailable.root,
                            contentView = binding.fragmentFailedDeliveriesNSV
                        ) { data ->
                            setOnNestedScrollViewListener(true)
                            setFailedDeliveriesAdapter(data)
                        }
                        if (state is UiState.Error) {
                            setOnNestedScrollViewListener(true)
                        }
                    }
                }
                launch {
                    failedDeliveriesViewModel.isLoadingMore.collect { loadingMore ->
                        binding.failedDeliveriesProgress.visibility = if (loadingMore) View.VISIBLE else View.GONE
                        binding.failedDeliveriesTitleProgress.visibility = if (loadingMore) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 3. View Setup
    private fun setFailedDeliveriesRecyclerView() {
        failedDeliveriesAdapter = FailedDeliveryAdapter()
        failedDeliveriesAdapter.setClickListener(object : FailedDeliveryAdapter.ClickListener {
            override fun onClickDetails(pos: Int, view: View) {
                failedDeliveriesList?.data?.getOrNull(pos)?.let {
                    failedDeliveryDetailsBottomDialogFragment = FailedDeliveryDetailsBottomDialogFragment.newInstance(it)
                    failedDeliveryDetailsBottomDialogFragment!!.show(
                        childFragmentManager,
                        "failedDeliveryDetailsBottomDialogFragment"
                    )
                }
            }
        })

        binding.fragmentFailedDeliveriesAllFailedDeliveriesRecyclerview.apply {
            adapter = failedDeliveriesAdapter
            if (oneTimeRecyclerViewActions) {
                oneTimeRecyclerViewActions = false

                shimmerItemCount =
                    encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT, 2) ?: 2
                shimmerLayoutManager = GridLayoutManager(requireContext(), ScreenSizeUtils.calculateNoOfColumns(context))
                layoutManager = GridLayoutManager(requireContext(), ScreenSizeUtils.calculateNoOfColumns(context))

                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation

                showShimmer()

                binding.fragmentFailedDeliveriesChipgroup.setOnCheckedStateChangeListener { _, checkedIds ->
                    if (checkedIds.isNotEmpty()) {
                        loadFailedDeliveries(forceReload = true, showShimmer = false)
                    }
                }
            }
        }
    }

    private fun setFailedDeliveriesAdapter(list: PaginatedResponse<FailedDeliveries>) {
        binding.failedDeliveriesCount.apply {
            list.meta?.total?.let { total ->
                if (total > 0) {
                    text = String.format(java.util.Locale.getDefault(), "%d", total)
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            } ?: run { visibility = View.GONE }
        }

        binding.fragmentFailedDeliveriesAllFailedDeliveriesRecyclerview.apply {
            failedDeliveriesList = list
            val data = list.data

            if (data.isNotEmpty()) {
                binding.fragmentFailedDeliveriesNoFailedDeliveries.visibility = View.GONE
            } else {
                binding.fragmentFailedDeliveriesNoFailedDeliveries.visibility = View.VISIBLE
            }

            failedDeliveriesAdapter.submitList(data.toList())

            if (!isTablet) {
                fragmentShown()
            }

            binding.animationFragment.stopAnimation()
        }
    }

    private fun setOnNestedScrollViewListener(status: Boolean) {
        if (status) {
            binding.fragmentFailedDeliveriesNSV.setOnScrollChangeListener(androidx.core.widget.NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                val threshold = 10 // or some small number to account for rounding errors
                if (scrollY + v.measuredHeight + threshold >= v.getChildAt(0).measuredHeight) {
                    // Consider this as being at the bottom
                    getDataFromWeb(null, isLoadMore = true)
                }
                updateHasReachedTopOfNsv(binding.fragmentFailedDeliveriesNSV)
            })
        } else {
            binding.fragmentFailedDeliveriesNSV.setOnScrollChangeListener(null as androidx.core.widget.NestedScrollView.OnScrollChangeListener?)
        }
    }

    // 5. Private Helpers / Public Methods
    fun getDataFromWeb(savedInstanceState: Bundle?, isLoadMore: Boolean = false, showShimmer: Boolean = true) {
        isSilentRefresh = !showShimmer
        if (isLoadMore) {
            val current = failedDeliveriesViewModel.currentData
            if (current == null || (current.meta?.current_page ?: 0) >= (current.meta?.last_page ?: 0)) {
                return
            }
            if (failedDeliveriesViewModel.isLoadingMore.value) {
                return
            }
            isSilentRefresh = true
            setOnNestedScrollViewListener(false)
            loadFailedDeliveries(forceReload = false, showShimmer = false)
        } else {
            setOnNestedScrollViewListener(false)
            loadFailedDeliveries(forceReload = (savedInstanceState == null), showShimmer = showShimmer)
        }
    }

    fun fragmentShown() {
        if (::failedDeliveriesAdapter.isInitialized) {
            encryptedSettingsManager?.putSettingsInt(
                SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT,
                failedDeliveriesList?.meta?.total ?: failedDeliveriesAdapter.itemCount
            )

            val latestId = failedDeliveriesList?.data?.firstOrNull()?.id ?: ""

            if (latestId.isNotEmpty()) {
                encryptedSettingsManager?.putSettingsString(
                    SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_LATEST_ID,
                    latestId
                )
                encryptedSettingsManager?.putSettingsString(
                    SettingsManager.PREFS.BACKGROUND_SERVICE_NOTIFIED_FAILED_DELIVERIES_LATEST_ID,
                    latestId
                )
            }
        }
    }

    override suspend fun onRefreshData() {
        if (!isAdded) {
            return
        }
        try {
            isSilentRefresh = true
            loadFailedDeliveries(forceReload = true, showShimmer = false)?.join()
        } catch (e: Exception) {
            LoggingHelper(requireContext()).addLog(
                LOGIMPORTANCE.CRITICAL.int,
                "Failed to refresh data, view lifecycle not available. $e",
                "FailedDeliveriesFragment",
                null
            )
        }
    }

    override fun onDeleted(failedDeliveryId: String) {
        failedDeliveryDetailsBottomDialogFragment?.dismissAllowingStateLoss()
        getDataFromWeb(null, showShimmer = false)
    }

    private fun getSelectedFilter(): String? {
        return when (binding.fragmentFailedDeliveriesChipgroup.checkedChipId) {
            R.id.fragment_failed_deliveries_chip_inbound -> "inbound"
            R.id.fragment_failed_deliveries_chip_inbound_quarantined -> "inbound_quarantined"
            R.id.fragment_failed_deliveries_chip_outbound -> "outbound"
            else -> null
        }
    }

    private fun loadFailedDeliveries(forceReload: Boolean = false, showShimmer: Boolean = true): kotlinx.coroutines.Job? {
        isSilentRefresh = !showShimmer
        if (getSelectedFilter() == null) {
            binding.fragmentFailedDeliveriesAllFailedDeliveriesTitle.text = getString(R.string.failed_deliveries)
        } else {
            binding.fragmentFailedDeliveriesAllFailedDeliveriesTitle.text = getString(R.string.failed_deliveries_filtered)
        }

        if (forceReload) {
            failedDeliveriesList = null
            if (showShimmer) {
                binding.fragmentFailedDeliveriesAllFailedDeliveriesRecyclerview.showShimmer()
            }
        }

        val currentData = failedDeliveriesViewModel.currentData
        if (forceReload || currentData == null || (currentData.meta?.current_page ?: 0) < (currentData.meta?.last_page ?: 0)) {
            return failedDeliveriesViewModel.loadFailedDeliveries(
                filter = getSelectedFilter(),
                forceRefresh = forceReload,
                isLoadMore = !forceReload && currentData != null
            )
        }
        return null
    }

    companion object {
        fun newInstance() = FailedDeliveriesFragment()
    }
}
