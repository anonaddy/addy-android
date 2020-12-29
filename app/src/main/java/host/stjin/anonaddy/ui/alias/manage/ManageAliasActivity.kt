package host.stjin.anonaddy.ui.alias.manage

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.databinding.ActivityManageAliasBinding
import host.stjin.anonaddy.databinding.AnonaddyCustomDialogBinding
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.DateTimeUtils
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
    private lateinit var aliasWatcher: AliasWatcher

    private lateinit var editAliasDescriptionBottomDialogFragment: EditAliasDescriptionBottomDialogFragment
    private lateinit var editAliasRecipientsBottomDialogFragment: EditAliasRecipientsBottomDialogFragment

    private lateinit var aliasId: String
    private var forceSwitch = false
    private var shouldDeactivateThisAlias = false

    /*
    https://stackoverflow.com/questions/50969390/view-visibility-state-loss-when-resuming-activity-with-previously-started-activi
     */
    private var progressBarVisibility = View.VISIBLE

    // Bug fix
    override fun onResume() {
        super.onResume()
        binding.activityManageAliasSettingsRLProgressbar.visibility = progressBarVisibility
    }

    private lateinit var binding: ActivityManageAliasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageAliasBinding.inflate(layoutInflater)
        val view = binding.root
        // Since this activity can be directly launched, set the dark mode.
        checkForDarkModeAndSetFlags()
        setContentView(view)
        setupToolbar(binding.activityManageAliasToolbar)
        networkHelper = NetworkHelper(this)
        aliasWatcher = AliasWatcher(this)


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

            // For a smooth overview, require the numbers here.
            // Charts will be updated in the background
            setChart(aliasForwardCount, aliasRepliedSentCount)
            // Finish shared elements transition
            ViewCompat.setTransitionName(binding.activityManageAliasChart, aliasId)
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
        /**
         * This activity can be called by an URI or Widget/Notification Intent.
         * Protect this part
         */
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            isAuthenticated { isAuthenticated ->
                if (isAuthenticated) {
                    setPageInfo()
                }
            }
        }

    }

    private fun setPageInfo() {
        binding.activityManageAliasSettingsRLLottieview.visibility = View.GONE

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

        binding.activityManageAliasChart.setDataPoints(
            floatArrayOf(
                value1,
                value2,
                shimmer
            )
        )



        binding.activityManageAliasChart.setCenterColor(R.color.LightDarkMode)

        binding.activityManageAliasForwardedCount.visibility = View.VISIBLE
        binding.activityManageAliasRepliesSentCount.visibility = View.VISIBLE
        binding.activityManageAliasForwardedCount.text = value1.roundToInt().toString()
        binding.activityManageAliasRepliesSentCount.text = value2.roundToInt().toString()
    }

    private fun setOnSwitchChangeListeners() {
        binding.activityManageAliasActiveSwitchLayout.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    binding.activityManageAliasActiveSwitchLayout.showProgressBar(true)
                    forceSwitch = false
                    if (checked) {
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
        })

        binding.activityManageAliasWatchSwitchLayout.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    forceSwitch = false
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
            binding.activityManageAliasActiveSwitchLayout.showProgressBar(false)
            if (result == "200") {
                binding.activityManageAliasActiveSwitchLayout.setTitle(resources.getString(R.string.alias_activated))
            } else {
                binding.activityManageAliasActiveSwitchLayout.setSwitchChecked(false)
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

        binding.activityManageAliasRestore.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                restoreAlias()
            }
        })
    }


    //TODO test
    private lateinit var restoreAliasDialog: AlertDialog
    val anonaddyCustomDialogBinding = AnonaddyCustomDialogBinding.inflate(LayoutInflater.from(this))
    private fun restoreAlias() {
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(binding.root)
        restoreAliasDialog = builder.create()
        restoreAliasDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        anonaddyCustomDialogBinding.dialogTitle.text = resources.getString(R.string.restore_alias)
        anonaddyCustomDialogBinding.dialogText.text =
            resources.getString(R.string.restore_alias_confirmation_desc)
        anonaddyCustomDialogBinding.dialogPositiveButton.text =
            resources.getString(R.string.restore_alias)
        anonaddyCustomDialogBinding.dialogPositiveButton.setOnClickListener {
            anonaddyCustomDialogBinding.dialogProgressbar.visibility = View.VISIBLE
            anonaddyCustomDialogBinding.dialogError.visibility = View.GONE
            anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = false
            anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = false

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                restoreAliasHttpRequest(aliasId, this@ManageAliasActivity)
            }
        }
        anonaddyCustomDialogBinding.dialogNegativeButton.setOnClickListener {
            restoreAliasDialog.dismiss()
        }
        // create and show the alert dialog
        restoreAliasDialog.show()
    }

    //TODO test
    private fun deleteAlias() {
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(binding.root)
        restoreAliasDialog = builder.create()
        restoreAliasDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        anonaddyCustomDialogBinding.dialogTitle.text = resources.getString(R.string.delete_alias)
        anonaddyCustomDialogBinding.dialogText.text =
            resources.getString(R.string.delete_alias_confirmation_desc)
        anonaddyCustomDialogBinding.dialogPositiveButton.text =
            resources.getString(R.string.delete_alias)
        anonaddyCustomDialogBinding.dialogPositiveButton.setOnClickListener {
            anonaddyCustomDialogBinding.dialogProgressbar.visibility = View.VISIBLE
            anonaddyCustomDialogBinding.dialogError.visibility = View.GONE
            anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = false
            anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = false

            GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
                deleteAliasHttpRequest(aliasId, this@ManageAliasActivity)
            }
        }
        anonaddyCustomDialogBinding.dialogNegativeButton.setOnClickListener {
            restoreAliasDialog.dismiss()
        }
        // create and show the alert dialog
        restoreAliasDialog.show()
    }

    //TODO test
    private suspend fun deleteAliasHttpRequest(id: String, context: Context) {
        networkHelper.deleteAlias({ result ->
            if (result == "204") {
                restoreAliasDialog.dismiss()
                finish()
            } else {
                anonaddyCustomDialogBinding.dialogProgressbar.visibility = View.INVISIBLE
                anonaddyCustomDialogBinding.dialogError.visibility = View.VISIBLE
                anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogError.text = context.resources.getString(
                    R.string.s_s,
                    context.resources.getString(R.string.error_deleting_alias), result
                )
            }
        }, id)
    }

    private suspend fun restoreAliasHttpRequest(id: String, context: Context) {
        networkHelper.restoreAlias({ result ->
            if (result == "200") {
                restoreAliasDialog.dismiss()
                setPage()
            } else {
                anonaddyCustomDialogBinding.dialogProgressbar.visibility = View.INVISIBLE
                anonaddyCustomDialogBinding.dialogError.visibility = View.VISIBLE
                anonaddyCustomDialogBinding.dialogNegativeButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogPositiveButton.isEnabled = true
                anonaddyCustomDialogBinding.dialogError.text = context.resources.getString(
                    R.string.s_s,
                    context.resources.getString(R.string.error_restoring_alias), result
                )
            }
        }, id)
    }

    private suspend fun getAliasInfo(id: String) {
        networkHelper.getSpecificAlias({ list ->

            if (list != null) {

                // Set email in textview
                binding.activityManageAliasEmail.text = list.email

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
                binding.activityManageAliasActiveSwitchLayout.setSwitchChecked(list.active)
                binding.activityManageAliasActiveSwitchLayout.setTitle(
                    if (list.active) resources.getString(R.string.alias_activated) else resources.getString(
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
                if (list.deleted_at != null) {
                    // Aliasdeleted is not null, thus deleted. disable all the layouts and alpha them

                    // Show restore and hide delete
                    binding.activityManageAliasRestore.visibility = View.VISIBLE
                    binding.activityManageAliasDelete.visibility = View.GONE
                    for (i in 0 until layout.childCount) {
                        val child = layout.getChildAt(i)

                        // As the childs are only sections, cast and set enabled state
                        // Do not disable the restore button. So disabled everything except the activity_manage_alias_restore
                        if (child.id != R.id.activity_manage_alias_restore) {
                            (child as SectionView).setLayoutEnabled(false)
                        }
                    }
                } else {
                    // Show delete and hide restore
                    binding.activityManageAliasRestore.visibility = View.GONE
                    binding.activityManageAliasDelete.visibility = View.VISIBLE

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

                binding.activityManageAliasRecipientsEdit.setDescription(recipients)


                // Initialise the bottomdialog
                editAliasRecipientsBottomDialogFragment =
                    EditAliasRecipientsBottomDialogFragment.newInstance(aliasId, list.recipients)


                // Set created at and updated at
                DateTimeUtils.turnStringIntoLocalString(list.created_at)?.let { binding.activityManageAliasCreatedAt.setDescription(it) }
                DateTimeUtils.turnStringIntoLocalString(list.updated_at)?.let { binding.activityManageAliasUpdatedAt.setDescription(it) }


                /**
                 * DESCRIPTION
                 */

                // Set description and initialise the bottomDialogFragment
                if (list.description != null) {
                    binding.activityManageAliasDescEdit.setDescription(list.description)
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
                    list.description
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
        editAliasDescriptionBottomDialogFragment.dismiss()
    }


    override fun recipientsEdited() {
        // Reload all info
        setPage()
        editAliasRecipientsBottomDialogFragment.dismiss()
    }
}