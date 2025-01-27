package host.stjin.anonaddy.ui.appsettings

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.wearable.Wearable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.LauncherIconsAdapter
import host.stjin.anonaddy.databinding.BottomsheetUiuxInterfaceBinding
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy_shared.controllers.LauncherIconController
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.LoggingHelper


class UIUXInterfaceBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var listener: AddUIUXInterfaceBottomDialogListener
    private var forceSwitch = false

    private var _binding: BottomsheetUiuxInterfaceBinding? = null
    private lateinit var settingsManager: SettingsManager

    // 1. Defines the listener interface with a method passing back data result.
    interface AddUIUXInterfaceBottomDialogListener {
        fun onDarkModeOff()
        fun onDarkModeOn()
        fun onDarkModeAutomatic()
        fun onApplyDynamicColors()
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetUiuxInterfaceBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = activity as AddUIUXInterfaceBottomDialogListener
        settingsManager = SettingsManager(false, requireContext())

        setOnClickListeners()
        setOnSwitchListeners()
        spinnerChangeListener(requireContext())
        loadSettings()

        when (settingsManager.getSettingsInt(SettingsManager.PREFS.DARK_MODE, -1)) {
            0 -> {
                binding.bsUiuxInterfaceOff.isChecked = true
            }
            1 -> {
                binding.bsUiuxInterfaceOn.isChecked = true
            }
            -1 -> {
                binding.bsUiuxInterfaceAutomatic.isChecked = true
            }
        }

        // 2. Setup a callback when a thesme is selected
        binding.bsUiuxInterfaceOff.setOnClickListener(this)
        binding.bsUiuxInterfaceOn.setOnClickListener(this)
        binding.bsUiuxInterfaceAutomatic.setOnClickListener(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.bsUiuxInterfaceSectionDynamicColors.visibility = View.VISIBLE
        } else {
            binding.bsUiuxInterfaceSectionDynamicColors.visibility = View.GONE
        }

        return root

    }

    private fun spinnerChangeListener(context: Context) {
        binding.bsUiuxInterfaceStartupPageMact.setOnItemClickListener { _, _, _, _ ->
            // Since the alias format changed, check if custom is available
            SettingsManager(false, context).putSettingsString(SettingsManager.PREFS.STARTUP_PAGE, STARTUP_PAGES[STARTUP_PAGES_NAME.indexOf(binding.bsUiuxInterfaceStartupPageMact.text.toString())])
        }
    }

    private var STARTUP_PAGES: List<String> = listOf()
    private var STARTUP_PAGES_NAME: List<String> = listOf()
    private fun fillSpinners(context: Context) {
        STARTUP_PAGES = this.resources.getStringArray(R.array.startup_page_options).toList()
        STARTUP_PAGES_NAME = this.resources.getStringArray(R.array.startup_page_options_names).toList()

        val startupPageAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            R.layout.dropdown_menu_popup_item,
            STARTUP_PAGES_NAME
        )
        binding.bsUiuxInterfaceStartupPageMact.setAdapter(startupPageAdapter)
    }


    override fun onResume() {
        super.onResume()
        loadSettings()
    }


    private fun loadSettings() {
        binding.bsUiuxInterfaceSectionDynamicColors.setSwitchChecked(settingsManager.getSettingsBool(SettingsManager.PREFS.DYNAMIC_COLORS))

        var startupPageValue = SettingsManager(false, requireContext()).getSettingsString(SettingsManager.PREFS.STARTUP_PAGE, "dashboard")
        fillSpinners(requireContext())

        // Check if the value exists in the array, reset to home if not (this could occur if eg. a tablet backup (which has more options) gets restored on mobile)
        if (STARTUP_PAGES.contains(startupPageValue)) {
            binding.bsUiuxInterfaceStartupPageMact.setText(STARTUP_PAGES_NAME[STARTUP_PAGES.indexOf(startupPageValue)], false)
        } else {
            SettingsManager(false, requireContext()).putSettingsString(SettingsManager.PREFS.STARTUP_PAGE, "dashboard")
            startupPageValue = "dashboard"
            binding.bsUiuxInterfaceStartupPageMact.setText(STARTUP_PAGES_NAME[STARTUP_PAGES.indexOf(startupPageValue)], false)

        }

        loadIcons()
    }

    private fun loadIcons() {
        val linearLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.bsUiuxInterfaceIconRv.layoutManager = linearLayoutManager

        val customAdapter = LauncherIconsAdapter(requireContext())
        customAdapter.setClickListener(object : LauncherIconsAdapter.ClickListener {
            override fun onClick(pos: Int, aView: View) {
                // Set status of all images accordingly
                for (i in 0 until customAdapter.itemCount) {
                    val viewholder = binding.bsUiuxInterfaceIconRv.findViewHolderForAdapterPosition(i) as LauncherIconsAdapter.ViewHolder
                    viewholder.animateImage(i == pos)
                }

                // Set icon for Wearable app
                setWearableIcon(customAdapter.getItem(pos))
            }
        })
        binding.bsUiuxInterfaceIconRv.adapter = customAdapter
    }

    private fun setWearableIcon(item: LauncherIconController.LauncherIcon) {
        if (BuildConfig.FLAVOR == "gplay") {
            try {
                val activity = activity as AppSettingsActivity
                val nodeClient = Wearable.getNodeClient(activity)
                nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                    // Send a message to all connected nodes
                    // Nodes with the app installed will receive this message and open the ManageAliasActivity
                    if (nodes.any()) {
                        for (node in nodes) {
                            Wearable.getMessageClient(activity).sendMessage(node.id, "/setIcon", item.key.toByteArray())
                        }
                    }
                }
            } catch (ex: Exception) {
                context?.let { LoggingHelper(it).addLog(LOGIMPORTANCE.WARNING.int, ex.toString(), "setWearableIcon", null) }
            }
        }
    }

    private fun setOnClickListeners() {
        binding.bsUiuxInterfaceSectionDynamicColors.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.bsUiuxInterfaceSectionDynamicColors.setSwitchChecked(!binding.bsUiuxInterfaceSectionDynamicColors.getSwitchChecked())
            }
        })
    }

    private fun setOnSwitchListeners() {
        binding.bsUiuxInterfaceSectionDynamicColors.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed || forceSwitch) {
                    settingsManager.putSettingsBool(SettingsManager.PREFS.DYNAMIC_COLORS, checked)
                    listener.onApplyDynamicColors()
                }
            }
        })
    }

    companion object {
        fun newInstance(): UIUXInterfaceBottomDialogFragment {
            return UIUXInterfaceBottomDialogFragment()
        }
    }


    override fun onClick(p0: View?) {
        if (p0 != null) {
            when (p0.id) {
                R.id.bs_uiux_interface_off -> {
                    listener.onDarkModeOff()
                }
                R.id.bs_uiux_interface_on -> {
                    listener.onDarkModeOn()
                }
                R.id.bs_uiux_interface_automatic -> {
                    listener.onDarkModeAutomatic()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}