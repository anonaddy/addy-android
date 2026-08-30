package host.stjin.anonaddy.ui.usernames

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
import host.stjin.anonaddy.adapter.UsernameAdapter
import host.stjin.anonaddy.databinding.FragmentUsernameSettingsBinding
import host.stjin.anonaddy.interfaces.Refreshable
import host.stjin.anonaddy.ui.base.BaseFragment
import host.stjin.anonaddy.ui.usernames.manage.ManageUsernameActivity
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.Usernames
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class UsernamesFragment : BaseFragment(), AddUsernameBottomDialogFragment.AddUsernameBottomDialogListener, Refreshable {

    // 1. Properties
    private val usernamesViewModel: UsernamesViewModel by viewModels()

    private var encryptedSettingsManager: SettingsManager? = null
    private var oneTimeRecyclerViewActions: Boolean = true

    private var addUsernameFragment: AddUsernameBottomDialogFragment = AddUsernameBottomDialogFragment.newInstance(0)
    private var _binding: FragmentUsernameSettingsBinding? = null
    private val binding get() = _binding!!

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data?.getBooleanExtra("shouldRefresh", false) == true) {
                getDataFromWeb(null, showShimmer = false)
            }
        }
    }

    private lateinit var usernamesAdapter: UsernameAdapter
    private lateinit var deleteUsernameSnackbar: Snackbar

    private var isSilentRefresh = false

    // 2. Lifecycle Methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsernameSettingsBinding.inflate(inflater, container, false)
        InsetUtils.applyBottomInset(binding.fragmentUsernameSettingsLL1)

        val root = binding.root

        encryptedSettingsManager = ServiceLocator.encryptedSettingsManager

        setStats()
        setOnClickListener()
        setNsvListener()
        setUsernamesRecyclerView()
        observeViewModel()
        getDataFromWeb(savedInstanceState)

        return root
    }

    private fun setNsvListener() {
        setupNsvScrollListener(binding.fragmentUsernameSettingsNSV)
    }

    private fun setHasReachedTopOfNsv() {
        updateHasReachedTopOfNsv(binding.fragmentUsernameSettingsNSV)
    }

    override fun onResume() {
        super.onResume()
        setHasReachedTopOfNsv()
    }

    // 4. Observers
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                usernamesViewModel.usernamesState.collect { state ->
                    handleUiState(
                        state,
                        shimmer = if (!isSilentRefresh) binding.fragmentUsernameSettingsAllUsernamesRecyclerview else null,
                        progress = if (isSilentRefresh) binding.usernamesProgress else null,
                        titleProgress = if (isSilentRefresh) binding.usernamesTitleProgress else null,
                        errorStringRes = R.string.error_obtaining_usernames
                    ) { data ->
                        setUsernamesAdapter(ArrayList(data))
                        setStats()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::deleteUsernameSnackbar.isInitialized && deleteUsernameSnackbar.isShown) {
            deleteUsernameSnackbar.dismiss()
        }
        _binding = null
    }

    // 3. View Setup
    private fun setOnClickListener() {
        binding.fragmentUsernameSettingsAddUsername.setOnClickListener {
            if (!addUsernameFragment.isAdded) {
                addUsernameFragment.show(
                    childFragmentManager,
                    "addUsernameFragment"
                )
            }
        }
    }

    private fun setUsernamesRecyclerView() {
        usernamesAdapter = UsernameAdapter()
        usernamesAdapter.setClickListener(object : UsernameAdapter.ClickListener {
            override fun onClickSettings(pos: Int, view: View) {
                val currentList = usernamesAdapter.currentList
                if (pos in currentList.indices) {
                    val intent = Intent(context, ManageUsernameActivity::class.java)
                    intent.putExtra("username_id", currentList[pos].id)
                    resultLauncher.launch(intent)
                }
            }

            override fun onClickDelete(pos: Int, view: View) {
                val currentList = usernamesAdapter.currentList
                if (pos in currentList.indices) {
                    deleteUsername(currentList[pos].id)
                }
            }
        })

        binding.fragmentUsernameSettingsAllUsernamesRecyclerview.apply {
            adapter = usernamesAdapter
            if (oneTimeRecyclerViewActions) {
                oneTimeRecyclerViewActions = false

                shimmerItemCount = encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_USERNAME_COUNT, 2) ?: 2
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

    private fun setUsernamesAdapter(list: ArrayList<Usernames>) {
        binding.usernameSettingsCount.apply {
            val total = list.size
            if (total > 0) {
                text = String.format(java.util.Locale.getDefault(), "%d", total)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        binding.fragmentUsernameSettingsAllUsernamesRecyclerview.apply {
            if (list.isNotEmpty()) {
                binding.fragmentUsernameSettingsNoUsernames.visibility = View.GONE
            } else {
                binding.fragmentUsernameSettingsNoUsernames.visibility = View.VISIBLE
            }

            encryptedSettingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_USERNAME_COUNT, list.size)
            usernamesAdapter.submitList(list.toList())
            binding.animationFragment.stopAnimation()
        }
    }

    // 5. Private Helpers / Public Methods
    fun getDataFromWeb(savedInstanceState: Bundle?, showShimmer: Boolean = true) {
        isSilentRefresh = !showShimmer
        setStats()
        viewLifecycleOwner.lifecycleScope.launch { getUserResource() }
        usernamesViewModel.loadUsernames(forceRefresh = (savedInstanceState == null))
    }

    private suspend fun getUserResource() {
        when (val result = usernamesViewModel.refreshUserResource()) {
            is NetworkResult.Success -> {
                if (!isAdded) return
                val user = result.data
                addUsernameFragment = AddUsernameBottomDialogFragment.newInstance(user.username_limit)
                setStats()
            }
            is NetworkResult.Error -> {
                if (!isAdded) return
                showError(result.error, R.string.error_obtaining_user)
            }
        }
    }

    private fun setStats() {
        val app = activity?.application as? AddyIoApp ?: return
        val userResource = app.userResourceOrNull ?: return
        binding.fragmentUsernameSettingsRLCountText.text =
            resources.getString(
                R.string.you_ve_used_d_out_of_d_usernames,
                userResource.username_count,
                userResource.username_limit
            )

        binding.fragmentUsernameSettingsAddUsername.isEnabled =
            userResource.username_count < userResource.username_limit
    }

    private fun deleteUsername(id: String) {
        MaterialDialogHelper.showMaterialDialog(
            context = requireContext(),
            title = resources.getString(R.string.delete_username),
            message = resources.getString(R.string.delete_username_desc_confirm),
            icon = R.drawable.ic_trash,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.delete),
            positiveButtonAction = {
                deleteUsernameSnackbar = SnackbarHelper.createSnackbar(
                    requireContext(),
                    this.resources.getString(R.string.deleting_username),
                    getSnackbarContainer(),
                    length = Snackbar.LENGTH_INDEFINITE
                )
                deleteUsernameSnackbar.show()

                viewLifecycleOwner.lifecycleScope.launch {
                    deleteUsernameHttpRequest(id)
                }
            }
        ).show()
    }

    private suspend fun deleteUsernameHttpRequest(id: String) {
        val result = usernamesViewModel.deleteUsername(id)
        if (result is NetworkResult.Success && result.data == "204") {
            deleteUsernameSnackbar.dismiss()
            getDataFromWeb(null, showShimmer = false)
        } else {
            val errorMsg = result.errorOrNull() ?: ""
            showError(errorMsg, R.string.error_deleting_username)
        }
    }

    override fun onAdded() {
        addUsernameFragment.dismissAllowingStateLoss()
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
                val usernamesJob = usernamesViewModel.loadUsernames(forceRefresh = true)
                userJob.join()
                usernamesJob.join()
            }
        } catch (e: Exception) {
            LoggingHelper(requireContext()).addLog(
                LOGIMPORTANCE.CRITICAL.int,
                "Failed to refresh data, view lifecycle not available. $e",
                "UsernamesFragment",
                null
            )
        }
    }

    companion object {
        fun newInstance() = UsernamesFragment()
    }
}
