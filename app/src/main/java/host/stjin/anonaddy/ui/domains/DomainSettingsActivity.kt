package host.stjin.anonaddy.ui.domains

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityDomainSettingsBinding
import kotlinx.coroutines.launch

class DomainSettingsActivity : BaseActivity() {


    private lateinit var binding: ActivityDomainSettingsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDomainSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.manage_domains,
            null,
            binding.activityDomainSettingsToolbar,
            R.drawable.ic_world
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
                        .replace(R.id.activity_domain_settings_fcv, DomainSettingsFragment())
                        .commit()
                }
            }
        }

    }

}