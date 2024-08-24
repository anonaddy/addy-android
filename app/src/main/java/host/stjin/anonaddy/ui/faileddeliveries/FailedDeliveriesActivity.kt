package host.stjin.anonaddy.ui.faileddeliveries

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityFailedDeliveriesBinding
import kotlinx.coroutines.launch

class FailedDeliveriesActivity : BaseActivity() {

    private lateinit var binding: ActivityFailedDeliveriesBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFailedDeliveriesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.failed_deliveries,
            null,
            binding.activityFailedDeliveriesSettingsToolbar,
            R.drawable.ic_mail_error
        )

        setPage()
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
                        .replace(R.id.activity_failed_deliveries_settings_fcv, FailedDeliveriesFragment())
                        .commit()
                }
            }
        }

    }

}