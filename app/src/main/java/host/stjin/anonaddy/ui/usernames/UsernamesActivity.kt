package host.stjin.anonaddy.ui.usernames

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityUsernameSettingsBinding
import kotlinx.coroutines.launch

class UsernamesActivity : BaseActivity() {
    private lateinit var binding: ActivityUsernameSettingsBinding

    private fun getFragment(): UsernamesFragment? {
        return supportFragmentManager.findFragmentById(R.id.activity_username_settings_fcv) as? UsernamesFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsernameSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.usernames,
            null,
            binding.activityUsernameSettingsToolbar,
            R.drawable.ic_user
        )

        setRefreshLayout()
        setPage()
    }

    // This only applies to <sw600Dp devices
    private fun setRefreshLayout() {
        binding.activityUsernameSettingsSwiperefresh.setOnRefreshListener {
            lifecycleScope.launch {
                getFragment()?.onRefreshData()
                binding.activityUsernameSettingsSwiperefresh.isRefreshing = false
            }
        }
    }

    private fun setPage() {
        requireAuthentication {
            if (supportFragmentManager.findFragmentById(R.id.activity_username_settings_fcv) == null) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.activity_username_settings_fcv, UsernamesFragment.newInstance())
                    .commit()
            }
        }
    }
}
