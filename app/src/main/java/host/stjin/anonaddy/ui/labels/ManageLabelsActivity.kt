package host.stjin.anonaddy.ui.labels

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityManageLabelsBinding
import kotlinx.coroutines.launch

class ManageLabelsActivity : BaseActivity() {
    private lateinit var binding: ActivityManageLabelsBinding

    private val manageLabelsFragment = ManageLabelsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageLabelsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.manage_labels,
            null,
            binding.activityManageLabelsNotificationsSettingsToolbar,
            R.drawable.ic_label
        )
        setRefreshLayout()

        setPage()
    }

    private fun setRefreshLayout() {
        binding.activityManageLabelsSwiperefresh.setOnRefreshListener {
            binding.activityManageLabelsSwiperefresh.isRefreshing = true

            manageLabelsFragment.getDataFromWeb(null) {
                binding.activityManageLabelsSwiperefresh.isRefreshing = false
            }
        }
    }

    private fun setPage() {
        lifecycleScope.launch {
            isAuthenticated { isAuthenticated ->
                if (isAuthenticated) {
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.activity_manage_labels_fcv, manageLabelsFragment)
                        .commit()
                }
            }
        }
    }
}