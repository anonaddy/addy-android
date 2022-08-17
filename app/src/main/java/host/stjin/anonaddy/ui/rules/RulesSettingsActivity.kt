package host.stjin.anonaddy.ui.rules

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.RulesAdapter
import host.stjin.anonaddy.databinding.ActivityRuleSettingsBinding
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch


class RulesSettingsActivity : BaseActivity() {

    private var networkHelper: NetworkHelper? = null
    private var encryptedSettingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true

    private lateinit var binding: ActivityRuleSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRuleSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activityManageRulesLL1)
        )

        setupToolbar(
            R.string.manage_rules,
            binding.activityManageRulesNSV,
            binding.activityManageRulesToolbar,
            R.drawable.ic_clipboard_list
        )

        encryptedSettingsManager = SettingsManager(true, this)
        networkHelper = NetworkHelper(this)

        setOnClickListener()
        // Called on OnResume()
        // getDataFromWeb()
    }

    private fun setOnClickListener() {
        binding.activityManageRulesCreateRules.setOnClickListener {
            val intent = Intent(this, CreateRuleActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getDataFromWeb() {
        // Get the latest data in the background, and update the values when loaded
        lifecycleScope.launch {
            getAllRulesAndSetView()
        }
    }


    private lateinit var rulesAdapter: RulesAdapter
    private suspend fun getAllRulesAndSetView() {
        binding.activityManageRulesAllRulesRecyclerview.apply {
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false
                shimmerItemCount = encryptedSettingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_RULES_COUNT, 10) ?: 10

                // set a LinearLayoutManager to handle Android
                // RecyclerView behavior
                layoutManager = LinearLayoutManager(this@RulesSettingsActivity)
                // set the custom adapter to the RecyclerView
                addItemDecoration(MarginItemDecoration(this.resources.getDimensionPixelSize(R.dimen.recyclerview_margin)))
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                layoutAnimation = animation
                showShimmer()
            }
            networkHelper?.getAllRules({ list, error ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                // Check if there are new rules since the latest list
                // If the list is the same, just return and don't bother re-init the layoutmanager
                if (::rulesAdapter.isInitialized && list == rulesAdapter.getList()) {
                    return@getAllRules
                }

                if (list != null) {

                    if (list.size > 0) {
                        binding.activityManageRulesNoRules.visibility = View.GONE
                    } else {
                        binding.activityManageRulesNoRules.visibility = View.VISIBLE
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
                                        SnackbarHelper.createSnackbar(
                                            this@RulesSettingsActivity,
                                            this@RulesSettingsActivity.resources.getString(R.string.changing_rules_order_success),
                                            binding.activityManageRulesCL
                                        ).show()
                                    } else {
                                        SnackbarHelper.createSnackbar(
                                            this@RulesSettingsActivity,
                                            this@RulesSettingsActivity.resources.getString(R.string.error_changing_rules_order) + "\n" + result,
                                            binding.activityManageRulesCL,
                                            LoggingHelper.LOGFILES.DEFAULT
                                        ).show()
                                    }
                                }, list)
                            }
                        }

                        override fun startDragging(viewHolder: RecyclerView.ViewHolder?) {
                            viewHolder?.let { itemTouchHelper.startDrag(it) }
                        }

                    })
                    adapter = rulesAdapter
                    itemTouchHelper.attachToRecyclerView(binding.activityManageRulesAllRulesRecyclerview)

                    binding.animationFragment.stopAnimation()
                    //binding.activityManageRulesNSV.animate().alpha(1.0f)  -> Do not animate as there is a shimmerview
                } else {

                    SnackbarHelper.createSnackbar(
                        this@RulesSettingsActivity,
                        this@RulesSettingsActivity.resources.getString(R.string.error_obtaining_rules) + "\n" + error,
                        binding.activityManageRulesCL
                    ).show()

                    // Show error animations
                    binding.activityManageRulesLL1.visibility = View.GONE
                    binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
                }
                hideShimmer()
            }, show404Toast = true)

        }

    }

    private suspend fun deactivateRule(ruleId: String) {
        networkHelper?.deactivateSpecificRule({ result ->
            if (result == "204") {
                getDataFromWeb()
                SnackbarHelper.createSnackbar(
                    this,
                    this@RulesSettingsActivity.resources.getString(R.string.rule_deactivated),
                    binding.activityManageRulesCL
                ).show()
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    this@RulesSettingsActivity.resources.getString(R.string.error_rules_active) + "\n" + result,
                    binding.activityManageRulesCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, ruleId)
    }

    private suspend fun activateRule(ruleId: String) {
        networkHelper?.activateSpecificRule({ rule, error ->
            if (rule != null) {
                getDataFromWeb()
                SnackbarHelper.createSnackbar(
                    this,
                    this@RulesSettingsActivity.resources.getString(R.string.rule_activated),
                    binding.activityManageRulesCL
                ).show()
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    this@RulesSettingsActivity.resources.getString(R.string.error_rules_active) + "\n" + error,
                    binding.activityManageRulesCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, ruleId)
    }


    private lateinit var deleteRuleSnackbar: Snackbar
    private fun deleteRule(id: String, context: Context) {
        MaterialDialogHelper.showMaterialDialog(
            context = this,
            title = resources.getString(R.string.delete_rule),
            message = resources.getString(R.string.delete_rule_desc_confirm),
            icon = R.drawable.ic_trash,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.delete),
            positiveButtonAction = {
                deleteRuleSnackbar = SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.deleting_rule),
                    binding.activityManageRulesCL,
                    length = Snackbar.LENGTH_INDEFINITE
                )
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
                getDataFromWeb()
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_deleting_rule), result
                    ),
                    binding.activityManageRulesCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, id)
    }

    override fun onResume() {
        super.onResume()
        // Get the latest data in the background, and update the values when loaded
        lifecycleScope.launch {
            getDataFromWeb()
        }
    }

    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(UP or DOWN or START or END, 0) {

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




