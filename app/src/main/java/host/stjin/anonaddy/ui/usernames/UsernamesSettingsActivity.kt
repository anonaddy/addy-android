package host.stjin.anonaddy.ui.usernames

import android.os.Bundle
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivityUsernameSettingsBinding

class UsernamesSettingsActivity : BaseActivity() {

    private lateinit var binding: ActivityUsernameSettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsernameSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawBehindNavBar(
            view,
            topViewsToShiftDownUsingMargin = arrayListOf(view),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activityUsernameSettingsFcv)
        )

        setupToolbar(
            R.string.manage_usernames,
            null,
            binding.activityUsernameSettingsToolbar,
            R.drawable.ic_users
        )


        supportFragmentManager
            .beginTransaction()
            .replace(R.id.activity_username_settings_fcv, UsernamesSettingsFragment())
            .commit()


    }


}