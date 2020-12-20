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
import kotlinx.android.synthetic.main.bottomsheet_backgroundserviceinterval.view.*


class BackgroundServiceIntervalBottomDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddBackgroundServiceIntervalBottomDialogListener


    // 1. Defines the listener interface with a method passing back data result.
    interface AddBackgroundServiceIntervalBottomDialogListener {
        fun setInterval(minutes: Int)
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
            R.layout.bottomsheet_backgroundserviceinterval, container,
            false
        )
        listener = activity as AddBackgroundServiceIntervalBottomDialogListener

        val settingsManager = SettingsManager(false, requireContext())
        when (settingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL, 30)) {
            15 -> {
                root.bs_backgroundserviceinterval_15.isChecked = true
            }
            30 -> {
                root.bs_backgroundserviceinterval_30.isChecked = true
            }
            60 -> {
                root.bs_backgroundserviceinterval_60.isChecked = true
            }
            120 -> {
                root.bs_backgroundserviceinterval_120.isChecked = true
            }
        }


        root.bs_backgroundserviceinterval_set_interval_button.setOnClickListener(this)
        return root

    }

    companion object {
        fun newInstance(): BackgroundServiceIntervalBottomDialogFragment {
            return BackgroundServiceIntervalBottomDialogFragment()
        }
    }


    override fun onClick(p0: View?) {
        if (p0 != null) {
            when (p0.id) {
                R.id.bs_backgroundserviceinterval_set_interval_button -> {
                    val root = view
                    when {
                        root?.bs_backgroundserviceinterval_15?.isChecked == true -> {
                            listener.setInterval(15)
                        }
                        root?.bs_backgroundserviceinterval_30?.isChecked == true -> {
                            listener.setInterval(30)
                        }
                        root?.bs_backgroundserviceinterval_60?.isChecked == true -> {
                            listener.setInterval(60)
                        }
                        root?.bs_backgroundserviceinterval_120?.isChecked == true -> {
                            listener.setInterval(120)
                        }
                    }
                }
            }
        }
    }
}