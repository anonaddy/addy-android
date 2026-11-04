package host.stjin.anonaddy.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.databinding.BottomsheetEmailVerificationBinding
import host.stjin.anonaddy.ui.base.BaseBottomSheetDialogFragment
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.launch

class EmailVerificationBottomDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {

    private var listener: EmailVerificationBottomDialogListener? = null
    private var _binding: BottomsheetEmailVerificationBinding? = null
    private val binding get() = _binding!!
    private var query: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        query = arguments?.getString(ARG_QUERY)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEmailVerificationBinding.inflate(inflater, container, false)
        listener = (parentFragment as? EmailVerificationBottomDialogListener) ?: (activity as? EmailVerificationBottomDialogListener)

        binding.bsEmailVerificationDesc.text = HtmlCompat.fromHtml(
            resources.getString(R.string.email_verification_desc),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        binding.bsEmailVerificationFooterExpiration.text = HtmlCompat.fromHtml(
            resources.getString(R.string.email_verification_footer_expiration),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        binding.bsEmailVerificationAcceptButton.setOnClickListener(this)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onClick(view: View?) {
        if (view?.id == R.id.bs_email_verification_accept_button) {
            val verifyQuery = query
            if (verifyQuery.isNullOrEmpty()) {
                dismiss()
                return
            }

            binding.bsEmailVerificationAcceptButton.startAnimation()
            isCancelable = false

            viewLifecycleOwner.lifecycleScope.launch {
                val userRepository = ServiceLocator.userRepository
                when (val result = userRepository.verifyRegistration(verifyQuery)) {
                    is NetworkResult.Success -> {
                        val apiKey = result.data
                        if (apiKey.isNotEmpty()) {
                            dismissAllowingStateLoss()
                            listener?.onVerificationSuccess(apiKey)
                        } else {
                            isCancelable = true
                            binding.bsEmailVerificationAcceptButton.revertAnimation()
                        }
                    }
                    is NetworkResult.Error -> {
                        isCancelable = true
                        binding.bsEmailVerificationAcceptButton.revertAnimation()

                        MaterialDialogHelper.showMaterialDialog(
                            context = requireContext(),
                            title = resources.getString(R.string.email_verification),
                            message = result.error,
                            icon = R.drawable.ic_key,
                            neutralButtonText = resources.getString(R.string.close)
                        ).show()
                    }
                }
            }
        }
    }

    interface EmailVerificationBottomDialogListener {
        fun onVerificationSuccess(apiKey: String)
    }

    companion object {
        private const val ARG_QUERY = "query"

        fun newInstance(query: String): EmailVerificationBottomDialogFragment {
            return EmailVerificationBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_QUERY, query)
                }
            }
        }
    }
}
