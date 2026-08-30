package host.stjin.anonaddy.ui.rules

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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.END
import androidx.recyclerview.widget.ItemTouchHelper.START
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy_shared.utils.GsonTools
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.adapter.RulesAdapter
import host.stjin.anonaddy.databinding.FragmentRuleSettingsBinding
import host.stjin.anonaddy.interfaces.Refreshable
import host.stjin.anonaddy.ui.base.BaseFragment
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy.ui.rules.manage.CreateRuleActivity
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.models.Rules
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class RulesFragment : BaseFragment(), Refreshable {

    // 1. Properties
    private val rulesViewModel: RulesViewModel by viewModels()

    private var recipients: ArrayList<Recipients>? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var oneTimeRecyclerViewActions: Boolean = true

    private var _binding: FragmentRuleSettingsBinding? = null
    private val binding get() = _binding!!

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data?.getBooleanExtra("shouldRefresh", false) == true) {
                getDataFromWeb(null, showShimmer = false)
            }
        }
    }

    private lateinit var rulesAdapter: RulesAdapter
    private lateinit var deleteRuleSnackbar: Snackbar

    private var isSilentRefresh = false
    private var hasOrderChanged = false

    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback = object : SimpleCallback(UP or DOWN or START or END, 0) {

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f

                if (hasOrderChanged) {
                    hasOrderChanged = false
                    val reorderedList = ArrayList(rulesAdapter.currentList)

                    viewLifecycleOwner.lifecycleScope.launch {
                        val result = rulesViewModel.reorderRules(reorderedList)
                        if (result is NetworkResult.Success && result.data == "200") {
                            showError(null, R.string.changing_rules_order_success, null, null)
                        } else {
                            val error = result.errorOrNull() ?: ""
                            showError(error, R.string.error_changing_rules_order)
                            getDataFromWeb(null, showShimmer = false)
                        }
                    }
                }
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from != RecyclerView.NO_POSITION && to != RecyclerView.NO_POSITION && from != to) {
                    rulesAdapter.onItemMove(from, to)
                    hasOrderChanged = true
                }
                return true
            }
        }
        ItemTouchHelper(simpleItemTouchCallback)
    }

    // 2. Lifecycle Methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRuleSettingsBinding.inflate(inflater, container, false)
        InsetUtils.applyBottomInset(binding.fragmentManageRulesLL1)

        val root = binding.root

        encryptedSettingsManager = ServiceLocator.encryptedSettingsManager

        setStats()
        setOnClickListener()
        setNsvListener()
        setRulesRecyclerView()
        observeViewModel()

        getDataFromWeb(savedInstanceState)

        return root
    }

    private fun setNsvListener() {
        setupNsvScrollListener(binding.fragmentManageRulesNSV)
    }

    private fun setHasReachedTopOfNsv() {
        updateHasReachedTopOfNsv(binding.fragmentManageRulesNSV)
    }

    override fun onResume() {
        super.onResume()
        setHasReachedTopOfNsv()
    }

    // 4. Observers
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                rulesViewModel.rulesState.collect { state ->
                    handleUiState(
                        state,
                        shimmer = if (!isSilentRefresh) binding.fragmentManageRulesAllRulesRecyclerview else null,
                        progress = if (isSilentRefresh) binding.rulesProgress else null,
                        titleProgress = if (isSilentRefresh) binding.rulesTitleProgress else null,
                        errorStringRes = R.string.error_obtaining_rules,
                        unavailableView = binding.fragmentContentUnavailable.root,
                        contentView = binding.fragmentManageRulesNSV
                    ) { data ->
                        val res = ArrayList(data.recipients)
                        recipients = res
                        val ruleList = ArrayList(data.rules)
                        setRulesAdapter(res, ruleList)
                        setStats()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::deleteRuleSnackbar.isInitialized && deleteRuleSnackbar.isShown) {
            deleteRuleSnackbar.dismiss()
        }
        _binding = null
    }

    // 3. View Setup
    private fun setOnClickListener() {
        binding.fragmentManageRulesCreateRules.setOnClickListener {
            val intent = Intent(requireContext(), CreateRuleActivity::class.java)
            intent.putExtra("recipients", GsonTools.gson.toJson(recipients))
            resultLauncher.launch(intent)
        }
    }

    private fun setRulesRecyclerView() {
        rulesAdapter = RulesAdapter(emptyList(), recipients, true)
        rulesAdapter.setClickListener(object : RulesAdapter.ClickListener {
            override fun onClickActivate(pos: Int, view: View) {
                val currentList = rulesAdapter.currentList
                if (pos in currentList.indices) {
                    if (currentList[pos].active) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            deactivateRule(currentList[pos].id)
                        }
                    } else {
                        viewLifecycleOwner.lifecycleScope.launch {
                            activateRule(currentList[pos].id)
                        }
                    }
                }
            }

            override fun onClickSettings(pos: Int, view: View) {
                val currentList = rulesAdapter.currentList
                if (pos in currentList.indices) {
                    val intent = Intent(context, CreateRuleActivity::class.java)
                    intent.putExtra("recipients", GsonTools.gson.toJson(recipients))
                    intent.putExtra("rule_id", currentList[pos].id)
                    resultLauncher.launch(intent)
                }
            }

            override fun onClickDelete(pos: Int, view: View) {
                val currentList = rulesAdapter.currentList
                if (pos in currentList.indices) {
                    deleteRule(currentList[pos].id)
                }
            }

            override fun startDragging(viewHolder: RecyclerView.ViewHolder?) {
                viewHolder?.let { itemTouchHelper.startDrag(it) }
            }
        })

        binding.fragmentManageRulesAllRulesRecyclerview.apply {
            adapter = rulesAdapter
            itemTouchHelper.attachToRecyclerView(this)
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

            if (oneTimeRecyclerViewActions) {
                oneTimeRecyclerViewActions = false
                shimmerItemCount = encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_RULES_COUNT, 10) ?: 10

                layoutManager = LinearLayoutManager(requireContext())
                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation
                showShimmer()
            }
        }
    }

    private fun setRulesAdapter(recipientsList: ArrayList<Recipients>, list: ArrayList<Rules>) {
        binding.ruleSettingsCount.apply {
            val total = list.size
            if (total > 0) {
                text = String.format(java.util.Locale.getDefault(), "%d", total)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        binding.fragmentManageRulesAllRulesRecyclerview.apply {
            recipients = recipientsList
            if (list.isNotEmpty()) {
                binding.fragmentManageRulesNoRules.visibility = View.GONE
            } else {
                binding.fragmentManageRulesNoRules.visibility = View.VISIBLE
            }

            encryptedSettingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_RULES_COUNT, list.size)
            rulesAdapter.updateRecipients(recipients)
            rulesAdapter.submitList(list)
            binding.animationFragment.stopAnimation()
        }
    }

    // 5. Private Helpers / Public Methods
    fun getDataFromWeb(savedInstanceState: Bundle?, showShimmer: Boolean = true) {
        isSilentRefresh = !showShimmer
        setStats()
        viewLifecycleOwner.lifecycleScope.launch { getUserResource() }
        rulesViewModel.loadRules(forceRefresh = (savedInstanceState == null))
    }

    override suspend fun onRefreshData() {
        if (!isAdded) {
            return
        }
        try {
            isSilentRefresh = true
            kotlinx.coroutines.coroutineScope {
                val userJob = launch { getUserResource() }
                val rulesJob = rulesViewModel.loadRules(forceRefresh = true)
                userJob.join()
                rulesJob.join()
            }
        } catch (e: Exception) {
            LoggingHelper(requireContext()).addLog(
                LOGIMPORTANCE.CRITICAL.int,
                "Failed to refresh data, view lifecycle not available. $e",
                "RulesFragment",
                null
            )
        }
    }

    private suspend fun getUserResource() {
        when (val result = rulesViewModel.refreshUserResource()) {
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
        val userResource = (activity?.application as? host.stjin.anonaddy_shared.AddyIoApp)?.userResourceOrNull ?: return
        binding.activityManageRulesSettingsLLCount.text = requireContext().resources.getString(
            R.string.you_ve_used_d_out_of_d_rules,
            userResource.active_rule_count,
            if (userResource.subscription != null) userResource.active_rule_limit else this.resources.getString(
                R.string.unlimited
            )
        )

        if (userResource.subscription != null) {
            binding.fragmentManageRulesCreateRules.isEnabled =
                userResource.active_rule_count < (userResource.active_rule_limit ?: Int.MAX_VALUE)
        } else {
            binding.fragmentManageRulesCreateRules.isEnabled = true
        }
    }

    private suspend fun deactivateRule(ruleId: String) {
        val result = rulesViewModel.deactivateRule(ruleId)
        if (result is NetworkResult.Success && result.data == "204") {
            getDataFromWeb(null, showShimmer = false)
            showError(null, R.string.rule_deactivated)
        } else {
            val error = result.errorOrNull() ?: ""
            showError(error, R.string.error_rules_active)
        }
    }

    private suspend fun activateRule(ruleId: String) {
        val result = rulesViewModel.activateRule(ruleId)
        if (result is NetworkResult.Success) {
            getDataFromWeb(null, showShimmer = false)
            showError(null, R.string.rule_activated)
        } else {
            val error = result.errorOrNull() ?: ""
            showError(error, R.string.error_rules_active)
        }
    }

    private fun deleteRule(id: String) {
        MaterialDialogHelper.showMaterialDialog(
            context = requireContext(),
            title = resources.getString(R.string.delete_rule),
            message = resources.getString(R.string.delete_rule_desc_confirm),
            icon = R.drawable.ic_trash,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.delete),
            positiveButtonAction = {
                deleteRuleSnackbar = SnackbarHelper.createSnackbar(
                    requireContext(),
                    this.resources.getString(R.string.deleting_rule),
                    getSnackbarContainer(),
                    length = Snackbar.LENGTH_INDEFINITE
                )
                deleteRuleSnackbar.show()
                viewLifecycleOwner.lifecycleScope.launch {
                    deleteRuleHttpRequest(id)
                }
            }
        ).show()
    }

    private suspend fun deleteRuleHttpRequest(id: String) {
        val result = rulesViewModel.deleteRule(id)
        if (result is NetworkResult.Success && result.data == "204") {
            deleteRuleSnackbar.dismiss()
            getDataFromWeb(null, showShimmer = false)
        } else {
            val error = result.errorOrNull() ?: ""
            showError(error, R.string.error_deleting_rule)
        }
    }

    companion object {
        fun newInstance() = RulesFragment()
    }
}
