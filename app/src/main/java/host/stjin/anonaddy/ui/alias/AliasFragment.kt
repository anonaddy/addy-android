package host.stjin.anonaddy.ui.alias

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.ScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import host.stjin.anonaddy.AnonAddyForAndroid
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.databinding.FragmentAliasBinding
import host.stjin.anonaddy.models.Aliases
import host.stjin.anonaddy.models.AliasesArray
import host.stjin.anonaddy.models.UserResource
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.utils.LoggingHelper
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.SnackbarHelper
import kotlinx.coroutines.launch


class AliasFragment : Fragment(), AddAliasBottomDialogFragment.AddAliasBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true


    companion object {
        fun newInstance() = AliasFragment()
    }

    private val addAliasBottomDialogFragment: AddAliasBottomDialogFragment =
        AddAliasBottomDialogFragment.newInstance()

    private var _binding: FragmentAliasBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAliasBinding.inflate(inflater, container, false)
        val root = binding.root

        settingsManager = SettingsManager(true, requireContext())
        networkHelper = NetworkHelper(requireContext())


        setOnClickListeners()
        setOnNestedScrollViewListener()
        getDataFromWeb()

        return root
    }

    private fun setOnNestedScrollViewListener() {
        binding.fragmentAliasNsv.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            if (-scrollY == v.measuredHeight - v.getChildAt(0).measuredHeight) {
                viewLifecycleOwner.lifecycleScope.launch {
                    // Bottom of NSV reached. Time to load more data (if available)
                    getAliasesAndAddThemToList()
                }
            }
        })
    }

    private val mScrollUpBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            binding.fragmentAliasNsv.post { binding.fragmentAliasNsv.fullScroll(ScrollView.FOCUS_UP) }
        }
    }


    override fun onPause() {
        super.onPause()
        activity?.unregisterReceiver(mScrollUpBroadcastReceiver)
    }

    private fun setAliasDropDown() {
        val items = listOf(
            this.resources.getString(R.string.all_aliases),
            this.resources.getString(R.string.active_aliases),
            this.resources.getString(R.string.inactive_aliases),
            this.resources.getString(R.string.deleted_aliases),
            this.resources.getString(R.string.watched_aliases)
        )
        val adapter = ArrayAdapter(binding.aliasAliasDropdownMact.context, R.layout.dropdown_menu_popup_item, items)
        binding.aliasAliasDropdownMact.setAdapter(adapter)

        binding.aliasAliasDropdownMact.setOnItemClickListener { _, _, _, _ ->
            setNetworkFilter()
            getDataFromWeb()
        }
    }

    private fun setNetworkFilter() {
        /**
         * ALIAS FILTERING
         */

        // Here the filter settings are being modified to only *retrieve* the items included that are in the MACT filter to
        // minimize the amount of data to return as much as possible
        when (binding.aliasAliasDropdownMact.text.toString()) {
            this.resources.getString(R.string.all_aliases) -> {
                activeOnly = false
                includeDeleted = true
                // Do nothing as the received list already contains everything
            }
            this.resources.getString(R.string.active_aliases) -> {
                // Filter out all the inactive aliases
                activeOnly = true
                includeDeleted = false
            }
            this.resources.getString(R.string.inactive_aliases) -> {
                // Filter out all the active aliases
                activeOnly = false
                includeDeleted = true
            }
            this.resources.getString(R.string.deleted_aliases) -> {
                // Filter out all the non-deleted aliases
                activeOnly = false
                includeDeleted = true
            }
            this.resources.getString(R.string.watched_aliases) -> {
                activeOnly = false
                includeDeleted = true
            }
        }

    }


    private fun getDataFromWeb() {
        binding.aliasListLL1.visibility = View.VISIBLE
        binding.aliasStatisticsRLLottieview.visibility = View.GONE
        binding.aliasNoAliases.visibility = View.GONE

        // Get the latest data in the background, and update the values when loaded
        viewLifecycleOwner.lifecycleScope.launch {
            getUserResource(requireContext())
            binding.aliasAllAliasesRecyclerview.showShimmer()
            getAliasesAndAddThemToList(forceReload = true)
        }


    }

    private suspend fun getUserResource(context: Context) {
        networkHelper?.getUserResource { user: UserResource?, result: String? ->
            if (user != null) {
                (activity?.application as AnonAddyForAndroid).userResource = user
                setAliasesStatistics(
                    context,
                    user.total_emails_forwarded,
                    user.total_emails_blocked,
                    user.total_emails_replied,
                    user.total_emails_sent
                )
            } else {
                val bottomNavView: BottomNavigationView? =
                    activity?.findViewById(R.id.nav_view)
                bottomNavView?.let {
                    SnackbarHelper.createSnackbar(
                        context,
                        context.resources.getString(R.string.error_obtaining_user) + "\n" + result,
                        it,
                        LoggingHelper.LOGFILES.DEFAULT
                    )
                        .apply {
                            anchorView = bottomNavView
                        }.show()
                }

            }
        }
    }


    // Decided to not load aliases when coming back to hold back on performance issues
    override fun onResume() {
        super.onResume()
        activity?.registerReceiver(mScrollUpBroadcastReceiver, IntentFilter("scroll_up"))

        // There is a bug where the dropdown does not get populated after refreshing the view (eg. switching dark/light mode)
        setAliasDropDown()
    }

    private fun setOnClickListeners() {
        binding.aliasAddAliasFab.setOnClickListener {
            if (!addAliasBottomDialogFragment.isAdded) {
                addAliasBottomDialogFragment.show(
                    childFragmentManager,
                    "addAliasBottomDialogFragment"
                )
            }
        }

        binding.aliasAddAlias.setOnClickListener {
            if (!addAliasBottomDialogFragment.isAdded) {
                addAliasBottomDialogFragment.show(
                    childFragmentManager,
                    "addAliasBottomDialogFragment"
                )
            }
        }
    }


    private suspend fun getAliasesAndAddThemToList(forceReload: Boolean = false) {

        if (forceReload) {
            aliasList = null
        }
        // Only obtain data and do a network call whenever there is actually more information on the API side to obtain
        // If alias == null
        // OR
        // If the page we're currently on is LOWER than the last page
        if (aliasList == null || aliasList?.meta?.current_page ?: 0 < aliasList?.meta?.last_page ?: 0) {
            binding.aliasProgress.visibility = View.VISIBLE
            networkHelper?.getAliases({ list ->
                if (list != null) {
                    if (aliasList == null || forceReload) {
                        // If aliasList is empty, assign it
                        aliasList = list

                        // Make sure to have the aliases in the array be filtered, aliases are being filtered on API level
                        // But some filters can only be applied locally
                        aliasList!!.data = getFilteredAliasList(list.data)

                        // If the list is empty, set noAliasVisibility to visible
                        if (list.data.size == 0) {
                            // Set to GONE in getDataFromWeb
                            binding.aliasNoAliases.visibility = View.VISIBLE
                        }
                    } else {
                        // If aliasList is not empty, set the meta and links and append the retrieved aliases to the list (as pagination is being used)
                        aliasList?.meta = list.meta
                        aliasList?.links = list.links
                        val filteredAliases = getFilteredAliasList(list.data)
                        aliasList?.data?.addAll(filteredAliases)


                        // Get the totalsize of the adapteritems
                        val totalSize = aliasAdapter?.itemCount ?: 0
                        // Tell the adapter there is new data (from the original size to the added items)
                        binding.aliasAllAliasesRecyclerview.post { aliasAdapter?.notifyItemRangeInserted(totalSize, filteredAliases.size - 1) }
                    }

                    // Set the count of aliases so that the shimmerview looks better next time (always 10 as of v3.3.0)
                    settingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_ALIAS_COUNT, list.data.size)


                    // Okay we got data, init the recyclerview
                    // If we do a force reload the recyclerview also needs to be re-init to show shimmerview as well as clear the adapter
                    initRecyclerview(forceReload)

                } else {
                    // Data could not be loaded
                    //TODO show toast instead?
                    binding.aliasListLL1.visibility = View.GONE
                    binding.aliasStatisticsRLLottieview.visibility = View.VISIBLE
                }

                binding.aliasAllAliasesRecyclerview.hideShimmer()
                binding.aliasProgress.visibility = View.GONE
            }, activeOnly, includeDeleted, page = (aliasList?.meta?.current_page ?: 0) + 1)
        }
    }

    private var aliasAdapter: AliasAdapter? = null
    private var aliasList: AliasesArray? = null
    private var activeOnly = false
    private var includeDeleted = true
    private fun initRecyclerview(forceReload: Boolean) {
        binding.aliasAllAliasesRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false
                shimmerItemCount = settingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_ALIAS_COUNT, 10) ?: 10
                shimmerLayoutManager = if (this.resources.getBoolean(R.bool.isTablet)) {
                    // set a GridLayoutManager for tablets
                    GridLayoutManager(activity, 2)
                } else {
                    LinearLayoutManager(activity)
                }

                layoutManager = if (this.resources.getBoolean(R.bool.isTablet)) {
                    // set a GridLayoutManager for tablets
                    GridLayoutManager(activity, 2)
                } else {
                    LinearLayoutManager(activity)
                }

                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))

                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation
            }

            if (forceReload) {
                aliasAdapter = AliasAdapter(aliasList!!.data, context)
                aliasAdapter!!.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                    override fun onClick(pos: Int) {
                        val intent = Intent(context, ManageAliasActivity::class.java)
                        // Pass data object in the bundle and populate details activity.
                        intent.putExtra("alias_id", aliasList!!.data[pos].id)
                        resultLauncher.launch(intent)
                    }

                    override fun onClickCopy(pos: Int, aView: View) {
                        val clipboard: ClipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val aliasEmailAddress = aliasList!!.data[pos].email
                        val clip = ClipData.newPlainText("alias", aliasEmailAddress)
                        clipboard.setPrimaryClip(clip)


                        hideFabForSnackBarTime()
                        val bottomNavView: BottomNavigationView? =
                            activity?.findViewById(R.id.nav_view)
                        bottomNavView?.let {
                            SnackbarHelper.createSnackbar(context, context.resources.getString(R.string.copied_alias), it).apply {
                                anchorView = bottomNavView
                            }.show()
                        }
                    }

                })
                adapter = aliasAdapter
            }
        }

    }

    private fun getFilteredAliasList(aliasesList: ArrayList<Aliases>): ArrayList<Aliases> {
        // Here the alias list is being modified to only have the items included that are in the MACT filter
        when (binding.aliasAliasDropdownMact.text.toString()) {
            this.resources.getString(R.string.all_aliases) -> {
                activeOnly = false
                includeDeleted = true
                // This filter is already being done on the networkHelper side
            }
            this.resources.getString(R.string.active_aliases) -> {
                // Filter out all the inactive aliases
                // This filter is already being done on the networkHelper side
            }
            this.resources.getString(R.string.inactive_aliases) -> {
                // Filter out all the active aliases
                aliasesList.removeAll { alias -> alias.active }
            }
            this.resources.getString(R.string.deleted_aliases) -> {
                // Filter out all the non-deleted aliases
                aliasesList.removeAll { alias -> alias.deleted_at == null }
            }
            this.resources.getString(R.string.watched_aliases) -> {
                activeOnly = false
                includeDeleted = true
                // Filter out all the non-watched aliases
                val aliasesToWatch = context?.let { AliasWatcher(it).getAliasesToWatch() }
                if (aliasesToWatch != null) {
                    aliasesList.removeAll { alias -> alias.id !in aliasesToWatch }
                }
            }
        }

        return aliasesList
    }

    private fun hideFabForSnackBarTime() {
        binding.aliasAddAliasFab.hide()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.aliasAddAliasFab.show()
        }, 3500)
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            if (data != null) {
                if (data.getBooleanExtra("should_update", false)) {
                    getDataFromWeb()
                }
            }
        }
    }

    private fun setAliasesStatistics(
        context: Context,
        forwarded: Int,
        blocked: Int,
        replied: Int,
        sent: Int
    ) {
        binding.aliasRepliedSentStatsTextview.text =
            context.resources.getString(R.string.replied_replied_sent_stat, replied, sent)
        binding.aliasForwardedBlockedStatsTextview.text =
            context.resources.getString(
                R.string.replied_forwarded_blocked_stat,
                forwarded,
                blocked
            )
    }

    override fun onAdded() {
        addAliasBottomDialogFragment.dismiss()
        // Get the latest data in the background, and update the values when loaded
        getDataFromWeb()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}