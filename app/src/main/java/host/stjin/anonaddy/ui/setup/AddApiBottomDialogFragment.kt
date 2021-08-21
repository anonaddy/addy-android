package host.stjin.anonaddy.ui.setup

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.lifecycle.lifecycleScope
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.NetworkHelper
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetApiBinding
import kotlinx.coroutines.launch

class AddApiBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {

    private var codeScanner: CodeScanner? = null
    private lateinit var listener: AddApiBottomDialogListener


    // 1. Defines the listener interface with a method passing back data result.
    interface AddApiBottomDialogListener {
        fun onClickSave(baseUrl: String, apiKey: String)
        fun onClickGetMyKey(baseUrl: String)
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


        // TODO ENABLE FEATURE WHEN ANONADDY IMPLEMENTED THIS
        // Make sure to also uncomment the <!-- BLOCK --> in manifest
        if (BuildConfig.DEBUG) {
            // Check that the device will let you use the camera
            val pm = context?.packageManager
            if (pm?.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) == true) {
                initQrScanner()
            }
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


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setIMEAnimation(binding.bsSetupRoot)
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
                    // Get the string part before the ; delimiter
                    binding.bsSetupInstanceTiet.setText(it.text.substringBeforeLast(";", ""))
                    // Get the string part after the ; delimiter
                    binding.bsSetupApikeyTiet.setText(it.text.substringAfterLast(";", ""))
                    verifyKey(requireContext())
                } else {
                    binding.bsSetupScannerViewDesc.text = context?.resources?.getString(R.string.api_setup_qr_code_scan_wrong)
                    codeScanner!!.startPreview()
                }

            }
        }
    }


    companion object {
        fun newInstance(): AddApiBottomDialogFragment {
            return AddApiBottomDialogFragment()
        }
    }

    private fun verifyKey(context: Context) {
        val apiKey = binding.bsSetupApikeyTiet.text.toString()
        val baseUrl = binding.bsSetupInstanceTiet.text.toString()
        binding.bsSetupApikeyGetButton.isEnabled = false

        // Animate the button to progress
        binding.bsSetupApikeySignInButton.startAnimation()

        viewLifecycleOwner.lifecycleScope.launch {
            verifyApiKey(context, apiKey, baseUrl)
        }
    }

    private suspend fun verifyApiKey(context: Context, apiKey: String, baseUrl: String) {
        val networkHelper = NetworkHelper(context)
        networkHelper.verifyApiKey(baseUrl, apiKey) { result ->
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

    override fun onClick(p0: View?) {
        if (p0 != null) {
            if (p0.id == R.id.bs_setup_apikey_sign_in_button) {
                verifyKey(
                    requireContext()
                )
            } else if (p0.id == R.id.bs_setup_apikey_get_button) {
                val baseUrl = binding.bsSetupInstanceTiet.text.toString()
                listener.onClickGetMyKey(baseUrl)
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

    private val CAMERA_REQUEST_CODE: Int = 1000


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    toggleQrCodeScanning()
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    binding.bsSetupScannerViewDesc.text = requireContext().resources.getString(R.string.qr_permissions_required)
                }
                return
            }
        }
    }

    private fun toggleQrCodeScanning() {
        // Check if camera permissions are granted
        if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PermissionChecker.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                ), 1000
            )
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
        return text.contains(";") && text.contains("http")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}