package host.stjin.anonaddy.ui.accountnotifications

import android.app.Dialog
import android.os.Bundle
import androidx.core.text.HtmlCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetAccountNotificationsDetailBinding
import host.stjin.anonaddy_shared.utils.DateTimeUtils


class AccountNotificationsDetailsBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private var created: String? = null
    private var title: String? = null
    private var text: String? = null
    private var linkText: String? = null
    private var link: String? = null

    private var listener: AddAccountNotificationsBottomDialogListener? = null

    private var _binding: BottomsheetAccountNotificationsDetailBinding? = null

    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            created = it.getString(ARG_CREATED)
            title = it.getString(ARG_TITLE)
            text = it.getString(ARG_TEXT)
            linkText = it.getString(ARG_LINK_TEXT)
            link = it.getString(ARG_LINK)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetAccountNotificationsDetailBinding.inflate(inflater, container, false)
        val root = binding.root

        // Listener only works when called from fragment or host activity
        listener = (parentFragment as? AddAccountNotificationsBottomDialogListener) ?: (activity as? AddAccountNotificationsBottomDialogListener)

        if (link != null) {
            binding.bsAccountNotificationsOpenButton.setOnClickListener(this)
        } else {
            binding.bsAccountNotificationsOpenButton.visibility = View.GONE
        }

        binding.bsAccountNotificationsTitle.text = title ?: ""
        binding.bsAccountNotificationsOpenButton.text = linkText ?: this.resources.getString(R.string.open_link)
        binding.bsAccountNotificationsCreated.text = created?.let { DateTimeUtils.convertStringToLocalTimeZoneString(it) } ?: ""

        binding.bsAccountNotificationsTextview.text = HtmlCompat.fromHtml(
            text ?: "",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

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
            if (p0.id == R.id.bs_account_notifications_open_button) {
                listener?.onOpenUrl(
                    link
                )
            }
        }
    }

    interface AddAccountNotificationsBottomDialogListener {
        fun onOpenUrl(url: String?)
    }

    companion object {
        private const val ARG_CREATED = "arg_created"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_TEXT = "arg_text"
        private const val ARG_LINK_TEXT = "arg_link_text"
        private const val ARG_LINK = "arg_link"

        fun newInstance(
            created: String,
            title: String,
            text: String,
            linkText: String?,
            link: String?
        ): AccountNotificationsDetailsBottomDialogFragment {
            return AccountNotificationsDetailsBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CREATED, created)
                    putString(ARG_TITLE, title)
                    putString(ARG_TEXT, text)
                    putString(ARG_LINK_TEXT, linkText)
                    putString(ARG_LINK, link)
                }
            }
        }
    }
}
