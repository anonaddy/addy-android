package host.stjin.anonaddy.ui.labels
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
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.adapter.LabelsAdapter
import host.stjin.anonaddy.adapter.SearchAdapter
import host.stjin.anonaddy.databinding.FragmentManageLabelsBinding
import host.stjin.anonaddy.interfaces.Refreshable
import host.stjin.anonaddy.ui.base.BaseFragment
import host.stjin.anonaddy.utils.InsetUtils
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.Labels
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class LabelsFragment : BaseFragment(), AddLabelBottomDialogFragment.AddLabelsBottomDialogListener, Refreshable {

    // 1. Properties
    private val labelsViewModel: LabelsViewModel by viewModels()

    private var encryptedSettingsManager: SettingsManager? = null
    private var oneTimeRecyclerViewActions: Boolean = true

    private var labelsAddBottomDialogFragment: AddLabelBottomDialogFragment? = null

    private var _binding: FragmentManageLabelsBinding? = null
    private val binding get() = _binding!!

    private lateinit var labelsAdapter: LabelsAdapter
    private lateinit var searchAdapter: SearchAdapter
    private var recentSearchesList: ArrayList<String> = arrayListOf()

    private lateinit var deleteLabelSnackbar: Snackbar

    private var isSilentRefresh = false

    // 2. Lifecycle Methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageLabelsBinding.inflate(inflater, container, false)
        InsetUtils.applyBottomInset(binding.fragmentLabelsLL1)

        val root = binding.root

        encryptedSettingsManager = ServiceLocator.encryptedSettingsManager

        setStats(0)
        setOnClickListener()
        setLabelsRecyclerView()
        setSearchRecyclerView()
        observeViewModel()
        getDataFromWeb(savedInstanceState)

        return root
    }

    // 4. Observers
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                labelsViewModel.labelsState.collect { state ->
                    handleUiState(
                        state,
                        shimmer = if (!isSilentRefresh) binding.fragmentLabelsAllLabelsRecyclerview else null,
                        progress = if (isSilentRefresh) binding.labelsProgress else null,
                        titleProgress = if (isSilentRefresh) binding.labelsTitleProgress else null,
                        errorStringRes = R.string.something_went_wrong_retrieving_labels
                    ) { data ->
                        setStats(data.size)
                        setLabelsAdapter(ArrayList(data))
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::deleteLabelSnackbar.isInitialized && deleteLabelSnackbar.isShown) {
            deleteLabelSnackbar.dismiss()
        }
        _binding = null
    }

    private fun setOnClickListener() {
        binding.labelsSearchView.editText.addTextChangedListener { text ->
            val searchText = text?.toString()?.trim()
            if (searchText.isNullOrEmpty()) {
                binding.labelsSearchBar.setText(null)
                getDataFromWeb(null)
            }
        }

        binding.labelsSearchView.addTransitionListener { _, _, newState ->
            if (newState == com.google.android.material.search.SearchView.TransitionState.HIDDEN) {
                val searchText = binding.labelsSearchView.text.toString().trim()
                if (searchText.isEmpty()) {
                    binding.labelsSearchBar.setText(null)
                    getDataFromWeb(null)
                }
            }
        }

        binding.labelsSearchView.editText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event?.action == android.view.KeyEvent.ACTION_DOWN &&
                        event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)
            ) {
                val inputMethodManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.labelsSearchView.windowToken, 0)

                binding.labelsSearchBar.setText(binding.labelsSearchView.text)
                binding.labelsSearchView.hide()

                val searchText = binding.labelsSearchView.text.toString().trim()
                saveRecentSearch(searchText)
                getDataFromWeb(null)
                true
            } else {
                false
            }
        }

        binding.fragmentLabelsAddLabelButton.setOnClickListener {
            labelsAddBottomDialogFragment = AddLabelBottomDialogFragment.newInstance(null)
            labelsAddBottomDialogFragment!!.show(
                childFragmentManager,
                "labelsAddBottomDialogFragment"
            )
        }
    }

    private fun loadRecentSearches() {
        val json = encryptedSettingsManager?.getSettingsString(SettingsManager.PREFS.RECENT_SEARCHES_LABELS)
        if (json != null) {
            val type = object : TypeToken<ArrayList<String>>() {}.type
            recentSearchesList = GsonTools.gson.fromJson(json, type) ?: arrayListOf()
        }
    }

    private fun updateRecentSearchesVisibility() {
        binding.labelsRecentSearchesLayout.visibility = if (recentSearchesList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun saveRecentSearch(search: String) {
        if (search.isEmpty()) return
        recentSearchesList.remove(search)
        recentSearchesList.add(0, search)
        if (recentSearchesList.size > 25) {
            recentSearchesList.removeAt(25)
        }
        val json = GsonTools.gson.toJson(recentSearchesList)
        encryptedSettingsManager?.putSettingsString(SettingsManager.PREFS.RECENT_SEARCHES_LABELS, json)
        searchAdapter.submitList(recentSearchesList.toList())
        updateRecentSearchesVisibility()
    }

    private fun setSearchRecyclerView() {
        binding.labelsSearchView.setupWithSearchBar(binding.labelsSearchBar)
        loadRecentSearches()
        updateRecentSearchesVisibility()
        searchAdapter = SearchAdapter(recentSearchesList)
        binding.labelsSearchViewRecyclerview.adapter = searchAdapter

        searchAdapter.setClickListener(object : SearchAdapter.ClickListener {
            override fun onClickSearchResult(pos: Int, aView: View) {
                val currentList = recentSearchesList
                if (pos in currentList.indices) {
                    val search = currentList[pos]
                    binding.labelsSearchBar.setText(search)
                    binding.labelsSearchView.setText(search)
                    binding.labelsSearchView.hide()

                    getDataFromWeb(null)
                }
            }
        })

        binding.labelsClearRecentSearches.setOnClickListener {
            recentSearchesList.clear()
            encryptedSettingsManager?.removeSetting(SettingsManager.PREFS.RECENT_SEARCHES_LABELS)
            searchAdapter.submitList(emptyList())
            updateRecentSearchesVisibility()
        }
    }

    // 3. View Setup
    private fun setLabelsRecyclerView() {
        labelsAdapter = LabelsAdapter()
        labelsAdapter.setClickListener(object : LabelsAdapter.ClickListener {
            override fun onClickDelete(pos: Int, view: View, id: String) {
                deleteLabel(id, requireContext())
            }

            override fun onClickEdit(pos: Int, view: View, label: Labels) {
                labelsAddBottomDialogFragment = AddLabelBottomDialogFragment.newInstance(label)
                labelsAddBottomDialogFragment!!.show(
                    childFragmentManager,
                    "labelsAddBottomDialogFragment"
                )
            }
        })

        binding.fragmentLabelsAllLabelsRecyclerview.apply {
            adapter = labelsAdapter
            if (oneTimeRecyclerViewActions) {
                oneTimeRecyclerViewActions = false

                shimmerItemCount = 2
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

    private fun setLabelsAdapter(list: ArrayList<Labels>) {
        binding.labelsCount.apply {
            val total = list.size
            if (total > 0) {
                text = String.format(java.util.Locale.getDefault(), "%d", total)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        binding.fragmentLabelsAllLabelsRecyclerview.apply {
            if (list.isNotEmpty()) {
                binding.fragmentLabelsNoLabels.visibility = View.GONE
            } else {
                binding.fragmentLabelsNoLabels.visibility = View.VISIBLE
            }

            labelsAdapter.submitList(list.toList())

            binding.animationFragment.stopAnimation()
        }
    }

    private fun setStats(currentCount: Int) {
        binding.fragmentLabelsRLCountText.text = resources.getString(
            R.string.you_ve_used_d_out_of_d_labels,
            currentCount,
            "100"
        )
    }

    // 5. Private Helpers / Public Methods
    fun getDataFromWeb(savedInstanceState: Bundle?, showShimmer: Boolean = true): kotlinx.coroutines.Job {
        isSilentRefresh = !showShimmer
        val searchText = binding.labelsSearchBar.text.toString().trim()
        return labelsViewModel.loadLabels(
            search = if (searchText.isEmpty()) null else searchText.lowercase(java.util.Locale.getDefault()),
            forceRefresh = (savedInstanceState == null)
        )
    }

    override fun onAddedLabelEntry(label: Labels) {
        labelsAddBottomDialogFragment?.dismissAllowingStateLoss()
        getDataFromWeb(null, showShimmer = false)
    }

    private fun deleteLabel(id: String, context: Context) {
        MaterialDialogHelper.showMaterialDialog(
            context = requireContext(),
            title = requireContext().resources.getString(R.string.delete_label),
            message = requireContext().resources.getString(R.string.delete_label_desc),
            icon = R.drawable.ic_trash,
            neutralButtonText = requireContext().resources.getString(R.string.cancel),
            positiveButtonText = requireContext().resources.getString(R.string.delete),
            positiveButtonAction = {
                deleteLabelSnackbar = SnackbarHelper.createSnackbar(
                    requireContext(),
                    requireContext().resources.getString(R.string.deleting_label),
                    getSnackbarContainer(),
                    length = Snackbar.LENGTH_INDEFINITE
                )
                deleteLabelSnackbar.show()

                viewLifecycleOwner.lifecycleScope.launch {
                    deleteLabelHttpRequest(id)
                }
            }
        ).show()
    }

    private suspend fun deleteLabelHttpRequest(id: String) {
        val result = labelsViewModel.deleteLabel(id)
        if (result is NetworkResult.Success) {
            deleteLabelSnackbar.dismiss()
            getDataFromWeb(null, showShimmer = false)
        } else {
            deleteLabelSnackbar.dismiss()
            showError(result.errorOrNull(), R.string.error_deleting_label)
        }
    }

    override suspend fun onRefreshData() {
        if (!isAdded) {
            return
        }
        try {
            isSilentRefresh = true
            getDataFromWeb(null, showShimmer = false).join()
        } catch (e: Exception) {
            LoggingHelper(requireContext()).addLog(
                LOGIMPORTANCE.CRITICAL.int,
                "Failed to refresh data, view lifecycle not available. $e",
                "LabelsFragment",
                null
            )
        }
    }

    companion object {
        fun newInstance() = LabelsFragment()
    }
}
