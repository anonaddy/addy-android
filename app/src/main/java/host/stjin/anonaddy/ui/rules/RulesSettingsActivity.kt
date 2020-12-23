package host.stjin.anonaddy.ui.rules

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.adapter.RulesAdapter
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import kotlinx.android.synthetic.main.activity_rule_settings.*
import kotlinx.android.synthetic.main.anonaddy_custom_dialog.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class RulesSettingsActivity : BaseActivity() {

    private var networkHelper: NetworkHelper? = null
    private var settingsManager: SettingsManager? = null
    private var shouldAnimateRecyclerview: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_settings)
        setupToolbar(activity_manage_rules_toolbar)

        settingsManager = SettingsManager(true, this)
        networkHelper = NetworkHelper(this)

        setOnClickListener()
        // Called on OnResume()
        // getDataFromWeb()
    }

    private fun setOnClickListener() {
        activity_manage_rules_create_rules.setOnClickListener {
            val intent = Intent(this, CreateRuleActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getDataFromWeb() {
        activity_manage_rules_LL1.visibility = View.VISIBLE
        activity_manage_rules_RL_lottieview.visibility = View.GONE

        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAllRules()
        }
    }


    private suspend fun getAllRules() {
        activity_manage_rules_all_rules_recyclerview.apply {
            if (itemDecorationCount > 0) {
                addItemDecoration(
                    DividerItemDecoration(
                        this.context,
                        (layoutManager as LinearLayoutManager).orientation
                    )
                )
            }
            // set a LinearLayoutManager to handle Android
            // RecyclerView behavior
            layoutManager = LinearLayoutManager(context)
            // set the custom adapter to the RecyclerView

            if (shouldAnimateRecyclerview) {
                shouldAnimateRecyclerview = false
                val resId: Int = R.anim.layout_animation_fall_down
                val animation = AnimationUtils.loadLayoutAnimation(context, resId)
                activity_manage_rules_all_rules_recyclerview.layoutAnimation = animation
            }


            networkHelper?.getAllRules { list ->
                // Sorted by created_at automatically
                //list?.sortByDescending { it.emails_forwarded }

                if (list != null) {

                    if (list.size > 0) {
                        activity_manage_rules_no_rules.visibility = View.GONE
                    } else {
                        activity_manage_rules_no_rules.visibility = View.VISIBLE
                    }

                    val rulesAdapter = RulesAdapter(list, true)
                    rulesAdapter.setClickListener(object : RulesAdapter.ClickListener {

                        override fun onClickActivate(pos: Int, aView: View) {
                            if (list[pos].active) {
                                GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                                    deactivateRule(list[pos].id)
                                }
                            } else {
                                GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
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

                            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                                networkHelper!!.reorderRules({ result ->
                                    if (result == "200") {
                                        val snackbar = Snackbar.make(
                                            activity_manage_rules_LL,
                                            this@RulesSettingsActivity.resources.getString(R.string.changing_rules_order_success),
                                            Snackbar.LENGTH_SHORT
                                        )
                                        snackbar.show()
                                    } else {
                                        val snackbar = Snackbar.make(
                                            activity_manage_rules_LL,
                                            this@RulesSettingsActivity.resources.getString(R.string.error_changing_rules_order) + "\n" + result,
                                            Snackbar.LENGTH_SHORT
                                        )
                                        if (SettingsManager(false, this@RulesSettingsActivity).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
                                            snackbar.setAction(R.string.logs) {
                                                val intent = Intent(this@RulesSettingsActivity, LogViewerActivity::class.java)
                                                startActivity(intent)
                                            }
                                        }
                                        snackbar.show()
                                    }
                                }, list)
                            }
                        }

                        override fun startDragging(viewHolder: RecyclerView.ViewHolder?) {
                            viewHolder?.let { itemTouchHelper.startDrag(it) }
                        }

                    })
                    adapter = rulesAdapter
                    activity_manage_rules_all_rules_recyclerview.hideShimmerAdapter()

                    itemTouchHelper.attachToRecyclerView(activity_manage_rules_all_rules_recyclerview)
                } else {
                    activity_manage_rules_LL1.visibility = View.GONE
                    activity_manage_rules_RL_lottieview.visibility = View.VISIBLE
                }
            }

        }

    }

    private suspend fun deactivateRule(ruleId: String) {
        networkHelper?.deactivateSpecificRule({ result ->
            if (result == "204") {
                getDataFromWeb()

                val snackbar = Snackbar.make(
                    activity_manage_rules_LL,
                    this@RulesSettingsActivity.resources.getString(R.string.rule_deactivated),
                    Snackbar.LENGTH_SHORT
                )
                snackbar.show()
            } else {
                val snackbar = Snackbar.make(
                    activity_manage_rules_LL,
                    this@RulesSettingsActivity.resources.getString(R.string.error_rules_active) + "\n" + result,
                    Snackbar.LENGTH_SHORT
                )
                if (SettingsManager(false, this@RulesSettingsActivity).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
                    snackbar.setAction(R.string.logs) {
                        val intent = Intent(this@RulesSettingsActivity, LogViewerActivity::class.java)
                        startActivity(intent)
                    }
                }
                snackbar.show()
            }
        }, ruleId)
    }

    private suspend fun activateRule(ruleId: String) {
        networkHelper?.activateSpecificRule({ result ->
            if (result == "200") {
                getDataFromWeb()

                val snackbar = Snackbar.make(
                    activity_manage_rules_LL,
                    this@RulesSettingsActivity.resources.getString(R.string.rule_activated),
                    Snackbar.LENGTH_SHORT
                )
                snackbar.show()
            } else {
                val snackbar = Snackbar.make(
                    activity_manage_rules_LL,
                    this@RulesSettingsActivity.resources.getString(R.string.error_rules_active) + "\n" + result,
                    Snackbar.LENGTH_SHORT
                )
                if (SettingsManager(false, this@RulesSettingsActivity).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
                    snackbar.setAction(R.string.logs) {
                        val intent = Intent(this@RulesSettingsActivity, LogViewerActivity::class.java)
                        startActivity(intent)
                    }
                }
                snackbar.show()
            }
        }, ruleId)
    }


    lateinit var dialog: AlertDialog
    private lateinit var customLayout: View
    private fun deleteRule(id: String, context: Context) {
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        // set the custom layout
        customLayout =
            layoutInflater.inflate(R.layout.anonaddy_custom_dialog, null)
        builder.setView(customLayout)
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        customLayout.dialog_title.text = context.resources.getString(R.string.delete_rule)
        customLayout.dialog_text.text = context.resources.getString(R.string.delete_rule_desc_confirm)
        customLayout.dialog_positive_button.text =
            context.resources.getString(R.string.delete_rule)
        customLayout.dialog_positive_button.setOnClickListener {
            customLayout.dialog_progressbar.visibility = View.VISIBLE
            customLayout.dialog_error.visibility = View.GONE
            customLayout.dialog_negative_button.isEnabled = false
            customLayout.dialog_positive_button.isEnabled = false

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                deleteRuleHttpRequest(id, context)
            }
        }
        customLayout.dialog_negative_button.setOnClickListener {
            dialog.dismiss()
        }
        // create and show the alert dialog
        dialog.show()
    }

    private suspend fun deleteRuleHttpRequest(id: String, context: Context) {
        networkHelper?.deleteRule({ result ->
            if (result == "204") {
                dialog.dismiss()
                getDataFromWeb()
            } else {
                customLayout.dialog_progressbar.visibility = View.INVISIBLE
                customLayout.dialog_error.visibility = View.VISIBLE
                customLayout.dialog_negative_button.isEnabled = true
                customLayout.dialog_positive_button.isEnabled = true
                customLayout.dialog_error.text = context.resources.getString(
                    R.string.s_s,
                    context.resources.getString(R.string.error_deleting_domain), result
                )
            }
        }, id)
    }

    override fun onResume() {
        super.onResume()
        // Get the latest data in the background, and update the values when loaded
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
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




