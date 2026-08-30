package host.stjin.anonaddy.ui.setup

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.security.KeyChain
import android.security.KeyChainAliasCallback
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.databinding.BottomsheetApiBinding
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.managers.SettingsManager.PREFS
import host.stjin.anonaddy_shared.models.LoginMfaRequired
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.repositories.LoginResult
import host.stjin.anonaddy_shared.repositories.UserRepository
import host.stjin.anonaddy_shared.utils.NetworkUtils
import kotlinx.coroutines.launch

class AddApiBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private var apiBaseUrl: String? = null
    private var codeScanner: CodeScanner? = null

    private var listener: AddApiBottomDialogListener? = null
    private lateinit var userRepository: UserRepository

    private var _binding: BottomsheetApiBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var expirationOptions: List<String> = listOf()

    private var expirationOptionNames: List<String> = listOf()

    private var otpMfaObject: LoginMfaRequired? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiBaseUrl = arguments?.getString(ARG_API_BASE_URL)
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

    private var localNetworkPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission granted, re-trigger verifyLogin
            lifecycleScope.launch {
                verifyLogin(requireContext())
            }
        } else {
            // Permission denied, show error
            binding.bsSetupInstanceTil.error = requireContext().resources.getString(R.string.local_network_permission_required)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetApiBinding.inflate(inflater, container, false)
        val root = binding.root

        listener = (parentFragment as? AddApiBottomDialogListener) ?: (activity as? AddApiBottomDialogListener)
        userRepository = ServiceLocator.userRepository


        // Check that the device will let you use the camera
        val pm = context?.packageManager
        if (pm?.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) == true) {
            initQrScanner()
        }


        // if apiBaseUrl set, lock it in and set username
        if (apiBaseUrl != null) {
            binding.bsSetupInstanceTiet.setText(apiBaseUrl)
            binding.bsSetupInstanceTiet.isEnabled = false
            binding.bsSetupInstanceTil.isEnabled = false

            binding.bsSetupApikeyUsernameTiet.setText((activity?.application as? AddyIoApp)?.userResourceOrNull?.username ?: "")
            binding.bsSetupApikeyUsernameTiet.isEnabled = false
            binding.bsSetupApikeyUsernameTil.isEnabled = false
        }

        // 2. Setup a callback when the "Done" button is pressed on keyboard
        binding.bsSetupApikeySignInButton.setOnClickListener(this)
        binding.bsSetupApikeyGetButton.setOnClickListener(this)
        binding.bsSetupApikeySelectCert.setOnClickListener(this)
        binding.bsSetupScannerView.setOnClickListener(this)


        binding.bsSetupApikeyTiet.setOnTouchListener { view, motionEvent ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            if ((motionEvent.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                view.parent.requestDisallowInterceptTouchEvent(false)
            }
            return@setOnTouchListener false
        }

        setMaterialButtonToggleGroupListener()
        fillSpinners(requireContext())
        checkForCertificate()

        return root
    }

    override fun onPause() {
        // Stop preview when the app gets suspended
        codeScanner?.stopPreview()
        // Release resources to prevent draining as well as giving other apps a chance to use the camera
        codeScanner?.releaseResources()
        super.onPause()
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

    override fun onClick(p0: View?) {
        if (p0 != null) {
            when (p0.id) {
                R.id.bs_setup_apikey_sign_in_button -> {
                    lifecycleScope.launch {
                        verifyLogin(
                            requireContext()
                        )
                    }
                }

                R.id.bs_setup_apikey_get_button -> {
                    val baseUrl = binding.bsSetupInstanceTiet.text.toString()

                    val url = "$baseUrl/settings/api"
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = url.toUri()
                    startActivity(i)
                }

                R.id.bs_setup_scanner_view -> {
                    toggleQrCodeScanning()
                }

                R.id.bs_setup_apikey_select_cert -> {
                    val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
                    val alias = encryptedSettingsManager.getSettingsString(PREFS.CERTIFICATE_ALIAS)
                    if (alias == null) {
                        selectCertificate()
                    } else {
                        encryptedSettingsManager.removeSetting(PREFS.CERTIFICATE_ALIAS)
                        Toast.makeText(requireContext(), requireContext().resources.getString(R.string.certificate_removed), Toast.LENGTH_SHORT)
                            .show()
                        checkForCertificate()
                    }
                }
            }
        }
    }

    private fun checkForCertificate() {
        val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
        val alias = encryptedSettingsManager.getSettingsString(PREFS.CERTIFICATE_ALIAS)

        if (alias != null) {
            val tintColor = ContextCompat.getColor(requireContext(), R.color.md_theme_tertiary) // Use a color resource
            binding.bsSetupApikeySelectCert.drawable.setTint(tintColor)
        } else {
            val tintColor = ContextCompat.getColor(requireContext(), R.color.BlackWhite) // Use a color resource
            binding.bsSetupApikeySelectCert.drawable.setTint(tintColor)

        }
    }

    private fun setMaterialButtonToggleGroupListener() {
        binding.bsSetupManualTypeUsernamePasswordButton.setOnClickListener {
            binding.bsSetupManualTypeUsernamePasswordButton.isChecked = true

            binding.bsSetupApikeyUsernameApiSection.visibility = View.GONE
            binding.bsSetupApikeyUsernamePasswordSection.visibility = View.VISIBLE
        }

        binding.bsSetupManualTypeApiButton.setOnClickListener {
            binding.bsSetupManualTypeApiButton.isChecked = true

            binding.bsSetupApikeyUsernameApiSection.visibility = View.VISIBLE
            binding.bsSetupApikeyUsernamePasswordSection.visibility = View.GONE
        }
    }

    private fun fillSpinners(context: Context) {
        expirationOptions = this.resources.getStringArray(R.array.expiration_options).toList()
        expirationOptionNames = this.resources.getStringArray(R.array.expiration_options_names).toList()

        val expirationAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            R.layout.dropdown_menu_popup_item,
            expirationOptionNames
        )
        binding.bsRegistrationFormExpirationMact.setAdapter(expirationAdapter)
    }

    private fun initQrScanner() {
        binding.bsSetupQrLL.visibility = View.VISIBLE
        // Initialize the codeScanner, this won't start the camera yet.
        codeScanner = CodeScanner(requireActivity(), binding.bsSetupScannerView)
        // Initialize the codeScanner
        codeScanner!!.decodeCallback = DecodeCallback {
            requireActivity().runOnUiThread {
                // Verify if the scanned QR code has all the properties
                if (isQrCodeFormattedCorrect(it.text)) {

                    binding.bsSetupManualTypeApiButton.isChecked = true

                    binding.bsSetupApikeyUsernameApiSection.visibility = View.VISIBLE
                    binding.bsSetupApikeyUsernamePasswordSection.visibility = View.GONE

                    // if apiBaseUrl set, do not set the baseURL using QR
                    if (apiBaseUrl == null) {
                        // Get the string part before the | delimiter
                        binding.bsSetupInstanceTiet.setText(it.text.substringBeforeLast("|", ""))
                    }
                    // Get the string part after the | delimiter
                    binding.bsSetupApikeyTiet.setText(it.text.substringAfterLast("|", ""))

                    lifecycleScope.launch {
                        verifyLogin(requireContext())
                    }

                } else {
                    MaterialDialogHelper.showMaterialDialog(
                        context = requireContext(),
                        title = resources.getString(R.string.api_setup_qr_code_scan_wrong),
                        message = resources.getString(R.string.api_setup_qr_code_scan_wrong_desc),
                        icon = R.drawable.ic_key,
                        neutralButtonAction = {
                            codeScanner!!.startPreview()
                        },
                        neutralButtonText = resources.getString(R.string.close)
                    ).setCancelable(false).show()

                }

            }
        }

        toggleQrCodeScanning()
    }

    private suspend fun verifyLogin(context: Context) {

        // Check if the instance is a valid web address and starts with https:// or http://
        if (!android.util.Patterns.WEB_URL.matcher(binding.bsSetupInstanceTiet.text.toString())
                .matches() || !(binding.bsSetupInstanceTiet.text?.startsWith("https://") == true || binding.bsSetupInstanceTiet.text?.startsWith("http://") == true)
        ) {
            binding.bsSetupInstanceTil.error =
                context.resources.getString(R.string.not_a_valid_web_address)
            return
        }

        var baseUrl = binding.bsSetupInstanceTiet.text.toString().trim()
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.removeSuffix("/")
        }
        binding.bsSetupInstanceTil.error = null

        // Proactive local network permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            if (NetworkUtils.isLocalAddressRobust(baseUrl)) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
                    localNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                    return
                }
            }
        }


        if (binding.bsSetupManualTypeApiButton.isChecked) {
            val apiKey = binding.bsSetupApikeyTiet.text.toString().trim()

            binding.bsSetupApikeyGetButton.isEnabled = false

            // Animate the button to progress
            binding.bsSetupApikeySignInButton.startAnimation()
            verifyApiKey(context, apiKey, baseUrl)

        } else {
            val expirationOption = expirationOptions[expirationOptionNames.indexOf(binding.bsRegistrationFormExpirationMact.text.toString())]

            binding.bsSetupApikeyUsernameTil.error = null
            binding.bsSetupApikeyPasswordTil.error = null
            binding.bsSetupApikeyOtpTil.error = null


            if (binding.bsSetupApikeyUsernameTiet.text.isNullOrEmpty()) {
                binding.bsSetupApikeyUsernameTil.error = requireContext().resources.getString(R.string.registration_username_empty)
                return
            }


            if (binding.bsSetupApikeyPasswordTiet.text.isNullOrEmpty()) {
                binding.bsSetupApikeyPasswordTil.error = requireContext().resources.getString(R.string.registration_password_empty)
                return
            }


            if (otpMfaObject != null) {
                if (binding.bsSetupApikeyOtpTiet.text.isNullOrEmpty()) {
                    binding.bsSetupApikeyOtpTil.error = requireContext().resources.getString(R.string.otp_required)
                    return
                }

                binding.bsSetupApikeySignInButton.startAnimation()
                viewLifecycleOwner.lifecycleScope.launch {
                    val mfaResult = userRepository.loginMfa(
                        baseUrl = baseUrl,
                        mfaKey = otpMfaObject!!.mfa_key,
                        otp = binding.bsSetupApikeyOtpTiet.text.toString(),
                        apiExpiration = expirationOption,
                        cookies = otpMfaObject!!.cookie
                    )
                    when (mfaResult) {
                        is NetworkResult.Success -> {
                            listener?.onClickSave(baseUrl, mfaResult.data.api_key)
                        }
                        is NetworkResult.Error -> {
                            this@AddApiBottomDialogFragment.otpMfaObject = null
                            binding.bsSetupApikeyOtpTil.visibility = View.GONE
                            binding.bsSetupApikeyOtpTiet.text = null

                            MaterialDialogHelper.showMaterialDialog(
                                context = requireContext(),
                                title = resources.getString(R.string.login),
                                message = mfaResult.error,
                                icon = R.drawable.ic_key,
                                neutralButtonText = resources.getString(R.string.close)
                            ).show()
                            binding.bsSetupApikeySignInButton.revertAnimation()
                        }
                    }
                }

            } else {
                binding.bsSetupApikeySignInButton.startAnimation()
                viewLifecycleOwner.lifecycleScope.launch {
                    when (val loginResult = userRepository.login(
                        baseUrl = baseUrl,
                        username = binding.bsSetupApikeyUsernameTiet.text.toString(),
                        password = binding.bsSetupApikeyPasswordTiet.text.toString(),
                        apiExpiration = expirationOption
                    )) {
                        is LoginResult.Success -> {
                            listener?.onClickSave(baseUrl, loginResult.login.api_key)
                        }
                        is LoginResult.MfaRequired -> {
                            this@AddApiBottomDialogFragment.otpMfaObject = loginResult.mfa
                            binding.bsSetupApikeyOtpTil.visibility = View.VISIBLE
                            binding.bsSetupApikeySignInButton.revertAnimation()
                        }
                        is LoginResult.Error -> {
                            val displayError = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
                                (loginResult.error ?: "") + "\n\n" + resources.getString(R.string.local_network_permission_rationale)
                            } else {
                                loginResult.error
                            }

                            MaterialDialogHelper.showMaterialDialog(
                                context = requireContext(),
                                title = resources.getString(R.string.login),
                                message = displayError,
                                icon = R.drawable.ic_key,
                                neutralButtonText = resources.getString(R.string.close)
                            ).show()
                            binding.bsSetupApikeySignInButton.revertAnimation()
                        }
                    }
                }
            }

        }


    }

    private fun verifyApiKey(context: Context, apiKey: String, baseUrl: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val verifyResult = userRepository.verifyApiKey(baseUrl, apiKey)) {
                is NetworkResult.Success -> {
                    val result = verifyResult.data
                    if (ServiceLocator.encryptedSettingsManager.getSettingsString(PREFS.API_KEY) == null ||
                        (activity?.application as? AddyIoApp)?.userResourceOrNull?.id == result.id
                    ) {
                        listener?.onClickSave(baseUrl, apiKey)
                    } else {
                        binding.bsSetupApikeyGetButton.isEnabled = true
                        binding.bsSetupApikeySignInButton.revertAnimation()

                        binding.bsSetupApikeyTil.error =
                            context.resources.getString(R.string.api_belongs_other_account)

                        toggleQrCodeScanning()
                    }
                }
                is NetworkResult.Error -> {
                    binding.bsSetupApikeyGetButton.isEnabled = true
                    binding.bsSetupApikeySignInButton.revertAnimation()

                    val displayError = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
                        (verifyResult.error ?: "") + "\n\n" + resources.getString(R.string.local_network_permission_rationale)
                    } else {
                        verifyResult.error
                    }

                    binding.bsSetupApikeyTil.error =
                        context.resources.getString(R.string.api_invalid) + "\n" + displayError

                    toggleQrCodeScanning()
                }
            }
        }
    }

    private fun selectCertificate() {
        KeyChain.choosePrivateKeyAlias(requireActivity(), object : KeyChainAliasCallback {
            override fun alias(alias: String?) {
                // If user denies access to the selected certificate
                if (alias == null) {
                    return
                }

                ServiceLocator.encryptedSettingsManager.putSettingsString(PREFS.CERTIFICATE_ALIAS, alias)
                ServiceLocator.settingsManager.putSettingsBool(
                    PREFS.NOTIFY_CERTIFICATE_EXPIRY,
                    true
                ) // Enable by default when a certificate has been selected

                // Since certificate expiry should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(requireContext()).scheduleBackgroundWorker()

                // Re-init as an alias was added
                userRepository = ServiceLocator.userRepository

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), requireContext().resources.getString(R.string.certificate_selected), Toast.LENGTH_SHORT).show()
                    checkForCertificate()
                }
            }
        }, null, null, null, null)
    }

    private fun toggleQrCodeScanning() {
        // Check if camera permissions are granted
        if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PermissionChecker.PERMISSION_GRANTED) {
            resultLauncher.launch(Manifest.permission.CAMERA)
        } else {
            binding.bsSetupScannerViewDesc.text = requireContext().resources.getString(R.string.api_setup_qr_code_scan_desc)

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

    // 1. Defines the listener interface with a method passing back data result.
    interface AddApiBottomDialogListener {
        fun onClickSave(baseUrl: String, apiKey: String)
    }

    companion object {
        private const val ARG_API_BASE_URL = "arg_api_base_url"

        fun newInstance(apiBaseUrl: String? = null): AddApiBottomDialogFragment {
            return AddApiBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_API_BASE_URL, apiBaseUrl)
                }
            }
        }
    }
}
