package host.stjin.anonaddy.ui.appsettings.wearos

import android.os.Bundle
import android.widget.CompoundButton
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAppSettingsWearosBinding
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy.utils.WearOSHelper
import host.stjin.anonaddy_shared.managers.SettingsManager


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

        loadNodes()
        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()
    }

    private val listOfNodes: ArrayList<Node> = arrayListOf()
    private var nodeClient: NodeClient? = null
    private fun loadNodes() {
        nodeClient = Wearable.getNodeClient(this)
        nodeClient!!.connectedNodes.addOnCompleteListener { nodes ->
            listOfNodes.clear()
            // Send a message to all connected nodes basically broadcasting itself.
            // Nodes with the app installed will receive this message and open the setup sheet
            for (node in nodes.result) {
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

                val materialDialog = MaterialAlertDialogBuilder(
                    this@AppSettingsWearOSActivity,
                    R.style.ThemeOverlay_Catalog_MaterialAlertDialog_Centered_FullWidthButtons
                )
                    .setTitle(resources.getString(R.string.select_wearable_device))
                    .setIcon(R.drawable.ic_device_watch)
                    .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }

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
        nodeClient?.localNode?.addOnCompleteListener { localnode ->
            val node = settingsManager.getSettingsString(SettingsManager.PREFS.SELECTED_WEAROS_DEVICE)
            if (node != null) {
                Wearable.getMessageClient(this).sendMessage(
                    node,
                    "/start",
                    localnode.result.displayName.toByteArray()
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
        nodeClient?.localNode?.addOnCompleteListener { localNode ->
            val node = settingsManager.getSettingsString(SettingsManager.PREFS.SELECTED_WEAROS_DEVICE)
            if (node != null) {
                Wearable.getMessageClient(this).sendMessage(
                    node,
                    "/reset",
                    localNode.result.displayName.toByteArray()
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