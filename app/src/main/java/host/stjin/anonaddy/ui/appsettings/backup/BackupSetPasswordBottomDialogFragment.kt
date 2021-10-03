package host.stjin.anonaddy.ui.appsettings.backup

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.databinding.BottomsheetSetBackupPasswordBinding


class BackupSetPasswordBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddBackupPasswordBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddBackupPasswordBottomDialogListener {
        fun onSaved()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetSetBackupPasswordBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetSetBackupPasswordBinding.inflate(inflater, container, false)
        val root = binding.root
        listener = activity as AddBackupPasswordBottomDialogListener


        // 2. Setup a callback when the "Done" button is pressed on keyboard
        binding.bsBackupPasswordSavePasswordButton.setOnClickListener(this)
        binding.bsBackupPasswordTiet.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                setBackupPassword(requireContext())
            }
            false
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setIMEAnimation(binding.bsBackupPasswordRoot)
        }

        return root

    }


    companion object {
        fun newInstance(): BackupSetPasswordBottomDialogFragment {
            return BackupSetPasswordBottomDialogFragment()
        }
    }

    private fun setBackupPassword(context: Context) {

        // Set error to null if username and alias is valid
        binding.bsBackupPasswordTil.error = null

        // Animate the button to progress
        binding.bsBackupPasswordSavePasswordButton.startAnimation()

        SettingsManager(true, context).putSettingsString(SettingsManager.PREFS.BACKUPS_PASSWORD, binding.bsBackupPasswordTiet.text.toString())
        listener.onSaved()
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_backup_password_save_password_button) {
                setBackupPassword(requireContext())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}