package host.stjin.anonaddy.ui.setup

import android.os.Bundle
import androidx.fragment.app.Fragment
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivitySetupNewBinding


class SetupNewActivity : BaseActivity() {
    override val requiresAuthentication: Boolean = false
    private lateinit var binding: ActivitySetupNewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in, R.anim.slide_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
        }
        super.onCreate(savedInstanceState)
        binding = ActivitySetupNewBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        switchFragments(SetupHow1Fragment())
    }

    fun switchFragments(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.setup_fragment, fragment)
            .commit()
    }
}
