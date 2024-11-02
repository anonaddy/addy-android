package host.stjin.anonaddy.ui.setup

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import host.stjin.anonaddy.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.BottomsheetRegistrationFormBinding
import host.stjin.anonaddy.utils.CustomPatterns
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy_shared.NetworkHelper
import kotlinx.coroutines.launch

class RegistrationFormBottomDialogFragment: BaseBottomSheetDialogFragment(), View.OnClickListener {


    private lateinit var listener: AddRegistrationFormBottomDialogFragmentListener

    // 1. Defines the listener interface with a method passing back data result.
    interface AddRegistrationFormBottomDialogFragmentListener {
        fun onRegistered()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    private var _binding: BottomsheetRegistrationFormBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetRegistrationFormBinding.inflate(inflater, container, false)
        val root = binding.root
        listener = parentFragment as AddRegistrationFormBottomDialogFragmentListener

        binding.bsRegistrationFormRegisterButton.setOnClickListener(this)
        binding.bsRegistrationFormPrivacyPolicyButton.setOnClickListener(this)
        binding.bsRegistrationFormTermsOfServiceButton.setOnClickListener(this)

        fillSpinners(requireContext())

        return root

    }


    private var EXPIRATIONS: List<String> = listOf()
    private var EXPIRATIONS_NAME: List<String> = listOf()
    private fun fillSpinners(context: Context) {
        EXPIRATIONS = this.resources.getStringArray(R.array.expiration_options).toList()
        EXPIRATIONS_NAME = this.resources.getStringArray(R.array.expiration_options_names).toList()

        val expirationAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            R.layout.dropdown_menu_popup_item,
            EXPIRATIONS_NAME
        )
        binding.bsRegistrationFormExpirationMact.setAdapter(expirationAdapter)
    }


    private suspend fun registerUser() {
        binding.bsRegistrationFormUsernameTil.error = null
        binding.bsRegistrationFormEmailTil.error = null
        binding.bsRegistrationFormEmailVerifyTil.error = null
        binding.bsRegistrationFormPasswordTil.error = null
        binding.bsRegistrationFormPasswordVerifyTil.error = null

        if (binding.bsRegistrationFormUsernameTiet.text.isNullOrEmpty()){
            binding.bsRegistrationFormUsernameTil.error = requireContext().resources.getString(R.string.registration_username_empty)
            return
        }

        if (binding.bsRegistrationFormEmailTiet.text.isNullOrEmpty()){
            binding.bsRegistrationFormEmailTil.error = requireContext().resources.getString(R.string.registration_address_empty)
            return
        }

        if (binding.bsRegistrationFormEmailVerifyTiet.text.isNullOrEmpty()){
            binding.bsRegistrationFormEmailVerifyTil.error = requireContext().resources.getString(R.string.registration_address_empty)
            return
        }

        if (binding.bsRegistrationFormPasswordTiet.text.isNullOrEmpty()){
            binding.bsRegistrationFormPasswordTil.error = requireContext().resources.getString(R.string.registration_password_empty)
            return
        }

        if (binding.bsRegistrationFormPasswordVerifyTiet.text.isNullOrEmpty()){
            binding.bsRegistrationFormPasswordVerifyTil.error = requireContext().resources.getString(R.string.registration_password_confirm_empty)
            return
        }

        if (binding.bsRegistrationFormEmailTiet.text.toString() != binding.bsRegistrationFormEmailVerifyTiet.text.toString()) {
            binding.bsRegistrationFormEmailVerifyTil.error = requireContext().resources.getString(R.string.registration_email_confirm_mismatch)
            return
        }

        if (binding.bsRegistrationFormPasswordTiet.text.toString() != binding.bsRegistrationFormPasswordVerifyTiet.text.toString()) {
            binding.bsRegistrationFormPasswordVerifyTil.error = requireContext().resources.getString(R.string.registration_password_confirm_mismatch)
            return
        }

        if (!CustomPatterns.EMAIL_ADDRESS.matcher(binding.bsRegistrationFormEmailTiet.text.toString())
                .matches()
        ) {
            binding.bsRegistrationFormEmailTil.error =
                requireContext().resources.getString(R.string.not_a_valid_address)
            return
        }

        if (!CustomPatterns.EMAIL_ADDRESS.matcher(binding.bsRegistrationFormEmailVerifyTiet.text.toString())
                .matches()
        ) {
            binding.bsRegistrationFormEmailVerifyTiet.error =
                requireContext().resources.getString(R.string.not_a_valid_address)
            return
        }

        val expirationOption =  EXPIRATIONS[EXPIRATIONS_NAME.indexOf(binding.bsRegistrationFormExpirationMact.text.toString())]

        binding.bsRegistrationFormRegisterButton.startAnimation()
        val networkHelper = NetworkHelper(requireContext())
        networkHelper.registration({ result ->
            if (result == "204") {
                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = resources.getString(R.string.registration_register),
                    message = resources.getString(R.string.registration_success_verification_required),
                    icon = R.drawable.ic_mdi_hand_wave_outline,
                    positiveButtonText = resources.getString(R.string.understood),
                    positiveButtonAction = {
                        listener.onRegistered()
                        dismiss()
                    }
                ).show()
            } else {
                binding.bsRegistrationFormRegisterButton.revertAnimation()

                MaterialDialogHelper.showMaterialDialog(
                    context = requireContext(),
                    title = resources.getString(R.string.registration_register),
                    message = result,
                    icon = R.drawable.ic_mdi_hand_wave_outline,
                    neutralButtonText = resources.getString(R.string.close)
                ).show()
            }
        }, username = binding.bsRegistrationFormUsernameTiet.text.toString(), email = binding.bsRegistrationFormEmailTiet.text.toString(), password = binding.bsRegistrationFormPasswordTiet.text.toString(), apiExpiration = expirationOption)
    }


    companion object {
        fun newInstance(): RegistrationFormBottomDialogFragment {
            return RegistrationFormBottomDialogFragment()
        }
    }


    override fun onClick(p0: View?) {
        if (p0 != null) {
            when (p0.id) {
                R.id.bs_registration_form_register_button -> {
                    viewLifecycleOwner.lifecycleScope.launch {
                        registerUser()
                    }
                }
                R.id.bs_registration_form_privacy_policy_button -> {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://addy.io/privacy?ref=appstore")
                    )
                    startActivity(browserIntent)
                }
                R.id.bs_registration_form_terms_of_service_button -> {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://addy.io/terms?ref=appstore")
                    )
                    startActivity(browserIntent)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}