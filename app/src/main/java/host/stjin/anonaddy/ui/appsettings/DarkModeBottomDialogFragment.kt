package host.stjin.anonaddy.ui.appsettings

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import kotlinx.android.synthetic.main.bottomsheet_darkmode.view.*


class DarkModeBottomDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {


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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // get the views and attach the listener
        val root = inflater.inflate(
            R.layout.bottomsheet_darkmode, container,
            false
        )
        listener = activity as AddDarkmodeBottomDialogListener

        val settingsManager = SettingsManager(false, requireContext())
        when (settingsManager.getSettingsInt("dark_mode", -1)) {
            0 -> {
                root.bs_darkmode_off.isChecked = true
            }
            1 -> {
                root.bs_darkmode_on.isChecked = true
            }
            -1 -> {
                root.bs_darkmode_automatic.isChecked = true
            }
        }

        // 2. Setup a callback when a theme is selected
        root.bs_darkmode_off.setOnClickListener(this)
        root.bs_darkmode_on.setOnClickListener(this)
        root.bs_darkmode_automatic.setOnClickListener(this)
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
}