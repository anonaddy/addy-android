package host.stjin.anonaddy.ui.appsettings

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.databinding.BottomsheetDarkmodeBinding


class DarkModeBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddDarkmodeBottomDialogListener


    // 1. Defines the listener interface with a method passing back data result.
    interface AddDarkmodeBottomDialogListener {
        fun onDarkModeOff()
        fun onDarkModeOn()
        fun onDarkModeAutomatic()
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetDarkmodeBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetDarkmodeBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = activity as AddDarkmodeBottomDialogListener

        val settingsManager = SettingsManager(false, requireContext())
        when (settingsManager.getSettingsInt(SettingsManager.PREFS.DARK_MODE, -1)) {
            0 -> {
                binding.bsDarkmodeOff.isChecked = true
            }
            1 -> {
                binding.bsDarkmodeOn.isChecked = true
            }
            -1 -> {
                binding.bsDarkmodeAutomatic.isChecked = true
            }
        }

        // 2. Setup a callback when a theme is selected
        binding.bsDarkmodeOff.setOnClickListener(this)
        binding.bsDarkmodeOn.setOnClickListener(this)
        binding.bsDarkmodeAutomatic.setOnClickListener(this)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setIMEAnimation(binding.bsDarkmodeRoot)
        }

        return root

    }

    companion object {
        fun newInstance(): DarkModeBottomDialogFragment {
            return DarkModeBottomDialogFragment()
        }
    }


    override fun onClick(p0: View?) {
        if (p0 != null) {
            when (p0.id) {
                R.id.bs_darkmode_off -> {
                    listener.onDarkModeOff()
                }
                R.id.bs_darkmode_on -> {
                    listener.onDarkModeOn()
                }
                R.id.bs_darkmode_automatic -> {
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