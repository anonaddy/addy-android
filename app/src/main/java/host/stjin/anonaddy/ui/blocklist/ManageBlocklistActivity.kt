package host.stjin.anonaddy.ui.blocklist

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityAccountNotificationsBinding
import host.stjin.anonaddy.databinding.ActivityManageBlocklistBinding
import kotlinx.coroutines.launch

class ManageBlocklistActivity : BaseActivity() {

    private lateinit var binding: ActivityManageBlocklistBinding
    private val manageBlocklistFragment = ManageBlocklistFragment()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBlocklistBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.blocklist,
            null,
            binding.activityManageBlocklistNotificationsSettingsToolbar,
            R.drawable.ic_forbid
        )
        setRefreshLayout()

        setPage()
    }

    // This only applies to <sw600Dp devices
    private fun setRefreshLayout() {
        binding.activityManageBlocklistSwiperefresh.setOnRefreshListener {
            binding.activityManageBlocklistSwiperefresh.isRefreshing = true

            manageBlocklistFragment.getDataFromWeb(null) {
                binding.activityManageBlocklistSwiperefresh.isRefreshing = false
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
                        .replace(R.id.activity_manage_blocklist_fcv, manageBlocklistFragment)
                        .commit()
                }
            }
        }

    }

}