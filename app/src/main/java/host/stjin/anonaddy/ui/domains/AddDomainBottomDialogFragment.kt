package host.stjin.anonaddy.ui.domains

import android.app.Dialog
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import kotlinx.android.synthetic.main.bottomsheet_adddomain.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class AddDomainBottomDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {


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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // get the views and attach the listener
        val root = inflater.inflate(
            R.layout.bottomsheet_adddomain, container,
            false
        )
        listener = activity as AddDomainBottomDialogListener


        // 2. Setup a callback when the "Done" button is pressed on keyboard
        root.bs_adddomain_domain_add_domain_button.setOnClickListener(this)
        root.bs_adddomain_domain_tiet.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                addDomain(root, requireContext())
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

    private fun addDomain(root: View, context: Context) {

        if (!android.util.Patterns.DOMAIN_NAME.matcher(root.bs_adddomain_domain_tiet.text.toString())
                .matches()
        ) {
            root.bs_adddomain_domain_til.error =
                context.resources.getString(R.string.not_a_valid_address)
            return
        }

        this.domain = root.bs_adddomain_domain_tiet.text.toString()
        // Set error to null if domain and alias is valid
        root.bs_adddomain_domain_til.error = null
        root.bs_adddomain_domain_add_domain_button.isEnabled = false
        root.bs_adddomain_domain_progressbar.visibility = View.VISIBLE
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            addDomainToAccount(
                root,
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
        root: View,
        context: Context,
        address: String
    ) {
        val networkHelper = NetworkHelper(context)
        networkHelper.addDomain({ result, body ->
            when (result) {
                "404" -> {
                    openSetup(root, body)
                }
                "201" -> {
                    handler.removeCallbacksAndMessages(null)
                    listener.onAdded()
                }
                else -> {
                    handler.removeCallbacksAndMessages(null)
                    root.bs_add_domain_setup_1.visibility = View.VISIBLE
                    root.bs_add_domain_setup_2.visibility = View.GONE
                    root.bs_adddomain_domain_add_domain_button.isEnabled = true
                    root.bs_adddomain_domain_progressbar.visibility = View.INVISIBLE
                    root.bs_adddomain_domain_til.error =
                        context.resources.getString(R.string.error_adding_domain) + "\n" + result
                }
            }
        }, address)
    }

    private val handler = Handler()
    private val runnableCode = Runnable {
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            addDomainToAccount(
                requireView(),
                requireContext(),
                this@AddDomainBottomDialogFragment.domain
            )
        }
    }


    private fun openSetup(root: View, body: String?) {
        if (root.bs_add_domain_setup_1.visibility != View.GONE) {
            var anim = AlphaAnimation(1.0f, 0.0f)
            anim.duration = 500
            anim.repeatMode = Animation.REVERSE
            root.bs_add_domain_setup_1.startAnimation(anim)
            Handler(Looper.getMainLooper()).postDelayed({
                root.bs_add_domain_setup_1.visibility = View.GONE
                root.bs_add_domain_setup_2.visibility = View.VISIBLE
                anim = AlphaAnimation(0.0f, 1.0f)
                anim.duration = 500
                anim.repeatMode = Animation.REVERSE
                root.bs_add_domain_setup_2.startAnimation(anim)
            }, anim.duration)
        }

        // Update body text
        updateSetupStatus(root, body)

        //Re-get the status in 10 seconds
        handler.postDelayed(runnableCode, 30000)
    }

    private fun updateSetupStatus(root: View, text: String?) {
        var anim = AlphaAnimation(1.0f, 0.0f)
        anim.duration = 500
        anim.repeatMode = Animation.REVERSE
        root.bs_add_domain_setup_2.startAnimation(anim)
        Handler(Looper.getMainLooper()).postDelayed({
            anim = AlphaAnimation(0.0f, 1.0f)
            anim.duration = 500
            anim.repeatMode = Animation.REVERSE
            root.bs_add_domain_setup_2_text.text = text
            root.bs_add_domain_setup_2.startAnimation(anim)
        }, anim.duration)
    }


    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_adddomain_domain_add_domain_button) {
                addDomain(requireView(), requireContext())
            }
        }
    }
}