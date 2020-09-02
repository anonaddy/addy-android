package host.stjin.anonaddy.ui.setup

import android.os.Bundle
import androidx.fragment.app.Fragment
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import kotlinx.android.synthetic.main.activity_setup_new.*


class SetupNewActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        this.overridePendingTransition(
            R.anim.slide_in,
            R.anim.slide_out
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_new)
        setupToolbar(setup_toolbar)
        switchFragments(SetupHow1Fragment())
    }

    fun switchFragments(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.setup_fragment, fragment)
            .commit()
    }

}