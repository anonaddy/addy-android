package host.stjin.anonaddy.ui.domains

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.DomainAdapter
import host.stjin.anonaddy.databinding.FragmentDomainSettingsBinding
import host.stjin.anonaddy.interfaces.Refreshable
import host.stjin.anonaddy.ui.base.BaseFragment
import host.stjin.anonaddy.ui.domains.manage.ManageDomainActivity
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class DomainsFragment : BaseFragment(), AddDomainBottomDialogFragment.AddDomainBottomDialogListener, Refreshable {

    // 1. Properties
    private val domainsViewModel: DomainsViewModel by viewModels()

    private var encryptedSettingsManager: SettingsManager? = null
    private var oneTimeRecyclerViewActions: Boolean = true

    private val addDomainFragment: AddDomainBottomDialogFragment = AddDomainBottomDialogFragment.newInstance()
    private var _binding: FragmentDomainSettingsBinding? = null
    private val binding get() = _binding!!

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data?.getBooleanExtra("shouldRefresh", false) == true) {
                getDataFromWeb(null, showShimmer = false)
            }
        }
    }

    private lateinit var domainsAdapter: DomainAdapter
    private lateinit var deleteDomainSnackbar: Snackbar

    private var isSilentRefresh = false

    // 2. Lifecycle Methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDomainSettingsBinding.inflate(inflater, container, false)
        InsetUtils.applyBottomInset(binding.fragmentDomainSettingsLL1)

        val root = binding.root

        encryptedSettingsManager = ServiceLocator.encryptedSettingsManager

        setStats()
        setOnClickListener()
        setNsvListener()
        setDomainsRecyclerView()
        observeViewModel()
        getDataFromWeb(savedInstanceState)

        return root
    }

    private fun setNsvListener() {
        setupNsvScrollListener(binding.fragmentDomainSettingsNSV)
    }

    private fun setHasReachedTopOfNsv() {
        updateHasReachedTopOfNsv(binding.fragmentDomainSettingsNSV)
    }

    override fun onResume() {
        super.onResume()
        setHasReachedTopOfNsv()
    }

    // 4. Observers
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                domainsViewModel.domainsState.collect { state ->
                    handleUiState(
                        state,
                        shimmer = if (!isSilentRefresh) binding.fragmentDomainSettingsAllDomainsRecyclerview else null,
                        progress = if (isSilentRefresh) binding.domainsProgress else null,
                        titleProgress = if (isSilentRefresh) binding.domainsTitleProgress else null,
                        errorStringRes = R.string.error_obtaining_domain
                    ) { data ->
                        setDomainsAdapter(ArrayList(data))
                        setStats()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::deleteDomainSnackbar.isInitialized && deleteDomainSnackbar.isShown) {
            deleteDomainSnackbar.dismiss()
        }
        _binding = null
    }

    // 3. View Setup
    private fun setOnClickListener() {
        binding.fragmentDomainSettingsAddDomain.setOnClickListener {
            if (!addDomainFragment.isAdded) {
                addDomainFragment.show(
                    childFragmentManager,
                    "addDomainFragment"
                )
            }
        }
    }

    private fun setDomainsRecyclerView() {
        domainsAdapter = DomainAdapter()
        domainsAdapter.setClickListener(object : DomainAdapter.ClickListener {
            override fun onClickSettings(pos: Int, view: View) {
                val currentList = domainsAdapter.currentList
                if (pos in currentList.indices) {
                    val intent = Intent(context, ManageDomainActivity::class.java)
                    intent.putExtra("domain_id", currentList[pos].id)
                    resultLauncher.launch(intent)
                }
            }

            override fun onClickDelete(pos: Int, view: View) {
                val currentList = domainsAdapter.currentList
                if (pos in currentList.indices) {
                    deleteDomain(currentList[pos].id)
                }
            }
        })

        binding.fragmentDomainSettingsAllDomainsRecyclerview.apply {
            adapter = domainsAdapter
            if (oneTimeRecyclerViewActions) {
                oneTimeRecyclerViewActions = false

                shimmerItemCount = encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DOMAIN_COUNT, 2) ?: 2
                shimmerLayoutManager = GridLayoutManager(requireContext(), ScreenSizeUtils.calculateNoOfColumns(context))
                layoutManager = GridLayoutManager(requireContext(), ScreenSizeUtils.calculateNoOfColumns(context))

                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation

                showShimmer()
            }
        }
    }

    private fun setDomainsAdapter(list: ArrayList<Domains>) {
        binding.domainSettingsCount.apply {
            val total = list.size
            if (total > 0) {
                text = String.format(java.util.Locale.getDefault(), "%d", total)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        binding.fragmentDomainSettingsAllDomainsRecyclerview.apply {
            if (list.isNotEmpty()) {
                binding.fragmentDomainSettingsNoDomains.visibility = View.GONE
            } else {
                binding.fragmentDomainSettingsNoDomains.visibility = View.VISIBLE
            }

            encryptedSettingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_DOMAIN_COUNT, list.size)
            domainsAdapter.submitList(list.toList())
            binding.animationFragment.stopAnimation()
        }
    }

    // 5. Private Helpers / Public Methods
    fun getDataFromWeb(savedInstanceState: Bundle?, showShimmer: Boolean = true) {
        isSilentRefresh = !showShimmer
        setStats()
        viewLifecycleOwner.lifecycleScope.launch { getUserResource() }
        domainsViewModel.loadDomains(forceRefresh = (savedInstanceState == null))
    }

    private suspend fun getUserResource() {
        when (val result = domainsViewModel.refreshUserResource()) {
            is NetworkResult.Success -> {
                if (!isAdded) return
                setStats()
            }
            is NetworkResult.Error -> {
                if (!isAdded) return
                showError(result.error, R.string.error_obtaining_user)
            }
        }
    }

    private fun setStats() {
        val userResource = (activity?.application as? AddyIoApp)?.userResourceOrNull ?: return
        binding.fragmentDomainSettingsRLCountText.text = resources.getString(
            R.string.you_ve_used_d_out_of_d_active_domains,
            userResource.active_domain_count,
            if (userResource.subscription != null) userResource.active_domain_limit else this.resources.getString(
                R.string.unlimited
            )
        )

        if (userResource.subscription != null) {
            binding.fragmentDomainSettingsAddDomain.isEnabled =
                userResource.active_domain_count < (userResource.active_domain_limit ?: Int.MAX_VALUE)
        } else {
            binding.fragmentDomainSettingsAddDomain.isEnabled = true
        }
    }

    private fun deleteDomain(id: String) {
        MaterialDialogHelper.showMaterialDialog(
            context = requireContext(),
            title = resources.getString(R.string.delete_domain),
            message = resources.getString(R.string.delete_domain_confirmation_desc),
            icon = R.drawable.ic_trash,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.delete),
            positiveButtonAction = {
                deleteDomainSnackbar = SnackbarHelper.createSnackbar(
                    requireContext(),
                    this.resources.getString(R.string.deleting_domain),
                    getSnackbarContainer(),
                    length = Snackbar.LENGTH_INDEFINITE
                )
                deleteDomainSnackbar.show()

                viewLifecycleOwner.lifecycleScope.launch {
                    deleteDomainHttpRequest(id)
                }
            }
        ).show()
    }

    private suspend fun deleteDomainHttpRequest(id: String) {
        val result = domainsViewModel.deleteDomain(id)
        if (result is NetworkResult.Success && result.data == "204") {
            deleteDomainSnackbar.dismiss()
            getDataFromWeb(null, showShimmer = false)
        } else {
            val errorMsg = result.errorOrNull() ?: ""
            showError(errorMsg, R.string.error_deleting_domain)
        }
    }

    override fun onAdded() {
        addDomainFragment.dismissAllowingStateLoss()
        getDataFromWeb(null, showShimmer = false)
    }

    override suspend fun onRefreshData() {
        if (!isAdded) {
            return
        }
        try {
            isSilentRefresh = true
            kotlinx.coroutines.coroutineScope {
                val userJob = launch { getUserResource() }
                val domainsJob = domainsViewModel.loadDomains(forceRefresh = true)
                userJob.join()
                domainsJob.join()
            }
        } catch (e: Exception) {
            LoggingHelper(requireContext()).addLog(
                LOGIMPORTANCE.CRITICAL.int,
                "Failed to refresh data, view lifecycle not available. $e",
                "DomainsFragment",
                null
            )
        }
    }

    companion object {
        fun newInstance() = DomainsFragment()
    }
}
