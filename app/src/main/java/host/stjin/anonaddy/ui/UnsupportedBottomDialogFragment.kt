package host.stjin.anonaddy.ui

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
import kotlinx.android.synthetic.main.bottomsheet_anonaddy_instance_version_unsupported.view.*


class UnsupportedBottomDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var listener: UnsupportedBottomDialogListener


    // 1. Defines the listener interface with a method passing back data result.
    interface UnsupportedBottomDialogListener {
        fun onClickHowToUpdate()
        fun onClickIgnore()
    }

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
            R.layout.bottomsheet_anonaddy_instance_version_unsupported, container,
            false
        )

        listener = activity as UnsupportedBottomDialogFragment.UnsupportedBottomDialogListener

        // 2. Setup a callback when the "Done" button is pressed on keyboard
        root.bs_anonaddy_instance_version_unsupported_how_to_update_button.setOnClickListener(this)
        root.bs_anonaddy_instance_version_unsupported_ignore_button.setOnClickListener(this)


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            root.bs_anonaddy_instance_version_unsupported_textview.text = Html.fromHtml(
                context?.resources?.getString(R.string.anonaddy_instance_version_unsupported),
                Html.FROM_HTML_MODE_LEGACY
            )
        } else {
            root.bs_anonaddy_instance_version_unsupported_textview.text =
                Html.fromHtml(context?.resources?.getString(R.string.anonaddy_instance_version_unsupported))
        }
        return root

    }

    companion object {
        fun newInstance(): UnsupportedBottomDialogFragment {
            return UnsupportedBottomDialogFragment()
        }
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_anonaddy_instance_version_unsupported_how_to_update_button) {
                listener.onClickHowToUpdate()
            } else if (p0.id == R.id.bs_anonaddy_instance_version_unsupported_ignore_button) {
                listener.onClickIgnore()
            }
        }
    }

}