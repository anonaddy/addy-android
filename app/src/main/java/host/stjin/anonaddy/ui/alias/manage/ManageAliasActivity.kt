package host.stjin.anonaddy.ui.alias.manage

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.utils.DateTimeUtils
import kotlinx.android.synthetic.main.activity_manage_alias.*
import kotlinx.android.synthetic.main.anonaddy_custom_dialog.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class ManageAliasActivity : BaseActivity(),
    EditAliasDescriptionBottomDialogFragment.AddEditAliasDescriptionBottomDialogListener,
    EditAliasRecipientsBottomDialogFragment.AddEditAliasRecipientsBottomDialogListener {

    lateinit var networkHelper: NetworkHelper

    private lateinit var editAliasDescriptionBottomDialogFragment: EditAliasDescriptionBottomDialogFragment
    private lateinit var editAliasRecipientsBottomDialogFragment: EditAliasRecipientsBottomDialogFragment

    private lateinit var aliasId: String
    private lateinit var aliasEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_alias)
        setupToolbar(activity_manage_alias_toolbar)
        networkHelper = NetworkHelper(applicationContext)


        val b = intent.extras
        val aliasId = b?.getString("alias_id")
        val email = b?.getString("alias_email")
        val aliasDeleted = b?.getString("alias_deleted")
        val aliasForwardCount = b?.getFloat("alias_forward_count")
        val aliasRepliedSentCount = b?.getFloat("alias_replied_sent_count")

        if (aliasId == null || email == null) {
            finish()
            return
        }
        this.aliasId = aliasId
        this.aliasEmail = email

        // For a smooth overview, we require the numbers here.
        // Charts will be updated in the background

        if (aliasForwardCount != null && aliasRepliedSentCount != null) {
            setChart(aliasForwardCount, aliasRepliedSentCount)
        }

        // Finish shared elements transition
        ViewCompat.setTransitionName(activity_manage_alias_chart, aliasId)

        setPage(aliasDeleted, email)
    }

    /*
    Disable and alpha view if the alias is deleted
     */
    private fun setPage(aliasDeleted: String?, email: String?) {
        val layout =
            findViewById<View>(R.id.activity_manage_alias_settings_LL) as LinearLayout

        // Set email
        activity_manage_alias_email.text = email

        if (aliasDeleted != null) {
            activity_manage_alias_restore.visibility = View.VISIBLE
            activity_manage_alias_delete.visibility = View.GONE

            // Only enable the restore button here.
            setOnRestoreClickListener()

            // Aliasdeleted is not null, thus deleted. We disable all the layouts and alpha them
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
            activity_manage_alias_restore.visibility = View.GONE
            activity_manage_alias_delete.visibility = View.VISIBLE

            // Only enable the clicklisteners if the alias is not disabled
            setOnClickListeners()

            // Aliasdeleted is null, thus not deleted. We enable all the layouts
            for (i in 0 until layout.childCount) {
                val child = layout.getChildAt(i)
                child.isEnabled = true
                child.alpha = 1f
                child.isClickable = true
            }


            // Initial set, we don't know the description here.
            editAliasDescriptionBottomDialogFragment =
                EditAliasDescriptionBottomDialogFragment.newInstance(aliasId, "")

            // Initial set, we don't know the recipients here.
            editAliasRecipientsBottomDialogFragment =
                EditAliasRecipientsBottomDialogFragment.newInstance(aliasId, null)
        }


        // Get the alias
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAliasInfo(aliasId)
        }
    }

    private fun setChart(value1: Float, value2: Float) {
        // Set the chart to 0/0
        activity_manage_alias_chart.setDataPoints(
            floatArrayOf(
                value1,
                value2
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
            if (compoundButton.isPressed) {
                activity_manage_alias_active_switch_progressbar.visibility = View.VISIBLE

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
        activity_manage_alias_active_switch_progressbar.visibility = View.GONE

        networkHelper.deactivateSpecificAlias({ result ->
            if (result == "204") {
                Snackbar.make(
                    findViewById(R.id.activity_manage_alias_LL),
                    applicationContext.resources.getString(R.string.alias_deactivated),
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                activity_manage_alias_active_switch.isChecked = true
                Snackbar.make(
                    findViewById(R.id.activity_manage_alias_LL),
                    applicationContext.resources.getString(R.string.error_edit_active) + "\n" + result,
                    Snackbar.LENGTH_SHORT
                ).show()
                //TODO action button with error details?
            }
        }, aliasId)
    }


    private suspend fun activateAlias() {
        activity_manage_alias_active_switch_progressbar.visibility = View.GONE


        networkHelper.activateSpecificAlias({ result ->
            if (result == "200") {
                Snackbar.make(
                    findViewById(R.id.activity_manage_alias_LL),
                    applicationContext.resources.getString(R.string.alias_activated),
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                activity_manage_alias_active_switch.isChecked = false
                Snackbar.make(
                    findViewById(R.id.activity_manage_alias_LL),
                    applicationContext.resources.getString(R.string.error_edit_active) + "\n" + result,
                    Snackbar.LENGTH_SHORT
                )//TODO set action?
                    .show()
            }
        }, aliasId)
    }


    lateinit var dialog: AlertDialog
    private lateinit var customLayout: View
    private fun setOnClickListeners() {
        activity_manage_alias_desc_edit.setOnClickListener {
            editAliasDescriptionBottomDialogFragment.show(
                supportFragmentManager,
                "editAliasDescriptionBottomDialogFragment"
            )
        }

        activity_manage_alias_recipients_edit.setOnClickListener {
            editAliasRecipientsBottomDialogFragment.show(
                supportFragmentManager,
                "editAliasRecipientsBottomDialogFragment"
            )
        }

        activity_manage_alias_delete.setOnClickListener {
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
    }

    private fun setOnRestoreClickListener() {
        activity_manage_alias_restore.setOnClickListener {
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
                setPage(null, aliasEmail)
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
                setChart(
                    list.emails_forwarded.toFloat(),
                    list.emails_replied.toFloat()
                )

                activity_manage_alias_active_switch.isChecked = list.active

                // Set the switch to disabled when the account is deleted.
                if (list.deleted_at == null) {
                    // Set the listener after the switch was changed
                    setOnSwitchChangeListeners()

                    activity_manage_alias_active_switch.isClickable = true
                    activity_manage_alias_active_switch.isEnabled = true
                } else {
                    activity_manage_alias_active_switch.isClickable = false
                    activity_manage_alias_active_switch.isEnabled = false
                }

                var recipients = ""
                var count = 0
                if (list.recipients != null) {

                    // get the first 2 recipients and list them
                    for (recipient in list.recipients) {
                        if (count < 2) {
                            recipients += recipient.email
                            if (count < 1) {
                                recipients += "\n"
                            }
                            count++
                        }
                    }

                    // Check if there are more than 2 recipients in the list
                    if (list.recipients.size > 2) {
                        // If this is the case add a "x more" on the third rule
                        // X is the total amount minus the 2 listed above
                        recipients += "\n"
                        recipients += applicationContext.resources.getString(
                            R.string._more,
                            list.recipients.size - 2
                        )
                    }
                } else {
                    // TODO Add default recipient between ()
                    recipients = applicationContext.resources.getString(
                        R.string.default_recipient
                    )
                }

                activity_manage_alias_recipients.text = recipients
                activity_manage_alias_created_at.text = DateTimeUtils.turnStringIntoLocalString(list.created_at)
                activity_manage_alias_updated_at.text = DateTimeUtils.turnStringIntoLocalString(list.updated_at)


                /*
                DESCRIPTION
                    re-assign the instance, this time with the right description for easy editing
                 */
                if (list.description != null) {
                    activity_manage_alias_desc.text = list.description
                    // We reset this value as it now includes the description
                    editAliasDescriptionBottomDialogFragment = list.description.let {
                        EditAliasDescriptionBottomDialogFragment.newInstance(
                            id,
                            it
                        )
                    }
                } else {
                    activity_manage_alias_desc.text = applicationContext.resources.getString(
                        R.string.no_description
                    )
                }


                /*
                RECIPIENTS
                    re-assign the instance, this time with the right recipients
                 */
                editAliasRecipientsBottomDialogFragment =
                    EditAliasRecipientsBottomDialogFragment.newInstance(aliasId, list.recipients)

            }
        }, id)
    }

    override fun onDestroy() {
        super.onDestroy()
        supportFinishAfterTransition()
    }

    override fun descriptionEdited(description: String) {
        activity_manage_alias_desc.text = description
        editAliasDescriptionBottomDialogFragment.dismiss()
    }


    override fun recipientsEdited() {
        // Reload all info
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAliasInfo(aliasId)
        }
        editAliasRecipientsBottomDialogFragment.dismiss()
    }
}