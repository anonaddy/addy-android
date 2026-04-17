package host.stjin.anonaddy.ui.blocklist

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.utils.InsetUtil
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.ScreenSizeUtils
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.adapter.BlocklistAdapter
import host.stjin.anonaddy.databinding.FragmentManageBlocklistBinding
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy_shared.models.BlocklistEntries
import host.stjin.anonaddy_shared.models.BlocklistEntriesArray
import host.stjin.anonaddy_shared.models.NewBlocklistEntry

class ManageBlocklistFragment : Fragment(), ManageBlocklistAddBottomDialogFragment.AddBlocklistBottomDialogListener {

    private var blocklistEntries: BlocklistEntriesArray? = null
    private var networkHelper: NetworkHelper? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var oneTimeRecyclerViewActions: Boolean = true

    private var manageBlocklistAddBottomDialogFragment: ManageBlocklistAddBottomDialogFragment? = null


    companion object {
        fun newInstance() = ManageBlocklistFragment()
    }


    private var _binding: FragmentManageBlocklistBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageBlocklistBinding.inflate(inflater, container, false)
        InsetUtil.applyBottomInset(binding.fragmentBlocklistLL1)
        val root = binding.root

        encryptedSettingsManager = SettingsManager(true, requireContext())
        networkHelper = NetworkHelper(requireContext())


        setBlocklistRecyclerView()
        getDataFromWeb(savedInstanceState)
        setOnClickListeners()

