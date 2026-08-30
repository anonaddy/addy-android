package host.stjin.anonaddy.ui.labels

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityLabelSettingsBinding
import kotlinx.coroutines.launch

class LabelsActivity : BaseActivity() {
    private lateinit var binding: ActivityLabelSettingsBinding

    private fun getFragment(): LabelsFragment? {
        return supportFragmentManager.findFragmentById(R.id.activity_label_settings_fcv) as? LabelsFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLabelSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.labels,
            null,
            binding.activityLabelSettingsToolbar,
            R.drawable.ic_label
        )

        setRefreshLayout()
        setPage()
    }

    // This only applies to <sw600Dp devices
    private fun setRefreshLayout() {
        binding.activityLabelSettingsSwiperefresh.setOnRefreshListener {
            lifecycleScope.launch {
                getFragment()?.onRefreshData()
                binding.activityLabelSettingsSwiperefresh.isRefreshing = false
            }
        }
    }

    private fun setPage() {
        requireAuthentication {
            if (supportFragmentManager.findFragmentById(R.id.activity_label_settings_fcv) == null) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.activity_label_settings_fcv, LabelsFragment.newInstance())
                    .commit()
            }
        }
    }
}
