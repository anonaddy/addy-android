package host.stjin.anonaddy.ui.rules

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.RulesAdapter
import host.stjin.anonaddy.databinding.ActivityRuleSettingsBinding
import host.stjin.anonaddy.databinding.AnonaddyCustomDialogBinding
import host.stjin.anonaddy.utils.LoggingHelper
import host.stjin.anonaddy.utils.MarginItemDecoration
import host.stjin.anonaddy.utils.SnackbarHelper
import kotlinx.coroutines.launch


class RulesSettingsActivity : BaseActivity() {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var OneTimeRecyclerViewActions: Boolean = true

    private lateinit var binding: ActivityRuleSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRuleSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(view, binding.activityManageRulesNSVRL)

        setupToolbar(binding.activityManageRulesToolbar.customToolbarOneHandedMaterialtoolbar, R.string.manage_rules)

        settingsManager = SettingsManager(true, this)
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
        binding.activityManageRulesLL1.visibility = View.VISIBLE
        binding.activityManageRulesRLLottieview.visibility = View.GONE

        // Get the latest data in the background, and update the values when loaded
        lifecycleScope.launch {
            getAllRulesAndSetView()
        }
    }


    private lateinit var rulesAdapter: RulesAdapter
    private suspend fun getAllRulesAndSetView() {
        binding.activityManageRulesAllRulesRecyclerview.apply {
            val test = paddingStart
            val test2 = paddingEnd
            if (OneTimeRecyclerViewActions) {
                OneTimeRecyclerViewActions = false
                shimmerItemCount = settingsManager?.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_RULES_COUNT, 10) ?: 10

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
            networkHelper?.getAllRules({ list ->
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
                    settingsManager?.putSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_RULES_COUNT, list.size)


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
                } else {
                    binding.activityManageRulesLL1.visibility = View.GONE
                    binding.activityManageRulesRLLottieview.visibility = View.VISIBLE
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
        networkHelper?.activateSpecificRule({ result ->
            if (result == "200") {
                getDataFromWeb()
                SnackbarHelper.createSnackbar(
                    this,
                    this@RulesSettingsActivity.resources.getString(R.string.rule_activated),
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


    lateinit var dialog: AlertDialog
    private fun deleteRule(id: String, context: Context) {
        val anonaddyCustomDialogBinding = AnonaddyCustomDialogBinding.inflate(LayoutInflater.from(this), null, false)

        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(anonaddyCustomDialogBinding.root)
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        anonaddyCustomDialogBinding.dialogTitle.text = context.resources.getString(R.string.delete_rule)
        anonaddyCustomDialogBinding.dialogText.text = context.resources.getString(R.string.delete_rule_desc_confirm)
        anonaddyCustomDialogBinding.dialogPositiveButton.text =
            context.resources.getString(R.string.delete)
        anonaddyCustomDialogBinding.dialogPositiveButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.softRed)
        anonaddyCustomDialogBinding.dialogPositiveButton.setTextColor(ContextCompat.getColor(this, R.color.AnonAddyCustomDialogSoftRedTextColor))

        anonaddyCustomDialogBinding.dialogPositiveButton.setOnClickListener {
            // Animate the button to progress
            anonaddyCustomDialogBinding.dialogPositiveButton.startAnimation()

            anonaddyCustomDialogBinding.dialogError.visibility = View.GONE
            anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = false
            anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = false

            lifecycleScope.launch {
                deleteRuleHttpRequest(id, context, anonaddyCustomDialogBinding)
            }
        }
        anonaddyCustomDialogBinding.dialogNegativeButton.setOnClickListener {
            dialog.dismiss()
        }
        // create and show the alert dialog
        dialog.show()
    }

    private suspend fun deleteRuleHttpRequest(id: String, context: Context, anonaddyCustomDialogBinding: AnonaddyCustomDialogBinding) {
        networkHelper?.deleteRule({ result ->
            if (result == "204") {
                dialog.dismiss()
                getDataFromWeb()
            } else {
                // Revert the button to normal
                anonaddyCustomDialogBinding.dialogPositiveButton.revertAnimation()

                anonaddyCustomDialogBinding.dialogError.visibility = View.VISIBLE
                anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogError.text = context.resources.getString(
                    R.string.s_s,
                    context.resources.getString(R.string.error_deleting_domain), result
                )
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




