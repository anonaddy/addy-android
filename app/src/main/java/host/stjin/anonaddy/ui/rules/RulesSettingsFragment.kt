package host.stjin.anonaddy.ui.rules

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.END
import androidx.recyclerview.widget.ItemTouchHelper.START
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.RulesAdapter
import host.stjin.anonaddy.databinding.FragmentRuleSettingsBinding
import host.stjin.anonaddy.ui.MainActivity
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Rules
import kotlinx.coroutines.launch


class RulesSettingsFragment : Fragment() {

    private var rules: ArrayList<Rules>? = null
    private var networkHelper: NetworkHelper? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true

    companion object {
        fun newInstance() = RulesSettingsFragment()
    }

    private var _binding: FragmentRuleSettingsBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRuleSettingsBinding.inflate(inflater, container, false)
        val root = binding.root

        encryptedSettingsManager = SettingsManager(true, requireContext())
        networkHelper = NetworkHelper(requireContext())

        setOnClickListener()
        setRulesRecyclerView()
        getDataFromWeb(savedInstanceState)

        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        val json = gson.toJson(rules)
        outState.putString("rules", json)
    }


    private fun setOnClickListener() {
        binding.fragmentManageRulesCreateRules.setOnClickListener {
            val intent = Intent(requireContext(), CreateRuleActivity::class.java)
            startActivity(intent)
        }
    }

    fun getDataFromWeb(savedInstanceState: Bundle?) {
        // Get the latest data in the background, and update the values when loaded
        lifecycleScope.launch {
            if (savedInstanceState != null) {
                val rulesJson = savedInstanceState.getString("rules")
                if (!rulesJson.isNullOrEmpty() && rulesJson != "null") {
                    val gson = Gson()

                    val myType = object : TypeToken<ArrayList<Rules>>() {}.type
                    val list = gson.fromJson<ArrayList<Rules>>(rulesJson, myType)
                    setRulesAdapter(list)
                } else {
                    // rulesJson could be null when an embedded activity is opened instantly
                    getAllRulesAndSetView()
                }
            } else {
                getAllRulesAndSetView()
            }
        }
    }

    private fun setRulesRecyclerView() {
        binding.fragmentManageRulesAllRulesRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false
                shimmerItemCount = encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_RULES_COUNT, 10) ?: 10

                // set a LinearLayoutManager to handle Android
                // RecyclerView behavior
                layoutManager = LinearLayoutManager(requireContext())
                // set the custom adapter to the RecyclerView
                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation
                showShimmer()
            }
        }
    }


    private lateinit var rulesAdapter: RulesAdapter
    private suspend fun getAllRulesAndSetView() {
        binding.fragmentManageRulesAllRulesRecyclerview.apply {
            networkHelper?.getAllRules({ list, error ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                // Check if there are new rules since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                if (::rulesAdapter.isInitialized && list == rulesAdapter.getList()) {
                    return@getAllRules
                }

                if (list != null) {
                    setRulesAdapter(list)
                } else {
                    if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                        SnackbarHelper.createSnackbar(
                            requireContext(),
                            requireContext().resources.getString(R.string.error_obtaining_rules) + "\n" + error,
                            (activity as MainActivity).findViewById(R.id.main_container),
                        ).show()
                    } else {
                        SnackbarHelper.createSnackbar(
                            requireContext(),
                            requireContext().resources.getString(R.string.error_obtaining_rules) + "\n" + error,
                            (activity as RulesSettingsActivity).findViewById(R.id.activity_rules_settings_CL),
                        ).show()
                    }

                    // Show error animations
                    binding.fragmentManageRulesLL1.visibility = View.GONE
                    binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
                }
                hideShimmer()
            }, show404Toast = true)

        }

    }

    private fun setRulesAdapter(list: java.util.ArrayList<Rules>) {
        binding.fragmentManageRulesAllRulesRecyclerview.apply {
            rules = list
            if (list.size > 0) {
                binding.fragmentManageRulesNoRules.visibility = View.GONE
            } else {
                binding.fragmentManageRulesNoRules.visibility = View.VISIBLE
            }

            // Set the count of aliases so that the shimmerview looks better next time
            encryptedSettingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_RULES_COUNT, list.size)


            rulesAdapter = RulesAdapter(list, true)
            rulesAdapter.setClickListener(object : RulesAdapter.ClickListener {

                override fun onClickActivate(pos: Int, aView: View) {
                    if (list[pos].active) {
                        lifecycleScope.launch {
                            deactivateRule(list[pos].id)
                        }
                    } else {
                        lifecycleScope.launch {
                            activateRule(list[pos].id)
                        }
                    }
                }

                override fun onClickSettings(pos: Int, aView: View) {
                    val intent = Intent(context, CreateRuleActivity::class.java)
                    intent.putExtra("rule_id", list[pos].id)
                    startActivity(intent)
                }


                override fun onClickDelete(pos: Int, aView: View) {
                    deleteRule(list[pos].id, context)
                }

                override fun onItemMove(fromPosition: Int, toPosition: Int) {
                    val itemToMove = list[fromPosition]
                    list.removeAt(fromPosition)
                    list.add(toPosition, itemToMove)

                    lifecycleScope.launch {
                        networkHelper!!.reorderRules({ result ->
                            if (result == "200") {
                                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                                    SnackbarHelper.createSnackbar(
                                        requireContext(),
                                        requireContext().resources.getString(R.string.changing_rules_order_success),
                                        (activity as MainActivity).findViewById(R.id.main_container),
                                    ).show()
                                } else {
                                    SnackbarHelper.createSnackbar(
                                        requireContext(),
                                        requireContext().resources.getString(R.string.changing_rules_order_success),
                                        (activity as RulesSettingsActivity).findViewById(R.id.activity_rules_settings_CL),
                                    ).show()
                                }
                            } else {
                                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                                    SnackbarHelper.createSnackbar(
                                        requireContext(),
                                        requireContext().resources.getString(R.string.error_changing_rules_order) + "\n" + result,
                                        (activity as MainActivity).findViewById(R.id.main_container),
                                    ).show()
                                } else {
                                    SnackbarHelper.createSnackbar(
                                        requireContext(),
                                        requireContext().resources.getString(R.string.error_changing_rules_order) + "\n" + result,
                                        (activity as RulesSettingsActivity).findViewById(R.id.activity_rules_settings_CL),
                                    ).show()
                                }
                            }
                        }, list)
                    }
                }

                override fun startDragging(viewHolder: RecyclerView.ViewHolder?) {
                    viewHolder?.let { itemTouchHelper.startDrag(it) }
                }

            })
            adapter = rulesAdapter
            itemTouchHelper.attachToRecyclerView(binding.fragmentManageRulesAllRulesRecyclerview)

            binding.animationFragment.stopAnimation()
            //binding.activityManageRulesNSV.animate().alpha(1.0f)  -> Do not animate as there is a shimmerview

        }
    }

    private suspend fun deactivateRule(ruleId: String) {
        networkHelper?.deactivateSpecificRule({ result ->
            if (result == "204") {
                getDataFromWeb(null)

                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        requireContext().resources.getString(R.string.rule_deactivated),
                        (activity as MainActivity).findViewById(R.id.main_container),
                    ).show()
                } else {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        requireContext().resources.getString(R.string.rule_deactivated),
                        (activity as RulesSettingsActivity).findViewById(R.id.activity_rules_settings_CL),
                    ).show()
                }
            } else {
                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        requireContext().resources.getString(R.string.error_rules_active) + "\n" + result,
                        (activity as MainActivity).findViewById(R.id.main_container),
                    ).show()
                } else {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        requireContext().resources.getString(R.string.error_rules_active) + "\n" + result,
                        (activity as RulesSettingsActivity).findViewById(R.id.activity_rules_settings_CL),
                    ).show()
                }
            }
        }, ruleId)
    }

    private suspend fun activateRule(ruleId: String) {
        networkHelper?.activateSpecificRule({ rule, error ->
            if (rule != null) {
                getDataFromWeb(null)
                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        requireContext().resources.getString(R.string.rule_activated),
                        (activity as MainActivity).findViewById(R.id.main_container),
                    ).show()
                } else {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        requireContext().resources.getString(R.string.rule_activated),
                        (activity as RulesSettingsActivity).findViewById(R.id.activity_rules_settings_CL),
                    ).show()
                }
            } else {
                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        requireContext().resources.getString(R.string.error_rules_active) + "\n" + error,
                        (activity as MainActivity).findViewById(R.id.main_container),
                    ).show()
                } else {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        requireContext().resources.getString(R.string.error_rules_active) + "\n" + error,
                        (activity as RulesSettingsActivity).findViewById(R.id.activity_rules_settings_CL),
                    ).show()
                }
            }
        }, ruleId)
    }


    private lateinit var deleteRuleSnackbar: Snackbar
    private fun deleteRule(id: String, context: Context) {
        MaterialDialogHelper.showMaterialDialog(
            context = requireContext(),
            title = resources.getString(R.string.delete_rule),
            message = resources.getString(R.string.delete_rule_desc_confirm),
            icon = R.drawable.ic_trash,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.delete),
            positiveButtonAction = {
                deleteRuleSnackbar = if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        this.resources.getString(R.string.deleting_rule),
                        (activity as MainActivity).findViewById(R.id.main_container),
                        length = Snackbar.LENGTH_INDEFINITE
                    )
                } else {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        this.resources.getString(R.string.deleting_rule),
                        (activity as MainActivity).findViewById(R.id.activity_domain_settings_CL),
                        length = Snackbar.LENGTH_INDEFINITE
                    )
                }

                deleteRuleSnackbar.show()
                lifecycleScope.launch {
                    deleteRuleHttpRequest(id, context)
                }
            }
        ).show()
    }

    private suspend fun deleteRuleHttpRequest(id: String, context: Context) {
        networkHelper?.deleteRule({ result ->
            if (result == "204") {
                deleteRuleSnackbar.dismiss()
                getDataFromWeb(null)
            } else {
                if (requireContext().resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        context.resources.getString(
                            R.string.s_s,
                            context.resources.getString(R.string.error_deleting_rule), result
                        ),
                        (activity as MainActivity).findViewById(R.id.main_container),
                    ).show()
                } else {
                    SnackbarHelper.createSnackbar(
                        requireContext(),
                        context.resources.getString(
                            R.string.s_s,
                            context.resources.getString(R.string.error_deleting_rule), result
                        ),
                        (activity as RulesSettingsActivity).findViewById(R.id.activity_rules_settings_CL),
                    ).show()
                }
            }
        }, id)
    }


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
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val adapter = recyclerView.adapter as RulesAdapter
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                adapter.moveItem(from, to)
                adapter.notifyItemMoved(from, to)

                return true
            }

        }

        ItemTouchHelper(simpleItemTouchCallback)
    }

}




