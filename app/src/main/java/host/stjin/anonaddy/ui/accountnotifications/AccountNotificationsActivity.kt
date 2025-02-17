package host.stjin.anonaddy.ui.accountnotifications

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAccountNotificationsBinding
import kotlinx.coroutines.launch

class AccountNotificationsActivity : BaseActivity() {

    private lateinit var binding: ActivityAccountNotificationsBinding
    private val accountNotificationsFragment = AccountNotificationsFragment()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountNotificationsBinding.inflate(layoutInflater)
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
            binding.activityAccountNotificationsSettingsSwiperefresh.isRefreshing = true

            accountNotificationsFragment.getDataFromWeb(null) {
                binding.activityAccountNotificationsSettingsSwiperefresh.isRefreshing = false
            }
        }
    }


    private fun setPage() {
        /**
         * This activity can be called by an URI or Widget/Notification Intent.
         * Protect this part
         */
        lifecycleScope.launch {
            isAuthenticated { isAuthenticated ->
                if (isAuthenticated) {
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.activity_account_notifications_settings_fcv, accountNotificationsFragment)
                        .commit()
                }
            }
        }

    }

}