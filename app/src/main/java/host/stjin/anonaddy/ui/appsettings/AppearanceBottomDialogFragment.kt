package host.stjin.anonaddy.ui.appsettings

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.wearable.Wearable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.adapter.LauncherIconsAdapter
import host.stjin.anonaddy.databinding.BottomsheetAppearanceBinding
import host.stjin.anonaddy.ui.customviews.SectionView
import host.stjin.anonaddy_shared.controllers.LauncherIconController
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.LoggingHelper


class AppearanceBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddAppearanceBottomDialogListener
    private var forceSwitch = false

    private var _binding: BottomsheetAppearanceBinding? = null
    private lateinit var settingsManager: SettingsManager


    // 1. Defines the listener interface with a method passing back data result.
    interface AddAppearanceBottomDialogListener {
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
        _binding = BottomsheetAppearanceBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = activity as AddAppearanceBottomDialogListener
        settingsManager = SettingsManager(false, requireContext())

        loadSettings()
        setOnClickListeners()
        setOnSwitchListeners()

        when (settingsManager.getSettingsInt(SettingsManager.PREFS.DARK_MODE, -1)) {
            0 -> {
                binding.bsAppearanceOff.isChecked = true
            }
            1 -> {
                binding.bsAppearanceOn.isChecked = true
            }
            -1 -> {
                binding.bsAppearanceAutomatic.isChecked = true
            }
        }

        // 2. Setup a callback when a thesme is selected
        binding.bsAppearanceOff.setOnClickListener(this)
        binding.bsAppearanceOn.setOnClickListener(this)
        binding.bsAppearanceAutomatic.setOnClickListener(this)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setIMEAnimation(binding.bsAppearanceRoot)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.bsAppearanceSectionDynamicColors.visibility = View.VISIBLE
        } else {
            binding.bsAppearanceSectionDynamicColors.visibility = View.GONE
        }

        return root

    }


    override fun onResume() {
        super.onResume()
        loadSettings()
    }


    private fun loadSettings() {
        binding.bsAppearanceSectionDynamicColors.setSwitchChecked(settingsManager.getSettingsBool(SettingsManager.PREFS.DYNAMIC_COLORS))

        loadIcons()
    }

    private fun loadIcons() {
        val linearLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.bsAppearanceIconRv.layoutManager = linearLayoutManager

        val customAdapter = LauncherIconsAdapter(requireContext())
        customAdapter.setClickListener(object : LauncherIconsAdapter.ClickListener {
            override fun onClick(pos: Int, aView: View) {
                // Set status of all images accordingly
                for (i in 0 until customAdapter.itemCount) {
                    val viewholder = binding.bsAppearanceIconRv.findViewHolderForAdapterPosition(i) as LauncherIconsAdapter.ViewHolder
                    viewholder.animateImage(i == pos)
                }

                // Set icon for Wearable app
                setWearableIcon(customAdapter.getItem(pos))
            }
        })
        binding.bsAppearanceIconRv.adapter = customAdapter
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
        binding.bsAppearanceSectionDynamicColors.setOnLayoutClickedListener(object : SectionView.OnLayoutClickedListener {
            override fun onClick() {
                forceSwitch = true
                binding.bsAppearanceSectionDynamicColors.setSwitchChecked(!binding.bsAppearanceSectionDynamicColors.getSwitchChecked())
            }
        })
    }

    private fun setOnSwitchListeners() {
        binding.bsAppearanceSectionDynamicColors.setOnSwitchCheckedChangedListener(object : SectionView.OnSwitchCheckedChangedListener {
            override fun onCheckedChange(compoundButton: CompoundButton, checked: Boolean) {
                if (compoundButton.isPressed || forceSwitch) {
                    settingsManager.putSettingsBool(SettingsManager.PREFS.DYNAMIC_COLORS, checked)
                    listener.onApplyDynamicColors()
                }
            }
        })
    }

    companion object {
        fun newInstance(): AppearanceBottomDialogFragment {
            return AppearanceBottomDialogFragment()
        }
    }


    override fun onClick(p0: View?) {
        if (p0 != null) {
            when (p0.id) {
                R.id.bs_appearance_off -> {
                    listener.onDarkModeOff()
                }
                R.id.bs_appearance_on -> {
                    listener.onDarkModeOn()
                }
                R.id.bs_appearance_automatic -> {
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