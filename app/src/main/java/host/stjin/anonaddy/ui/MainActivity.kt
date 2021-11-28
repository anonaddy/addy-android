package host.stjin.anonaddy.ui


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import host.stjin.anonaddy.*
import host.stjin.anonaddy.databinding.ActivityMainBinding
import host.stjin.anonaddy.databinding.ActivityMainBinding.inflate
import host.stjin.anonaddy.models.*
import host.stjin.anonaddy.service.BackgroundWorkerHelper
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
        drawBehindNavBar(binding.root, binding.activityMainViewpager)

        isAuthenticated { isAuthenticated ->
            if (isAuthenticated) {
                lifecycleScope.launch {
                    loadMainActivity()
                    checkForUpdates()
                    // Schedule the background worker (in case this has not been done before) (this will cancel if already scheduled)
                    BackgroundWorkerHelper(this@MainActivity).scheduleBackgroundWorker()
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        initialiseMainAppBar()
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

        binding.navView.setOnItemSelectedListener {
            switchFragments(it.itemId)
            false
        }

        binding.mainAppBarInclude.toolbar.setOnClickListener {
            val intent = Intent("scroll_up")
            sendBroadcast(intent)
            binding.mainAppBarInclude.appBar.setExpanded(true, true)
        }

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
        val usernameInitials = (this.application as AnonAddyForAndroid).userResource.username.take(2).uppercase(Locale.getDefault())
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

        lifecycleScope.launch {
            checkForNewFailedDeliveries()
        }
    }

    private suspend fun checkForUpdates() {
        Updater.isUpdateAvailable({ updateAvailable: Boolean, _: String?, _: Boolean ->

            // Set the update status in profileBottomDialogFragment
            profileBottomDialogFragment.updateAvailable = updateAvailable

            if (updateAvailable) {
                // An update is available, set the update  profile bottomdialog fragment
                if (binding.mainAppBarInclude.mainTopBarUserInitialsUpdateIcon.visibility != View.VISIBLE) {
                    // loading the animation of
                    // zoom_in.xml file into a variable
                    val animZoomIn = AnimationUtils.loadAnimation(
                        this,
                        R.anim.zoom_in
                    )
                    animZoomIn.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(p0: Animation?) {
                            binding.mainAppBarInclude.mainTopBarUserInitialsUpdateIcon.visibility = View.VISIBLE
                        }

                        override fun onAnimationEnd(p0: Animation?) {
                            //
                        }

                        override fun onAnimationRepeat(p0: Animation?) {
                            //
                        }
                    }
                    )
                    binding.mainAppBarInclude.mainTopBarUserInitialsUpdateIcon.startAnimation(animZoomIn)
                }
            }
            // No else because an update does not "just" disappear...
        }, this)
    }

    /*
    This method checks if there are new failed deliveries
    It does this by getting the current failed delivery count, if that count is bigger than the failed deliveries in the cache that means there are new failed
    deliveries.

    As BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT is only updated in the service and in the FailedDeliveriesActivity that means that the red
    indicator is only visible if:

    - The activity has not been opened since there were new items.
    - There are more failed deliveries than the server cached last time (in which case the user should have got a notification)
     */
    private suspend fun checkForNewFailedDeliveries() {
        val networkHelper = NetworkHelper(this)
        val encryptedSettingsManager = SettingsManager(true, this)
        networkHelper.getAllFailedDeliveries({ result ->
            val currentFailedDeliveries =
                encryptedSettingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT)
            if (result?.size ?: 0 > currentFailedDeliveries) {
                if (binding.mainAppBarInclude.mainTopBarFailedDeliveriesNewItemsIcon.visibility != View.VISIBLE) {
                    // loading the animation of
                    // zoom_in.xml file into a variable
                    val animZoomIn = AnimationUtils.loadAnimation(
                        this,
                        R.anim.zoom_in
                    )
                    animZoomIn.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(p0: Animation?) {
                            binding.mainAppBarInclude.mainTopBarFailedDeliveriesNewItemsIcon.visibility = View.VISIBLE
                        }

                        override fun onAnimationEnd(p0: Animation?) {
                            //
                        }

                        override fun onAnimationRepeat(p0: Animation?) {
                            //
                        }
                    }
                    )
                    binding.mainAppBarInclude.mainTopBarFailedDeliveriesNewItemsIcon.startAnimation(animZoomIn)
                }
            } else {
                if (binding.mainAppBarInclude.mainTopBarFailedDeliveriesNewItemsIcon.visibility != View.INVISIBLE) {

                    // loading the animation of
                    // zoom_in.xml file into a variable
                    val animZoomOut = AnimationUtils.loadAnimation(
                        this,
                        R.anim.zoom_out
                    )
                    animZoomOut.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(p0: Animation?) {
                            //
                        }

                        override fun onAnimationEnd(p0: Animation?) {
                            binding.mainAppBarInclude.mainTopBarFailedDeliveriesNewItemsIcon.visibility = View.INVISIBLE
                        }

                        override fun onAnimationRepeat(p0: Animation?) {
                            //
                        }
                    }
                    )
                    binding.mainAppBarInclude.mainTopBarFailedDeliveriesNewItemsIcon.startAnimation(animZoomOut)
                }
            }
        }, show404Toast = false)
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
        resultLauncher.launch(intent)
    }


    // When returning from the search activity, load the appropriate screen
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data

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