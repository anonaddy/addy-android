package host.stjin.anonaddy.ui


import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.databinding.ActivityMainBinding
import host.stjin.anonaddy.databinding.ActivityMainBinding.inflate
import host.stjin.anonaddy.models.*
import host.stjin.anonaddy.ui.alias.AliasFragment
import host.stjin.anonaddy.ui.appsettings.update.ChangelogBottomDialogFragment
import host.stjin.anonaddy.ui.domains.DomainSettingsActivity
import host.stjin.anonaddy.ui.faileddeliveries.FailedDeliveriesActivity
import host.stjin.anonaddy.ui.home.HomeFragment
import host.stjin.anonaddy.ui.recipients.RecipientsFragment
import host.stjin.anonaddy.ui.rules.RulesSettingsActivity
import host.stjin.anonaddy.ui.search.SearchActivity
import host.stjin.anonaddy.ui.search.SearchBottomDialogFragment
import host.stjin.anonaddy.ui.usernames.UsernamesSettingsActivity
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs


class MainActivity : BaseActivity(), SearchBottomDialogFragment.AddSearchBottomDialogListener {


    private val SEARCH_CONSTANT: Int = 1
    private val searchBottomDialogFragment: SearchBottomDialogFragment =
        SearchBottomDialogFragment.newInstance()

    private val profileBottomDialogFragment: ProfileBottomDialogFragment =
        ProfileBottomDialogFragment.newInstance()


    private val fragmentList = arrayListOf(
        HomeFragment.newInstance(),
        AliasFragment.newInstance(),
        RecipientsFragment.newInstance()
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        lifecycleScope.launch {
            isAuthenticated { isAuthenticated ->
                if (isAuthenticated) {
                    loadMainActivity()
                }
            }
        }

    }

    private lateinit var binding: ActivityMainBinding
    private fun loadMainActivity() {
        showChangeLog()

        binding.activityMainViewpager.adapter = MainViewpagerAdapter(this, fragmentList)
        binding.activityMainViewpager.offscreenPageLimit = 3
        // Allow swiping through the pages
        binding.activityMainViewpager.isUserInputEnabled = true
        binding.activityMainViewpager.setPageTransformer { page, position ->
            val normalizedposition = abs(abs(position) - 1)
            page.alpha = normalizedposition
        }

        binding.activityMainViewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        binding.navView.menu.findItem(R.id.navigation_home).isChecked = true
                        changeTopBarTitle(this@MainActivity.resources.getString(R.string.title_home))
                    }
                    1 -> {
                        binding.navView.menu.findItem(R.id.navigation_alias).isChecked = true
                        changeTopBarTitle(this@MainActivity.resources.getString(R.string.title_aliases))
                    }
                    2 -> {
                        binding.navView.menu.findItem(R.id.navigation_recipients).isChecked = true
                        changeTopBarTitle(this@MainActivity.resources.getString(R.string.title_recipients))
                    }
                }
                super.onPageSelected(position)
            }
        })

        binding.navView.setOnNavigationItemSelectedListener {
            switchFragments(it.itemId)
            false
        }

        initialiseMainAppBar()
    }

    private fun showChangeLog() {
        // Check the version code in the sharedpreferences, if the one in the preferences is older than the current one, the app got updated.
        // Show the changelog
        val settingsManager = SettingsManager(false, this)
        if (settingsManager.getSettingsInt(SettingsManager.PREFS.VERSION_CODE) < BuildConfig.VERSION_CODE) {
            val addChangelogBottomDialogFragment: ChangelogBottomDialogFragment =
                ChangelogBottomDialogFragment.newInstance()
            addChangelogBottomDialogFragment.show(
                supportFragmentManager,
                "MainActivity:addChangelogBottomDialogFragment"
            )
        }

        // Write the current version code to prevent double triggering
        settingsManager.putSettingsInt(SettingsManager.PREFS.VERSION_CODE, BuildConfig.VERSION_CODE)
    }

    private fun initialiseMainAppBar() {
        // Figure out the from name initials
        val usernameInitials = User.userResource.username.take(2).uppercase(Locale.getDefault())
        binding.mainAppBarInclude.mainTopBarUserInitials.text = usernameInitials

        binding.mainAppBarInclude.mainTopBarUserInitials.setOnClickListener {
            if (!profileBottomDialogFragment.isAdded) {
                profileBottomDialogFragment.show(
                    supportFragmentManager,
                    "profileBottomDialogFragment"
                )
            }
        }

        binding.mainAppBarInclude.mainTopBarSearchIcon.setOnClickListener {
            if (!searchBottomDialogFragment.isAdded) {
                searchBottomDialogFragment.show(
                    supportFragmentManager,
                    "searchBottomDialogFragment"
                )
            }
        }

        binding.mainAppBarInclude.mainTopBarFailedDeliveriesIcon.setOnClickListener {
            val intent = Intent(this, FailedDeliveriesActivity::class.java)
            startActivity(intent)
        }

    }
    private fun changeTopBarTitle(title: String) {
        binding.mainAppBarInclude.collapsingToolbar.title = title
    }

    fun switchFragments(fragment: Int) {
        when (fragment) {
            R.id.navigation_home -> binding.activityMainViewpager.currentItem = 0
            R.id.navigation_alias -> binding.activityMainViewpager.currentItem = 1
            R.id.navigation_recipients -> binding.activityMainViewpager.currentItem = 2
        }
    }

    override fun onSearch(
        filteredAliases: ArrayList<Aliases>,
        filteredRecipients: ArrayList<Recipients>,
        filteredDomains: ArrayList<Domains>,
        filteredUsernames: ArrayList<Usernames>,
        filteredRules: ArrayList<Rules>,
        filteredFailedDeliveries: ArrayList<FailedDeliveries>
    ) {

        SearchActivity.FilteredLists.filteredAliases = filteredAliases
        SearchActivity.FilteredLists.filteredRecipients = filteredRecipients
        SearchActivity.FilteredLists.filteredDomains = filteredDomains
        SearchActivity.FilteredLists.filteredUsernames = filteredUsernames
        SearchActivity.FilteredLists.filteredRules = filteredRules
        SearchActivity.FilteredLists.filteredFailedDeliveries = filteredFailedDeliveries

        searchBottomDialogFragment.dismiss()
        val intent = Intent(this, SearchActivity::class.java)
        startActivityForResult(intent, SEARCH_CONSTANT)
    }


    // When returning from the search activity, load the appropriate screen
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == SEARCH_CONSTANT) {
            if (data != null) {
                if (data.hasExtra("target")) {
                    when (data.extras?.getString("target")) {
                        SearchActivity.SearchTargets.ALIASES.activity -> {
                            switchFragments(R.id.navigation_alias)
                        }
                        SearchActivity.SearchTargets.RECIPIENTS.activity -> {
                            switchFragments(R.id.navigation_recipients)
                        }
                        SearchActivity.SearchTargets.DOMAINS.activity -> {
                            val intent = Intent(this, DomainSettingsActivity::class.java)
                            startActivity(intent)
                        }
                        SearchActivity.SearchTargets.USERNAMES.activity -> {
                            val intent = Intent(this, UsernamesSettingsActivity::class.java)
                            startActivity(intent)
                        }
                        SearchActivity.SearchTargets.RULES.activity -> {
                            val intent = Intent(this, RulesSettingsActivity::class.java)
                            startActivity(intent)
                        }
                        SearchActivity.SearchTargets.FAILED_DELIVERIES.activity -> {
                            val intent = Intent(this, FailedDeliveriesActivity::class.java)
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }

}