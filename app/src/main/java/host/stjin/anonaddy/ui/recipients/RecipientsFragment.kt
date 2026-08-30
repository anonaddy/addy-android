package host.stjin.anonaddy.ui.recipients

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.adapter.RecipientAdapter
import host.stjin.anonaddy.databinding.FragmentRecipientsBinding
import host.stjin.anonaddy.interfaces.Refreshable
import host.stjin.anonaddy.ui.base.BaseFragment
import host.stjin.anonaddy.ui.base.SharedScrollViewModel
import host.stjin.anonaddy.ui.recipients.manage.ManageRecipientActivity
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class RecipientsFragment : BaseFragment(), AddRecipientBottomDialogFragment.AddRecipientBottomDialogListener, Refreshable {

    // 1. Properties
    private val recipientsViewModel: RecipientsViewModel by viewModels()
    private val sharedScrollViewModel: SharedScrollViewModel by activityViewModels()

    private var encryptedSettingsManager: SettingsManager? = null
    private var oneTimeRecyclerViewActions: Boolean = true

    private val addRecipientsFragment: AddRecipientBottomDialogFragment = AddRecipientBottomDialogFragment.newInstance()
    private var _binding: FragmentRecipientsBinding? = null
    private val binding get() = _binding!!

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data?.getBooleanExtra("shouldRefresh", false) == true) {
                getDataFromWeb(null, showShimmer = false)
            }
        }
    }

    private lateinit var recipientAdapter: RecipientAdapter
    private lateinit var deleteRecipientSnackbar: Snackbar

    private var isSilentRefresh = false

    // 2. Lifecycle Methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipientsBinding.inflate(inflater, container, false)

        val root = binding.root
        encryptedSettingsManager = ServiceLocator.encryptedSettingsManager

        setStats()
        setOnClickListener()
        setNsvListener()
        setRecipientRecyclerView()
        observeViewModel()

        getDataFromWeb(savedInstanceState)

        return root
    }

    override fun onResume() {
        super.onResume()
        setHasReachedTopOfNsv()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::deleteRecipientSnackbar.isInitialized && deleteRecipientSnackbar.isShown) {
            deleteRecipientSnackbar.dismiss()
        }
        _binding = null
    }

    // 3. View Setup
    private fun setOnClickListener() {
        binding.recipientsAddRecipients.setOnClickListener {
            if (!addRecipientsFragment.isAdded) {
                addRecipientsFragment.show(
                    childFragmentManager,
                    "addRecipientsFragment"
                )
            }
        }
    }

    private fun setNsvListener() {
        setupNsvScrollListener(binding.recipientsNSV)
    }

    private fun setRecipientRecyclerView() {
        recipientAdapter = RecipientAdapter()
        recipientAdapter.setClickListener(object : RecipientAdapter.ClickListener {
            override fun onClickSettings(pos: Int, view: View) {
                val currentList = recipientAdapter.currentList
                if (pos in currentList.indices) {
                    val intent = Intent(context, ManageRecipientActivity::class.java)
                    intent.putExtra("recipient_id", currentList[pos].id)
                    resultLauncher.launch(intent)
                }
            }

            override fun onClickResend(pos: Int, view: View) {
                val currentList = recipientAdapter.currentList
                if (pos in currentList.indices) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        resendConfirmationMailRecipient(currentList[pos].id)
                    }
                }
            }

            override fun onClickDelete(pos: Int, view: View) {
                val currentList = recipientAdapter.currentList
                if (pos in currentList.indices) {
                    deleteRecipient(currentList[pos].id, requireContext())
                }
            }
        })

        binding.recipientsAllRecipientsRecyclerview.apply {
            adapter = recipientAdapter
            if (oneTimeRecyclerViewActions) {
                oneTimeRecyclerViewActions = false

                shimmerItemCount = encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_RECIPIENT_COUNT, 2) ?: 2
                shimmerLayoutManager = GridLayoutManager(activity, ScreenSizeUtils.calculateNoOfColumns(context))
                layoutManager = GridLayoutManager(activity, ScreenSizeUtils.calculateNoOfColumns(context))
                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation

                showShimmer()

                binding.recipientsChipgroup.setOnCheckedStateChangeListener { _, checkedIds ->
                    if (checkedIds.isNotEmpty()) {
                        getDataFromWeb(null, showShimmer = false)
                    }
                }
            }
        }
    }

    private fun setRecipientAdapter(list: ArrayList<Recipients>) {
        binding.recipientsCount.apply {
            val total = list.size
            if (total > 0) {
                text = String.format(java.util.Locale.getDefault(), "%d", total)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        binding.recipientsAllRecipientsRecyclerview.apply {
            encryptedSettingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_RECIPIENT_COUNT, list.size)
            recipientAdapter.submitList(list.toList())
            hideShimmer()
        }
    }

    // 4. Observers
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    recipientsViewModel.recipientsState.collect { state ->
                        handleUiState(
                            state,
                            shimmer = if (!isSilentRefresh) binding.recipientsAllRecipientsRecyclerview else null,
                            progress = if (isSilentRefresh) binding.recipientsProgress else null,
                            titleProgress = if (isSilentRefresh) binding.recipientsTitleProgress else null,
                            errorStringRes = R.string.error_obtaining_recipients
                        ) { data ->
                            setRecipientAdapter(ArrayList(data))
                            setStats()
                        }
                    }
                }
                launch {
                    sharedScrollViewModel.scrollEvents.collect {
                        _binding?.recipientsNSV?.smoothScrollTo(0, 0)
                    }
                }
            }
        }
    }

    // 5. Private Helpers / Public Methods
    fun getDataFromWeb(savedInstanceState: Bundle?, showShimmer: Boolean = true) {
        isSilentRefresh = !showShimmer
        setStats()
        viewLifecycleOwner.lifecycleScope.launch { getUserResource() }
        recipientsViewModel.loadRecipients(forceRefresh = (savedInstanceState == null), verifiedOnly = getSelectedFilter())
    }

    private fun getSelectedFilter(): Boolean {
        return binding.recipientsChipgroup.checkedChipId == R.id.recipients_chip_verified_only
    }

    private fun setHasReachedTopOfNsv() {
        updateHasReachedTopOfNsv(binding.recipientsNSV)
    }

    private fun setStats() {
        val app = activity?.application as? AddyIoApp ?: return
        val userResource = app.userResourceOrNull ?: return
        binding.activityRecipientSettingsLLCount.text = requireContext().resources.getString(
            R.string.you_ve_used_d_out_of_d_recipients,
            userResource.recipient_count,
            if (userResource.subscription != null) userResource.recipient_limit else this.resources.getString(
                R.string.unlimited
            )
        )

        if (userResource.subscription != null) {
            binding.recipientsAddRecipients.isEnabled =
                userResource.recipient_count < (userResource.recipient_limit ?: Int.MAX_VALUE)
        } else {
            binding.recipientsAddRecipients.isEnabled = true
        }
    }

    private suspend fun getUserResource() {
        when (val result = recipientsViewModel.refreshUserResource()) {
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

    private suspend fun resendConfirmationMailRecipient(id: String) {
        val result = recipientsViewModel.resendVerificationEmail(id)
        if (result is NetworkResult.Success && result.data == "200") {
            verificationEmailSentSnackbar()
        } else {
            val error = result.errorOrNull() ?: ""
            showError(error, R.string.error_resend_verification, showLog = null)
        }
    }

    private fun verificationEmailSentSnackbar() {
        showError(null, R.string.verification_email_has_been_sent)
    }

    private fun deleteRecipient(id: String, context: Context) {
        MaterialDialogHelper.showMaterialDialog(
            context = context,
            title = resources.getString(R.string.delete_recipient),
            message = resources.getString(R.string.delete_recipient_desc),
            icon = R.drawable.ic_trash,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.delete),
            positiveButtonAction = {
                deleteRecipientSnackbar = SnackbarHelper.createSnackbar(
                    requireContext(),
                    this.resources.getString(R.string.deleting_recipient),
                    getSnackbarContainer(),
                    length = Snackbar.LENGTH_INDEFINITE
                )
                deleteRecipientSnackbar.show()

                viewLifecycleOwner.lifecycleScope.launch {
                    deleteRecipientHttpRequest(id)
                }
            }
        ).show()
    }

    private suspend fun deleteRecipientHttpRequest(id: String) {
        val result = recipientsViewModel.deleteRecipient(id)
        if (result is NetworkResult.Success && result.data == "204") {
            deleteRecipientSnackbar.dismiss()
            getDataFromWeb(null, showShimmer = false)
        } else {
            val error = result.errorOrNull() ?: ""
            showError(error, R.string.error_deleting_recipient)
        }
    }

    override fun onAdded() {
        addRecipientsFragment.dismissAllowingStateLoss()
        verificationEmailSentSnackbar()
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
                val recipientsJob = recipientsViewModel.loadRecipients(forceRefresh = true, verifiedOnly = getSelectedFilter())
                userJob.join()
                recipientsJob.join()
            }
        } catch (e: Exception) {
            LoggingHelper(requireContext()).addLog(
                LOGIMPORTANCE.CRITICAL.int,
                "Failed to refresh data, view lifecycle not available. $e",
                "RecipientsFragment",
                null
            )
        }
    }

    companion object {
        fun newInstance() = RecipientsFragment()
    }
}
