package host.stjin.anonaddy.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import host.stjin.anonaddy.databinding.FragmentSetupHow1Binding
import host.stjin.anonaddy.utils.InsetUtil

class SetupHow1Fragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val forward = MaterialSharedAxis(MaterialSharedAxis.X, false)
        enterTransition = forward

        val backward = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = backward
    }


    private var _binding: FragmentSetupHow1Binding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupHow1Binding.inflate(inflater, container, false)
        InsetUtil.applyBottomInset(binding.setupHow1Ll)

        val root = binding.root

        binding.setupHow1ButtonNext.setOnClickListener {
            (activity as SetupNewActivity).switchFragments(SetupHow2Fragment())
        }

        binding.setupHow1Iv.setOnClickListener {
            (activity as SetupNewActivity).onBackPressedDispatcher.onBackPressed()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}