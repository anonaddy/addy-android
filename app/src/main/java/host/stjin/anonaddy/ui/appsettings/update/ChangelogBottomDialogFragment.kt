package host.stjin.anonaddy.ui.appsettings.update

import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetChangelogBinding


class ChangelogBottomDialogFragment : BaseBottomSheetDialogFragment() {
    private var _binding: BottomsheetChangelogBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetChangelogBinding.inflate(inflater, container, false)
        val root = binding.root


        context?.resources?.getString(R.string.app_changelog)?.let {
            binding.bsChangelogTextview.text = androidx.core.text.HtmlCompat.fromHtml(it, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY)
        }

        // Allow hyperlinks to be clicked
        binding.bsChangelogTextview.movementMethod = LinkMovementMethod.getInstance()

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

    companion object {
        fun newInstance(): ChangelogBottomDialogFragment {
            return ChangelogBottomDialogFragment()
        }
    }
}
