package host.stjin.anonaddy.ui.alias.manage

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import app.futured.donut.DonutSection
import com.google.android.gms.wearable.Wearable
import com.google.android.material.snackbar.Snackbar
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityManageAliasBinding
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.AnonAddyUtils
import host.stjin.anonaddy.utils.AnonAddyUtils.getSendAddress
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.SUBSCRIPTIONS
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils


class ManageAliasActivity : BaseActivity(),
    EditAliasDescriptionBottomDialogFragment.AddEditAliasDescriptionBottomDialogListener,
    EditAliasFromNameBottomDialogFragment.AddEditAliasFromNameBottomDialogListener,
    EditAliasRecipientsBottomDialogFragment.AddEditAliasRecipientsBottomDialogListener,
    EditAliasSendMailRecipientBottomDialogFragment.AddEditAliasSendMailRecipientBottomDialogListener {

    lateinit var networkHelper: NetworkHelper
    private lateinit var aliasWatcher: AliasWatcher
    private var shouldRefreshOnFinish = false

    private lateinit var editAliasDescriptionBottomDialogFragment: EditAliasDescriptionBottomDialogFragment
    private lateinit var editAliasFromNameBottomDialogFragment: EditAliasFromNameBottomDialogFragment
    private lateinit var editAliasRecipientsBottomDialogFragment: EditAliasRecipientsBottomDialogFragment
    private lateinit var editAliasSendMailRecipientBottomDialogFragment: EditAliasSendMailRecipientBottomDialogFragment

    private var alias: Aliases? = null
        set(value) {
            field = value
            value?.let { updateUi(it) }
        }
    private var forceSwitch = false
    private var shouldDeactivateThisAlias = false


    // This value is here to keep track if the activity to which we return on finishWithUpdate should update its data.
    // Basically, whenever some information is changed we flip the boolean to true.
    private lateinit var binding: ActivityManageAliasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageAliasBinding.inflate(layoutInflater)
        val view = binding.root
        // Since this activity can be directly launched, set the dark mode.
        checkForDarkModeAndSetFlags()
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activityManageAliasNSVLL)
        )

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

            // Used in ActionReceiver
            shouldDeactivateThisAlias = b.getBoolean("shouldDeactivateThisAlias", false)

            if (aliasId == null) {
                finish()
                return
            }
            setPage(aliasId)
        } else if (intent.action != null) {
            // /deactivate URI's
            val data: Uri? = intent?.data
            if (data.toString().contains("/deactivate")) {
                val aliasId = StringUtils.substringBetween(data.toString(), "deactivate/", "?")
                shouldDeactivateThisAlias = true
                setPage(aliasId)
            }
        }
    }


    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(
            view.width, view.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }


    private fun addAliasAsShortcut() {
        val encryptedSettingsManager = SettingsManager(true, this)
        if (!encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.PRIVACY_MODE)) {

            // Only add shortcuts when PRIVACY_MODE is disabled to hide aliases
            val intent = Intent(Intent.ACTION_MAIN, Uri.EMPTY, this, ManageAliasActivity::class.java)
            // Pass data object in the bundle and populate details activity.
            intent.putExtra("alias_id", alias!!.id)

            val bitmap = getBitmapFromView(binding.activityManageAliasChart)

            val shortcut = ShortcutInfoCompat.Builder(this, alias!!.id)
                .setShortLabel(alias!!.email)
                .setLongLabel(alias!!.email)
                .setIcon(IconCompat.createWithBitmap(bitmap))
                .setIntent(
                    intent
                ).build()


            try {
                ShortcutManagerCompat.getDynamicShortcuts(this).also { shortcuts ->
                    val maxShortcutsCount = ShortcutManagerCompat.getMaxShortcutCountPerActivity(this)
                    if (shortcuts.count() == maxShortcutsCount) {
                        shortcuts.removeLastOrNull()
                        shortcuts.add(0, shortcut)
                        ShortcutManagerCompat.setDynamicShortcuts(this, shortcuts)
                    } else {
                        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
                    }
                }
            } catch (exception: Throwable) {
                ShortcutManagerCompat.removeAllDynamicShortcuts(this)
            }
        }
    }

    private fun setPage(aliasId: String) {
        /**
         * This activity can be called by an URI or Widget/Notification Intent.
         * Protect this part
         */
        lifecycleScope.launch {
            isAuthenticated { isAuthenticated ->
                if (isAuthenticated) {
                    setPageInfo(aliasId)
                }
            }
        }

    }

    private fun setPageInfo(aliasId: String) {
        // Get the alias
        lifecycleScope.launch {
            getAliasInfo(aliasId)
            loadNodes()
        }
    }

    private fun loadNodes() {
        if (BuildConfig.FLAVOR == "gplay") {
            try {
                // TODO Maybe add option menu when multiple wearables are connected
                val nodeClient = Wearable.getNodeClient(this)
                nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                    // Send a message to all connected nodes
                    // Nodes with the app installed will receive this message and open the ManageAliasActivity
                    if (nodes.any()) {
                        if (this@ManageAliasActivity.alias != null) {
                            toolbarSetAction(binding.activityManageAliasToolbar, R.drawable.ic_send_to_device_watch) {
                                for (node in nodes) {
                                    Wearable.getMessageClient(this)
                                        .sendMessage(node.id, "/showAlias", this@ManageAliasActivity.alias!!.id.toByteArray())
                                }
                                SnackbarHelper.createSnackbar(
                                    this,
                                    this.resources.getString(R.string.check_your_wearable),
                                    binding.activityManageAliasCL
                                ).show()
                            }
                        }

                    }
                }
            } catch (ex: Exception) {
                LoggingHelper(this).addLog(LOGIMPORTANCE.WARNING.int, ex.toString(), "loadNodes", null)
            }
        }
    }

    private fun setChart(forwarded: Float, replied: Float, blocked: Float, sent: Float) {
        val listOfDonutSection: ArrayList<DonutSection> = arrayListOf()
        // DONUT
        val section1 = DonutSection(
            name = binding.activityManageAliasChart.context.resources.getString(R.string.d_forwarded, forwarded.toInt()),
            color = ContextCompat.getColor(this, R.color.portalOrange),
            amount = forwarded
        )
        // Always show section 1
        listOfDonutSection.add(section1)

        if (replied > 0) {
            val section2 = DonutSection(
                name = binding.activityManageAliasChart.context.resources.getString(R.string.d_replied, replied.toInt()),
                color = ContextCompat.getColor(this, R.color.portalBlue),
                amount = replied
            )
            listOfDonutSection.add(section2)
        }

        if (sent > 0) {
            val section3 = DonutSection(
                name = binding.activityManageAliasChart.context.resources.getString(R.string.d_sent, sent.toInt()),
                color = ContextCompat.getColor(this, R.color.easternBlue),
                amount = sent
            )
            listOfDonutSection.add(section3)
        }

        if (blocked > 0) {
            val section4 = DonutSection(
                name = binding.activityManageAliasChart.context.resources.getString(R.string.d_blocked, blocked.toInt()),
                color = ContextCompat.getColor(this, R.color.softRed),
                amount = blocked
            )
            listOfDonutSection.add(section4)
        }
        binding.activityManageAliasChart.cap = listOfDonutSection.sumOf { it.amount.toInt() }.toFloat()

        // Sort the list by amount so that the biggest number will fill the whole ring
        binding.activityManageAliasChart.submitData(listOfDonutSection.sortedBy { it.amount })
        // DONUT

        binding.activityManageAliasForwardedCount.text = this.resources.getString(R.string.d_forwarded, forwarded.toInt())
        binding.activityManageAliasRepliesBlockedCount.text = this.resources.getString(R.string.d_blocked, blocked.toInt())
        binding.activityManageAliasSentCount.text = this.resources.getString(R.string.d_sent, sent.toInt())
        binding.activityManageAliasRepliedCount.text = this.resources.getString(R.string.d_replied, replied.toInt())
    }

    private fun setOnSwitchChangeListeners() {
        binding.activityManageAliasGeneralActions.activityManageAliasActiveSwitchLayout.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    binding.activityManageAliasGeneralActions.activityManageAliasActiveSwitchLayout.showProgressBar(true)
                    forceSwitch = false
                    shouldRefreshOnFinish = true
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

        binding.activityManageAliasGeneralActions.activityManageAliasWatchSwitchLayout.setOnSwitchCheckedChangedListener(object :
            SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    forceSwitch = false
                    shouldRefreshOnFinish = true
                    if (checked) {
                        // In case the alias could not be added to watchlist, the switch will be reverted
                        binding.activityManageAliasGeneralActions.activityManageAliasWatchSwitchLayout.setSwitchChecked(
                            aliasWatcher.addAliasToWatch(
                                this@ManageAliasActivity.alias!!.id
                            )
                        )

                    } else {
                        aliasWatcher.removeAliasToWatch(this@ManageAliasActivity.alias!!.id)
                    }
                }
            }
        })
    }

    override fun finish() {
        val resultIntent = Intent()
        resultIntent.putExtra("shouldRefresh", shouldRefreshOnFinish)
        setResult(RESULT_OK, resultIntent)
        super.finish()
    }

    private suspend fun deactivateAlias() {
        networkHelper.deactivateSpecificAlias({ result ->
            binding.activityManageAliasGeneralActions.activityManageAliasActiveSwitchLayout.showProgressBar(false)
            if (result == "204") {
                this.alias!!.active = false
                shouldRefreshOnFinish = true
                updateUi(this.alias!!)

                if (shouldDeactivateThisAlias) {
                    shouldDeactivateThisAlias = false
                    SnackbarHelper.createSnackbar(
                        this,
                        this.resources.getString(R.string.alias_deactivated),
                        binding.activityManageAliasCL
                    ).show()
                }

            } else {
                binding.activityManageAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(true)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    binding.activityManageAliasCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this@ManageAliasActivity.alias!!.id)
    }


    private suspend fun activateAlias() {
        networkHelper.activateSpecificAlias({ alias, result ->
            binding.activityManageAliasGeneralActions.activityManageAliasActiveSwitchLayout.showProgressBar(false)
            if (alias != null) {
                this.alias = alias
                shouldRefreshOnFinish = true
            } else {
                binding.activityManageAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(false)
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_edit_active) + "\n" + result,
                    binding.activityManageAliasCL,
                    LoggingHelper.LOGFILES.DEFAULT
                ).show()
            }
        }, this@ManageAliasActivity.alias!!.id)
    }


    private fun setOnClickListeners() {
        binding.activityManageAliasGeneralActions.activityManageAliasActiveSwitchLayout.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityManageAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(!binding.activityManageAliasGeneralActions.activityManageAliasActiveSwitchLayout.getSwitchChecked())
            }
        })


        binding.activityManageAliasGeneralActions.activityManageAliasWatchSwitchLayout.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityManageAliasGeneralActions.activityManageAliasWatchSwitchLayout.setSwitchChecked(!binding.activityManageAliasGeneralActions.activityManageAliasWatchSwitchLayout.getSwitchChecked())
            }
        })

        binding.activityManageAliasGeneralActions.activityManageAliasDescEdit.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editAliasDescriptionBottomDialogFragment.isAdded) {
                    editAliasDescriptionBottomDialogFragment.show(
                        supportFragmentManager,
                        "editAliasDescriptionBottomDialogFragment"
                    )
                }
            }
        })

        binding.activityManageAliasGeneralActions.activityManageAliasRecipientsEdit.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editAliasRecipientsBottomDialogFragment.isAdded) {
                    editAliasRecipientsBottomDialogFragment.show(
                        supportFragmentManager,
                        "editAliasRecipientsBottomDialogFragment"
                    )
                }
            }
        })

        binding.activityManageAliasGeneralActions.activityManageAliasFromNameEdit.setOnLayoutClickedListener(object :
            SectionView.OnLayoutClickedListener {
            override fun onClick() {
                if (!editAliasFromNameBottomDialogFragment.isAdded) {
                    editAliasFromNameBottomDialogFragment.show(
                        supportFragmentManager,
                        "editAliasFromNameBottomDialogFragment"
                    )
                }
            }
        })

        binding.activityManageAliasGeneralActions.activityManageAliasDelete.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                deleteAlias()
            }
        })

        binding.activityManageAliasGeneralActions.activityManageAliasForget.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forgetAlias()
            }
        })

        binding.activityManageAliasGeneralActions.activityManageAliasRestore.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
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
        MaterialDialogHelper.aliasRestoreDialog(
            context = this
        ) {
            restoreAliasSnackbar = SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.restoring_alias),
                binding.activityManageAliasCL,
                length = Snackbar.LENGTH_INDEFINITE
            )
            restoreAliasSnackbar.show()
            lifecycleScope.launch {
                restoreAliasHttpRequest(this@ManageAliasActivity.alias!!.id, this@ManageAliasActivity)
            }
        }
    }

    private lateinit var deleteAliasSnackbar: Snackbar
    private fun deleteAlias() {
        MaterialDialogHelper.aliasDeleteDialog(
            context = this
        ) {
            deleteAliasSnackbar = SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.deleting_alias),
                binding.activityManageAliasCL,
                length = Snackbar.LENGTH_INDEFINITE
            )
            deleteAliasSnackbar.show()
            lifecycleScope.launch {
                deleteAliasHttpRequest(this@ManageAliasActivity.alias!!.id, this@ManageAliasActivity)
            }
        }
    }

    private lateinit var forgetAliasSnackbar: Snackbar
    private fun forgetAlias() {
        MaterialDialogHelper.aliasForgetDialog(
            context = this
        ) {
            forgetAliasSnackbar = SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.forgetting_alias),
                binding.activityManageAliasCL,
                length = Snackbar.LENGTH_INDEFINITE
            )
            forgetAliasSnackbar.show()
            lifecycleScope.launch {
                forgetAliasHttpRequest(this@ManageAliasActivity.alias!!.id, this@ManageAliasActivity)
            }
        }
    }

    private suspend fun deleteAliasHttpRequest(id: String, context: Context) {
        networkHelper.deleteAlias({ result ->
            if (result == "204") {
                deleteAliasSnackbar.dismiss()
                shouldRefreshOnFinish = true
                finish()
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
                shouldRefreshOnFinish = true
                finish()
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

    private suspend fun restoreAliasHttpRequest(id: String, context: Context) {
        networkHelper.restoreAlias({ alias, error ->
            if (alias != null) {
                restoreAliasSnackbar.dismiss()
                shouldRefreshOnFinish = true
                this.alias = alias
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    context.resources.getString(
                        R.string.s_s,
                        context.resources.getString(R.string.error_restoring_alias), error
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
                // Triggers updateUi
                this.alias = alias
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.error_obtaining_alias) + "\n" + error,
                    binding.activityManageAliasCL
                ).show()

                // Show error animations
                binding.activityManageAliasSettingsLL.visibility = View.GONE
                binding.animationFragment.playAnimation(false, R.drawable.ic_loading_logo_error)
            }
        }, id)
    }

    private fun updateUi(alias: Aliases) {


        // Set the AliasShortcut here, to make sure the donut is rendered
        Handler(Looper.getMainLooper()).postDelayed({
            // Unauthenticated, clear settings
            addAliasAsShortcut()
        }, binding.activityManageAliasChart.animationDurationMs)

        // Set email in textview
        binding.activityManageAliasEmail.text = alias.email

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
        binding.activityManageAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(alias.active)
        binding.activityManageAliasGeneralActions.activityManageAliasActiveSwitchLayout.setTitle(
            if (alias.active) resources.getString(R.string.alias_activated) else resources.getString(
                R.string.alias_deactivated
            )
        )

        // Set watch switch status
        binding.activityManageAliasGeneralActions.activityManageAliasWatchSwitchLayout.setSwitchChecked(
            aliasWatcher.getAliasesToWatch().contains(this@ManageAliasActivity.alias!!.id)
        )


        /**
         * LAYOUT
         */

        // This layout only contains SectionViews
        val layout =
            findViewById<View>(R.id.activity_manage_alias_general_actions) as LinearLayout
        if (alias.deleted_at != null) {
            // Aliasdeleted is not null, thus deleted. disable all the layouts and alpha them

            // Show restore and hide delete
            binding.activityManageAliasGeneralActions.activityManageAliasRestore.visibility = View.VISIBLE
            binding.activityManageAliasGeneralActions.activityManageAliasForget.visibility = View.VISIBLE
            binding.activityManageAliasGeneralActions.activityManageAliasDelete.visibility = View.GONE
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
            binding.activityManageAliasGeneralActions.activityManageAliasRestore.visibility = View.GONE
            binding.activityManageAliasGeneralActions.activityManageAliasDelete.visibility = View.VISIBLE
            binding.activityManageAliasGeneralActions.activityManageAliasForget.visibility = View.VISIBLE

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
        if (alias.recipients != null && alias.recipients!!.isNotEmpty()) {
            // get the first 2 recipients and list them

            val buf = StringBuilder()
            for (recipient in alias.recipients!!) {
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
            if (alias.recipients!!.size > 2) {
                // If this is the case add a "x more" on the third rule
                // X is the total amount minus the 2 listed above
                recipients += "\n"
                recipients += this.resources.getString(
                    R.string._more,
                    alias.recipients!!.size - 2
                )
            }
        } else {
            recipients = this.resources.getString(
                R.string.default_recipient
            )
        }

        binding.activityManageAliasGeneralActions.activityManageAliasRecipientsEdit.setDescription(recipients)


        // Initialise the bottomdialog
        editAliasRecipientsBottomDialogFragment =
            EditAliasRecipientsBottomDialogFragment.newInstance(this@ManageAliasActivity.alias!!.id, alias.recipients)


        // Set created at and updated at
        DateTimeUtils.turnStringIntoLocalString(alias.created_at)
            ?.let { binding.activityManageAliasGeneralActions.activityManageAliasCreatedAt.setDescription(it) }
        DateTimeUtils.turnStringIntoLocalString(alias.updated_at)
            ?.let { binding.activityManageAliasGeneralActions.activityManageAliasUpdatedAt.setDescription(it) }


        /**
         * DESCRIPTION
         */

        // Set description and initialise the bottomDialogFragment
        if (alias.description != null) {
            binding.activityManageAliasGeneralActions.activityManageAliasDescEdit.setDescription(alias.description)
        } else {
            binding.activityManageAliasGeneralActions.activityManageAliasDescEdit.setDescription(
                this.resources.getString(
                    R.string.alias_no_description
                )
            )
        }

        // reset this value as it now includes the description
        editAliasDescriptionBottomDialogFragment = EditAliasDescriptionBottomDialogFragment.newInstance(
            alias.id,
            alias.description
        )


        /**
         * FROM NAME
         */


        // Not available for free subscriptions
        if ((this.application as AddyIoApp).userResource.subscription == SUBSCRIPTIONS.FREE.subscription) {
            binding.activityManageAliasGeneralActions.activityManageAliasFromNameEdit.setLayoutEnabled(false)
            binding.activityManageAliasGeneralActions.activityManageAliasFromNameEdit.setDescription(
                this.resources.getString(
                    R.string.feature_not_available_subscription
                )
            )
        } else {
            // Set description and initialise the bottomDialogFragment
            if (alias.from_name != null) {
                binding.activityManageAliasGeneralActions.activityManageAliasFromNameEdit.setDescription(alias.from_name)
            } else {
                binding.activityManageAliasGeneralActions.activityManageAliasFromNameEdit.setDescription(
                    this.resources.getString(
                        R.string.alias_no_from_name
                    )
                )
            }

            // reset this value as it now includes the description
            editAliasFromNameBottomDialogFragment = EditAliasFromNameBottomDialogFragment.newInstance(
                alias.id,
                alias.email,
                alias.from_name
            )


        }

        binding.animationFragment.stopAnimation()
        binding.activityManageAliasNSV.animate().alpha(1.0f)
        binding.activityManageAliasSettingsLL.visibility = View.VISIBLE

        setOnSwitchChangeListeners()
        setOnClickListeners()

        // Is set true by the intent action, do this after the switchchangelistener is set.
        if (shouldDeactivateThisAlias) {
            // Deactive switch
            forceSwitch = true
            binding.activityManageAliasGeneralActions.activityManageAliasActiveSwitchLayout.setSwitchChecked(false)
        }
    }


    override fun descriptionEdited(alias: Aliases) {
        shouldRefreshOnFinish = true
        editAliasDescriptionBottomDialogFragment.dismissAllowingStateLoss()

        // Do this last, will trigger updateUI as well as re-init editAliasDescriptionBottomDialogFragment
        this.alias = alias
    }

    override fun fromNameEdited(alias: Aliases) {
        shouldRefreshOnFinish = true
        editAliasFromNameBottomDialogFragment.dismissAllowingStateLoss()

        // Do this last, will trigger updateUI as well as re-init editAliasFromNameBottomDialogFragment
        this.alias = alias
    }


    override fun recipientsEdited(alias: Aliases) {
        // This changes the last updated time of the alias which is being shown in the recyclerview in the aliasFragment.
        // So we update the list when coming back
        shouldRefreshOnFinish = true
        editAliasRecipientsBottomDialogFragment.dismissAllowingStateLoss()

        // Do this last, will trigger updateUI as well as re-init editAliasDescriptionBottomDialogFragment
        this.alias = alias
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