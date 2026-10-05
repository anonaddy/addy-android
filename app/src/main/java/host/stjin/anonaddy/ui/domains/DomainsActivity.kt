package host.stjin.anonaddy.ui.domains

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityDomainSettingsBinding
import kotlinx.coroutines.launch

class DomainsActivity : BaseActivity() {
    private lateinit var binding: ActivityDomainSettingsBinding

    private fun getFragment(): DomainsFragment? {
        return supportFragmentManager.findFragmentById(R.id.activity_domain_settings_fcv) as? DomainsFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDomainSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.domains,
            null,
            binding.activityDomainSettingsToolbar,
            R.drawable.ic_world
        )

        setRefreshLayout()
        setPage()
    }

    // This only applies to <sw600Dp devices
    private fun setRefreshLayout() {
        binding.activityDomainSettingsSwiperefresh.setOnRefreshListener {
            lifecycleScope.launch {
                getFragment()?.onRefreshData()
                binding.activityDomainSettingsSwiperefresh.isRefreshing = false
            }
        }
    }

    private fun setPage() {
        requireAuthentication {
            if (supportFragmentManager.findFragmentById(R.id.activity_domain_settings_fcv) == null) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.activity_domain_settings_fcv, DomainsFragment.newInstance())
                    .commit()
            }
        }
    }
}
