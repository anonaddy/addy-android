package host.stjin.anonaddy.ui.blocklist

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityBlocklistSettingsBinding
import kotlinx.coroutines.launch

class BlocklistActivity : BaseActivity() {
    private lateinit var binding: ActivityBlocklistSettingsBinding

    private fun getFragment(): BlocklistFragment? {
        return supportFragmentManager.findFragmentById(R.id.activity_blocklist_settings_fcv) as? BlocklistFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlocklistSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.blocklist,
            null,
            binding.activityBlocklistSettingsToolbar,
            R.drawable.ic_close
        )

        setRefreshLayout()
        setPage()
    }

    // This only applies to <sw600Dp devices
    private fun setRefreshLayout() {
        binding.activityBlocklistSettingsSwiperefresh.setOnRefreshListener {
            lifecycleScope.launch {
                getFragment()?.onRefreshData()
                binding.activityBlocklistSettingsSwiperefresh.isRefreshing = false
            }
        }
    }

    private fun setPage() {
        requireAuthentication {
            if (supportFragmentManager.findFragmentById(R.id.activity_blocklist_settings_fcv) == null) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.activity_blocklist_settings_fcv, BlocklistFragment.newInstance())
                    .commit()
            }
        }
    }
}
