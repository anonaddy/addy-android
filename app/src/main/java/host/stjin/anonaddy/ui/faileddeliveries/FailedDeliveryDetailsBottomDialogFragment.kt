package host.stjin.anonaddy.ui.faileddeliveries
import host.stjin.anonaddy_shared.utils.GsonTools

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetFailedDeliveryDetailBinding
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.models.FailedDeliveries
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.NewBlocklistEntry
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.os.Environment



class FailedDeliveryDetailsBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private val viewModel: FailedDeliveriesViewModel by activityViewModels()
    private var failedDelivery: FailedDeliveries? = null

    private var listener: AddFailedDeliveryBottomDialogListener? = null

    private var _binding: BottomsheetFailedDeliveryDetailBinding? = null

    private val binding get() = _binding!!

    private var fileToSave: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_FAILED_DELIVERY_JSON)?.let { json ->
            failedDelivery = try {
                GsonTools.gson.fromJson(json, FailedDeliveries::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    private val saveFileResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("message/rfc822")) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                            FileInputStream(fileToSave).use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, requireContext().resources.getString(R.string.file_saved_succesfully), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            LoggingHelper(requireContext()).addLog(LOGIMPORTANCE.CRITICAL.int, e.toString(), "saveFileResultLauncher", null)
                            Toast.makeText(context, requireContext().resources.getString(R.string.failed_to_save_file), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetFailedDeliveryDetailBinding.inflate(inflater, container, false)
        val root = binding.root

        val delivery = failedDelivery
        if (delivery != null) {

            // Could be opened from searchactivity
            listener = (parentFragment as? AddFailedDeliveryBottomDialogListener) ?: (activity as? AddFailedDeliveryBottomDialogListener)

            binding.bsFailedDeliveriesDeleteButton.setOnClickListener(this)


            if (delivery.is_stored && !delivery.quarantined && !delivery.resent && delivery.email_type == "F") {
                binding.bsFailedDeliveriesResendButton.visibility = View.VISIBLE
                binding.bsFailedDeliveriesResendButton.setOnClickListener(this)
            } else {
                binding.bsFailedDeliveriesResendButton.visibility = View.GONE
            }

            if (delivery.is_stored) {
                binding.bsFailedDeliveriesDownloadButton.visibility = View.VISIBLE
                binding.bsFailedDeliveriesDownloadButton.setOnClickListener(this)
            } else {
                binding.bsFailedDeliveriesDownloadButton.visibility = View.GONE
            }

            val hasFreeSub = (activity?.application as? AddyIoApp)?.userResourceOrNull?.hasUserFreeSubscription ?: false
            if (delivery.sender != null && !hasFreeSub) {
                binding.bsFailedDeliveriesBlockSenderButton.visibility = View.VISIBLE
                binding.bsFailedDeliveriesBlockSenderButton.setOnClickListener(this)
            } else {
                binding.bsFailedDeliveriesBlockSenderButton.visibility = View.GONE
            }

            binding.bsFailedDeliveriesTextviewType.text = delivery.email_type_text

            binding.bsFailedDeliveriesTextview.text = androidx.core.text.HtmlCompat.fromHtml(
                requireContext().resources.getString(
                    R.string.failed_delivery_details_text,
                    DateTimeUtils.convertStringToLocalTimeZoneString(delivery.created_at),
                    delivery.destination ?: "",
                    delivery.alias_email ?: "",
                    delivery.sender ?: "",
                    delivery.remote_mta,
                    DateTimeUtils.convertStringToLocalTimeZoneString(delivery.attempted_at),
                    delivery.code
                ),
                androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
            )


        } else {
            dismiss()
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

    fun saveFileToUserLocation(file: File) {
        fileToSave = file
        saveFileResultLauncher.launch(file.name)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_failed_deliveries_resend_button) {
                resendFailedDelivery(
                    requireContext()
                )
            } else if (p0.id == R.id.bs_failed_deliveries_delete_button) {
                deleteFailedDelivery(
                    requireContext()
                )
            } else if (p0.id == R.id.bs_failed_deliveries_download_button) {
                downloadFailedDelivery(
                    requireContext()
                )
            } else if (p0.id == R.id.bs_failed_deliveries_block_sender_button) {
                blockSender(
                    requireContext()
                )
            }
        }
    }

    private fun deleteFailedDelivery(context: Context) {
        // Animate the button to progress
        binding.bsFailedDeliveriesDeleteButton.startAnimation()

        lifecycleScope.launch {
            deleteFailedDeliveryHttp(context)
        }
    }

    private fun resendFailedDelivery(context: Context) {
        MaterialDialogHelper.showMaterialDialog(
            context = context,
            title = resources.getString(R.string.resend_failed_delivery),
            message = resources.getString(R.string.resend_failed_delivery_confirmation_desc),
            icon = R.drawable.ic_mail_error,
            neutralButtonText = resources.getString(R.string.cancel),
            positiveButtonText = resources.getString(R.string.resend),
            positiveButtonAction = {
                // Animate the button to progress
                binding.bsFailedDeliveriesResendButton.startAnimation()

                lifecycleScope.launch {
                    resendFailedDeliveryHttp(context)
                }
            },
        ).show()
    }

    private suspend fun resendFailedDeliveryHttp(context: Context) {
        val result = viewModel.resendFailedDelivery(failedDelivery!!.id)
        if (result is NetworkResult.Success) {
            // Animate the button to progress
            binding.bsFailedDeliveriesResendButton.revertAnimation()

            MaterialDialogHelper.showMaterialDialog(
                context = requireContext(),
                title = resources.getString(R.string.resend_failed_delivery),
                message = context.resources.getString(R.string.failed_delivery_resend_success),
                icon = R.drawable.ic_mail_error,
                neutralButtonText = resources.getString(R.string.close)
            ).show()
        } else {
            // Animate the button to progress
            binding.bsFailedDeliveriesResendButton.revertAnimation()

            MaterialDialogHelper.showMaterialDialog(
                context = requireContext(),
                title = resources.getString(R.string.resend_failed_delivery),
                message = context.resources.getString(R.string.error_resending_failed_delivery) + "\n" + (result.errorOrNull() ?: ""),
                icon = R.drawable.ic_mail_error,
                neutralButtonText = resources.getString(R.string.close)
            ).show()
        }
    }

    private suspend fun deleteFailedDeliveryHttp(context: Context) {
        val currentFailedDelivery = failedDelivery ?: return
        val result = viewModel.deleteFailedDelivery(currentFailedDelivery.id)
        if (result is NetworkResult.Success && result.data == "204") {
            listener?.onDeleted(currentFailedDelivery.id)
        } else {
            // Animate the button to progress
            binding.bsFailedDeliveriesDeleteButton.revertAnimation()

            MaterialDialogHelper.showMaterialDialog(
                context = requireContext(),
                title = resources.getString(R.string.delete_failed_delivery),
                message = context.resources.getString(R.string.error_delete_failed_delivery) + "\n" + (result.errorOrNull() ?: ""),
                icon = R.drawable.ic_mail_error,
                neutralButtonText = resources.getString(R.string.close)
            ).show()
        }
    }

    private fun downloadFailedDelivery(context: Context) {
        // Animate the button to progress
        binding.bsFailedDeliveriesDownloadButton.startAnimation()

        lifecycleScope.launch {
            downloadFailedDeliveryHttp(context)
        }
    }

    private suspend fun downloadFailedDeliveryHttp(context: Context) {
        val currentFailedDelivery = failedDelivery ?: return
        val result = viewModel.downloadSpecificFailedDelivery(currentFailedDelivery.id)
        if (result is NetworkResult.Success) {
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(directory, "${currentFailedDelivery.id}.eml")
            try {
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { it.write(result.data) }
                }
                saveFileToUserLocation(file)
            } catch (e: Exception) {
                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = resources.getString(R.string.download_failed_delivery),
                    message = context.resources.getString(R.string.error_downloading_failed_delivery) + "\n" + e.message,
                    icon = R.drawable.ic_mail_error,
                    neutralButtonText = resources.getString(R.string.close)
                ).show()
            }
            binding.bsFailedDeliveriesDownloadButton.revertAnimation()
        } else {
            binding.bsFailedDeliveriesDownloadButton.revertAnimation()

            MaterialDialogHelper.showMaterialDialog(
                context = requireContext(),
                title = resources.getString(R.string.download_failed_delivery),
                message = context.resources.getString(R.string.error_downloading_failed_delivery) + "\n" + (result.errorOrNull() ?: ""),
                icon = R.drawable.ic_mail_error,
                neutralButtonText = resources.getString(R.string.close)
            ).show()
        }
    }

    private fun blockSender(context: Context) {
        val sender = failedDelivery?.sender ?: return
        val domain = if (sender.contains("@")) sender.substringAfterLast("@").trim() else null

        if (!domain.isNullOrEmpty()) {
            val options = arrayOf(
                "${resources.getString(R.string.email)}: $sender",
                "${resources.getString(R.string.domain)}: $domain"
            )
            var selectedIndex = 0

            val dialog = MaterialDialogHelper.showMaterialDialog(
                context = context,
                title = resources.getString(R.string.blocklist_add),
                icon = R.drawable.ic_forbid,
                neutralButtonText = resources.getString(R.string.cancel),
                positiveButtonText = resources.getString(R.string.blocklist_add),
                positiveButtonAction = {
                    val type = if (selectedIndex == 0) "email" else "domain"
                    val value = if (selectedIndex == 0) sender else domain
                    binding.bsFailedDeliveriesBlockSenderButton.startAnimation()
                    lifecycleScope.launch {
                        blockSenderHttp(context, type, value)
                    }
                }
            )
            dialog.setSingleChoiceItems(options, selectedIndex) { _, which ->
                selectedIndex = which
            }
            dialog.show()
        } else {
            MaterialDialogHelper.showMaterialDialog(
                context = context,
                title = resources.getString(R.string.blocklist_add),
                message = resources.getString(R.string.blocklist_add_confirm_desc, sender),
                icon = R.drawable.ic_forbid,
                neutralButtonText = resources.getString(R.string.cancel),
                positiveButtonText = resources.getString(R.string.blocklist_add),
                positiveButtonAction = {
                    binding.bsFailedDeliveriesBlockSenderButton.startAnimation()
                    lifecycleScope.launch {
                        blockSenderHttp(context, "email", sender)
                    }
                },
            ).show()
        }
    }

    private suspend fun blockSenderHttp(context: Context, type: String, value: String) {
        val result = viewModel.addBlocklistEntry(NewBlocklistEntry(type, value))
        binding.bsFailedDeliveriesBlockSenderButton.revertAnimation()
        when (result) {
            is NetworkResult.Success -> {
                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = resources.getString(R.string.blocklist_add),
                    message = if (type == "domain") {
                        context.resources.getString(R.string.blocklist_add_domain_success)
                    } else {
                        context.resources.getString(R.string.blocklist_add_success)
                    },
                    icon = R.drawable.ic_forbid,
                    neutralButtonText = resources.getString(R.string.close)
                ).show()
            }
            is NetworkResult.Error -> {
                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = resources.getString(R.string.blocklist_add),
                    message = context.resources.getString(R.string.error_adding_blocklist_entry) + "\n" + result.error,
                    icon = R.drawable.ic_forbid,
                    neutralButtonText = resources.getString(R.string.close)
                ).show()
            }
        }
    }

    interface AddFailedDeliveryBottomDialogListener {
        fun onDeleted(failedDeliveryId: String)
    }

    companion object {
        private const val ARG_FAILED_DELIVERY_JSON = "arg_failed_delivery_json"

        fun newInstance(
            failedDelivery: FailedDeliveries
        ): FailedDeliveryDetailsBottomDialogFragment {
            return FailedDeliveryDetailsBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FAILED_DELIVERY_JSON, GsonTools.gson.toJson(failedDelivery))
                }
            }
        }
    }
}
