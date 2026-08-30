package host.stjin.anonaddy.ui.appsettings.wearos
import host.stjin.anonaddy_shared.utils.GsonTools

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
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.databinding.BottomsheetSetupWearosBinding
import host.stjin.anonaddy.notifications.NotificationHelper
import host.stjin.anonaddy.utils.WearOSHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.managers.SettingsManager
import kotlinx.coroutines.launch


class SetupWearOSBottomDialogFragment :
    BaseBottomSheetDialogFragment(),
    View.OnClickListener {
    private var nodeId: String? = null
    private var nodeDisplayName: String? = null

    private var listener: AddSetupWearOSBottomDialogListener? = null

    private var _binding: BottomsheetSetupWearosBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            nodeId = it.getString(ARG_NODE_ID)
            nodeDisplayName = it.getString(ARG_NODE_DISPLAY_NAME)
        }
    }

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
                (activity?.application as? AddyIoApp)?.userResourceOrNull?.username ?: ""
            )

            listener = (parentFragment as? AddSetupWearOSBottomDialogListener) ?: (activity as? AddSetupWearOSBottomDialogListener)
            binding.bsSetupWearosConfirmButton.setOnClickListener(this)
            binding.bsSetupWearosNegativeButton.setOnClickListener(this)
        } else {
            Toast.makeText(context, this.resources.getString(R.string.wearable_device_invalid), Toast.LENGTH_SHORT).show()
            listener?.onDismissed()
        }
        return root

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            when (p0.id) {
                R.id.bs_setup_wearos_confirm_button -> {
                    setupWearableDevice()
                }

                R.id.bs_setup_wearos_negative_button -> {
                    context?.let { ServiceLocator.settingsManager.putSettingsBool(SettingsManager.PREFS.DISABLE_WEAROS_QUICK_SETUP_DIALOG, true) }
                    Toast.makeText(context, this.resources.getString(R.string.wearable_setup_skip_setup), Toast.LENGTH_SHORT).show()
                    listener?.onDismissed()
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener?.onDismissed()
    }

    private fun setupWearableDevice() {

        /**
         * This is a sensitive action
         * Protect this part
         */
        if (nodeId != null) {
            val wearActivity = activity as? SetupWearOSBottomSheetActivity ?: return
            viewLifecycleOwner.lifecycleScope.launch {
                wearActivity.isAuthenticated(shouldFinishOnError = false) { isAuthenticated ->
                    if (isAuthenticated) {
                        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                        binding.bsSetupWearosErrorMessage.visibility = View.INVISIBLE
                        binding.bsSetupWearosConfirmButton.startAnimation()

                        val configuration = GsonTools.gson.toJson(WearOSHelper.createWearOSConfiguration())
                        Wearable.getMessageClient(wearActivity).sendMessage(
                            nodeId!!,
                            "/setup",
                            configuration.toByteArray()
                        ).addOnSuccessListener {
                            notificationManager.cancel(NotificationHelper.NEW_WEARABLE_PAIRING_REQUEST_NOTIFICATION_ID)
                            Toast.makeText(
                                context,
                                this@SetupWearOSBottomDialogFragment.resources.getString(R.string.wearable_setup_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            listener?.onDismissed()
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

    // 1. Defines the listener interface with a method passing back data result.
    interface AddSetupWearOSBottomDialogListener {
        fun onDismissed()
    }

    companion object {
        private const val ARG_NODE_ID = "arg_node_id"
        private const val ARG_NODE_DISPLAY_NAME = "arg_node_display_name"

        fun newInstance(nodeId: String?, nodeDisplayName: String?): SetupWearOSBottomDialogFragment {
            return SetupWearOSBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NODE_ID, nodeId)
                    putString(ARG_NODE_DISPLAY_NAME, nodeDisplayName)
                }
            }
        }
    }
}
