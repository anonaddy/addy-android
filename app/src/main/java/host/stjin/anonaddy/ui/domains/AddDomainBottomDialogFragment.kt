package host.stjin.anonaddy.ui.domains

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetAdddomainBinding
import host.stjin.anonaddy_shared.NetworkHelper
import kotlinx.coroutines.launch


class AddDomainBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddDomainBottomDialogListener
    private lateinit var domain: String

    // 1. Defines the listener interface with a method passing back data result.
    interface AddDomainBottomDialogListener {
        fun onAdded()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetAdddomainBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetAdddomainBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = parentFragment as AddDomainBottomDialogListener


        // 2. Setup a callback when the "Done" button is pressed on keyboard
        binding.bsAdddomainDomainAddDomainButton.setOnClickListener(this)
        binding.bsAdddomainDomainTiet.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                addDomain(requireContext())
            }
            false
        }

        return root

    }


    companion object {
        fun newInstance(): AddDomainBottomDialogFragment {
            return AddDomainBottomDialogFragment()
        }
    }

    private fun addDomain(context: Context) {

        if (!android.util.Patterns.DOMAIN_NAME.matcher(binding.bsAdddomainDomainTiet.text.toString())
                .matches()
        ) {
            binding.bsAdddomainDomainTil.error =
                context.resources.getString(R.string.not_a_valid_address)
            return
        }

        this.domain = binding.bsAdddomainDomainTiet.text.toString()
        // Set error to null if domain and alias is valid
        binding.bsAdddomainDomainTil.error = null

        // Animate the button to progress
        binding.bsAdddomainDomainAddDomainButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            addDomainToAccount(
                context,
                this@AddDomainBottomDialogFragment.domain
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        handler.removeCallbacksAndMessages(null)
    }

    private suspend fun addDomainToAccount(
        context: Context,
        address: String
    ) {
        val networkHelper = NetworkHelper(context)
        networkHelper.addDomain({ _, error, body ->
            when (error) {
                "404" -> {
                    openSetup(body)
                }
                "201" -> {
                    handler.removeCallbacksAndMessages(null)
                    listener.onAdded()
                }
                else -> {
                    handler.removeCallbacksAndMessages(null)
                    binding.bsAddDomainSetup1.visibility = View.VISIBLE
                    binding.bsAddDomainSetup2.visibility = View.GONE

                    // Revert the button to normal
                    binding.bsAdddomainDomainAddDomainButton.revertAnimation()

                    binding.bsAdddomainDomainTil.error =
                        context.resources.getString(R.string.error_adding_domain) + "\n" + body
                }
            }
        }, address)
    }

    private val handler =  Handler(Looper.getMainLooper())
    private val runnableCode = Runnable {
        viewLifecycleOwner.lifecycleScope.launch {
            addDomainToAccount(
                requireContext(),
                this@AddDomainBottomDialogFragment.domain
            )
        }
    }


    private fun openSetup(body: String?) {
        if (binding.bsAddDomainSetup1.visibility != View.GONE) {
            var anim = AlphaAnimation(1.0f, 0.0f)
            anim.duration = 500
            anim.repeatMode = Animation.REVERSE
            binding.bsAddDomainSetup1.startAnimation(anim)
            Handler(Looper.getMainLooper()).postDelayed({
                binding.bsAddDomainSetup1.visibility = View.GONE
                binding.bsAddDomainSetup2.visibility = View.VISIBLE
                anim = AlphaAnimation(0.0f, 1.0f)
                anim.duration = 500
                anim.repeatMode = Animation.REVERSE
                binding.bsAddDomainSetup2.startAnimation(anim)
            }, anim.duration)
        }

        if (!body.isNullOrEmpty()){
            val range = body.indexOf("aa-verify=")
            if (range != -1) {
                val result = body.substring(range)

                val clipboard: ClipboardManager =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("verification_record", result)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(requireContext(), resources.getString(R.string.verification_record_copied_to_clipboard), Toast.LENGTH_LONG).show()
            }
        }


        // Update body text
        updateSetupStatus(body)

        //Re-get the status in 30 seconds
        handler.postDelayed(runnableCode, 30000)
    }

    private fun updateSetupStatus(text: String?) {
        var anim = AlphaAnimation(1.0f, 0.0f)
        anim.duration = 500
        anim.repeatMode = Animation.REVERSE
        binding.bsAddDomainSetup2.startAnimation(anim)
        Handler(Looper.getMainLooper()).postDelayed({
            anim = AlphaAnimation(0.0f, 1.0f)
            anim.duration = 500
            anim.repeatMode = Animation.REVERSE
            binding.bsAddDomainSetup2Text.text = text
            binding.bsAddDomainSetup2.startAnimation(anim)
        }, anim.duration)
    }


    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_adddomain_domain_add_domain_button) {
                addDomain(requireContext())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}