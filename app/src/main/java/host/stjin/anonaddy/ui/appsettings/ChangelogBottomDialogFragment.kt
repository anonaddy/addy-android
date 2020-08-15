package host.stjin.anonaddy.ui.appsettings

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import host.stjin.anonaddy.R
import kotlinx.android.synthetic.main.bottomsheet_changelog.view.*


class ChangelogBottomDialogFragment : BottomSheetDialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // get the views and attach the listener
        val root = inflater.inflate(
            R.layout.bottomsheet_changelog, container,
            false
        )


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            root.bs_changelog_textview.text = Html.fromHtml(
                context?.resources?.getString(R.string.app_changelog),
                Html.FROM_HTML_MODE_LEGACY
            )
        } else {
            root.bs_changelog_textview.text =
                Html.fromHtml(context?.resources?.getString(R.string.app_changelog))
        }
        return root

    }

    companion object {
        fun newInstance(): ChangelogBottomDialogFragment {
            return ChangelogBottomDialogFragment()
        }
    }

}