package host.stjin.anonaddy.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetAddyioInstanceVersionUnsupportedBinding


class UnsupportedBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private var listener: UnsupportedBottomDialogListener? = null

    private var _binding: BottomsheetAddyioInstanceVersionUnsupportedBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetAddyioInstanceVersionUnsupportedBinding.inflate(inflater, container, false)
        // get the views and attach the listener
        val root = binding.root

        listener = (parentFragment as? UnsupportedBottomDialogListener) ?: (activity as? UnsupportedBottomDialogListener)

        // 2. Setup a callback when the "Done" button is pressed on keyboard
        binding.bsAnonaddyInstanceVersionUnsupportedHowToUpdateButton.setOnClickListener(this)
        binding.bsAnonaddyInstanceVersionUnsupportedIgnoreButton.setOnClickListener(this)


        context?.resources?.getString(R.string.addyio_instance_version_unsupported)?.let {
            binding.bsAnonaddyInstanceVersionUnsupportedTextview.text = androidx.core.text.HtmlCompat.fromHtml(it, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY)
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

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener?.onClickIgnore()
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_anonaddy_instance_version_unsupported_how_to_update_button) {
                listener?.onClickHowToUpdate()
            } else if (p0.id == R.id.bs_anonaddy_instance_version_unsupported_ignore_button) {
                listener?.onClickIgnore()
            }
        }
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface UnsupportedBottomDialogListener {
        fun onClickHowToUpdate()
        fun onClickIgnore()
    }

    companion object {
        fun newInstance(): UnsupportedBottomDialogFragment {
            return UnsupportedBottomDialogFragment()
        }
    }
}
