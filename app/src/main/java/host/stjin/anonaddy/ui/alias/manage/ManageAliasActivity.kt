package host.stjin.anonaddy.ui.alias.manage

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import app.futured.donut.DonutSection
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityManageAliasBinding
import host.stjin.anonaddy.models.Aliases
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.*
import host.stjin.anonaddy.utils.AnonAddyUtils.getSendAddress
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils


class ManageAliasActivity : BaseActivity(),
    EditAliasDescriptionBottomDialogFragment.AddEditAliasDescriptionBottomDialogListener,
    EditAliasRecipientsBottomDialogFragment.AddEditAliasRecipientsBottomDialogListener,
    EditAliasSendMailRecipientBottomDialogFragment.AddEditAliasSendMailRecipientBottomDialogListener {

    lateinit var networkHelper: NetworkHelper
    private lateinit var aliasWatcher: AliasWatcher

    private lateinit var editAliasDescriptionBottomDialogFragment: EditAliasDescriptionBottomDialogFragment
    private lateinit var editAliasRecipientsBottomDialogFragment: EditAliasRecipientsBottomDialogFragment
    private lateinit var editAliasSendMailRecipientBottomDialogFragment: EditAliasSendMailRecipientBottomDialogFragment

    private lateinit var aliasId: String
    private var alias: Aliases? = null
    private var forceSwitch = false
    private var shouldDeactivateThisAlias = false


    // This value is here to keep track if the activity to which we return on finishWithUpdate should update its data.
    // Basically, whenever some information is changed we flip the boolean to true.
    private var shouldUpdate: Boolean = false

    /*
    https://stackoverflow.com/questions/50969390/view-visibility-state-loss-when-resuming-activity-with-previously-started-activi
     */
    private var progressBarVisibility = View.VISIBLE
    private lateinit var binding: ActivityManageAliasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageAliasBinding.inflate(layoutInflater)
        val view = binding.root
        // Since this activity can be directly launched, set the dark mode.
        checkForDarkModeAndSetFlags()
        setContentView(view)
        drawBehindNavBar(view, binding.activityManageAliasNSVLL)

        setupToolbar(
            R.string.edit_alias,
            binding.activityManageAliasNSV,
            binding.activityManageAliasToolbar,
            R.drawable.ic_email_at
        )

        networkHelper = NetworkHelper(this)
        aliasWatcher = AliasWatcher(this)

        val intent = intent
        val b = intent.extras
        if (b?.getString("alias_id") != null) {
            // Intents
            val aliasId = b.getString("alias_id")
            if (aliasId == null) {
                finish()
                return
            }
            this.aliasId = aliasId
            setPage()

        } else if (intent.action != null) {
            // /deactivate URI's
            val data: Uri? = intent?.data
            if (data.toString().contains("/deactivate")) {
                val aliasId = StringUtils.substringBetween(data.toString(), "deactivate/", "?")
                this.aliasId = aliasId
                shouldDeactivateThisAlias = true
                setPage()
            }
        }
    }

    // Override onbackpressed, this is the only way to close the activity (besides deleting the alias)
    override fun onBackPressed() {
        finishWithUpdate()
    }

    private fun setPage() {
        /**
         * This activity can be called by an URI or Widget/Notification Intent.
         * Protect this part
         */
        lifecycleScope.launch {
            isAuthenticated { isAuthenticated ->
                if (isAuthenticated) {
                    setPageInfo()
                }
            }
        }

    }

    private fun setPageInfo() {
        // Get the alias
        lifecycleScope.launch {
            getAliasInfo(aliasId)
        }
    }

    private fun setChart(forwarded: Float, replied: Float, blocked: Float, sent: Float) {
        val listOfDonutSection: ArrayList<DonutSection> = arrayListOf()
        var donutCap = 0f
        // DONUT
        val section1 = DonutSection(
            name = binding.activityManageAliasChart.context.resources.getString(R.string.d_forwarded, forwarded.toInt()),
            color = ContextCompat.getColor(this, R.color.portalOrange),
            amount = forwarded
        )
        // Always show section 1
        listOfDonutSection.add(section1)
        donutCap += forwarded

        if (replied > 0) {
            val section2 = DonutSection(
                name = binding.activityManageAliasChart.context.resources.getString(R.string.d_replied, replied.toInt()),
                color = ContextCompat.getColor(this, R.color.portalBlue),
                amount = replied
            )
            listOfDonutSection.add(section2)
            donutCap += replied
        }

        if (sent > 0) {
            val section3 = DonutSection(
                name = binding.activityManageAliasChart.context.resources.getString(R.string.d_sent, sent.toInt()),
                color = ContextCompat.getColor(this, R.color.easternBlue),
                amount = sent
            )
            listOfDonutSection.add(section3)
            donutCap += sent
        }

        if (blocked > 0) {
            val section4 = DonutSection(
                name = binding.activityManageAliasChart.context.resources.getString(R.string.d_blocked, blocked.toInt()),
                color = ContextCompat.getColor(this, R.color.softRed),
                amount = blocked
            )
            listOfDonutSection.add(section4)
            donutCap += blocked
        }
        binding.activityManageAliasChart.cap = donutCap

        // Sort the list by amount so that the biggest number will fill the whole ring
        binding.activityManageAliasChart.submitData(listOfDonutSection.sortedBy { it.amount })
        // DONUT


        binding.activityManageAliasForwardedCount.text = this.resources.getString(R.string.d_forwarded, forwarded.toInt())
        binding.activityManageAliasRepliesBlockedCount.text = this.resources.getString(R.string.d_blocked, blocked.toInt())
        binding.activityManageAliasSentCount.text = this.resources.getString(R.string.d_sent, sent.toInt())
        binding.activityManageAliasRepliedCount.text = this.resources.getString(R.string.d_replied, replied.toInt())


        binding.activityManageAliasStatsLL.animate().alpha(1.0f)
        binding.activityManageAliasActionsLL.animate().alpha(1.0f)
    }

    private fun setOnSwitchChangeListeners() {
        binding.activityManageAliasActiveSwitchLayout.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    binding.activityManageAliasActiveSwitchLayout.showProgressBar(true)
                    forceSwitch = false
                    shouldUpdate = true
                    if (checked) {
                        lifecycleScope.launch {
                            activateAlias()
                        }
                    } else {
                        lifecycleScope.launch {
                            deactivateAlias()
                        }
                    }
                }
            }
        })

        binding.activityManageAliasWatchSwitchLayout.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    forceSwitch = false
                    shouldUpdate = true
                    if (checked) {
                        aliasWatcher.addAliasToWatch(aliasId)
                    } else {
                        aliasWatcher.removeAliasToWatch(aliasId)
                    }
                }
            }
        })
    }


    private suspend fun deactivateAlias() {
        networkHelper.deactivateSpecificAlias({ result ->
            binding.activityManageAliasActiveSwitchLayout.showProgressBar(false)
            if (result == "204") {
                binding.activityManageAliasActiveSwitchLayout.setTitle(resources.getString(R.string.alias_deactivated))
            } else {
                binding.activityManageAliasActiveSwitchLayout.setSwitchChecked(true)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    binding.activityManageAliasCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, aliasId)
    }


    private suspend fun activateAlias() {
        networkHelper.activateSpecificAlias({ result ->
            binding.activityManageAliasActiveSwitchLayout.showProgressBar(false)
            if (result == "200") {
                binding.activityManageAliasActiveSwitchLayout.setTitle(resources.getString(R.string.alias_activated))
            } else {
                binding.activityManageAliasActiveSwitchLayout.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    binding.activityManageAliasCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, aliasId)
    }


    private fun setOnClickListeners() {
        binding.activityManageAliasActiveSwitchLayout.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityManageAliasActiveSwitchLayout.setSwitchChecked(!binding.activityManageAliasActiveSwitchLayout.getSwitchChecked())
            }
        })


        binding.activityManageAliasWatchSwitchLayout.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityManageAliasWatchSwitchLayout.setSwitchChecked(!binding.activityManageAliasWatchSwitchLayout.getSwitchChecked())
            }
        })

        binding.activityManageAliasDescEdit.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editAliasDescriptionBottomDialogFragment.isAdded) {
                    editAliasDescriptionBottomDialogFragment.show(
                        supportFragmentManager,
                        "editAliasDescriptionBottomDialogFragment"
                    )
                }
            }
        })

        binding.activityManageAliasRecipientsEdit.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editAliasRecipientsBottomDialogFragment.isAdded) {
                    editAliasRecipientsBottomDialogFragment.show(
                        supportFragmentManager,
                        "editAliasRecipientsBottomDialogFragment"
                    )
                }
            }
        })

        binding.activityManageAliasDelete.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                deleteAlias()
            }
        })

        binding.activityManageAliasForget.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forgetAlias()
            }
        })

        binding.activityManageAliasRestore.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                restoreAlias()
            }
        })

        binding.activityManageAliasCopy.setOnClickListener {
            val clipboard: ClipboardManager =
                this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("alias", binding.activityManageAliasEmail.text)
            clipboard.setPrimaryClip(clip)
            SnackbarHelper.createSnackbar(this, this.resources.getString(R.string.copied_alias), binding.activityManageAliasCL).show()
        }

        binding.activityManageAliasSend.setOnClickListener {
            if (!editAliasSendMailRecipientBottomDialogFragment.isAdded) {
                editAliasSendMailRecipientBottomDialogFragment.show(
                    supportFragmentManager,
                    "editAliasSendMailRecipientBottomDialogFragment"
                )
            }
        }
    }


    private lateinit var restoreAliasSnackbar: Snackbar
    private fun restoreAlias() {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Catalog_MaterialAlertDialog_Centered_FullWidthButtons)
            .setTitle(resources.getString(R.string.restore_alias))
            .setIcon(R.drawable.ic_trash_off)
            .setMessage(resources.getString(R.string.restore_alias_confirmation_desc))
            .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(resources.getString(R.string.restore)) { _, _ ->
                restoreAliasSnackbar = SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.restoring_alias),
                    binding.activityManageAliasCL,
                    length = Snackbar.LENGTH_INDEFINITE
                )
                restoreAliasSnackbar.show()
                lifecycleScope.launch {
                    restoreAliasHttpRequest(aliasId, this@ManageAliasActivity)
                }
            }
            .show()
    }

    private lateinit var deleteAliasSnackbar: Snackbar
    private fun deleteAlias() {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Catalog_MaterialAlertDialog_Centered_FullWidthButtons)
            .setTitle(resources.getString(R.string.delete_alias))
            .setIcon(R.drawable.ic_trash)
            .setMessage(resources.getString(R.string.delete_alias_confirmation_desc))
            .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ ->
                deleteAliasSnackbar = SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.deleting_alias),
                    binding.activityManageAliasCL,
                    length = Snackbar.LENGTH_INDEFINITE
                )
                deleteAliasSnackbar.show()
                lifecycleScope.launch {
                    deleteAliasHttpRequest(aliasId, this@ManageAliasActivity)
                }
            }
            .show()
    }

    private lateinit var forgetAliasSnackbar: Snackbar
    private fun forgetAlias() {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Catalog_MaterialAlertDialog_Centered_FullWidthButtons)
            .setTitle(resources.getString(R.string.forget_alias))
            .setIcon(R.drawable.ic_eraser)
            .setMessage(resources.getString(R.string.forget_alias_confirmation_desc))
            .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(resources.getString(R.string.forget)) { _, _ ->
                forgetAliasSnackbar = SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.forgetting_alias),
                    binding.activityManageAliasCL,
                    length = Snackbar.LENGTH_INDEFINITE
                )
                forgetAliasSnackbar.show()
                lifecycleScope.launch {
                    forgetAliasHttpRequest(aliasId, this@ManageAliasActivity)
                }
            }
            .show()
    }

    private suspend fun deleteAliasHttpRequest(id: String, context: Context) {
        networkHelper.deleteAlias({ result ->
            if (result == "204") {
                deleteAliasSnackbar.dismiss()
                shouldUpdate = true
                finishWithUpdate()
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_deleting_alias), result
                    ),
                    binding.activityManageAliasCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, id)
    }

    private suspend fun forgetAliasHttpRequest(id: String, context: Context) {
        networkHelper.forgetAlias({ result ->
            if (result == "204") {
                forgetAliasSnackbar.dismiss()
                shouldUpdate = true
                finishWithUpdate()
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_forgetting_alias), result
                    ),
                    binding.activityManageAliasCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, id)
    }

    private fun finishWithUpdate() {
        val intent = Intent()
        intent.putExtra("should_update", shouldUpdate)
        setResult(RESULT_OK, intent)
        finish()
    }

    private suspend fun restoreAliasHttpRequest(id: String, context: Context) {
        networkHelper.restoreAlias({ result ->
            if (result == "200") {
                restoreAliasSnackbar.dismiss()
                shouldUpdate = true
                setPage()
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_restoring_alias), result
                    ),
                    binding.activityManageAliasCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, id)
    }

    private suspend fun getAliasInfo(id: String) {
        networkHelper.getSpecificAlias({ alias, error ->

            if (alias != null) {
                this.alias = alias

                // Set email in textview
                binding.activityManageAliasEmail.text = alias.email
                binding.activityManageAliasEmail.animate().alpha(1.0f)

                editAliasSendMailRecipientBottomDialogFragment = EditAliasSendMailRecipientBottomDialogFragment.newInstance(alias.email)

                /**
                 * CHART
                 */

                // Update chart
                setChart(
                    alias.emails_forwarded.toFloat(),
                    alias.emails_replied.toFloat(),
                    alias.emails_blocked.toFloat(),
                    alias.emails_sent.toFloat()
                )

                /**
                 *  SWITCH STATUS
                 */

                // Set switch status
                binding.activityManageAliasActiveSwitchLayout.setSwitchChecked(alias.active)
                binding.activityManageAliasActiveSwitchLayout.setTitle(
                    if (alias.active) resources.getString(R.string.alias_activated) else resources.getString(
                        R.string.alias_deactivated
                    )
                )

                // Set watch switch status
                binding.activityManageAliasWatchSwitchLayout.setSwitchChecked(aliasWatcher.getAliasesToWatch()?.contains(aliasId) ?: false)


                /**
                 * LAYOUT
                 */

                // This layout only contains SectionViews
                val layout =
                    findViewById<View>(R.id.activity_manage_alias_settings_LL1) as LinearLayout
                if (alias.deleted_at != null) {
                    // Aliasdeleted is not null, thus deleted. disable all the layouts and alpha them

                    // Show restore and hide delete
                    binding.activityManageAliasRestore.visibility = View.VISIBLE
                    binding.activityManageAliasForget.visibility = View.VISIBLE
                    binding.activityManageAliasDelete.visibility = View.GONE
                    for (i in 0 until layout.childCount) {
                        val child = layout.getChildAt(i)

                        // As the childs are only sections, cast and set enabled state
                        // Do not disable the restore button. So disabled everything except the activity_manage_alias_restore
                        if (child.id != R.id.activity_manage_alias_restore && child.id != R.id.activity_manage_alias_forget) {
                            (child as SectionView).setLayoutEnabled(false)
                        }
                    }
                } else {
                    // Show delete and hide restore
                    binding.activityManageAliasRestore.visibility = View.GONE
                    binding.activityManageAliasDelete.visibility = View.VISIBLE
                    binding.activityManageAliasForget.visibility = View.VISIBLE

                    // As the childs are only sections, cast and set enabled state
                    // Aliasdeleted is null, thus not deleted. enable all the layouts
                    for (i in 0 until layout.childCount) {
                        val child = layout.getChildAt(i)

                        (child as SectionView).setLayoutEnabled(true)
                    }
                }

                /**
                 * RECIPIENTS
                 */

                // Set recipients
                var recipients: String
                var count = 0
                if (alias.recipients != null && alias.recipients.isNotEmpty()) {
                    // get the first 2 recipients and list them

                    val buf = StringBuilder()
                    for (recipient in alias.recipients) {
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
                    if (alias.recipients.size > 2) {
                        // If this is the case add a "x more" on the third rule
                        // X is the total amount minus the 2 listed above
                        recipients += "\n"
                        recipients += this.resources.getString(
                            R.string._more,
                            alias.recipients.size - 2
                        )
                    }
                } else {
                    recipients = this.resources.getString(
                        R.string.default_recipient
                    )
                }

                binding.activityManageAliasRecipientsEdit.setDescription(recipients)


                // Initialise the bottomdialog
                editAliasRecipientsBottomDialogFragment =
                    EditAliasRecipientsBottomDialogFragment.newInstance(aliasId, alias.recipients)


                // Set created at and updated at
                DateTimeUtils.turnStringIntoLocalString(alias.created_at)?.let { binding.activityManageAliasCreatedAt.setDescription(it) }
                DateTimeUtils.turnStringIntoLocalString(alias.updated_at)?.let { binding.activityManageAliasUpdatedAt.setDescription(it) }


                /**
                 * DESCRIPTION
                 */

                // Set description and initialise the bottomDialogFragment
                if (alias.description != null) {
                    binding.activityManageAliasDescEdit.setDescription(alias.description)
                } else {
                    binding.activityManageAliasDescEdit.setDescription(
                        this.resources.getString(
                            R.string.alias_no_description
                        )
                    )
                }

                // reset this value as it now includes the description
                editAliasDescriptionBottomDialogFragment = EditAliasDescriptionBottomDialogFragment.newInstance(
                    id,
                    alias.description
                )


                binding.activityManageAliasSettingsRLProgressbar.visibility = View.GONE
                progressBarVisibility = View.GONE
                binding.activityManageAliasSettingsLL.visibility = View.VISIBLE

                setOnSwitchChangeListeners()
                setOnClickListeners()

                // Is set true by the intent action, do this after the switchchangelistener is set.
                if (shouldDeactivateThisAlias) {
                    // Deactive switch
                    forceSwitch = true
                    binding.activityManageAliasActiveSwitchLayout.setSwitchChecked(false)
                }
            } else {

                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_obtaining_alias) + "\n" + error,
                    binding.activityManageAliasCL
                ).show()

                binding.activityManageAliasSettingsRLProgressbar.visibility = View.GONE
                progressBarVisibility = View.GONE
                binding.activityManageAliasSettingsLL.visibility = View.GONE

                // Show no internet animations
                binding.activityManageAliasSettingsRLLottieview.visibility = View.VISIBLE
            }


        }, id)
    }


    override fun descriptionEdited(description: String) {
        setPage()
        shouldUpdate = true
        editAliasDescriptionBottomDialogFragment.dismissAllowingStateLoss()
    }


    override fun recipientsEdited() {
        // Reload all info
        setPage()

        // This changes the last updated time of the alias which is being shown in the recyclerview in the aliasFragment.
        // So we update the list when coming back
        shouldUpdate = true
        editAliasRecipientsBottomDialogFragment.dismissAllowingStateLoss()
    }


    override fun onPressSend(toString: String) {
        // Get recipients
        val recipients = alias?.let { getSendAddress(toString, it) }

        // In case some email apps do not receive EXTRA_EMAIL properly. Copy the email addresses to clipboard as well
        val clipboard: ClipboardManager =
            this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("recipients", recipients?.joinToString(";"))
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, this.resources.getString(R.string.copied_recipients), Toast.LENGTH_LONG).show()

        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, recipients)
        if (intent.resolveActivity(packageManager) != null) {
            AnonAddyUtils.startShareSheetActivityExcludingOwnApp(this, intent, this.resources.getString(R.string.send_mail))
        }
        editAliasSendMailRecipientBottomDialogFragment.dismissAllowingStateLoss()
    }
}