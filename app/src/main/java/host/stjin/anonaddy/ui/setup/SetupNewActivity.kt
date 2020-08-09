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
        setupToolbar()
        switchFragments(SetupHow1Fragment())
    }

    private fun setupToolbar() {
        setup_toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24) // need to set the icon here to have a navigation icon. You can simple create an vector image by "Vector Asset" and using here
        setup_toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    fun switchFragments(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.setup_fragment, fragment)
            .commit()
    }

}