package host.stjin.anonaddy.ui.usernames.manage

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.DateTimeUtils
import kotlinx.android.synthetic.main.activity_manage_usernames.*
import kotlinx.android.synthetic.main.anonaddy_custom_dialog.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class ManageUsernamesActivity : BaseActivity(),
    EditUsernameDescriptionBottomDialogFragment.AddEditUsernameDescriptionBottomDialogListener,
    EditUsernameRecipientBottomDialogFragment.AddEditUsernameRecipientBottomDialogListener {

    lateinit var networkHelper: NetworkHelper

    private lateinit var editUsernameDescriptionBottomDialogFragment: EditUsernameDescriptionBottomDialogFragment
    private lateinit var editUsernameRecipientBottomDialogFragment: EditUsernameRecipientBottomDialogFragment

    private lateinit var usernameId: String
    private var forceSwitch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_usernames)
        setupToolbar(activity_manage_username_toolbar)
        networkHelper = NetworkHelper(this)


        val b = intent.extras
        val usernameId = b?.getString("username_id")

        if (usernameId == null) {
            finish()
            return
        }
        this.usernameId = usernameId
        setPage()
    }


    private fun setPage() {
        activity_manage_username_RL_lottieview.visibility = View.GONE
        // Get the username
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getUsernameInfo(usernameId)
        }
    }

    private fun setOnSwitchChangeListeners() {
        activity_manage_username_active_switch_layout.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    activity_manage_username_active_switch_layout.showProgressBar(true)
                    forceSwitch = false
                    if (checked) {
                        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                            activateUsername()
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                            deactivateUsername()
                        }
                    }
                }
            }
        })
    }

    private suspend fun deactivateUsername() {
        networkHelper.deactivateSpecificUsername({ result ->
            activity_manage_username_active_switch_layout.showProgressBar(false)
            if (result == "204") {
                activity_manage_username_active_switch_layout.setTitle(resources.getString(R.string.username_deactivated))
            } else {
                activity_manage_username_active_switch_layout.setSwitchChecked(true)
                val snackbar = Snackbar.make(
                    findViewById(R.id.activity_manage_username_LL),
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
        }, usernameId)
    }


    private suspend fun activateUsername() {
        networkHelper.activateSpecificUsername({ result ->
            activity_manage_username_active_switch_layout.showProgressBar(false)
            if (result == "200") {
                activity_manage_username_active_switch_layout.setTitle(resources.getString(R.string.username_activated))
            } else {
                activity_manage_username_active_switch_layout.setSwitchChecked(false)
                val snackbar = Snackbar.make(
                    findViewById(R.id.activity_manage_username_LL),
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
        }, usernameId)
    }


    private fun setOnClickListeners() {
        activity_manage_username_active_switch_layout.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                activity_manage_username_active_switch_layout.setSwitchChecked(!activity_manage_username_active_switch_layout.getSwitchChecked())
            }
        })


        activity_manage_username_desc_edit.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editUsernameDescriptionBottomDialogFragment.isAdded) {
                    editUsernameDescriptionBottomDialogFragment.show(
                        supportFragmentManager,
                        "editUsernameDescriptionBottomDialogFragment"
                    )
                }
            }
        })


        activity_manage_username_recipients_edit.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editUsernameRecipientBottomDialogFragment.isAdded) {
                    editUsernameRecipientBottomDialogFragment.show(
                        supportFragmentManager,
                        "editUsernameRecipientsBottomDialogFragment"
                    )
                }
            }
        })


        activity_manage_username_delete.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                deleteUsername(usernameId)
            }
        })
    }


    private lateinit var deleteUsernameDialog: AlertDialog
    private lateinit var deleteUsernameCustomLayout: View
    private fun deleteUsername(id: String) {
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        // set the custom layout
        deleteUsernameCustomLayout =
            layoutInflater.inflate(R.layout.anonaddy_custom_dialog, null)
        builder.setView(deleteUsernameCustomLayout)
        deleteUsernameDialog = builder.create()
        deleteUsernameDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        deleteUsernameCustomLayout.dialog_title.text = resources.getString(R.string.delete_username)
        deleteUsernameCustomLayout.dialog_text.text = resources.getString(R.string.delete_username_desc_confirm)
        deleteUsernameCustomLayout.dialog_positive_button.text =
            resources.getString(R.string.delete_username)
        deleteUsernameCustomLayout.dialog_positive_button.setOnClickListener {
            deleteUsernameCustomLayout.dialog_progressbar.visibility = View.VISIBLE
            deleteUsernameCustomLayout.dialog_error.visibility = View.GONE
            deleteUsernameCustomLayout.dialog_negative_button.isEnabled = false
            deleteUsernameCustomLayout.dialog_positive_button.isEnabled = false

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                deleteUsernameHttpRequest(id, this@ManageUsernamesActivity)
            }
        }
        deleteUsernameCustomLayout.dialog_negative_button.setOnClickListener {
            deleteUsernameDialog.dismiss()
        }
        // create and show the alert dialog
        deleteUsernameDialog.show()
    }


    private suspend fun deleteUsernameHttpRequest(id: String, context: Context) {
        networkHelper.deleteUsername({ result ->
            if (result == "204") {
                deleteUsernameDialog.dismiss()
                finish()
            } else {
                deleteUsernameCustomLayout.dialog_progressbar.visibility = View.INVISIBLE
                deleteUsernameCustomLayout.dialog_error.visibility = View.VISIBLE
                deleteUsernameCustomLayout.dialog_negative_button.isEnabled = true
                deleteUsernameCustomLayout.dialog_positive_button.isEnabled = true
                deleteUsernameCustomLayout.dialog_error.text = context.resources.getString(
                    R.string.s_s,
                    context.resources.getString(R.string.error_deleting_username), result
                )
            }
        }, id)
    }


    private suspend fun getUsernameInfo(id: String) {
        networkHelper.getSpecificUsername({ list ->

            if (list != null) {
                /**
                 *  SWITCH STATUS
                 */

                activity_manage_username_active_switch_layout.setSwitchChecked(list.active)
                activity_manage_username_active_switch_layout.setTitle(
                    if (list.active) resources.getString(R.string.username_activated) else resources.getString(R.string.username_deactivated)
                )


                /**
                 * TEXT
                 */

                var totalForwarded = 0
                var totalBlocked = 0
                var totalReplies = 0
                var totalSent = 0
                val totalAliases = list.aliases?.size
                var aliases = ""

                val buf = StringBuilder()

                if (list.aliases != null) {
                    for (alias in list.aliases) {
                        totalForwarded += alias.emails_forwarded
                        totalBlocked += alias.emails_blocked
                        totalReplies += alias.emails_replied
                        totalSent += alias.emails_sent

                        if (buf.isNotEmpty()) {
                            buf.append("\n")
                        }
                        buf.append(alias.email)
                    }
                    aliases = buf.toString()
                }

                activity_manage_username_aliases_title_textview.text = resources.getString(R.string.username_aliases_d, totalAliases)
                activity_manage_username_basic_textview.text = resources.getString(
                    R.string.manage_username_basic_info,
                    list.username,
                    DateTimeUtils.turnStringIntoLocalString(list.created_at),
                    DateTimeUtils.turnStringIntoLocalString(list.updated_at),
                    totalForwarded, totalBlocked, totalReplies, totalSent
                )

                activity_manage_username_aliases_textview.text = aliases

                /**
                 * RECIPIENTS
                 */

                // Set recipient
                val recipients: String = list.default_recipient?.email ?: this.resources.getString(
                    R.string.default_recipient_s, User.userResourceExtended.default_recipient_email
                )

                activity_manage_username_recipients_edit.setDescription(recipients)


                // Set this value as it now includes the default email
                editUsernameRecipientBottomDialogFragment =
                    EditUsernameRecipientBottomDialogFragment.newInstance(usernameId, list.default_recipient?.email)


                /**
                 * DESCRIPTION
                 */

                // Set description and initialise the bottomDialogFragment
                if (list.description != null) {
                    activity_manage_username_desc_edit.setDescription(list.description)
                } else {
                    activity_manage_username_desc_edit.setDescription(
                        this.resources.getString(
                            R.string.username_no_description
                        )
                    )
                }

                // Set this value as it now includes the description
                editUsernameDescriptionBottomDialogFragment = EditUsernameDescriptionBottomDialogFragment.newInstance(
                    id,
                    list.description
                )


                activity_manage_username_RL_progressbar.visibility = View.GONE
                activity_manage_username_LL1.visibility = View.VISIBLE

                setOnSwitchChangeListeners()
                setOnClickListeners()
            } else {
                activity_manage_username_RL_progressbar.visibility = View.GONE
                activity_manage_username_LL1.visibility = View.GONE

                // Show no internet animations
                activity_manage_username_RL_lottieview.visibility = View.VISIBLE
            }
        }, id)
    }


    override fun descriptionEdited(description: String) {
        setPage()
        editUsernameDescriptionBottomDialogFragment.dismiss()
    }

    override fun recipientEdited() {
        setPage()
        editUsernameRecipientBottomDialogFragment.dismiss()
    }
}