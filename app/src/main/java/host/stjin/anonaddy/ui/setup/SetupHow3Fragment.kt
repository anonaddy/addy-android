package host.stjin.anonaddy.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import host.stjin.anonaddy.R
import kotlinx.android.synthetic.main.fragment_setup_how3.view.*

class SetupHow3Fragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val forward = MaterialSharedAxis(MaterialSharedAxis.X, true)
        enterTransition = forward

        val backward = MaterialSharedAxis(MaterialSharedAxis.X, false)
        returnTransition = backward
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_setup_how3, container, false)

        root.setup_how_3_button_next.setOnClickListener {
            (activity as SetupNewActivity).switchFragments(SetupHow4Fragment())
        }

        root.setup_how_3_button_previous.setOnClickListener {
            (activity as SetupNewActivity).switchFragments(SetupHow2Fragment())
        }

        return root
    }

}