package host.stjin.anonaddy.ui.setup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import host.stjin.anonaddy.databinding.FragmentSetupHow4Binding
import host.stjin.anonaddy.databinding.FragmentSetupHow4Binding.inflate


class SetupHow4Fragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val forward = MaterialSharedAxis(MaterialSharedAxis.X, true)
        enterTransition = forward

        val backward = MaterialSharedAxis(MaterialSharedAxis.X, false)
        returnTransition = backward
    }

    private var _binding: FragmentSetupHow4Binding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflate(inflater, container, false)
        // Inflate the layout for this fragment
        val root = binding.root

        binding.setupHow4ButtonNext.setOnClickListener {
            val url = "https://app.anonaddy.com/register"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
            (activity as SetupNewActivity).finish()
        }

        binding.setupHow4Iv.setOnClickListener {
            (activity as SetupNewActivity).switchFragments(SetupHow3Fragment())
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}