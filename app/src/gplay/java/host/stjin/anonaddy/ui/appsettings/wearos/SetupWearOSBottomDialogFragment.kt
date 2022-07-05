package host.stjin.anonaddy.ui.appsettings.wearos

import android.app.Activity
import android.app.Dialog
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.Wearable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetSetupWearosBinding
import host.stjin.anonaddy.notifications.NotificationHelper
import host.stjin.anonaddy.utils.WearOSHelper
import host.stjin.anonaddy_shared.AnonAddyForAndroid
import host.stjin.anonaddy_shared.managers.SettingsManager
import kotlinx.coroutines.launch


class SetupWearOSBottomDialogFragment(private val parentActivity: Activity, private val nodeId: String?, private val nodeDisplayName: String?) :
    BaseBottomSheetDialogFragment(),
    View.OnClickListener {


    private lateinit var listener: AddSetupWearOSBottomDialogListener


    // 1. Defines the listener interface with a method passing back data result.
    interface AddSetupWearOSBottomDialogListener {
        fun onDismissed()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetSetupWearosBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetSetupWearosBinding.inflate(inflater, container, false)
        val root = binding.root

        if (nodeId != null) {
            binding.bsSetupWearosDesc.text = this.resources.getString(
                R.string.setup_wearable_app_desc,
                nodeDisplayName,
                (activity?.application as AnonAddyForAndroid).userResource.username
            )

            listener = activity as AddSetupWearOSBottomDialogListener
            binding.bsSetupWearosConfirmButton.setOnClickListener(this)
            binding.bsSetupWearosNegativeButton.setOnClickListener(this)
        } else {
            Toast.makeText(context, this.resources.getString(R.string.wearable_device_invalid), Toast.LENGTH_SHORT).show()
            listener.onDismissed()
        }
        return root

    }


    companion object {
        fun newInstance(parentActivity: Activity, nodeId: String?, nodeDisplayName: String?): SetupWearOSBottomDialogFragment {
            return SetupWearOSBottomDialogFragment(parentActivity, nodeId, nodeDisplayName)
        }
    }


    override fun onClick(p0: View?) {
        if (p0 != null) {
            when (p0.id) {
                R.id.bs_setup_wearos_confirm_button -> {
                    setupWearableDevice()
                }
                R.id.bs_setup_wearos_negative_button -> {
                    context?.let { SettingsManager(false, it).putSettingsBool(SettingsManager.PREFS.DISABLE_WEAROS_QUICK_SETUP_DIALOG, true) }
                    Toast.makeText(context, this.resources.getString(R.string.wearable_setup_skip_setup), Toast.LENGTH_SHORT).show()
                    listener.onDismissed()
                }
            }
        }
    }

    private fun setupWearableDevice() {

        /**
         * This is a sensitive action
         * Protect this part
         */
        if (nodeId != null) {
            lifecycleScope.launch {
                (activity as SetupWearOSBottomSheetActivity).isAuthenticated(shouldFinishOnError = false) { isAuthenticated ->
                    if (isAuthenticated) {
                        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                        binding.bsSetupWearosErrorMessage.visibility = View.INVISIBLE
                        binding.bsSetupWearosConfirmButton.startAnimation()

                        val configuration = Gson().toJson(WearOSHelper(parentActivity).createWearOSConfiguration())
                        Wearable.getMessageClient(activity as SetupWearOSBottomSheetActivity).sendMessage(
                            nodeId,
                            "/setup",
                            configuration.toByteArray()
                        ).addOnSuccessListener {
                            notificationManager.cancel(NotificationHelper.NEW_WEARABLE_PAIRING_REQUEST_NOTIFICATION_ID)
                            Toast.makeText(
                                context,
                                this@SetupWearOSBottomDialogFragment.resources.getString(R.string.wearable_setup_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            listener.onDismissed()
                        }.addOnCanceledListener {
                            binding.bsSetupWearosErrorMessage.visibility = View.VISIBLE
                            binding.bsSetupWearosErrorMessage.text =
                                this@SetupWearOSBottomDialogFragment.resources.getString(R.string.wearable_setup_canceled)
                            binding.bsSetupWearosConfirmButton.revertAnimation()
                        }.addOnFailureListener {
                            binding.bsSetupWearosErrorMessage.visibility = View.VISIBLE
                            binding.bsSetupWearosErrorMessage.text =
                                this@SetupWearOSBottomDialogFragment.resources.getString(R.string.wearable_setup_failed)
                            binding.bsSetupWearosConfirmButton.revertAnimation()
                        }
                    }
                }
            }
        }

    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener.onDismissed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}