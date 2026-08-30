package host.stjin.anonaddy.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.transition.MaterialSharedAxis
import host.stjin.anonaddy.databinding.FragmentSetupHow4Binding
import host.stjin.anonaddy.ui.base.BaseFragment
import host.stjin.anonaddy.utils.InsetUtils

class SetupHow4Fragment : BaseFragment(), RegistrationFormBottomDialogFragment.AddRegistrationFormBottomDialogFragmentListener {
    private var _binding: FragmentSetupHow4Binding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val forward = MaterialSharedAxis(MaterialSharedAxis.X, false)
        enterTransition = forward

        val backward = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = backward
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupHow4Binding.inflate(inflater, container, false)
        InsetUtils.applyBottomInset(binding.setupHow4Ll)

        val root = binding.root

        binding.setupHow4ButtonNext.setOnClickListener {
            val registrationFormBottomDialogFragment: RegistrationFormBottomDialogFragment =
                RegistrationFormBottomDialogFragment.newInstance()

            if (!registrationFormBottomDialogFragment.isAdded) {
                registrationFormBottomDialogFragment.show(
                    childFragmentManager,
                    "registrationFormBottomDialogFragment"
                )
            }
        }

        binding.setupHow4Iv.setOnClickListener {
            (activity as? SetupNewActivity)?.switchFragments(SetupHow3Fragment())
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRegistered() {
        (activity as? SetupNewActivity)?.finish()
    }
}
