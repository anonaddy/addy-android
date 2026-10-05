package host.stjin.anonaddy.ui.faileddeliveries

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityFailedDeliveriesSettingsBinding
import kotlinx.coroutines.launch

class FailedDeliveriesActivity : BaseActivity() {
    private lateinit var binding: ActivityFailedDeliveriesSettingsBinding

    private fun getFragment(): FailedDeliveriesFragment? {
        return supportFragmentManager.findFragmentById(R.id.activity_failed_deliveries_settings_fcv) as? FailedDeliveriesFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFailedDeliveriesSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.failed_deliveries,
            null,
            binding.activityFailedDeliveriesSettingsToolbar,
            R.drawable.ic_mail_error
        )
        setRefreshLayout()
        setPage()
    }

    // This only applies to <sw600Dp devices
    private fun setRefreshLayout() {
        binding.activityFailedDeliveriesSettingsSwiperefresh.setOnRefreshListener {
            lifecycleScope.launch {
                getFragment()?.onRefreshData()
                binding.activityFailedDeliveriesSettingsSwiperefresh.isRefreshing = false
            }
        }
    }

    private fun setPage() {
        requireAuthentication {
            if (supportFragmentManager.findFragmentById(R.id.activity_failed_deliveries_settings_fcv) == null) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.activity_failed_deliveries_settings_fcv, FailedDeliveriesFragment.newInstance())
                    .commit()
            }
        }
    }
}