        return root
    }


    private fun setOnClickListeners() {
        binding.fragmentBlocklistAddBlocklistEntryButton.setOnClickListener {
            manageBlocklistAddBottomDialogFragment = ManageBlocklistAddBottomDialogFragment()
            manageBlocklistAddBottomDialogFragment!!.show(
                childFragmentManager,
                "manageBlocklistAddBottomDialogFragment"
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        val json = gson.toJson(blocklistEntries)
        outState.putString("blocklistEntries", json)
    }


    private fun setOnNestedScrollViewListener(set: Boolean) {
        if (set) {
            binding.fragmentBlocklistNSV.setOnScrollChangeListener(androidx.core.widget.NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                val threshold = 10 // or some small number to account for rounding errors
                if (scrollY + v.measuredHeight + threshold >= v.getChildAt(0).measuredHeight) {
                    // Consider this as being at the bottom
                    viewLifecycleOwner.lifecycleScope.launch {
                        // Bottom of NSV reached. Time to load more data (if available)
                        getAllBlocklistEntriesAndSetRecyclerview()
                    }
                }
            })
        } else {
            binding.fragmentBlocklistNSV.setOnScrollChangeListener(null as androidx.core.widget.NestedScrollView.OnScrollChangeListener?)
        }
    }


    fun getDataFromWeb(savedInstanceState: Bundle?, callback: () -> Unit? = {}) {
        // Get the latest data in the background, and update the values when loaded
        lifecycleScope.launch {
            if (savedInstanceState != null) {

                val blocklistEntriesJson = savedInstanceState.getString("blocklistEntries")
                if (blocklistEntriesJson!!.isNotEmpty() && blocklistEntriesJson != "null") {
                    val gson = Gson()
                    val list = gson.fromJson(blocklistEntriesJson, BlocklistEntriesArray::class.java)
                    setBlocklistAdapter(list, true)
                } else {
                    // blocklistEntriesJson could be null when an embedded activity is opened instantly
                    getAllBlocklistEntriesAndSetRecyclerview(forceReload = true)
                }

            } else {
                getAllBlocklistEntriesAndSetRecyclerview(forceReload = true)
            }
            callback()
        }
    }


    private fun setBlocklistRecyclerView() {
        binding.fragmentBlocklistAllBlocklistRecyclerview.apply {
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
                        viewLifecycleOwner.lifecycleScope.launch {
                            getAllBlocklistEntriesAndSetRecyclerview(forceReload = true)
                        }
                    }
                }
            }
        }
    }

    private fun getSelectedFilter(): String? {
        return when (binding.fragmentBlocklistChipgroup.checkedChipId) {
            R.id.fragment_blocklist_chip_domain -> "domain"
            R.id.fragment_blocklist_chip_email -> "email"
            else -> null
        }
    }

    private lateinit var blocklistAdapter: BlocklistAdapter
    private suspend fun getAllBlocklistEntriesAndSetRecyclerview(forceReload: Boolean = false) {

        if (getSelectedFilter() == null){
            binding.fragmentBlocklistAllBlocklistTitle.text = getString(R.string.blocklist_entries)
        } else {
            binding.fragmentBlocklistAllBlocklistTitle.text = getString(R.string.blocklist_entries_filtered)
        }

        if (forceReload) {
            binding.fragmentBlocklistAllBlocklistRecyclerview.showShimmer()
            blocklistEntries = null
        }
        if (blocklistEntries == null || (blocklistEntries?.meta?.current_page ?: 0) < (blocklistEntries?.meta?.last_page ?: 0)) {
            binding.fragmentBlocklistProgress.visibility = View.VISIBLE
            setOnNestedScrollViewListener(false)
            binding.fragmentBlocklistAllBlocklistRecyclerview.apply {
                networkHelper?.getAllBlocklistEntries(
                    page = (blocklistEntries?.meta?.current_page ?: 0) + 1,
                    size = 25,
                    filter = getSelectedFilter()
                ) { entries, error ->
                    // Check if there are new account notifications since the latest list
                    // If the list is the same, just return and don't bother re-init the layoutmanager
                    if (::blocklistAdapter.isInitialized && entries?.data == blocklistAdapter.getList()) {
                        setOnNestedScrollViewListener(true)
                        hideShimmer()
                        binding.fragmentBlocklistProgress.visibility = View.GONE
                        return@getAllBlocklistEntries
                    }

                    if (entries != null) {
                        setBlocklistAdapter(entries, forceReload)
                    } else {
                        // If the error is 404, the feature is unavailable, let the user know that the feature is not available
                        if (error == "404") {
                            binding.fragmentBlocklistLL1.visibility = View.GONE
                            binding.root.findViewById<View>(R.id.fragment_content_unavailable).visibility = View.VISIBLE
                        } else {
                            if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                                SnackbarHelper.createSnackbar(
                                    requireContext(),
                                    requireContext().resources.getString(R.string.something_went_wrong_retrieving_blocklist_entries) + "\n" + error,
                                    (activity as MainActivity).findViewById(R.id.main_container),
                                    LoggingHelper.LOGFILES.DEFAULT
                                ).show()
                            } else {
                                SnackbarHelper.createSnackbar(
                                    requireContext(),
                                    requireContext().resources.getString(R.string.something_went_wrong_retrieving_blocklist_entries) + "\n" + error,
                                    (activity as ManageBlocklistActivity).findViewById(R.id.activity_manage_blocklist_CL),
                                    LoggingHelper.LOGFILES.DEFAULT
                                ).show()
                            }

                            // Show error animations
                            binding.fragmentBlocklistLL1.visibility = View.GONE
                            binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
                        }


                    }
                    binding.fragmentBlocklistProgress.visibility = View.GONE
                    hideShimmer()
                    setOnNestedScrollViewListener(true)
                }

            }

        }
    }

    private fun fragmentShown() {
        if (::blocklistAdapter.isInitialized) {
            // Set the count of account notifications so that the shimmerview looks better next time AND so that we can use it for the backgroundservice AND mark this a read for the badge
            encryptedSettingsManager?.putSettingsInt(
                SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_BLOCKLIST_ENTRIES_COUNT,
                blocklistAdapter.itemCount
            )
        }
    }


    private fun setBlocklistAdapter(list: BlocklistEntriesArray, forceReload: Boolean) {
        binding.fragmentBlocklistAllBlocklistRecyclerview.apply {
            if (blocklistEntries == null || forceReload) {
                // If blocklistEntries is empty, assign it
                blocklistEntries = list
            } else {
                // If blocklistEntries is not empty, set the meta and links and append the retrieved failedDeliveries to the list (as pagination is being used)
                blocklistEntries?.meta = list.meta
                blocklistEntries?.links = list.links
                blocklistEntries?.data?.addAll(list.data)

                // Get the totalsize of the adapteritems
                val totalSize = blocklistAdapter.itemCount
                // Tell the adapter there is new data (from the original size to the added items)
                binding.fragmentBlocklistAllBlocklistRecyclerview.post { blocklistAdapter.notifyItemRangeInserted(totalSize, list.data.size - 1) }
            }

            val data = blocklistEntries?.data ?: list.data

            if (data.isNotEmpty()) {
                binding.fragmentBlocklistNoBlocklist.visibility = View.GONE
            } else {
                binding.fragmentBlocklistNoBlocklist.visibility = View.VISIBLE
            }


            blocklistAdapter = BlocklistAdapter(data)
            blocklistAdapter.setClickListener(object : BlocklistAdapter.ClickListener {

                override fun onClickDelete(pos: Int, aView: View, id: String) {
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
                                binding.fragmentBlocklistCL,
                                length = Snackbar.LENGTH_INDEFINITE
                            )
                            deleteBlocklistSnackbar.show()
                            lifecycleScope.launch {
                                deleteBlocklistEntryHttpRequest(id, requireContext())
                            }
                        }
                    ).show()
                }

            })
            adapter = blocklistAdapter


            // Since this activity is always in foreground (no fragments in the MainActivity, always update the cache data
            fragmentShown()


            binding.animationFragment.stopAnimation()
        }
    }

    private lateinit var deleteBlocklistSnackbar: Snackbar

    private suspend fun deleteBlocklistEntryHttpRequest(id: String, context: Context) {
        networkHelper?.deleteBlocklistEntry({ result ->
            if (result == "204") {
                deleteBlocklistSnackbar.dismiss()
                
                val index = blocklistEntries?.data?.indexOfFirst { it.id == id } ?: -1
                if (index != -1) {
                    blocklistEntries?.data?.removeAt(index)
                    blocklistAdapter.notifyItemRemoved(index)
                    
                    if (blocklistEntries?.data?.isEmpty() == true) {
                        binding.fragmentBlocklistNoBlocklist.visibility = View.VISIBLE
                    }
                    fragmentShown()
                } else {
                    getDataFromWeb(null)
                }
            } else {

                if (context.resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        context.resources.getString(
                            R.string.s_s,
                            context.resources.getString(R.string.error_deleting_blocklist_entry), result
                        ),
                        (activity as MainActivity).findViewById(R.id.main_container),
                        LoggingHelper.LOGFILES.DEFAULT
                    ).show()
                } else {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        context.resources.getString(
                            R.string.s_s,
                            context.resources.getString(R.string.error_deleting_blocklist_entry), result
                        ),
                        (activity as ManageBlocklistActivity).findViewById(R.id.activity_manage_blocklist_CL),
                        LoggingHelper.LOGFILES.DEFAULT
                    ).show()
                }
            }
        }, id)
    }

    override fun onAddedBlocklistEntry(newBlocklistEntry: NewBlocklistEntry) {
        manageBlocklistAddBottomDialogFragment?.dismissAllowingStateLoss()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb(null)
    }
}