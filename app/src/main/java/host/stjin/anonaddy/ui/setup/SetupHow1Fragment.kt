package host.stjin.anonaddy.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import host.stjin.anonaddy.R
import kotlinx.android.synthetic.main.fragment_setup_how1.view.*

class SetupHow1Fragment : Fragment() {

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
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_setup_how1, container, false)

        root.setup_how_1_button_next.setOnClickListener {
            (activity as SetupNewActivity).switchFragments(SetupHow2Fragment())
        }

        return root
    }
}