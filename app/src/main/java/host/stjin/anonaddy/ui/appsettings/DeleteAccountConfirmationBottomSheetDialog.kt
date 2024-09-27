package host.stjin.anonaddy.ui.appsettings

import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetDeleteAccountConfirmationBinding
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy_shared.NetworkHelper
import kotlinx.coroutines.launch


class DeleteAccountConfirmationBottomSheetDialog : BaseBottomSheetDialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    lateinit var mainHandler: Handler

    private var secondsRemaining = 11
    private var _binding: BottomsheetDeleteAccountConfirmationBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetDeleteAccountConfirmationBinding.inflate(inflater, container, false)
        val root = binding.root

        mainHandler = Handler(Looper.getMainLooper())

        return root

    }


    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(updateBackground)
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(updateBackground)
    }

    private val updateBackground = object : Runnable {
        override fun run() {
            secondsRemaining -= 1

            if (secondsRemaining == 0) {
                binding.bsDeleteAccountConfirmationButton.isEnabled = true
                binding.bsDeleteAccountConfirmationButton.alpha = 1f
                binding.bsDeleteAccountConfirmationButton.text = this@DeleteAccountConfirmationBottomSheetDialog.resources.getString(R.string.delete_account)
                binding.bsDeleteAccountConfirmationButton.setOnClickListener {
                    deleteAccountConfirmationDialog()
                }
            } else {
                binding.bsDeleteAccountConfirmationButton.text = this@DeleteAccountConfirmationBottomSheetDialog.resources.getString(R.string.delete_account_countdown, secondsRemaining)
                mainHandler.postDelayed(this, 1000)

            }


        }
    }

    private fun deleteAccountConfirmationDialog(message: String? = null) {
        var password = ""

        MaterialDialogHelper.showMaterialDialog(
            context = requireContext(),
            title = resources.getString(R.string.delete_account),
            message = message ?: resources.getString(R.string.delete_account_confirmation_alert),
            icon = R.drawable.ic_user_minus,
            textInputHint = resources.getString(R.string.delete_account_confirmation_password),
            getPasswordInput = { text: String ->
                password = text
            },
            positiveButtonText = resources.getString(R.string.delete_account),
            positiveButtonAction = {
                viewLifecycleOwner.lifecycleScope.launch {
                    deleteAccount(password)
                }
            },
            neutralButtonText = resources.getString(R.string.close)
        ).show()
    }

    private suspend fun deleteAccount(password: String) {
        NetworkHelper(requireContext()).deleteAccount ({ result ->
            when (result) {
                "204" -> {
                    (requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                }
                "422" -> {
                    deleteAccountConfirmationDialog(resources.getString(R.string.delete_account_failed))
                }
                else -> {
                    MaterialDialogHelper.showMaterialDialog(
                        context = requireContext(),
                        title = resources.getString(R.string.delete_account),
                        message = result,
                        icon = R.drawable.ic_user_minus,
                        neutralButtonText = resources.getString(R.string.close)
                    ).show()
                }
            }
        }, password = password)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): DeleteAccountConfirmationBottomSheetDialog {
            return DeleteAccountConfirmationBottomSheetDialog()
        }
    }
}