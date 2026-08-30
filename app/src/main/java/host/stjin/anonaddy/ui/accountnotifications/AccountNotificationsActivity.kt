package host.stjin.anonaddy.ui.accountnotifications

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAccountNotificationsSettingsBinding
import kotlinx.coroutines.launch

class AccountNotificationsActivity : BaseActivity() {
    private lateinit var binding: ActivityAccountNotificationsSettingsBinding

    private fun getFragment(): AccountNotificationsFragment? {
        return supportFragmentManager.findFragmentById(R.id.activity_account_notifications_settings_fcv) as? AccountNotificationsFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountNotificationsSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.account_notifications,
            null,
            binding.activityAccountNotificationsSettingsToolbar,
            R.drawable.ic_bell
        )
        setRefreshLayout()
        setPage()
    }

    // This only applies to <sw600Dp devices
    private fun setRefreshLayout() {
        binding.activityAccountNotificationsSettingsSwiperefresh.setOnRefreshListener {
            lifecycleScope.launch {
                getFragment()?.onRefreshData()
                binding.activityAccountNotificationsSettingsSwiperefresh.isRefreshing = false
            }
        }
    }

    private fun setPage() {
        requireAuthentication {
            if (supportFragmentManager.findFragmentById(R.id.activity_account_notifications_settings_fcv) == null) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.activity_account_notifications_settings_fcv, AccountNotificationsFragment.newInstance())
                    .commit()
            }
        }
    }
}
