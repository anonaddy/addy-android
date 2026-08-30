package host.stjin.anonaddy.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.FragmentHomeBinding
import host.stjin.anonaddy.interfaces.Refreshable
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.ui.aliases.SharedFilterViewModel
import host.stjin.anonaddy.ui.base.BaseFragment
import host.stjin.anonaddy.ui.base.SharedScrollViewModel
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.NumberUtils.roundOffDecimal
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment(), Refreshable {

    private val homeViewModel: HomeViewModel by viewModels()
    private val sharedFilterViewModel: SharedFilterViewModel by activityViewModels()
    private val sharedScrollViewModel: SharedScrollViewModel by activityViewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        setOnClickListeners()
        setNsvListener()
        observeViewModel()

        homeViewModel.loadUserResource(forceRefresh = savedInstanceState == null)

        return root
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    homeViewModel.userResourceState.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                setStatistics(state.data)
                            }
                            is UiState.Error -> {
                                showError(state.message, R.string.error_obtaining_user)
                            }
                            is UiState.Loading -> {}
                        }
                    }
                }
                launch {
                    sharedScrollViewModel.scrollEvents.collect {
                        _binding?.homeStatisticsNSV?.smoothScrollTo(0, 0)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setHasReachedTopOfNsv()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setOnClickListeners() {
        binding.homeStatCardTotalAliases.setOnLayoutClickedListener {
            MaterialDialogHelper.showMaterialDialog(
                context = requireContext(),
                title = requireContext().resources.getString(R.string.apply_filter),
                message = requireContext().resources.getString(R.string.apply_filter_desc),
                icon = R.drawable.ic_filter,
                neutralButtonText = requireContext().resources.getString(R.string.cancel),
                positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                positiveButtonAction = {
                    sharedFilterViewModel.applyFilter(
                        AliasSortFilter(
                            onlyActiveAliases = false,
                            onlyDeletedAliases = false,
                            onlyInactiveAliases = false,
                            onlyWatchedAliases = false,
                            onlyPinnedAliases = false,
                            sort = null,
                            sortDesc = false,
                            filter = null
                        )
                    )
                    (activity as? MainActivity)?.navigateTo(R.id.navigation_alias)
                }
            ).show()
        }

        binding.homeStatCardActiveAliases.setOnLayoutClickedListener {
            MaterialDialogHelper.showMaterialDialog(
                context = requireContext(),
                title = requireContext().resources.getString(R.string.apply_filter),
                message = requireContext().resources.getString(R.string.apply_filter_desc),
                icon = R.drawable.ic_filter,
                neutralButtonText = requireContext().resources.getString(R.string.cancel),
                positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                positiveButtonAction = {
                    sharedFilterViewModel.applyFilter(
                        AliasSortFilter(
                            onlyActiveAliases = true,
                            onlyDeletedAliases = false,
                            onlyInactiveAliases = false,
                            onlyWatchedAliases = false,
                            onlyPinnedAliases = false,
                            sort = null,
                            sortDesc = false,
                            filter = null
                        )
                    )
                    (activity as? MainActivity)?.navigateTo(R.id.navigation_alias)
                }
            ).show()
        }

        binding.homeStatCardInactiveAliases.setOnLayoutClickedListener {
            MaterialDialogHelper.showMaterialDialog(
                context = requireContext(),
                title = requireContext().resources.getString(R.string.apply_filter),
                message = requireContext().resources.getString(R.string.apply_filter_desc),
                icon = R.drawable.ic_filter,
                neutralButtonText = requireContext().resources.getString(R.string.cancel),
                positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                positiveButtonAction = {
                    sharedFilterViewModel.applyFilter(
                        AliasSortFilter(
                            onlyActiveAliases = false,
                            onlyDeletedAliases = false,
                            onlyInactiveAliases = true,
                            onlyWatchedAliases = false,
                            onlyPinnedAliases = false,
                            sort = null,
                            sortDesc = false,
                            filter = null
                        )
                    )
                    (activity as? MainActivity)?.navigateTo(R.id.navigation_alias)
                }
            ).show()
        }

        binding.homeStatCardDeletedAliases.setOnLayoutClickedListener {
            MaterialDialogHelper.showMaterialDialog(
                context = requireContext(),
                title = requireContext().resources.getString(R.string.apply_filter),
                message = requireContext().resources.getString(R.string.apply_filter_desc),
                icon = R.drawable.ic_filter,
                neutralButtonText = requireContext().resources.getString(R.string.cancel),
                positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                positiveButtonAction = {
                    sharedFilterViewModel.applyFilter(
                        AliasSortFilter(
                            onlyActiveAliases = false,
                            onlyDeletedAliases = true,
                            onlyInactiveAliases = false,
                            onlyWatchedAliases = false,
                            onlyPinnedAliases = false,
                            sort = null,
                            sortDesc = false,
                            filter = null
                        )
                    )
                    (activity as? MainActivity)?.navigateTo(R.id.navigation_alias)
                }
            ).show()
        }

        binding.homeStatPinnedAliases.setOnLayoutClickedListener {
            MaterialDialogHelper.showMaterialDialog(
                context = requireContext(),
                title = requireContext().resources.getString(R.string.apply_filter),
                message = requireContext().resources.getString(R.string.apply_filter_desc),
                icon = R.drawable.ic_filter,
                neutralButtonText = requireContext().resources.getString(R.string.cancel),
                positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                positiveButtonAction = {
                    sharedFilterViewModel.applyFilter(
                        AliasSortFilter(
                            onlyActiveAliases = false,
                            onlyDeletedAliases = false,
                            onlyInactiveAliases = false,
                            onlyWatchedAliases = false,
                            onlyPinnedAliases = true,
                            sort = null,
                            sortDesc = false,
                            filter = null
                        )
                    )
                    (activity as? MainActivity)?.navigateTo(R.id.navigation_alias)
                }
            ).show()
        }

        binding.homeStatWatchedAliases.setOnLayoutClickedListener {
            MaterialDialogHelper.showMaterialDialog(
                context = requireContext(),
                title = requireContext().resources.getString(R.string.apply_filter),
                message = requireContext().resources.getString(R.string.apply_filter_desc),
                icon = R.drawable.ic_filter,
                neutralButtonText = requireContext().resources.getString(R.string.cancel),
                positiveButtonText = requireContext().resources.getString(R.string.apply_filter),
                positiveButtonAction = {
                    val aliasWatcher = AliasWatcher(requireContext())
                    val aliasesToWatch = aliasWatcher.getAliasesToWatch().toList()
                    if (aliasesToWatch.isNotEmpty()) {
                        sharedFilterViewModel.applyFilter(
                            AliasSortFilter(
                                onlyActiveAliases = false,
                                onlyDeletedAliases = false,
                                onlyInactiveAliases = false,
                                onlyWatchedAliases = true,
                                onlyPinnedAliases = false,
                                sort = null,
                                sortDesc = false,
                                filter = null
                            )
                        )
                    }
                    (activity as? MainActivity)?.navigateTo(R.id.navigation_alias)
                }
            ).show()
        }

        binding.homeStatCardTotalRecipients.setOnLayoutClickedListener { (activity as? MainActivity)?.navigateTo(R.id.navigation_recipients) }
    }

    private fun setNsvListener() {
        setupNsvScrollListener(binding.homeStatisticsNSV)
    }

    private fun setHasReachedTopOfNsv() {
        updateHasReachedTopOfNsv(binding.homeStatisticsNSV)
    }

    private fun setStatistics(userResource: UserResource = (activity?.application as AddyIoApp).userResource) {
        val currMonthlyBandwidth = userResource.bandwidth.toDouble() / 1024 / 1024
        val maxMonthlyBandwidth = (userResource.bandwidth_limit ?: 0) / 1024 / 1024

        binding.homeStatCardForwarded.setDescription(userResource.total_emails_forwarded.toString())
        binding.homeStatCardBlocked.setDescription(userResource.total_emails_blocked.toString())
        binding.homeStatCardReplies.setDescription(userResource.total_emails_replied.toString())
        binding.homeStatCardSent.setDescription(userResource.total_emails_sent.toString())

        val bandwidthText = if (maxMonthlyBandwidth.compareTo(0) == 0) {
            this.resources.getString(R.string.home_bandwidth_text, roundOffDecimal(currMonthlyBandwidth).toString(), "∞")
        } else {
            this.resources.getString(R.string.home_bandwidth_text, roundOffDecimal(currMonthlyBandwidth).toString(), maxMonthlyBandwidth.toString())
        }

        binding.homeStatCardBandwidth.setDescription(bandwidthText)

        if (maxMonthlyBandwidth > 0) {
            binding.homeStatCardBandwidth.setProgress(currMonthlyBandwidth.toFloat() / maxMonthlyBandwidth.toFloat() * 100)
        }

        binding.homeStatCardTotalAliases.setDescription(userResource.total_aliases.toString())
        binding.homeStatCardActiveAliases.setDescription(userResource.total_active_aliases.toString())
        binding.homeStatCardInactiveAliases.setDescription(userResource.total_inactive_aliases.toString())
        binding.homeStatCardDeletedAliases.setDescription(userResource.total_deleted_aliases.toString())
        binding.homeStatPinnedAliases.setDescription(userResource.total_pinned_aliases.toString())
        binding.homeStatCardTotalRecipients.setDescription(userResource.recipient_count.toString())

        val aliasWatcher = AliasWatcher(requireContext())
        val aliasesToWatch = aliasWatcher.getAliasesToWatch().toList()
        binding.homeStatWatchedAliases.setDescription(aliasesToWatch.size.toString())

        if (aliasesToWatch.isEmpty()) {
            binding.homeStatWatchedAliases.setButtonText(requireContext().resources.getString(R.string.start_watching))
        } else {
            binding.homeStatWatchedAliases.setButtonText(requireContext().resources.getString(R.string.view_watched))
        }
    }

    override suspend fun onRefreshData() {
        if (!isAdded) return
        try {
            homeViewModel.loadUserResource(forceRefresh = true).join()
        } catch (e: Exception) {
            LoggingHelper(requireContext()).addLog(
                LOGIMPORTANCE.CRITICAL.int,
                "Failed to refresh data, view lifecycle not available. $e",
                "HomeFragment",
                null
            )
        }
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
