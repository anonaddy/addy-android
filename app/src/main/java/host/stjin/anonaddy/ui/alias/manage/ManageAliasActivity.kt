package host.stjin.anonaddy.ui.alias.manage

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import kotlinx.android.synthetic.main.activity_manage_alias.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ManageAliasActivity : BaseActivity(),
    EditAliasDescriptionBottomDialogFragment.AddEditAliasDescriptionBottomDialogListener,
    EditAliasRecipientsBottomDialogFragment.AddEditAliasRecipientsBottomDialogListener {

    lateinit var networkHelper: NetworkHelper

    lateinit var editAliasDescriptionBottomDialogFragment: EditAliasDescriptionBottomDialogFragment
    lateinit var editAliasRecipientsBottomDialogFragment: EditAliasRecipientsBottomDialogFragment

    lateinit var aliasId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_alias)
        setupToolbar(activity_manage_alias_toolbar)
        networkHelper = NetworkHelper(applicationContext)


        val b = intent.extras
        val aliasId = b?.getString("alias_id")
        val email = b?.getString("alias_email")
        val aliasLocal = b?.getString("alias_local")

        if (aliasId == null) {
            finish()
            return
        }

        this.aliasId = aliasId

        ViewCompat.setTransitionName(activity_manage_alias_chart, aliasId)

        setOnClickListeners()

        // Initial set, we don't know the description here.
        editAliasDescriptionBottomDialogFragment =
            EditAliasDescriptionBottomDialogFragment.newInstance(aliasId, "")


        // Initial set, we don't know the recipients here.
        editAliasRecipientsBottomDialogFragment =
            EditAliasRecipientsBottomDialogFragment.newInstance(aliasId, null)


        activity_manage_alias_prefix.text = email


        // Get the alias
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getAliasInfo(aliasId)
        }

    }

    private fun setOnSwitchChangeListeners() {
        activity_manage_alias_active_switch.setOnCheckedChangeListener { _, b ->
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
                Snackbar.make(
                    findViewById(R.id.activity_manage_alias_LL),
                    applicationContext.resources.getString(R.string.alias_deactivated),
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
                Snackbar.make(
                    findViewById(R.id.activity_manage_alias_LL),
                    applicationContext.resources.getString(R.string.error_edit_active) + "\n" + result,
                    Snackbar.LENGTH_SHORT
                )//TODO set action?
                    .show()
            }
        }, aliasId)
    }


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
    }

    private suspend fun getAliasInfo(id: String) {
        networkHelper.getSpecificAlias({ list ->

            if (list != null) {
                activity_manage_alias_chart.setDataPoints(
                    floatArrayOf(
                        list.emails_forwarded.toFloat(),
                        list.emails_replied.toFloat()
                    )
                )
                activity_manage_alias_chart.setCenterColor(android.R.color.transparent)
                activity_manage_alias_chart.setSliceColor(
                    intArrayOf(
                        R.color.portalOrange,
                        R.color.portalBlue
                    )
                )


                activity_manage_alias_active_switch.isChecked = list.active
                // Set the listener after the switch was changed
                setOnSwitchChangeListeners()


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
                    //TODO add default recipient
                }

                activity_manage_alias_recipients.text = recipients
                activity_manage_alias_created_at.text = list.created_at
                activity_manage_alias_updated_at.text = list.updated_at


                /*
                DESCRIPTION
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