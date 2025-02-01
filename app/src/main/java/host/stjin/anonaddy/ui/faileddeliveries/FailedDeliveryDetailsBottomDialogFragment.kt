package host.stjin.anonaddy.ui.faileddeliveries

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetFailedDeliveryDetailBinding
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream


class FailedDeliveryDetailsBottomDialogFragment(
    private val failedDeliveryId: String?,
    private val created: String?,
    private val attempted: String?,
    private val alias: String?,
    private val recipient: String?,
    private val type: String?,
    private val remoteMTA: String?,
    private val sender: String?,
    private val code: String?,
    private val isStored: Boolean,
) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddFailedDeliveryBottomDialogListener

    interface AddFailedDeliveryBottomDialogListener {
        fun onDeleted(failedDeliveryId: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetFailedDeliveryDetailBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetFailedDeliveryDetailBinding.inflate(inflater, container, false)
        val root = binding.root


        // Check if failedDeliveryId is null to prevent a "could not find Fragment constructor when changing theme or rotating when the dialog is open"
        if (failedDeliveryId != null) {

            // Could be opened from searchactivity
            if (parentFragment != null) {
                listener = parentFragment as AddFailedDeliveryBottomDialogListener
            } else if (activity != null) {
                listener = activity as AddFailedDeliveryBottomDialogListener
            }

            binding.bsFailedDeliveriesDeleteButton.setOnClickListener(this)

            if (isStored){
                binding.bsFailedDeliveriesDownloadButton.visibility = View.VISIBLE
                binding.bsFailedDeliveriesDownloadButton.setOnClickListener(this)
            } else {
                binding.bsFailedDeliveriesDownloadButton.visibility = View.GONE
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.bsFailedDeliveriesTextview.text = Html.fromHtml(
                    context?.resources?.getString(
                        R.string.failed_delivery_details_text,
                        created,
                        attempted,
                        alias,
                        recipient,
                        type,
                        remoteMTA,
                        sender,
                        code
                    ),
                    Html.FROM_HTML_MODE_LEGACY
                )
            } else {
                binding.bsFailedDeliveriesTextview.text =
                    Html.fromHtml(
                        context?.resources?.getString(
                            R.string.failed_delivery_details_text,
                            created,
                            attempted,
                            alias,
                            recipient,
                            type,
                            remoteMTA,
                            sender,
                            code
                        )
                    )
            }
        } else {
            dismiss()
        }
        return root

    }


    private fun deleteFailedDelivery(context: Context) {
        // Hide error text
        binding.bsFailedDeliveriesError.visibility = View.GONE

        // Animate the button to progress
        binding.bsFailedDeliveriesDeleteButton.startAnimation()

        lifecycleScope.launch {
            deleteFailedDeliveryHttp(context)
        }
    }

    private suspend fun deleteFailedDeliveryHttp(context: Context) {
        val networkHelper = NetworkHelper(context)
        networkHelper.deleteFailedDelivery({ result ->
            if (result == "204") {
                listener.onDeleted(failedDeliveryId)
            } else {
                // Animate the button to progress
                binding.bsFailedDeliveriesDeleteButton.revertAnimation()

                binding.bsFailedDeliveriesError.visibility = View.VISIBLE
                binding.bsFailedDeliveriesError.text =
                    context.resources.getString(R.string.error_delete_failed_delivery) + "\n" + result
            }
            // aliasId is never null at this point, hence the !!
        }, failedDeliveryId!!)
    }

    private fun downloadFailedDelivery(context: Context) {
        // Hide error text
        binding.bsFailedDeliveriesError.visibility = View.GONE

        // Animate the button to progress
        binding.bsFailedDeliveriesDownloadButton.startAnimation()

        lifecycleScope.launch {
            downloadFailedDeliveryHttp(context)
        }
    }

    private suspend fun downloadFailedDeliveryHttp(context: Context) {
        val networkHelper = NetworkHelper(context)
        networkHelper.downloadSpecificFailedDelivery(context, { result, error ->
            if (result != null) {
                saveFileToUserLocation(result)
                binding.bsFailedDeliveriesDownloadButton.revertAnimation()

            } else {
                // Animate the button to progress
                binding.bsFailedDeliveriesDownloadButton.revertAnimation()

                binding.bsFailedDeliveriesError.visibility = View.VISIBLE
                binding.bsFailedDeliveriesError.text =
                    context.resources.getString(R.string.error_download_failed_delivery) + "\n" + result
            }
            // aliasId is never null at this point, hence the !!
        }, failedDeliveryId!!)
    }


    private var fileToSave: File? = null
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

    fun saveFileToUserLocation(file: File) {
        fileToSave = file
        saveFileResultLauncher.launch(file.name)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_failed_deliveries_delete_button) {
                deleteFailedDelivery(
                    requireContext()
                )
            } else if (p0.id == R.id.bs_failed_deliveries_download_button) {
                downloadFailedDelivery(
                    requireContext()
                )
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            failedDeliveryId: String?,
            created: String?,
            attempted: String?,
            alias: String?,
            recipient: String?,
            type: String?,
            remoteMTA: String?,
            sender: String?,
            code: String?,
            isStored: Boolean
        ): FailedDeliveryDetailsBottomDialogFragment {
            return FailedDeliveryDetailsBottomDialogFragment(failedDeliveryId, created, attempted, alias, recipient, type, remoteMTA, sender, code, isStored)
        }
    }
}