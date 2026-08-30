package host.stjin.anonaddy.ui.intent

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.databinding.BottomsheetIntentBinding


class IntentBottomDialogFragment : BaseBottomSheetDialogFragment() {
    private var listener: IntentBottomDialogListener? = null

    private var _binding: BottomsheetIntentBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetIntentBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = (parentFragment as? IntentBottomDialogListener) ?: (activity as? IntentBottomDialogListener)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCancel(dialog: DialogInterface) {
        listener?.onClose()
        super.onCancel(dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    fun setText(string: String) {
        if (_binding != null) {
            binding.bsIntentTextview.text = string
        }
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface IntentBottomDialogListener {
        fun onClose()
    }

    companion object {
        fun newInstance(): IntentBottomDialogFragment {
            return IntentBottomDialogFragment()
        }
    }
}
