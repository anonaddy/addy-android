package host.stjin.anonaddy.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import host.stjin.anonaddy.R
import kotlinx.android.synthetic.main.fragment_setup_how2.view.*

class SetupHow2Fragment : Fragment() {

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
        val root = inflater.inflate(R.layout.fragment_setup_how2, container, false)

        root.setup_how_2_button_next.setOnClickListener {
            (activity as SetupNewActivity).switchFragments(SetupHow3Fragment())
        }
        root.setup_how_2_button_previous.setOnClickListener {
            (activity as SetupNewActivity).switchFragments(SetupHow1Fragment())
        }


        return root
    }
}