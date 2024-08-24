package host.stjin.anonaddy.ui.setup

import android.app.Dialog
import android.net.Uri
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
import host.stjin.anonaddy.databinding.BottomsheetSetupEnterBackupPasswordBinding
import host.stjin.anonaddy.service.BackupHelper


class BackupPasswordBottomDialogFragment(private val fileToDecryptUri: Uri) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddBackupPasswordBottomDialogListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddBackupPasswordBottomDialogListener {
        fun onBackupRestoreCompleted()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetSetupEnterBackupPasswordBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetSetupEnterBackupPasswordBinding.inflate(inflater, container, false)
        val root = binding.root
        listener = activity as AddBackupPasswordBottomDialogListener


        // 2. Setup a callback when the "Done" button is pressed on keyboard
        binding.bsSetupEnterBackupPasswordSavePasswordButton.setOnClickListener(this)
        binding.bsSetupEnterBackupPasswordTiet.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                setBackupPassword()
            }
            false
        }

        return root

    }


    companion object {
        fun newInstance(fileToDecryptUri: Uri): BackupPasswordBottomDialogFragment {
            return BackupPasswordBottomDialogFragment(fileToDecryptUri)
        }
    }

    private fun setBackupPassword() {

        // Set error to null if username and alias is valid
        binding.bsSetupEnterBackupPasswordTil.error = null

        // Animate the button to progress
        binding.bsSetupEnterBackupPasswordSavePasswordButton.startAnimation()

        if (BackupHelper(requireContext()).restoreBackup(fileToDecryptUri, binding.bsSetupEnterBackupPasswordTiet.text.toString())) {
            listener.onBackupRestoreCompleted()
        } else {
            binding.bsSetupEnterBackupPasswordSavePasswordButton.revertAnimation()
            // Showlogs is false because the logviewer is not available here yet
            binding.bsSetupEnterBackupPasswordTil.error = this.resources.getString(R.string.restore_failed)
        }
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_setup_enter_backup_password_save_password_button) {
                setBackupPassword()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}