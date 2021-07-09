package host.stjin.anonaddy.ui.faileddeliveries

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetFailedDeliveryDetailBinding


class FailedDeliveryDetailsBottomDialogFragment(
    private val created: String?,
    private val alias: String?,
    private val recipient: String?,
    private val type: String?,
    private val remoteMTA: String?,
    private val sender: String?,
    private val code: String?
) : BaseBottomSheetDialogFragment() {


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


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.bsFailedDeliveriesTextview.text = Html.fromHtml(
                context?.resources?.getString(R.string.failed_delivery_details_text, created, alias, recipient, type, remoteMTA, sender, code),
                Html.FROM_HTML_MODE_LEGACY
            )
        } else {
            binding.bsFailedDeliveriesTextview.text =
                Html.fromHtml(
                    context?.resources?.getString(
                        R.string.failed_delivery_details_text,
                        created,
                        alias,
                        recipient,
                        type,
                        remoteMTA,
                        sender,
                        code
                    )
                )
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setIMEAnimation(binding.bsFailedDeliveriesRoot)
        }

        return root

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            created: String?,
            alias: String?,
            recipient: String?,
            type: String?,
            remoteMTA: String?,
            sender: String?,
            code: String?
        ): FailedDeliveryDetailsBottomDialogFragment {
            return FailedDeliveryDetailsBottomDialogFragment(created, alias, recipient, type, remoteMTA, sender, code)
        }
    }
}