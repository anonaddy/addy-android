package host.stjin.anonaddy.ui.appsettings.update

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetChangelogBinding


class ChangelogBottomDialogFragment : BaseBottomSheetDialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetChangelogBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetChangelogBinding.inflate(inflater, container, false)
        val root = binding.root


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.bsChangelogTextview.text = Html.fromHtml(
                context?.resources?.getString(R.string.app_changelog),
                Html.FROM_HTML_MODE_LEGACY
            )
        } else {
            binding.bsChangelogTextview.text =
                Html.fromHtml(context?.resources?.getString(R.string.app_changelog))
        }

        // Allow hyperlinks to be clicked
        binding.bsChangelogTextview.movementMethod = LinkMovementMethod.getInstance()



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setIMEAnimation(binding.bsChangelogRoot)
        }

        return root

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): ChangelogBottomDialogFragment {
            return ChangelogBottomDialogFragment()
        }
    }
}