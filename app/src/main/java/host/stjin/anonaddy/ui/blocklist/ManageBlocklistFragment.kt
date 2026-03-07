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
import host.stjin.anonaddy_shared.models.AccountNotifications
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.adapter.BlocklistAdapter
import host.stjin.anonaddy.databinding.FragmentManageBlocklistBinding
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy_shared.models.BlocklistEntries
import host.stjin.anonaddy_shared.models.NewBlocklistEntry

class ManageBlocklistFragment : Fragment(), ManageBlocklistAddBottomDialogFragment.AddBlocklistBottomDialogListener {

    private var blocklistEntries: ArrayList<BlocklistEntries>? = null
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
        InsetUtil.applyBottomInset(binding.fragmentBlocklistCL)
        val root = binding.root

        encryptedSettingsManager = SettingsManager(true, requireContext())
        networkHelper = NetworkHelper(requireContext())


        setBlocklistRecyclerView()
        getDataFromWeb(savedInstanceState)
        setOnClickListeners()

        return root
    }


    private fun setOnClickListeners() {
        binding.aliasAddBlocklistEntryFab.setOnClickListener {
            manageBlocklistAddBottomDialogFragment = ManageBlocklistAddBottomDialogFragment()
            manageBlocklistAddBottomDialogFragment!!.show(
                childFragmentManager,
                "manageBlocklistAddBottomDialogFragment"
            )
        }

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


    fun getDataFromWeb(savedInstanceState: Bundle?, callback: () -> Unit? = {}) {
        // Get the latest data in the background, and update the values when loaded
        lifecycleScope.launch {
            if (savedInstanceState != null) {

                val blocklistEntries = savedInstanceState.getString("blocklistEntries")
                if (blocklistEntries!!.isNotEmpty() && blocklistEntries != "null") {
                    val gson = Gson()

                    val myType = object : TypeToken<ArrayList<AccountNotifications>>() {}.type
                    val list = gson.fromJson<ArrayList<BlocklistEntries>>(blocklistEntries, myType)
                    setBlocklistAdapter(list)
                } else {
                    // blocklistEntriesJson could be null when an embedded activity is opened instantly
                    getAllBlocklistEntriesAndSetRecyclerview()
                }

            } else {
                getAllBlocklistEntriesAndSetRecyclerview()
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
            }
        }
    }

    private lateinit var blocklistAdapter: BlocklistAdapter
    private suspend fun getAllBlocklistEntriesAndSetRecyclerview() {
        binding.fragmentBlocklistAllBlocklistRecyclerview.apply {
            networkHelper?.getAllBlocklistEntries { entries, error ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                // Check if there are new account notifications since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                if (::blocklistAdapter.isInitialized && entries == blocklistAdapter.getList()) {
                    return@getAllBlocklistEntries
                }

                if (entries != null) {
                    setBlocklistAdapter(entries)
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
                hideShimmer()
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


    private fun setBlocklistAdapter(list: ArrayList<BlocklistEntries>) {
        binding.fragmentBlocklistAllBlocklistRecyclerview.apply {
            blocklistEntries = list
            if (list.isNotEmpty()) {
                binding.fragmentBlocklistNoBlocklist.visibility = View.GONE
            } else {
                binding.fragmentBlocklistNoBlocklist.visibility = View.VISIBLE
            }


            blocklistAdapter = BlocklistAdapter(list)
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
            //binding.activityAccountNotificationsNSV.animate().alpha(1.0f) -> Do not animate as there is a shimmerview
        }
    }

    private lateinit var deleteBlocklistSnackbar: Snackbar

    private suspend fun deleteBlocklistEntryHttpRequest(id: String, context: Context) {
        networkHelper?.deleteBlocklistEntry({ result ->
            if (result == "204") {
                deleteBlocklistSnackbar.dismiss()
                getDataFromWeb(null)
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