package host.stjin.anonaddy.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import host.stjin.anonaddy.databinding.FragmentSetupHow2Binding
import host.stjin.anonaddy.utils.InsetUtil

class SetupHow2Fragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val forward = MaterialSharedAxis(MaterialSharedAxis.X, true)
        enterTransition = forward

        val backward = MaterialSharedAxis(MaterialSharedAxis.X, false)
        returnTransition = backward
    }

    private var _binding: FragmentSetupHow2Binding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupHow2Binding.inflate(inflater, container, false)
        InsetUtil.applyBottomInset(binding.setupHow2Ll)

        val root = binding.root

        binding.setupHow2ButtonNext.setOnClickListener {
            (activity as SetupNewActivity).switchFragments(SetupHow3Fragment())
        }
        binding.setupHow2Iv.setOnClickListener {
            (activity as SetupNewActivity).switchFragments(SetupHow1Fragment())
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}