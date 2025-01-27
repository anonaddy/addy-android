package host.stjin.anonaddy.ui.faileddeliveries

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetFailedDeliveryDetailBinding
import host.stjin.anonaddy_shared.NetworkHelper
import kotlinx.coroutines.launch
import java.io.File


class FailedDeliveryDetailsBottomDialogFragment(
    private val failedDeliveryId: String?,
    private val created: String?,
    private val attempted: String?,
    private val alias: String?,
    private val recipient: String?,
    private val type: String?,
    private val remoteMTA: String?,
    private val sender: String?,
    private val code: String?
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
            binding.bsFailedDeliveriesDownloadButton.setOnClickListener(this)

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
                openEMLFile(context, result)
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

    fun openEMLFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.applicationContext.packageName}.provider", // Make sure this matches your FileProvider authority in the manifest
            file
        )

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "message/rfc822") // MIME type for EML files
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Necessary for providing access to the file

        // Check if there's an app to handle this intent
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // No app can handle this MIME type, show a message or handle it otherwise
            Toast.makeText(context, "No app found to open this file type.", Toast.LENGTH_SHORT).show()
        }
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
            code: String?
        ): FailedDeliveryDetailsBottomDialogFragment {
            return FailedDeliveryDetailsBottomDialogFragment(failedDeliveryId, created, attempted, alias, recipient, type, remoteMTA, sender, code)
        }
    }
}