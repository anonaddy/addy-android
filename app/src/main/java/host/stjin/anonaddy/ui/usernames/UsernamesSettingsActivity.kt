package host.stjin.anonaddy.ui.usernames

import android.os.Bundle
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityUsernameSettingsBinding

class UsernamesSettingsActivity : BaseActivity() {

    private lateinit var binding: ActivityUsernameSettingsBinding
    private val usernamesSettingsFragment = UsernamesSettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsernameSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupToolbar(
            R.string.manage_usernames,
            null,
            binding.activityUsernameSettingsToolbar,
            R.drawable.ic_users
        )
        setRefreshLayout()


        supportFragmentManager
            .beginTransaction()
            .replace(R.id.activity_username_settings_fcv, usernamesSettingsFragment)
            .commit()


    }

    // This only applies to <sw600Dp devices
    private fun setRefreshLayout() {
        binding.activityUsernameSettingsSwiperefresh.setOnRefreshListener {
            binding.activityUsernameSettingsSwiperefresh.isRefreshing = true

            usernamesSettingsFragment.getDataFromWeb(null) {
                binding.activityUsernameSettingsSwiperefresh.isRefreshing = false
            }
        }
    }

}