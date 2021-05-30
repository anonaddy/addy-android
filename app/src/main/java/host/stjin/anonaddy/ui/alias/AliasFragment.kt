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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.AliasAdapter
import host.stjin.anonaddy.databinding.FragmentAliasBinding
import host.stjin.anonaddy.models.Aliases
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity
import host.stjin.anonaddy.utils.MarginItemDecoration
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class AliasFragment : Fragment(), AddAliasBottomDialogFragment.AddAliasBottomDialogListener {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var shouldAnimateRecyclerview: Boolean = true


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

        // Called on OnResume() as well, call this in onCreateView so the viewpager can serve loaded fragments
        getDataFromWeb()

        return root
    }


    private fun getDataFromWeb() {
        binding.aliasListLL1.visibility = View.VISIBLE
        binding.aliasStatisticsRLLottieview.visibility = View.GONE

        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllAliasesAndSetStatistics()
            // Set forceUpdate to false (if it was true) to prevent the lists from reloading every onresume
            forceUpdate = false
        }
    }

    // Update list of aliases when coming back
    override fun onResume() {
        super.onResume()
        getDataFromWeb()
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

        binding.aliasShowDeletedAliasToggle.setOnClickListener {
            toggleDeletedItems()
        }

        binding.aliasShowDeletedAliasToggleLL.setOnClickListener {
            toggleDeletedItems()
        }
    }

    private fun toggleDeletedItems() {
        if (binding.aliasDeletedAliasesRecyclerview.visibility == View.GONE) {
            binding.aliasDeletedAliasesRecyclerview.visibility = View.VISIBLE
            binding.aliasShowDeletedAliasToggle.setIconResource(R.drawable.ic_menu_up_outline)
        } else {
            binding.aliasDeletedAliasesRecyclerview.visibility = View.GONE
            binding.aliasShowDeletedAliasToggle.setIconResource(R.drawable.ic_menu_down_outline)
        }
    }

    private lateinit var aliasAdapter: AliasAdapter
    private var previousList: ArrayList<Aliases> = arrayListOf()
    private var hasSetItemDecoration = false
    private var hasSetItemDecorationDeleted = false
    private suspend fun getAllAliasesAndSetStatistics() {

        binding.aliasAllAliasesRecyclerview.apply {

            networkHelper?.getAliases({ list ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                // Check if there are new aliases since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                // Unless forceUpdate is true. If forceupdate is true, always update
                if (::aliasAdapter.isInitialized && list == previousList && !forceUpdate) {
                    return@getAliases
                }

                layoutManager = if (this.resources.getBoolean(R.bool.isTablet)) {
                    // set a GridLayoutManager for tablets
                    GridLayoutManager(activity, 2)
                } else {
                    LinearLayoutManager(activity)
                }

                if (!hasSetItemDecoration) {
                    addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))
                    hasSetItemDecoration = true
                }

                if (shouldAnimateRecyclerview) {
                    shouldAnimateRecyclerview = false
                    val resId: Int = R.anim.layout_animation_fall_down
                    val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                    layoutAnimation = animation
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


                    val nonDeletedAliases: ArrayList<Aliases> = arrayListOf()
                    val onlyDeletedList: ArrayList<Aliases> = arrayListOf()
                    for (alias in list) {
                        if (alias.deleted_at != null) {
                            onlyDeletedList.add(alias)
                        } else {
                            nonDeletedAliases.add(alias)
                        }
                    }

                    if (nonDeletedAliases.size > 0) {
                        binding.aliasNoAliases.visibility = View.GONE
                    } else {
                        binding.aliasNoAliases.visibility = View.VISIBLE
                    }

                    /**
                     * ALIAS LIST
                     */
                    aliasAdapter = AliasAdapter(nonDeletedAliases, context)
                    aliasAdapter.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                        override fun onClick(pos: Int) {
                            val intent = Intent(context, ManageAliasActivity::class.java)
                            // Pass data object in the bundle and populate details activity.
                            intent.putExtra("alias_id", nonDeletedAliases[pos].id)
                            resultLauncher.launch(intent)
                        }

                        override fun onClickCopy(pos: Int, aView: View) {
                            val clipboard: ClipboardManager =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val aliasEmailAddress = nonDeletedAliases[pos].email
                            val clip = ClipData.newPlainText("alias", aliasEmailAddress)
                            clipboard.setPrimaryClip(clip)

                            val bottomNavView: BottomNavigationView? =
                                activity?.findViewById(R.id.nav_view)
                            bottomNavView?.let {
                                Snackbar.make(
                                    it,
                                    context.resources.getString(R.string.copied_alias),
                                    Snackbar.LENGTH_SHORT
                                ).apply {
                                    anchorView = bottomNavView
                                }.show()
                            }
                        }

                    })
                    adapter = aliasAdapter
                    hideShimmerAdapter()
                    setAllDeletedAliases(onlyDeletedList)
                } else {
                    binding.aliasListLL1.visibility = View.GONE
                    binding.aliasStatisticsRLLottieview.visibility = View.VISIBLE
                }
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

    private lateinit var deletedAliasAdapter: AliasAdapter
    private fun setAllDeletedAliases(deletedAliased: ArrayList<Aliases>?) {
        binding.aliasDeletedAliasesRecyclerview.apply {

            // Check if there are new aliases since the latest list
            // If the list is the same, just return and don't bother re-init the layoutmanager
            // Unless forceUpdate is true. If forceupdate is true, always update
            if (::deletedAliasAdapter.isInitialized && deletedAliased == deletedAliasAdapter.getList() && !forceUpdate) {
                return
            }

            layoutManager = if (this.resources.getBoolean(R.bool.isTablet)) {
                // set a GridLayoutManager for tablets
                GridLayoutManager(activity, 2)
            } else {
                LinearLayoutManager(activity)
            }

            if (!hasSetItemDecorationDeleted) {
                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))
                hasSetItemDecorationDeleted = true
            }

            if (shouldAnimateRecyclerview) {
                shouldAnimateRecyclerview = false
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation
            }

            if (deletedAliased != null) {

                /**
                 * Seperate the deleted and non-deleted aliases
                 */


                if (deletedAliased.size > 0) {
                    binding.aliasNoDeletedAliases.visibility = View.GONE
                } else {
                    binding.aliasNoDeletedAliases.visibility = View.VISIBLE
                }


                /**
                 * ALIAS LIST
                 */
                deletedAliasAdapter = AliasAdapter(deletedAliased, context)
                deletedAliasAdapter.setClickOnAliasClickListener(object : AliasAdapter.ClickListener {
                    override fun onClick(pos: Int) {
                        val intent = Intent(context, ManageAliasActivity::class.java)
                        // Pass data object in the bundle and populate details activity.
                        intent.putExtra("alias_id", deletedAliased[pos].id)
                        resultLauncher.launch(intent)
                    }

                    override fun onClickCopy(pos: Int, aView: View) {
                        val clipboard: ClipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val aliasEmailAddress = deletedAliased[pos].email
                        val clip = ClipData.newPlainText("alias", aliasEmailAddress)
                        clipboard.setPrimaryClip(clip)

                        val bottomNavView: BottomNavigationView? =
                            activity?.findViewById(R.id.nav_view)
                        bottomNavView?.let {
                            Snackbar.make(
                                it,
                                context.resources.getString(R.string.copied_alias),
                                Snackbar.LENGTH_SHORT
                            ).apply {
                                anchorView = bottomNavView
                            }.show()
                        }
                    }

                })
                adapter = deletedAliasAdapter
                hideShimmerAdapter()
            } else {
                binding.aliasListLL1.visibility = View.GONE
                binding.aliasStatisticsRLLottieview.visibility = View.VISIBLE
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