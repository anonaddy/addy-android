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
import host.stjin.anonaddy.databinding.BottomsheetBackgroundserviceintervalBinding


class BackgroundServiceIntervalBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {


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

    private var _binding: BottomsheetBackgroundserviceintervalBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetBackgroundserviceintervalBinding.inflate(inflater, container, false)
        val root = binding.root
        listener = activity as AddBackgroundServiceIntervalBottomDialogListener

        binding.bsBackgroundserviceintervalSetIntervalButton.setOnClickListener(this)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setIMEAnimation(binding.bsBackgroundserviceintervalRoot)
        }

        return root

    }

    override fun onResume() {
        super.onResume()

        val settingsManager = SettingsManager(false, requireContext())
        when (settingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_INTERVAL, 30)) {
            15 -> {
                binding.bsBackgroundserviceinterval15.isChecked = true
            }
            30 -> {
                binding.bsBackgroundserviceinterval30.isChecked = true
            }
            60 -> {
                binding.bsBackgroundserviceinterval60.isChecked = true
            }
            120 -> {
                binding.bsBackgroundserviceinterval120.isChecked = true
            }
        }


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
                    when {
                        binding.bsBackgroundserviceinterval15.isChecked -> {
                            listener.setInterval(15)
                        }
                        binding.bsBackgroundserviceinterval30.isChecked -> {
                            listener.setInterval(30)
                        }
                        binding.bsBackgroundserviceinterval60.isChecked -> {
                            listener.setInterval(60)
                        }
                        binding.bsBackgroundserviceinterval120.isChecked -> {
                            listener.setInterval(120)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}