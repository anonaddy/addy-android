package host.stjin.anonaddy.ui.accountnotifications

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
import host.stjin.anonaddy.databinding.BottomsheetAccountNotificationsDetailBinding
import host.stjin.anonaddy_shared.utils.DateTimeUtils


class AccountNotificationsDetailsBottomDialogFragment(
    private val created: String,
    private val title: String,
    private val text: String,
    private val linkText: String?,
    private val link: String?
) : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddAccountNotificationsBottomDialogListener

    interface AddAccountNotificationsBottomDialogListener {
        fun onOpenUrl(url: String?)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetAccountNotificationsDetailBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetAccountNotificationsDetailBinding.inflate(inflater, container, false)
        val root = binding.root

        // Listener only works when called from fragment (this sheet can be called from notification)
        if (parentFragment != null) {
            listener = parentFragment as AddAccountNotificationsBottomDialogListener
        } else if (activity != null) {
            listener = activity as AddAccountNotificationsBottomDialogListener
        }

        if (link != null) {
            binding.bsAccountNotificationsOpenButton.setOnClickListener(this)
        } else {
            binding.bsAccountNotificationsOpenButton.visibility = View.GONE
        }

        binding.bsAccountNotificationsTitle.text = title
        binding.bsAccountNotificationsOpenButton.text = linkText ?: this.resources.getString(R.string.open_link)
        binding.bsAccountNotificationsCreated.text = DateTimeUtils.turnStringIntoLocalString(created)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.bsAccountNotificationsTextview.text = Html.fromHtml(
                text,
                Html.FROM_HTML_MODE_LEGACY
            )
        } else {
            binding.bsAccountNotificationsTextview.text =
                text
        }

        return root

    }


    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_account_notifications_open_button) {
                listener.onOpenUrl(
                    link
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
            created: String,
            title: String,
            text: String,
            linkText: String,
            link: String
        ): AccountNotificationsDetailsBottomDialogFragment {
            return AccountNotificationsDetailsBottomDialogFragment(created, title, text, linkText, link)
        }
    }
}