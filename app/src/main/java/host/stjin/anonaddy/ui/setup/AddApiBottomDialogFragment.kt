package host.stjin.anonaddy.ui.setup

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.lifecycle.lifecycleScope
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetApiBinding
import host.stjin.anonaddy_shared.NetworkHelper
import kotlinx.coroutines.launch

class AddApiBottomDialogFragment(private val apiBaseUrl: String?) : BaseBottomSheetDialogFragment(), View.OnClickListener {

    private var codeScanner: CodeScanner? = null
    private lateinit var listener: AddApiBottomDialogListener
    private var networkHelper: NetworkHelper? = null


    // 1. Defines the listener interface with a method passing back data result.
    interface AddApiBottomDialogListener {
        fun onClickSave(baseUrl: String, apiKey: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetApiBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetApiBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = activity as AddApiBottomDialogListener
        networkHelper = NetworkHelper(requireContext())


        // Check that the device will let you use the camera
        val pm = context?.packageManager
        if (pm?.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) == true) {
            initQrScanner()
        }


        // if apiBaseUrl set, lock it in
        if (apiBaseUrl != null) {
            binding.bsSetupInstanceTiet.setText(apiBaseUrl)
            binding.bsSetupInstanceTiet.isEnabled = false
            binding.bsSetupInstanceTil.isEnabled = false
        }

        // 2. Setup a callback when the "Done" button is pressed on keyboard
        binding.bsSetupApikeySignInButton.setOnClickListener(this)
        binding.bsSetupApikeyGetButton.setOnClickListener(this)
        binding.bsSetupApikeyTiet.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                verifyKey(requireContext())
            }
            false
        }


        binding.bsSetupApikeyTiet.setOnTouchListener { view, motionEvent ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            if ((motionEvent.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                view.parent.requestDisallowInterceptTouchEvent(false)
            }
            return@setOnTouchListener false
        }

        binding.bsSetupScannerView.setOnClickListener {
            toggleQrCodeScanning()
        }

        return root
    }

    private fun initQrScanner() {
        binding.bsSetupQrLL.visibility = View.VISIBLE
        binding.bsSetupManualApikeyTextview.text = context?.resources?.getString(R.string.api_obtain_camera_available)
        // Initialize the codeScanner, this won't start the camera yet.
        codeScanner = CodeScanner(requireActivity(), binding.bsSetupScannerView)
        // Initialize the codeScanner
        codeScanner!!.decodeCallback = DecodeCallback {
            requireActivity().runOnUiThread {
                // Verify if the scanned QR code has all the properties
                if (isQrCodeFormattedCorrect(it.text)) {

                    // if apiBaseUrl set, do not set the baseURL using QR
                    if (apiBaseUrl == null) {
                        // Get the string part before the | delimiter
                        binding.bsSetupInstanceTiet.setText(it.text.substringBeforeLast("|", ""))
                    }
                    // Get the string part after the | delimiter
                    binding.bsSetupApikeyTiet.setText(it.text.substringAfterLast("|", ""))
                    verifyKey(requireContext())
                } else {
                    binding.bsSetupScannerViewDesc.text = context?.resources?.getString(R.string.api_setup_qr_code_scan_wrong)
                    codeScanner!!.startPreview()
                }

            }
        }
    }


    companion object {
        fun newInstance(apiBaseUrl: String? = null): AddApiBottomDialogFragment {
            return AddApiBottomDialogFragment(apiBaseUrl)
        }
    }

    private fun verifyKey(context: Context) {
        var apiKey = binding.bsSetupApikeyTiet.text.toString()
        val baseUrl = binding.bsSetupInstanceTiet.text.toString()

        binding.bsSetupInstanceTil.error = null
        // Check if the alias is a valid web address and starts with https:// or http://
        if (!android.util.Patterns.WEB_URL.matcher(binding.bsSetupInstanceTiet.text.toString())
                .matches() || !(binding.bsSetupInstanceTiet.text?.startsWith("https://") == true || binding.bsSetupInstanceTiet.text?.startsWith("http://") == true)
        ) {
            binding.bsSetupInstanceTil.error =
                context.resources.getString(R.string.not_a_valid_web_address)
            return
        }

        binding.bsSetupApikeyGetButton.isEnabled = false

        // Animate the button to progress
        binding.bsSetupApikeySignInButton.startAnimation()


        // WORKAROUND #0002 START
        // Google (Play) refused my update a few times due to a lack of "testing credentials"
        // Google Play Console does not allow me to provide 1000+ char API keys for testing credentials.
        // This workaround checks if the entered API key starts with "https://" and if so. Will download the raw body content from the webpage and
        // use that as API key instead.
        //
        // This way 1000+ char API keys can be shortened to very short URL's
        // Maybe someone else finds another use for this as well :P

        viewLifecycleOwner.lifecycleScope.launch {
            if (apiKey.startsWith("https://")) {
                // API key start with https://
                // Perform a body-download of given URL and set that as API key instead
                networkHelper?.downloadBody(apiKey) { result, error ->
                    if (result != null) {
                        apiKey = result
                        verifyApiKey(context, apiKey, baseUrl)
                    } else {
                        binding.bsSetupApikeyGetButton.isEnabled = true

                        // Revert the button to normal
                        binding.bsSetupApikeySignInButton.revertAnimation()

                        binding.bsSetupApikeyTil.error =
                            context.resources.getString(R.string.api_invalid) + "\n" + error
                    }
                }
            } else {
                verifyApiKey(context, apiKey, baseUrl)
            }
        }

    }

    private fun verifyApiKey(context: Context, apiKey: String, baseUrl: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            networkHelper?.verifyApiKey(baseUrl, apiKey) { result ->
                if (result == "200") {
                    listener.onClickSave(baseUrl, apiKey)
                } else {
                    binding.bsSetupApikeyGetButton.isEnabled = true

                    // Revert the button to normal
                    binding.bsSetupApikeySignInButton.revertAnimation()

                    binding.bsSetupApikeyTil.error =
                        context.resources.getString(R.string.api_invalid) + "\n" + result
                }
            }
        }
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_setup_apikey_sign_in_button) {
                verifyKey(
                    requireContext()
                )
            } else if (p0.id == R.id.bs_setup_apikey_get_button) {
                val baseUrl = binding.bsSetupInstanceTiet.text.toString()

                val url = "$baseUrl/settings/api"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        }
    }


    override fun onPause() {
        // Stop preview when the app gets suspended
        codeScanner?.stopPreview()
        // Release resources to prevent draining as well as giving other apps a chance to use the camera
        codeScanner?.releaseResources()
        super.onPause()
    }

    private var resultLauncher: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        when (result) {
            true -> toggleQrCodeScanning()
            false -> {
                // Explain to the user that the feature is unavailable because
                // the features requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
                binding.bsSetupScannerViewDesc.text = requireContext().resources.getString(R.string.qr_permissions_required)
            }
        }
    }

    private fun toggleQrCodeScanning() {
        // Check if camera permissions are granted
        if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PermissionChecker.PERMISSION_GRANTED) {
            resultLauncher.launch(Manifest.permission.CAMERA)
        } else {
            // If codeScanner is initialized, switch between start en stopPreview
            if (codeScanner?.isPreviewActive == true) {
                codeScanner?.stopPreview()
                // Release resources to prevent draining as well as giving other apps a chance to use the camera
                codeScanner?.releaseResources()
            } else {
                codeScanner?.startPreview()
            }
        }
    }

    private fun isQrCodeFormattedCorrect(text: String): Boolean {
        return text.contains("|") && text.contains("http")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}