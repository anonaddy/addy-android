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
import host.stjin.anonaddy.databinding.BottomsheetAnonaddyInstanceVersionUnsupportedBinding


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

    private var _binding: BottomsheetAnonaddyInstanceVersionUnsupportedBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetAnonaddyInstanceVersionUnsupportedBinding.inflate(inflater, container, false)
        // get the views and attach the listener
        val root = binding.root

        listener = activity as UnsupportedBottomDialogListener

        // 2. Setup a callback when the "Done" button is pressed on keyboard
        binding.bsAnonaddyInstanceVersionUnsupportedHowToUpdateButton.setOnClickListener(this)
        binding.bsAnonaddyInstanceVersionUnsupportedIgnoreButton.setOnClickListener(this)


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            binding.bsAnonaddyInstanceVersionUnsupportedTextview.text = Html.fromHtml(
                context?.resources?.getString(R.string.anonaddy_instance_version_unsupported),
                Html.FROM_HTML_MODE_LEGACY
            )
        } else {
            binding.bsAnonaddyInstanceVersionUnsupportedTextview.text =
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}