package host.stjin.anonaddy.ui.recipients.manage

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
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.DateTimeUtils
import kotlinx.android.synthetic.main.activity_manage_recipients.*
import kotlinx.android.synthetic.main.anonaddy_custom_dialog.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class ManageRecipientsActivity : BaseActivity(),
    AddRecipientPublicGpgKeyBottomDialogFragment.AddEditGpgKeyBottomDialogListener {

    lateinit var networkHelper: NetworkHelper

    private lateinit var addRecipientPublicGpgKeyBottomDialogFragment: AddRecipientPublicGpgKeyBottomDialogFragment

    private lateinit var recipientId: String
    private var forceSwitch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_recipients)
        setupToolbar(activity_manage_recipient_toolbar)
        networkHelper = NetworkHelper(this)


        val b = intent.extras
        val recipientId = b?.getString("recipient_id")

        if (recipientId == null) {
            finish()
            return
        }
        this.recipientId = recipientId

        setPage()
    }


    private fun setPage() {
        activity_manage_recipient_RL_lottieview.visibility = View.GONE

        // Get the recipient
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            getRecipientInfo(recipientId)
        }
    }

    private fun setOnSwitchChangeListeners(fingerprint: String?) {

        activity_manage_recipient_active.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    activity_manage_recipient_active.showProgressBar(true)
                    forceSwitch = false

                    if (checked) {
                        if (fingerprint != null) {
                            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                                enableEncryption()
                            }
                        } else {
                            activity_manage_recipient_active.showProgressBar(false)
                            activity_manage_recipient_active.setSwitchChecked(false)
                            if (!addRecipientPublicGpgKeyBottomDialogFragment.isAdded) {
                                addRecipientPublicGpgKeyBottomDialogFragment.show(
                                    supportFragmentManager,
                                    "editrecipientDescriptionBottomDialogFragment"
                                )
                            }
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                            disableEncryption()
                        }
                    }
                }
            }
        })
    }


    private suspend fun disableEncryption() {
        networkHelper.disableEncryptionRecipient({ result ->
            activity_manage_recipient_active.showProgressBar(false)
            if (result == "204") {
                activity_manage_recipient_active.setTitle(resources.getString(R.string.encryption_disabled))
            } else {
                activity_manage_recipient_active.setSwitchChecked(true)
                val snackbar = Snackbar.make(
                    findViewById(R.id.activity_manage_recipient_LL),
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
        }, recipientId)
    }


    private suspend fun enableEncryption() {
        networkHelper.enableEncryptionRecipient({ result ->
            activity_manage_recipient_active.showProgressBar(false)
            if (result == "200") {
                activity_manage_recipient_active.setTitle(resources.getString(R.string.encryption_enabled))
            } else {
                activity_manage_recipient_active.setSwitchChecked(false)
                val snackbar = Snackbar.make(
                    findViewById(R.id.activity_manage_recipient_LL),
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
        }, recipientId)
    }


    private fun setOnClickListeners() {
        activity_manage_recipient_change_gpg_key.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!addRecipientPublicGpgKeyBottomDialogFragment.isAdded) {
                    addRecipientPublicGpgKeyBottomDialogFragment.show(
                        supportFragmentManager,
                        "editrecipientDescriptionBottomDialogFragment"
                    )
                }
            }
        })

        activity_manage_recipient_remove_gpg_key.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                removeGpgKey(recipientId)
            }
        })

        activity_manage_recipient_delete.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                deleteRecipient(recipientId)
            }
        })

        activity_manage_recipient_active.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                activity_manage_recipient_active.setSwitchChecked(!activity_manage_recipient_active.getSwitchChecked())
            }
        })

    }


    private lateinit var removeGpgKeyDialog: AlertDialog
    private lateinit var removeGpgKeyCustomLayout: View
    private fun removeGpgKey(id: String) {
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        // set the custom layout
        removeGpgKeyCustomLayout =
            layoutInflater.inflate(R.layout.anonaddy_custom_dialog, null)
        builder.setView(removeGpgKeyCustomLayout)
        removeGpgKeyDialog = builder.create()
        removeGpgKeyDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        removeGpgKeyCustomLayout.dialog_title.text = resources.getString(R.string.remove_public_key)
        removeGpgKeyCustomLayout.dialog_text.text = resources.getString(R.string.remove_public_key_desc)
        removeGpgKeyCustomLayout.dialog_positive_button.text =
            resources.getString(R.string.remove_public_key)
        removeGpgKeyCustomLayout.dialog_positive_button.setOnClickListener {
            removeGpgKeyCustomLayout.dialog_progressbar.visibility = View.VISIBLE
            removeGpgKeyCustomLayout.dialog_error.visibility = View.GONE
            removeGpgKeyCustomLayout.dialog_negative_button.isEnabled = false
            removeGpgKeyCustomLayout.dialog_positive_button.isEnabled = false

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                removeGpgKeyHttpRequest(id, this@ManageRecipientsActivity)
            }
        }
        removeGpgKeyCustomLayout.dialog_negative_button.setOnClickListener {
            removeGpgKeyDialog.dismiss()
        }
        // create and show the alert dialog
        removeGpgKeyDialog.show()
    }

    private lateinit var deleteRecipientDialog: AlertDialog
    private lateinit var deleteRecipientcustomLayout: View
    private fun deleteRecipient(id: String) {
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        // set the custom layout
        deleteRecipientcustomLayout =
            layoutInflater.inflate(R.layout.anonaddy_custom_dialog, null)
        builder.setView(deleteRecipientcustomLayout)
        deleteRecipientDialog = builder.create()
        deleteRecipientDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        deleteRecipientcustomLayout.dialog_title.text = resources.getString(R.string.delete_recipient)
        deleteRecipientcustomLayout.dialog_text.text = resources.getString(R.string.delete_recipient_desc)
        deleteRecipientcustomLayout.dialog_positive_button.text =
            resources.getString(R.string.delete_recipient)
        deleteRecipientcustomLayout.dialog_positive_button.setOnClickListener {
            deleteRecipientcustomLayout.dialog_progressbar.visibility = View.VISIBLE
            deleteRecipientcustomLayout.dialog_error.visibility = View.GONE
            deleteRecipientcustomLayout.dialog_negative_button.isEnabled = false
            deleteRecipientcustomLayout.dialog_positive_button.isEnabled = false

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                deleteRecipientHttpRequest(id, this@ManageRecipientsActivity)
            }
        }
        deleteRecipientcustomLayout.dialog_negative_button.setOnClickListener {
            deleteRecipientDialog.dismiss()
        }
        // create and show the alert dialog
        deleteRecipientDialog.show()
    }

    private suspend fun deleteRecipientHttpRequest(id: String, context: Context) {
        networkHelper.deleteRecipient({ result ->
            if (result == "204") {
                deleteRecipientDialog.dismiss()
                finish()
            } else {
                deleteRecipientcustomLayout.dialog_progressbar.visibility = View.INVISIBLE
                deleteRecipientcustomLayout.dialog_error.visibility = View.VISIBLE
                deleteRecipientcustomLayout.dialog_negative_button.isEnabled = true
                deleteRecipientcustomLayout.dialog_positive_button.isEnabled = true
                deleteRecipientcustomLayout.dialog_error.text = context.resources.getString(
                    R.string.s_s,
                    context.resources.getString(R.string.error_deleting_recipient), result
                )
            }
        }, id)
    }

    private suspend fun removeGpgKeyHttpRequest(id: String, context: Context) {
        networkHelper.removeEncryptionKeyRecipient({ result ->
            if (result == "204") {
                removeGpgKeyDialog.dismiss()
                setPage()
            } else {
                removeGpgKeyCustomLayout.dialog_progressbar.visibility = View.INVISIBLE
                removeGpgKeyCustomLayout.dialog_error.visibility = View.VISIBLE
                removeGpgKeyCustomLayout.dialog_negative_button.isEnabled = true
                removeGpgKeyCustomLayout.dialog_positive_button.isEnabled = true
                removeGpgKeyCustomLayout.dialog_error.text = context.resources.getString(
                    R.string.s_s,
                    context.resources.getString(R.string.error_removing_gpg_key), result
                )
            }
        }, id)
    }


    private suspend fun getRecipientInfo(id: String) {
        networkHelper.getSpecificRecipient({ list, _ ->

            if (list != null) {

                /**
                 *  SWITCH STATUS
                 */

                activity_manage_recipient_active.setSwitchChecked(list.should_encrypt)
                activity_manage_recipient_active.setTitle(
                    if (list.should_encrypt) resources.getString(R.string.encryption_enabled) else resources.getString(
                        R.string.encryption_disabled
                    )
                )


                // Set switchlistener after loading
                setOnSwitchChangeListeners(list.fingerprint)

                // Set the fingerprint BottomDialogFragment
                addRecipientPublicGpgKeyBottomDialogFragment =
                    AddRecipientPublicGpgKeyBottomDialogFragment.newInstance(recipientId)

                /**
                 * Fingerprint LAYOUT
                 */

                // If there is a fingerprint, enable the remove button.
                // If there is no fingerptint, do not enable the remove button
                if (list.fingerprint != null) {
                    activity_manage_recipient_remove_gpg_key.setLayoutEnabled(true)
                    activity_manage_recipient_change_gpg_key.setTitle(resources.getString(R.string.change_public_gpg_key))
                    activity_manage_recipient_encryption_textview.text = resources.getString(R.string.fingerprint_s, list.fingerprint)
                } else {
                    activity_manage_recipient_remove_gpg_key.setLayoutEnabled(false)
                    activity_manage_recipient_change_gpg_key.setTitle(resources.getString(R.string.add_public_gpg_key))
                    activity_manage_recipient_encryption_textview.text = resources.getString(R.string.encryption_disabled)
                }


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

                activity_manage_recipient_aliases_title_textview.text = resources.getString(R.string.recipient_aliases_d, totalAliases)
                activity_manage_recipient_basic_textview.text = resources.getString(
                    R.string.manage_recipient_basic_info,
                    list.email,
                    DateTimeUtils.turnStringIntoLocalString(list.created_at),
                    DateTimeUtils.turnStringIntoLocalString(list.updated_at),
                    totalForwarded, totalBlocked, totalReplies, totalSent
                )

                activity_manage_recipient_aliases_textview.text = aliases

                activity_manage_recipient_RL_progressbar.visibility = View.GONE
                activity_manage_recipient_LL1.visibility = View.VISIBLE

                setOnClickListeners()
            } else {
                activity_manage_recipient_RL_progressbar.visibility = View.GONE
                activity_manage_recipient_LL1.visibility = View.GONE

                // Show no internet animations
                activity_manage_recipient_RL_lottieview.visibility = View.VISIBLE
            }
        }, id)
    }


    override fun onKeyAdded() {
        setPage()
        addRecipientPublicGpgKeyBottomDialogFragment.dismiss()
    }
}