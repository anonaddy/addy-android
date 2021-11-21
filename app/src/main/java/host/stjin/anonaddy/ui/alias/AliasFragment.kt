package host.stjin.anonaddy.ui.alias

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.databinding.FragmentAliasBinding
import host.stjin.anonaddy.models.Aliases
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
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


        setAliasDropDown()
        setOnClickListeners()

        // Called on OnResume() as well, call this in onCreateView so the viewpager can serve loaded fragments
        getDataFromWeb()

        return root
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
            forceUpdate = true
            getDataFromWeb()
        }
    }


    private fun getDataFromWeb() {
        binding.aliasListLL1.visibility = View.VISIBLE
        binding.aliasStatisticsRLLottieview.visibility = View.GONE

        // Get the latest data in the background, and update the values when loaded
        viewLifecycleOwner.lifecycleScope.launch {
            getAllAliasesAndSetStatistics()
            // Set forceUpdate to false (if it was true) to prevent the lists from reloading every onresume
            forceUpdate = false
        }
    }


    // Decided to not load aliases when coming back to hold back on performance issues


    override fun onResume() {
        super.onResume()

        // There is a bug where the dropdown does not get populated after refreshing the view (eg. switching dark/light mode)
        setAliasDropDown()
    }

    private fun setOnClickListeners() {

        binding.aliasAddAlias.setOnClickListener {
            if (!addAliasBottomDialogFragment.isAdded) {
                addAliasBottomDialogFragment.show(
                    childFragmentManager,
                    "addAliasBottomDialogFragment"
                )
            }
        }
    }


    private lateinit var aliasAdapter: AliasAdapter
    private var previousList: ArrayList<Aliases> = arrayListOf()
    private suspend fun getAllAliasesAndSetStatistics() {
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

            showShimmer()

            networkHelper?.getAliases({ list ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                // Check if there are new aliases since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                // Unless forceUpdate is true. If forceupdate is true, always update
                if (::aliasAdapter.isInitialized && list == previousList && !forceUpdate) {
                    hideShimmer()
                    return@getAliases
                }

                /**
                 * Count the totals for the aliases statistics
                 * Done here because otherwise would need to get the aliases twice from the web
                 */

                if (list != null) {
                    previousList = list

                    var forwarded = 0
                    var blocked = 0
                    var replied = 0
                    var sent = 0

                    for (alias in list) {
                        forwarded += alias.emails_forwarded
                        blocked += alias.emails_blocked
                        replied += alias.emails_replied
                        sent += alias.emails_sent
                    }

                    // Set the actual statistics
                    setAliasesStatistics(context, forwarded, blocked, replied, sent)


                    if (list.size > 0) {
                        binding.aliasNoAliases.visibility = View.GONE
                    } else {
                        binding.aliasNoAliases.visibility = View.VISIBLE
                    }


                    // Set the count of aliases so that the shimmerview looks better next time
                    settingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_ALIAS_COUNT, list.size)

                    /**
                     * ALIAS LIST
                     */


                    // Here the alias list is being modified to only have the items included that are in the MACT filter
                    when (binding.aliasAliasDropdownMact.text.toString()) {
                        this.resources.getString(R.string.all_aliases) -> {
                            // Do nothing as the received list already contains everything
                        }
                        this.resources.getString(R.string.active_aliases) -> {
                            // Filter out all the inactive aliases
                            list.removeAll { alias -> !alias.active }
                        }
                        this.resources.getString(R.string.inactive_aliases) -> {
                            // Filter out all the active aliases
                            list.removeAll { alias -> alias.active }
                        }
                        this.resources.getString(R.string.deleted_aliases) -> {
                            // Filter out all the non-deleted aliases
                            list.removeAll { alias -> alias.deleted_at == null }
                        }
                        this.resources.getString(R.string.watched_aliases) -> {
                            // Filter out all the non-watched aliases
                            val aliasesToWatch = AliasWatcher(context).getAliasesToWatch()
                            if (aliasesToWatch != null) {
                                list.removeAll { alias -> alias.id !in aliasesToWatch }
                            }
                        }
                    }

                    aliasAdapter = AliasAdapter(list, context)
                    aliasAdapter.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                        override fun onClick(pos: Int) {
                            val intent = Intent(context, ManageAliasActivity::class.java)
                            // Pass data object in the bundle and populate details activity.
                            intent.putExtra("alias_id", list[pos].id)
                            resultLauncher.launch(intent)
                        }

                        override fun onClickCopy(pos: Int, aView: View) {
                            val clipboard: ClipboardManager =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val aliasEmailAddress = list[pos].email
                            val clip = ClipData.newPlainText("alias", aliasEmailAddress)
                            clipboard.setPrimaryClip(clip)

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
                } else {
                    binding.aliasListLL1.visibility = View.GONE
                    binding.aliasStatisticsRLLottieview.visibility = View.VISIBLE
                }
                hideShimmer()
            }, activeOnly = false, includeDeleted = true)
        }

    }

    // This value is there to force updating the alias recyclerview in case "Watch alias" has been enabled.
    private var forceUpdate = false

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            if (data != null) {
                if (data.getBooleanExtra("should_update", false)) {
                    forceUpdate = true
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