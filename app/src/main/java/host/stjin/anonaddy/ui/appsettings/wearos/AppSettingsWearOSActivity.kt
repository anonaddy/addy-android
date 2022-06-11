package host.stjin.anonaddy.ui.appsettings.wearos

import android.content.Intent
import android.os.Bundle
import android.widget.CompoundButton
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAppSettingsWearosBinding
import host.stjin.anonaddy.ui.appsettings.logs.LogViewerActivity
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy.utils.WearOSHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.utils.LoggingHelper


class AppSettingsWearOSActivity : BaseActivity() {

    private var forceSwitch = false
    private lateinit var settingsManager: SettingsManager
    private lateinit var binding: ActivityAppSettingsWearosBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsWearosBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.appsettingsWearosNSVLL)
        )

        settingsManager = SettingsManager(false, this)

        setupToolbar(
            R.string.anonaddy_for_wearables,
            binding.appsettingsWearosNSV,
            binding.appsettingsWearosToolbar,
            R.drawable.ic_device_watch
        )

        checkIfApiIsAvailable()
        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
    }

    private fun checkIfApiIsAvailable() {
        nodeClient = Wearable.getNodeClient(this)
        nodeClient!!.connectedNodes.addOnSuccessListener {
            // nodes available, so reload the nodes
            loadNodes()
        }.addOnFailureListener {
            MaterialDialogHelper.showMaterialDialog(
                context = this,
                title = resources.getString(R.string.wearable_api_not_available),
                message = resources.getString(R.string.wearable_api_not_available_desc),
                icon = R.drawable.ic_brand_google_play,
                positiveButtonText = resources.getString(R.string.i_understand),
                positiveButtonAction = {
                    finish()
                }
            ).setCancelable(false).show()
        }
    }

    private val listOfNodes: ArrayList<Node> = arrayListOf()
    private var nodeClient: NodeClient? = null
    private fun loadNodes() {
        nodeClient!!.connectedNodes.addOnSuccessListener { nodes ->
            listOfNodes.clear()
            for (node in nodes) {
                listOfNodes.add(node)
            }

            // Node array has been filled with data, so reload the data
            loadSettings()
        }
    }

    private fun setOnSwitchListeners() {
        binding.activityAppSettingsWearosSectionQuickSetup.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                // Using forceswitch can toggle onCheckedChangeListener programmatically without having to press the actual switch
                if (compoundButton.isPressed || forceSwitch) {
                    forceSwitch = false
                    settingsManager.putSettingsBool(SettingsManager.PREFS.DISABLE_WEAROS_QUICK_SETUP_DIALOG, !checked)
                }
            }
        })
    }

    private fun loadSettings() {
        // Nothing to load
        binding.activityAppSettingsWearosSectionQuickSetup.setSwitchChecked(
            !settingsManager.getSettingsBool(
                SettingsManager.PREFS.DISABLE_WEAROS_QUICK_SETUP_DIALOG
            )
        )

        binding.activityAppSettingsWearosSectionStart.setLayoutEnabled(listOfNodes.any())
        binding.activityAppSettingsWearosSectionSetup.setLayoutEnabled(listOfNodes.any())
        binding.activityAppSettingsWearosSectionReset.setLayoutEnabled(listOfNodes.any())
        binding.activityAppSettingsWearosSectionShowLogs.setLayoutEnabled(listOfNodes.any())

        if (listOfNodes.any()) {
            val selectedNode = listOfNodes.find { it.id == settingsManager.getSettingsString(SettingsManager.PREFS.SELECTED_WEAROS_DEVICE) }
            if (selectedNode != null) {
                binding.activityAppSettingsWearosSectionSelectDevice.setDescription(selectedNode.displayName)
            } else {
                // The previously selected node is not available anymore, set the first node as selected now and reload.
                settingsManager.putSettingsString(SettingsManager.PREFS.SELECTED_WEAROS_DEVICE, listOfNodes[0].id)
                loadSettings()
            }
        } else {
            binding.activityAppSettingsWearosSectionSelectDevice.setDescription(this.resources.getString(R.string.no_wearable_devices_available))
        }
    }


    // If the user comes back from eg. settings re-check for node changes
    override fun onResume() {
        super.onResume()
        loadSettings()
        loadNodes()
    }

    private fun setOnClickListeners() {
        binding.activityAppSettingsWearosSectionSelectDevice.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val nodeListItems = arrayListOf<CharSequence>()
                listOfNodes.forEach { nodeListItems.add(it.displayName) }
                val nodeListItemsCS: Array<CharSequence> = nodeListItems.toArray(arrayOfNulls<CharSequence>(nodeListItems.size))


                val materialDialog = MaterialDialogHelper.showMaterialDialog(
                    context = this@AppSettingsWearOSActivity,
                    title = resources.getString(R.string.select_wearable_device),
                    icon = R.drawable.ic_device_watch,
                    neutralButtonText = resources.getString(R.string.cancel),
                )

                if (listOfNodes.any()) {
                    materialDialog.setSingleChoiceItems(
                        nodeListItemsCS,
                        listOfNodes.indexOfFirst { it.id == settingsManager.getSettingsString(SettingsManager.PREFS.SELECTED_WEAROS_DEVICE) }
                    ) { dialog, which ->
                        settingsManager.putSettingsString(SettingsManager.PREFS.SELECTED_WEAROS_DEVICE, listOfNodes[which].id)
                        loadNodes()
                        dialog.dismiss()
                    }
                } else {
                    materialDialog.setMessage(this@AppSettingsWearOSActivity.resources.getString(R.string.no_wearable_devices_available))
                }
                materialDialog.show()
            }
        })

        binding.activityAppSettingsWearosSectionStart.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                startAppOnWearable()
            }
        })

        binding.activityAppSettingsWearosSectionShowLogs.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                val intent = Intent(this@AppSettingsWearOSActivity, LogViewerActivity::class.java)
                intent.putExtra("logfile", LoggingHelper.LOGFILES.WEAROS_LOGS.filename)
                startActivity(intent)
            }
        })

        binding.activityAppSettingsWearosSectionReset.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                resetAppOnWearable()
            }
        })

        binding.activityAppSettingsWearosSectionSetup.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                setupAppOnWearable()
            }
        })

        binding.activityAppSettingsWearosSectionQuickSetup.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.activityAppSettingsWearosSectionQuickSetup.setSwitchChecked(!binding.activityAppSettingsWearosSectionQuickSetup.getSwitchChecked())
            }
        })
    }

    private fun startAppOnWearable() {
        nodeClient?.localNode?.addOnSuccessListener { localnode ->
            val node = settingsManager.getSettingsString(SettingsManager.PREFS.SELECTED_WEAROS_DEVICE)
            if (node != null) {
                Wearable.getMessageClient(this).sendMessage(
                    node,
                    "/start",
                    localnode.displayName.toByteArray()
                ).addOnSuccessListener {
                    SnackbarHelper.createSnackbar(
                        this,
                        this.resources.getString(R.string.wearable_start_success),
                        binding.appsettingsWearosCL
                    ).show()
                }.addOnCanceledListener {
                    SnackbarHelper.createSnackbar(
                        this,
                        this.resources.getString(R.string.wearable_start_canceled),
                        binding.appsettingsWearosCL
                    ).show()
                }.addOnFailureListener {
                    SnackbarHelper.createSnackbar(
                        this,
                        this.resources.getString(R.string.wearable_start_failed),
                        binding.appsettingsWearosCL
                    ).show()
                }
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.wearable_device_invalid),
                    binding.appsettingsWearosCL
                ).show()
            }
        }
    }

    private fun resetAppOnWearable() {
        nodeClient?.localNode?.addOnSuccessListener { localNode ->
            val node = settingsManager.getSettingsString(SettingsManager.PREFS.SELECTED_WEAROS_DEVICE)
            if (node != null) {
                Wearable.getMessageClient(this).sendMessage(
                    node,
                    "/reset",
                    localNode.displayName.toByteArray()
                ).addOnSuccessListener {
                    SnackbarHelper.createSnackbar(
                        this,
                        this.resources.getString(R.string.wearable_reset_success),
                        binding.appsettingsWearosCL
                    ).show()
                }.addOnCanceledListener {
                    SnackbarHelper.createSnackbar(
                        this,
                        this.resources.getString(R.string.wearable_reset_canceled),
                        binding.appsettingsWearosCL
                    ).show()
                }.addOnFailureListener {
                    SnackbarHelper.createSnackbar(
                        this,
                        this.resources.getString(R.string.wearable_reset_failed),
                        binding.appsettingsWearosCL
                    ).show()
                }
            } else {
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.wearable_device_invalid),
                    binding.appsettingsWearosCL
                ).show()
            }
        }
    }

    private fun setupAppOnWearable() {
        val node = settingsManager.getSettingsString(SettingsManager.PREFS.SELECTED_WEAROS_DEVICE)

        if (node != null) {
            val configuration = Gson().toJson(WearOSHelper(this).createWearOSConfiguration())
            Wearable.getMessageClient(this).sendMessage(
                node,
                "/setup",
                configuration.toByteArray()
            ).addOnSuccessListener {
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.wearable_setup_success),
                    binding.appsettingsWearosCL
                ).show()
            }.addOnCanceledListener {
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.wearable_setup_canceled),
                    binding.appsettingsWearosCL
                ).show()
            }.addOnFailureListener {
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.wearable_setup_failed),
                    binding.appsettingsWearosCL
                ).show()
            }
        } else {
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.wearable_device_invalid),
                binding.appsettingsWearosCL
            ).show()
        }


    }
}