package host.stjin.anonaddy.ui.alias.manage

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.utils.DateTimeUtils
import kotlinx.android.synthetic.main.activity_manage_alias.*
import kotlinx.android.synthetic.main.anonaddy_custom_dialog.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import kotlin.math.roundToInt


class ManageAliasActivity : BaseActivity(),
    EditAliasDescriptionBottomDialogFragment.AddEditAliasDescriptionBottomDialogListener,
    EditAliasRecipientsBottomDialogFragment.AddEditAliasRecipientsBottomDialogListener {

    lateinit var networkHelper: NetworkHelper

    private lateinit var editAliasDescriptionBottomDialogFragment: EditAliasDescriptionBottomDialogFragment
    private lateinit var editAliasRecipientsBottomDialogFragment: EditAliasRecipientsBottomDialogFragment

    private lateinit var aliasId: String
    private var forceSwitch = false
    private var shouldDeactivateThisAlias = false

    /*
    https://stackoverflow.com/questions/50969390/view-visibility-state-loss-when-resuming-activity-with-previously-started-activi
     */
    private var progressBarVisibility = View.VISIBLE


    override fun onResume() {
        super.onResume()
        activity_manage_alias_settings_RL_progressbar.visibility = progressBarVisibility
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_alias)
        setupToolbar(activity_manage_alias_toolbar)
        networkHelper = NetworkHelper(this)


        val b = intent.extras
        if (b?.getString("alias_id") != null) {
            // Intents
            val aliasId = b.getString("alias_id")
            val aliasForwardCount = b.getInt("alias_forward_count").toFloat()
            val aliasRepliedSentCount = b.getInt("alias_replied_sent_count").toFloat()

            if (aliasId == null) {
                finish()
                return
            }
            this.aliasId = aliasId

            // For a smooth overview, we require the numbers here.
            // Charts will be updated in the background
            setChart(aliasForwardCount, aliasRepliedSentCount)
            // Finish shared elements transition
            ViewCompat.setTransitionName(activity_manage_alias_chart, aliasId)
            setPage()

        } else if (intent.action != null) {
            // /deactivate URI's
            val data: Uri? = intent?.data
            val aliasId = StringUtils.substringBetween(data.toString(), "deactivate/", "?")
            this.aliasId = aliasId
            shouldDeactivateThisAlias = true
            setPage()
        }
    }


    private fun setPage() {
        // Get the alias
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAliasInfo(aliasId)
        }
    }

    private fun setChart(value1: Float, value2: Float) {
        // Set the chart to 0/0

        var shimmer = 0f
        // If both forwarded and replied are 0, make shimmer 1 to create a gray circle
        if (value1 == 0f && value2 == 0f) {
            shimmer = 1f
        }

        activity_manage_alias_chart.setDataPoints(
            floatArrayOf(
                value1,
                value2,
                shimmer
            )
        )



        activity_manage_alias_chart.setCenterColor(R.color.LightDarkMode)

        activity_manage_alias_forwarded_count.visibility = View.VISIBLE
        activity_manage_alias_replies_sent_count.visibility = View.VISIBLE
        activity_manage_alias_forwarded_count.text = value1.roundToInt().toString()
        activity_manage_alias_replies_sent_count.text = value2.roundToInt().toString()
    }

    private fun setOnSwitchChangeListeners() {
        activity_manage_alias_active_switch.setOnCheckedChangeListener { compoundButton, b ->
            // Using forceswitch we can toggle onCheckedChangeListener programmatically without having to press the actual switch
            if (compoundButton.isPressed || forceSwitch) {
                activity_manage_alias_active_switch_progressbar.visibility = View.VISIBLE
                forceSwitch = false
                if (b) {
                    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                        activateAlias()
                    }
                } else {
                    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                        deactivateAlias()
                    }
                }
            }
        }
    }


    private suspend fun deactivateAlias() {
        networkHelper.deactivateSpecificAlias({ result ->
            activity_manage_alias_active_switch_progressbar.visibility = View.GONE
            if (result == "204") {
                activity_manage_alias_status_textview.text = resources.getString(R.string.alias_deactivated)
            } else {
                activity_manage_alias_active_switch.isChecked = true
                val snackbar = Snackbar.make(
                    findViewById(R.id.activity_manage_alias_LL),
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    Snackbar.LENGTH_SHORT
                )
                if (SettingsManager(false, this).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
                    snackbar.setAction(R.string.logs) {
                        val intent = Intent(this, LogViewerActivity::class.java)
                        startActivity(intent)
                    }
                }
                snackbar.show()

            }
        }, aliasId)
    }


    private suspend fun activateAlias() {
        networkHelper.activateSpecificAlias({ result ->
            activity_manage_alias_active_switch_progressbar.visibility = View.GONE
            if (result == "200") {
                activity_manage_alias_status_textview.text = resources.getString(R.string.alias_activated)
            } else {
                activity_manage_alias_active_switch.isChecked = false
                val snackbar = Snackbar.make(
                    findViewById(R.id.activity_manage_alias_LL),
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    Snackbar.LENGTH_SHORT
                )
                if (SettingsManager(false, this).getSettingsBool(SettingsManager.PREFS.STORE_LOGS)) {
                    snackbar.setAction(R.string.logs) {
                        val intent = Intent(this, LogViewerActivity::class.java)
                        startActivity(intent)
                    }
                }
                snackbar.show()
            }
        }, aliasId)
    }



    private fun setOnClickListeners() {
        activity_manage_alias_active_switch_layout.setOnClickListener {
            forceSwitch = true
            activity_manage_alias_active_switch.isChecked = !activity_manage_alias_active_switch.isChecked
        }

        activity_manage_alias_desc_edit.setOnClickListener {
            if (!editAliasDescriptionBottomDialogFragment.isAdded) {
                editAliasDescriptionBottomDialogFragment.show(
                    supportFragmentManager,
                    "editAliasDescriptionBottomDialogFragment"
                )
            }
        }

        activity_manage_alias_recipients_edit.setOnClickListener {
            if (!editAliasRecipientsBottomDialogFragment.isAdded) {
                editAliasRecipientsBottomDialogFragment.show(
                    supportFragmentManager,
                    "editAliasRecipientsBottomDialogFragment"
                )
            }
        }

        activity_manage_alias_delete.setOnClickListener {
            deleteAlias()
        }


        activity_manage_alias_restore.setOnClickListener {
            restoreAlias()
        }
    }


    lateinit var dialog: AlertDialog
    private lateinit var customLayout: View
    private fun restoreAlias() {
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        // set the custom layout
        customLayout =
            layoutInflater.inflate(R.layout.anonaddy_custom_dialog, null)
        builder.setView(customLayout)
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        customLayout.dialog_title.text = resources.getString(R.string.restore_alias)
        customLayout.dialog_text.text =
            resources.getString(R.string.restore_alias_confirmation_desc)
        customLayout.dialog_positive_button.text =
            resources.getString(R.string.restore_alias)
        customLayout.dialog_positive_button.setOnClickListener {
            customLayout.dialog_progressbar.visibility = View.VISIBLE
            customLayout.dialog_error.visibility = View.GONE
            customLayout.dialog_negative_button.isEnabled = false
            customLayout.dialog_positive_button.isEnabled = false

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                restoreAliasHttpRequest(aliasId, this@ManageAliasActivity)
            }
        }
        customLayout.dialog_negative_button.setOnClickListener {
            dialog.dismiss()
        }
        // create and show the alert dialog
        dialog.show()
    }

    private fun deleteAlias() {
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        // set the custom layout
        customLayout =
            layoutInflater.inflate(R.layout.anonaddy_custom_dialog, null)
        builder.setView(customLayout)
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        customLayout.dialog_title.text = resources.getString(R.string.delete_alias)
        customLayout.dialog_text.text =
            resources.getString(R.string.delete_alias_confirmation_desc)
        customLayout.dialog_positive_button.text =
            resources.getString(R.string.delete_alias)
        customLayout.dialog_positive_button.setOnClickListener {
            customLayout.dialog_progressbar.visibility = View.VISIBLE
            customLayout.dialog_error.visibility = View.GONE
            customLayout.dialog_negative_button.isEnabled = false
            customLayout.dialog_positive_button.isEnabled = false

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                deleteAliasHttpRequest(aliasId, this@ManageAliasActivity)
            }
        }
        customLayout.dialog_negative_button.setOnClickListener {
            dialog.dismiss()
        }
        // create and show the alert dialog
        dialog.show()
    }

    private suspend fun deleteAliasHttpRequest(id: String, context: Context) {
        networkHelper.deleteAlias({ result ->
            if (result == "204") {
                dialog.dismiss()
                finish()
            } else {
                customLayout.dialog_progressbar.visibility = View.INVISIBLE
                customLayout.dialog_error.visibility = View.VISIBLE
                customLayout.dialog_negative_button.isEnabled = true
                customLayout.dialog_positive_button.isEnabled = true
                customLayout.dialog_error.text =
                    context.resources.getString(R.string.error_deleting_alias) + "\n" + result
            }
        }, id)
    }

    private suspend fun restoreAliasHttpRequest(id: String, context: Context) {
        networkHelper.restoreAlias({ result ->
            if (result == "200") {
                dialog.dismiss()
                setPage()
            } else {
                customLayout.dialog_progressbar.visibility = View.INVISIBLE
                customLayout.dialog_error.visibility = View.VISIBLE
                customLayout.dialog_negative_button.isEnabled = true
                customLayout.dialog_positive_button.isEnabled = true
                customLayout.dialog_error.text =
                    context.resources.getString(R.string.error_restoring_alias) + "\n" + result
            }
        }, id)
    }

    private suspend fun getAliasInfo(id: String) {
        networkHelper.getSpecificAlias({ list ->

            if (list != null) {

                // Set email in textview
                activity_manage_alias_email.text = list.email

                /**
                 * CHART
                 */

                // Update chart
                setChart(
                    list.emails_forwarded.toFloat(),
                    list.emails_replied.toFloat()
                )

                /**
                 *  SWITCH STATUS
                 */

                // Set switch status
                activity_manage_alias_active_switch.isChecked = list.active
                activity_manage_alias_status_textview.text =
                    if (list.active) resources.getString(R.string.alias_activated) else resources.getString(R.string.alias_deactivated)


                // Set the switch to disabled when the account is deleted. Else unlock it
                if (list.deleted_at == null) {
                    activity_manage_alias_active_switch.isClickable = true
                    activity_manage_alias_active_switch.isEnabled = true
                } else {
                    activity_manage_alias_active_switch.isClickable = false
                    activity_manage_alias_active_switch.isEnabled = false
                }

                /**
                 * LAYOUT
                 */

                val layout =
                    findViewById<View>(R.id.activity_manage_alias_settings_LL) as LinearLayout
                if (list.deleted_at != null) {
                    // Aliasdeleted is not null, thus deleted. We disable all the layouts and alpha them

                    // Show restore and hide delete
                    activity_manage_alias_restore.visibility = View.VISIBLE
                    activity_manage_alias_delete.visibility = View.GONE
                    for (i in 0 until layout.childCount) {
                        val child = layout.getChildAt(i)

                        // Do not disable the restore button
                        if (child.id == R.id.activity_manage_alias_restore) {
                            child.isEnabled = true
                            child.alpha = 1f
                            child.isClickable = true
                        } else {
                            child.isEnabled = false
                            child.alpha = 0.5f
                            child.isClickable = false
                        }
                    }
                } else {
                    // Show delete and hide restore
                    activity_manage_alias_restore.visibility = View.GONE
                    activity_manage_alias_delete.visibility = View.VISIBLE

                    // Aliasdeleted is null, thus not deleted. We enable all the layouts
                    for (i in 0 until layout.childCount) {
                        val child = layout.getChildAt(i)
                        child.isEnabled = true
                        child.alpha = 1f
                        child.isClickable = true
                    }
                }

                /**
                 * RECIPIENTS
                 */

                // Set recipients
                var recipients: String
                var count = 0
                if (list.recipients != null && list.recipients.isNotEmpty()) {
                    // get the first 2 recipients and list them

                    val buf = StringBuilder()
                    for (recipient in list.recipients) {
                        if (count < 2) {
                            if (buf.isNotEmpty()) {
                                buf.append("\n")
                            }
                            buf.append(recipient.email)
                            count++
                        }
                    }
                    recipients = buf.toString()

                    // Check if there are more than 2 recipients in the list
                    if (list.recipients.size > 2) {
                        // If this is the case add a "x more" on the third rule
                        // X is the total amount minus the 2 listed above
                        recipients += "\n"
                        recipients += this.resources.getString(
                            R.string._more,
                            list.recipients.size - 2
                        )
                    }
                } else {
                    recipients = this.resources.getString(
                        R.string.default_recipient
                    )
                }

                activity_manage_alias_recipients.text = recipients


                // Initialise the bottomdialog
                editAliasRecipientsBottomDialogFragment =
                    EditAliasRecipientsBottomDialogFragment.newInstance(aliasId, list.recipients)


                // Set created at and updated at
                activity_manage_alias_created_at.text = DateTimeUtils.turnStringIntoLocalString(list.created_at)
                activity_manage_alias_updated_at.text = DateTimeUtils.turnStringIntoLocalString(list.updated_at)


                /**
                 * DESCRIPTION
                 */

                // Set description and initialise the bottomDialogFragment
                if (list.description != null) {
                    activity_manage_alias_desc.text = list.description
                } else {
                    activity_manage_alias_desc.text = this.resources.getString(
                        R.string.alias_no_description
                    )
                }

                // We reset this value as it now includes the description
                editAliasDescriptionBottomDialogFragment = EditAliasDescriptionBottomDialogFragment.newInstance(
                    id,
                    list.description
                )


                activity_manage_alias_settings_RL_progressbar.visibility = View.GONE
                progressBarVisibility = View.GONE
                activity_manage_alias_settings_LL.visibility = View.VISIBLE

                setOnSwitchChangeListeners()
                setOnClickListeners()

                if (shouldDeactivateThisAlias) {
                    // Deactive switch
                    forceSwitch = true
                    activity_manage_alias_active_switch.isChecked = false
                }
            } else {
                activity_manage_alias_settings_RL_progressbar.visibility = View.GONE
                progressBarVisibility = View.GONE
                activity_manage_alias_settings_LL.visibility = View.GONE

                // Show no internet animations
                activity_manage_alias_settings_RL_lottieview.visibility = View.VISIBLE
            }


        }, id)
    }

    override fun onDestroy() {
        super.onDestroy()
        supportFinishAfterTransition()
    }

    override fun descriptionEdited(description: String) {
        setPage()
        editAliasDescriptionBottomDialogFragment.dismiss()
    }


    override fun recipientsEdited() {
        // Reload all info
        setPage()
        editAliasRecipientsBottomDialogFragment.dismiss()
    }
}