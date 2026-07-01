package host.stjin.anonaddy.ui.labels

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.LabelsAdapter
import host.stjin.anonaddy.adapter.SearchAdapter
import host.stjin.anonaddy.databinding.FragmentManageLabelsBinding
import host.stjin.anonaddy.interfaces.Refreshable
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.utils.InsetUtil
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.Labels
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch

class ManageLabelsFragment : Fragment(), ManageLabelsAddBottomDialogFragment.AddLabelsBottomDialogListener, Refreshable {

    private var labelsEntries: ArrayList<Labels>? = null
    private var networkHelper: NetworkHelper? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var oneTimeRecyclerViewActions: Boolean = true

    private var manageLabelsAddBottomDialogFragment: ManageLabelsAddBottomDialogFragment? = null

    private var _binding: FragmentManageLabelsBinding? = null
    private val binding get() = _binding!!

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data?.getBooleanExtra("shouldRefresh", false) == true) {
                getDataFromWeb(null)
            }
        }
    }

    private lateinit var labelsAdapter: LabelsAdapter
    private lateinit var searchAdapter: SearchAdapter
    private var recentSearchesList: ArrayList<String> = arrayListOf()

    private lateinit var deleteLabelSnackbar: Snackbar


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageLabelsBinding.inflate(inflater, container, false)
        InsetUtil.applyBottomInset(binding.fragmentLabelsLL1)

        val root = binding.root

        encryptedSettingsManager = SettingsManager(true, requireContext())
        networkHelper = NetworkHelper(requireContext())

        setStats(0)
        setOnClickListener()
        setLabelsRecyclerView()
        setSearchRecyclerView()
        getDataFromWeb(savedInstanceState)

        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        val json = gson.toJson(labelsEntries)
        outState.putString("labelsEntries", json)
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
            manageLabelsAddBottomDialogFragment = ManageLabelsAddBottomDialogFragment.newInstance(null)
            manageLabelsAddBottomDialogFragment!!.show(
                childFragmentManager,
                "manageLabelsAddBottomDialogFragment"
            )
        }
    }


    private fun loadRecentSearches() {
        val settingsManager = SettingsManager(true, requireContext())
        val json = settingsManager.getSettingsString(SettingsManager.PREFS.RECENT_SEARCHES_LABELS)
        if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<ArrayList<String>>() {}.type
            recentSearchesList = Gson().fromJson(json, type) ?: arrayListOf()
        }
    }

    private fun updateRecentSearchesVisibility() {
        binding.labelsRecentSearchesLayout.visibility = if (recentSearchesList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun saveRecentSearch(search: String) {
        if (search.isEmpty()) return
        recentSearchesList.remove(search)
        recentSearchesList.add(0, search)
        // Keep max 25 recent searches
        if (recentSearchesList.size > 25) {
            recentSearchesList.removeAt(25)
        }
        val json = Gson().toJson(recentSearchesList)
        SettingsManager(false, requireContext()).putSettingsString(SettingsManager.PREFS.RECENT_SEARCHES_LABELS, json)
        searchAdapter.notifyDataSetChanged()
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
                val search = recentSearchesList[pos]
                binding.labelsSearchBar.setText(search)
                binding.labelsSearchView.setText(search)
                binding.labelsSearchView.hide()

                getDataFromWeb(null)
            }
        })

        binding.labelsClearRecentSearches.setOnClickListener {
            recentSearchesList.clear()
            SettingsManager(false, requireContext()).removeSetting(SettingsManager.PREFS.RECENT_SEARCHES_LABELS)
            searchAdapter.notifyDataSetChanged()
            updateRecentSearchesVisibility()
        }
    }


    private fun setLabelsRecyclerView() {
        binding.fragmentLabelsAllLabelsRecyclerview.apply {
            if (oneTimeRecyclerViewActions) {
                oneTimeRecyclerViewActions = false
                shimmerItemCount = 5
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
                text = total.toString()
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        binding.fragmentLabelsAllLabelsRecyclerview.apply {
            labelsEntries = list
            if (list.isNotEmpty()) {
                binding.fragmentLabelsNoLabels.visibility = View.GONE
            } else {
                binding.fragmentLabelsNoLabels.visibility = View.VISIBLE
            }

            labelsAdapter = LabelsAdapter(list)
            labelsAdapter.setClickListener(object : LabelsAdapter.ClickListener {
                override fun onClickDelete(pos: Int, aView: View, id: String) {
                    deleteLabel(id, context)
                }

                override fun onClickEdit(pos: Int, aView: View, label: Labels) {
                    manageLabelsAddBottomDialogFragment = ManageLabelsAddBottomDialogFragment.newInstance(label)
                    manageLabelsAddBottomDialogFragment!!.show(
                        childFragmentManager,
                        "manageLabelsAddBottomDialogFragment"
                    )
                }
            })
            adapter = labelsAdapter

            binding.animationFragment.stopAnimation()
            binding.fragmentLabelsNSV.animate().alpha(1.0f)
        }
    }

    /*
    TODO: Get from user resource?
     */
    private fun setStats(currentCount: Int) {
        binding.fragmentLabelsRLCountText.text = resources.getString(
            R.string.you_ve_used_d_out_of_d_labels,
            currentCount,
            "100"
        )
    }

    fun getDataFromWeb(savedInstanceState: Bundle?, callback: () -> Unit? = {}) {
        lifecycleScope.launch {
            if (savedInstanceState != null) {
                val labelsJson = savedInstanceState.getString("labelsEntries")
                if (!labelsJson.isNullOrEmpty() && labelsJson != "null") {
                    val gson = Gson()
                    val myType = object : TypeToken<ArrayList<Labels>>() {}.type
                    val list = gson.fromJson<ArrayList<Labels>>(labelsJson, myType)

                    setStats(list.size)
                    setLabelsAdapter(list)
                } else {
                    getAllLabelsAndSetView()
                }
            } else {
                getAllLabelsAndSetView()
            }
            callback()
        }
    }

    private suspend fun getAllLabelsAndSetView() {
        binding.fragmentLabelsAllLabelsRecyclerview.showShimmer()
        binding.fragmentLabelsAllLabelsRecyclerview.apply {
            val searchText = binding.labelsSearchBar.text.toString().trim()
            networkHelper?.getAllLabels(
                search = if (searchText.isEmpty()) null else searchText.lowercase(java.util.Locale.getDefault())
            ) { list, error ->
                if (list != null) {
                    setStats(list.size)
                    setLabelsAdapter(list)
                } else {
                    if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                        SnackbarHelper.createSnackbar(
                            requireContext(),
                            requireContext().resources.getString(R.string.something_went_wrong_retrieving_labels) + "\n" + error,
                            (activity as? MainActivity)?.findViewById(R.id.main_container) ?: requireView(),
                            LoggingHelper.LOGFILES.DEFAULT
                        ).show()
                    } else {
                        SnackbarHelper.createSnackbar(
                            requireContext(),
                            requireContext().resources.getString(R.string.something_went_wrong_retrieving_labels) + "\n" + error,
                            (activity as ManageLabelsActivity).findViewById(R.id.activity_manage_labels_CL),
                            LoggingHelper.LOGFILES.DEFAULT
                        ).show()
                    }

                    binding.fragmentLabelsLL1.visibility = View.GONE
                    binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
                }
            }
        }
    }

    override fun onAddedLabelEntry(label: Labels) {
        manageLabelsAddBottomDialogFragment?.dismissAllowingStateLoss()
        binding.animationFragment.playAnimation(true, R.drawable.ic_loading_logo)
        binding.fragmentLabelsNSV.alpha = 0.0f
        getDataFromWeb(null)
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
                val container = if (context.resources.getBoolean(R.bool.isTablet)) {
                    (activity as? MainActivity)?.findViewById(R.id.main_container) ?: requireView()
                } else {
                    (activity as ManageLabelsActivity).findViewById(R.id.activity_manage_labels_CL)
                }

                deleteLabelSnackbar = SnackbarHelper.createSnackbar(
                    requireContext(),
                    requireContext().resources.getString(R.string.deleting_label),
                    container,
                    length = Snackbar.LENGTH_INDEFINITE
                )
                deleteLabelSnackbar.show()

                lifecycleScope.launch {
                    deleteLabelHttpRequest(id, context)
                }
            }
        ).show()
    }

    private suspend fun deleteLabelHttpRequest(id: String, context: Context) {
        val container = if (context.resources.getBoolean(R.bool.isTablet)) {
            (activity as? MainActivity)?.findViewById(R.id.main_container) ?: requireView()
        } else {
            (activity as ManageLabelsActivity).findViewById(R.id.activity_manage_labels_CL)
        }


        networkHelper?.deleteLabel({ result ->
                if (result == null) {
                    deleteLabelSnackbar.dismiss()
                    getDataFromWeb(null)
                } else {
                    deleteLabelSnackbar.dismiss()
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        requireContext().resources.getString(R.string.error_deleting_label) + "\n" + result,
                        container,
                        LoggingHelper.LOGFILES.DEFAULT
                    ).show()
                }
            }, id)

    }

    override fun onRefreshData() {
        // The key is to check if the view is created before proceeding.
        // `viewLifecycleOwner` can be used as a proxy for this check.
        if (!isAdded) return

        // Use a try-catch as an ultimate safeguard against rare lifecycle race conditions.
        try {
            // This ensures the coroutine is launched only when the view's lifecycle is active.
            viewLifecycleOwner.lifecycleScope.launch {
                getDataFromWeb(null)
            }
        } catch (e: IllegalStateException) {
            // Log the error if the lifecycle state was somehow invalid despite the check.
            LoggingHelper(requireContext()).addLog(
                LOGIMPORTANCE.CRITICAL.int,
                "Failed to refresh data, view lifecycle not available. $e",
                "DomainSettingsFragment",
                null
            )
        }
    }
}