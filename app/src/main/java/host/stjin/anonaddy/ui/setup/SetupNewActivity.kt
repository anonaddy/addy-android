package host.stjin.anonaddy.ui.setup

import android.os.Bundle
import androidx.fragment.app.Fragment
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivitySetupNewBinding


class SetupNewActivity : BaseActivity() {

    private lateinit var binding: ActivitySetupNewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        this.overridePendingTransition(
            R.anim.slide_in,
            R.anim.slide_out
        )
        super.onCreate(savedInstanceState)
        binding = ActivitySetupNewBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setupToolbar(binding.setupToolbar.customToolbarOneHandedMaterialtoolbar, R.string.how_does_it_work)
        switchFragments(SetupHow1Fragment())
    }

    fun switchFragments(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.setup_fragment, fragment)
            .commit()
    }

}