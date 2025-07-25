package host.stjin.anonaddy.ui

import android.app.NotificationManager
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.security.KeyChain
import android.security.KeyChainAliasCallback
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.wearable.Wearable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.Updater
import host.stjin.anonaddy.databinding.ActivityMainBinding
import host.stjin.anonaddy.databinding.ActivityMainBinding.inflate
import host.stjin.anonaddy.notifications.NotificationHelper
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.accountnotifications.AccountNotificationsActivity
import host.stjin.anonaddy.ui.alias.AliasFragment
import host.stjin.anonaddy.ui.appsettings.AppSettingsActivity
import host.stjin.anonaddy.ui.appsettings.update.ChangelogBottomDialogFragment
import host.stjin.anonaddy.ui.customviews.refreshlayout.RefreshLayout
import host.stjin.anonaddy.ui.domains.DomainSettingsActivity
import host.stjin.anonaddy.ui.domains.DomainSettingsFragment
import host.stjin.anonaddy.ui.faileddeliveries.FailedDeliveriesActivity
import host.stjin.anonaddy.ui.faileddeliveries.FailedDeliveriesFragment
import host.stjin.anonaddy.ui.home.HomeFragment
import host.stjin.anonaddy.ui.recipients.RecipientsFragment
import host.stjin.anonaddy.ui.rules.RulesSettingsActivity
import host.stjin.anonaddy.ui.rules.RulesSettingsFragment
import host.stjin.anonaddy.ui.search.SearchActivity
import host.stjin.anonaddy.ui.search.SearchBottomDialogFragment
import host.stjin.anonaddy.ui.setup.AddApiBottomDialogFragment
import host.stjin.anonaddy.ui.usernames.UsernamesSettingsActivity
import host.stjin.anonaddy.ui.usernames.UsernamesSettingsFragment
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy.utils.WearOSHelper
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.managers.SettingsManager.PREFS
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.Domains
import host.stjin.anonaddy_shared.models.FailedDeliveries
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.models.Recipients
import host.stjin.anonaddy_shared.models.Rules
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.models.Usernames
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ocpsoft.prettytime.PrettyTime
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import androidx.core.net.toUri


object MainActivityTimeClass {
    private var lastGeneralRefresh = Date()

    fun updateLastGeneralRefresh() {
        lastGeneralRefresh = Date()
    }

    fun isPast5Minutes(): Boolean {
        val fiveMinutesInMillis = 5 * 60 * 1000
        return Date().time - lastGeneralRefresh.time > fiveMinutesInMillis
    }
}

class MainActivity : BaseActivity(), SearchBottomDialogFragment.AddSearchBottomDialogListener, AddApiBottomDialogFragment.AddApiBottomDialogListener {


    private val searchBottomDialogFragment: SearchBottomDialogFragment =
        SearchBottomDialogFragment.newInstance()

    private val profileBottomDialogFragment: ProfileBottomDialogFragment =
        ProfileBottomDialogFragment.newInstance()


    private var addApiBottomDialogFragment: AddApiBottomDialogFragment =
        AddApiBottomDialogFragment.newInstance()


    private lateinit var networkHelper: NetworkHelper

    lateinit var viewPager: ViewPager2


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        networkHelper = NetworkHelper(this@MainActivity)

        isAuthenticated { isAuthenticated ->
            if (isAuthenticated) {
                lifecycleScope.launch {
                    loadMainActivity()
                    // No need to check for updates on recreation of the activity
                    if (savedInstanceState == null) {
                        checkForUpdates()
                        checkForApiExpiration()
                        checkForCertificateExpiration()
                        checkForSubscriptionExpiration()
                        checkForNewFailedDeliveries()
                        checkForNewAccountNotifications()

                        // Schedule the background worker (in case this has not been done before) (this will cancel if already scheduled)
                        BackgroundWorkerHelper(this@MainActivity).scheduleBackgroundWorker()

                    }
                }
            }
        }

        if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            setOnBigScreenClickListener()
        }

        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            setRefreshLayout()
        }

        if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            binding.activityMainViewpagerSw600dp!!.isUserInputEnabled = false

            setRailVersion()
        }


        if (AddyIo.isUsingHostedInstance) {
            if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
                binding.navRail!!.headerView?.findViewById<LinearLayout>(R.id.navigation_rail_fab_account_notifications_LL)!!.visibility = View.VISIBLE
            } else {
                binding.mainAppBarInclude!!.mainTopBarAccountNotificationsIconRL.visibility = View.VISIBLE
            }
        } else {
            if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
                binding.navRail!!.headerView?.findViewById<LinearLayout>(R.id.navigation_rail_fab_account_notifications_LL)!!.visibility = View.GONE
            } else {
                binding.mainAppBarInclude!!.mainTopBarAccountNotificationsIconRL.visibility = View.GONE
            }

        }


    }


    // Make sure the viewPager is ABOVE the bottomnavbar
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            binding.navView!!.viewTreeObserver.addOnGlobalLayoutListener(
                object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        // gets called after layout has been done but before display
                        // so we can get the height then hide the view


                        val height = binding.navView!!.height // Ahaha!  Gotcha
                        binding.activityMainViewpager!!.setPadding(0,0,0,height)

                        binding.navView!!.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    }
                })

        }

    }


    // Only for Sw600>
    private fun setRailVersion() {
        val railVersionText =
            if (AddyIo.isUsingHostedInstance) this.resources.getString(R.string.hosted) else AddyIo.VERSIONSTRING
        binding.navRail!!.headerView?.findViewById<TextView>(R.id.navigation_rail_fab_version)!!.text = railVersionText
    }

    private fun setOnBigScreenClickListener() {
        binding.navRail!!.headerView?.findViewById<FloatingActionButton>(R.id.navigation_rail_fab_settings)!!.setOnClickListener {
            val intent = Intent(this, AppSettingsActivity::class.java)
            startActivity(intent)
        }

        binding.navRail!!.headerView?.findViewById<FloatingActionButton>(R.id.navigation_rail_fab_account_notifications)!!.setOnClickListener {
            val intent = Intent(this, AccountNotificationsActivity::class.java)
            startActivity(intent)
        }

        binding.searchBar?.setOnClickListener {
            openSearch()
        }

        binding.navigationRailUserRefresh?.setOnClickListener {
            (binding.navigationRailUserRefresh!!.compoundDrawables[0] as AnimatedVectorDrawable).start()
            refreshAllData()
        }

    }

    // This only applies to <sw600Dp devices
    private fun setRefreshLayout() {
        binding.refreshLayout!!.setOnRefreshListener(object : RefreshLayout.OnRefreshListener {
            override fun refresh() {
                changeTopBarSubTitle(
                    binding.mainAppBarInclude!!.mainTopBarSubtitle,
                    binding.mainAppBarInclude!!.mainTopBarTitle,
                    this@MainActivity.resources.getString(R.string.refreshing_data)
                )
                shimmerTopBarSubTitle(binding.mainAppBarInclude!!.mainTopBarSubtitleShimmerframelayout, true)

                refreshAllData()


                // Since a bunch of different calls are being made, it is very hard to keep progress of everything.
                // Just hide the refresh text after 2 seconds.
                // TODO Any way to keep track of all this?
                Handler(Looper.getMainLooper()).postDelayed({
                    // Unauthenticated, clear settings
                    binding.refreshLayout!!.finishRefreshing()
                    shimmerTopBarSubTitle(binding.mainAppBarInclude!!.mainTopBarSubtitleShimmerframelayout, true)
                    changeTopBarSubTitle(
                        binding.mainAppBarInclude!!.mainTopBarSubtitle,
                        binding.mainAppBarInclude!!.mainTopBarTitle,
                        null
                    )
                }, 2000)

            }

            override fun pullDown(pixelsMoved: Float, shouldRefreshOnRelease: Boolean) {
                if (pixelsMoved > 50) {
                    if (shouldRefreshOnRelease) {
                        changeTopBarSubTitle(
                            binding.mainAppBarInclude!!.mainTopBarSubtitle,
                            binding.mainAppBarInclude!!.mainTopBarTitle,
                            this@MainActivity.resources.getString(R.string.release_to_refresh)
                        )
                    } else {
                        changeTopBarSubTitle(
                            binding.mainAppBarInclude!!.mainTopBarSubtitle,
                            binding.mainAppBarInclude!!.mainTopBarTitle,
                            this@MainActivity.resources.getString(R.string.pull_down_to_refresh)
                        )
                    }
                } else {
                    changeTopBarSubTitle(
                        binding.mainAppBarInclude!!.mainTopBarSubtitle,
                        binding.mainAppBarInclude!!.mainTopBarTitle,
                        null
                    )
                }

            }

            override fun cancel() {
                changeTopBarSubTitle(
                    binding.mainAppBarInclude!!.mainTopBarSubtitle,
                    binding.mainAppBarInclude!!.mainTopBarTitle,
                    null
                )
            }
        })
    }

    fun refreshAllData() {
        // Refresh all data in child fragments
        val homeFragment: HomeFragment? = (viewPager.adapter as MainViewpagerAdapter).getFragmentByTag("HomeFragment") as HomeFragment?
        val aliasFragment: AliasFragment? = (viewPager.adapter as MainViewpagerAdapter).getFragmentByTag("AliasFragment") as AliasFragment?
        val recipientsFragment: RecipientsFragment? = (viewPager.adapter as MainViewpagerAdapter).getFragmentByTag("RecipientsFragment") as RecipientsFragment?
        homeFragment?.getDataFromWeb(null)
        aliasFragment?.getDataFromWeb(null)
        recipientsFragment?.getDataFromWeb(null)


        if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            val usernamesSettingsFragment: UsernamesSettingsFragment? = (viewPager.adapter as MainViewpagerAdapter).getFragmentByTag("UsernamesSettingsFragment") as UsernamesSettingsFragment?
            usernamesSettingsFragment?.getDataFromWeb(null)

            val domainSettingsFragment: DomainSettingsFragment? = (viewPager.adapter as MainViewpagerAdapter).getFragmentByTag("DomainSettingsFragment") as DomainSettingsFragment?
            domainSettingsFragment?.getDataFromWeb(null)

            val rulesSettingsFragment: RulesSettingsFragment? = (viewPager.adapter as MainViewpagerAdapter).getFragmentByTag("RulesSettingsFragment") as RulesSettingsFragment?
            rulesSettingsFragment?.getDataFromWeb(null)

            val failedDeliveriesFragment: FailedDeliveriesFragment? = (viewPager.adapter as MainViewpagerAdapter).getFragmentByTag("FailedDeliveriesFragment") as FailedDeliveriesFragment?
            failedDeliveriesFragment?.getDataFromWeb(null)
        }

        // Check for updates and check API expiration key
        lifecycleScope.launch {
            checkForUpdates()
            checkForApiExpiration()
            checkForCertificateExpiration()
            checkForSubscriptionExpiration()
            checkForNewFailedDeliveries()
            checkForNewAccountNotifications()
        }

        MainActivityTimeClass.updateLastGeneralRefresh()

    }


    override fun onResume() {
        super.onResume()
        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            initialiseMainAppBar()
        }
        checkForPermissions()


        if (MainActivityTimeClass.isPast5Minutes()) {
            //println("More than 5 minutes have passed since the last general refresh.")

            // Refresh general data when coming back from the background to the foreground
            refreshAllData()
        }


    }


    private lateinit var binding: ActivityMainBinding
    private fun loadMainActivity() {
        showChangeLog()

        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            setupRefreshLayout(binding.mainAppBarInclude!!.appBar, binding.refreshLayout!!)
        }

        val navView = if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) binding.navRail!! else binding.navView!!
        viewPager =
            if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) binding.activityMainViewpagerSw600dp!! else binding.activityMainViewpager!!

        val fragmentList = if (resources.getBoolean(R.bool.isTablet)) {
            arrayListOf(
                HomeFragment.newInstance(),
                AliasFragment.newInstance(),
                RecipientsFragment.newInstance(),
                UsernamesSettingsFragment.newInstance(),
                DomainSettingsFragment.newInstance(),
                RulesSettingsFragment.newInstance(),
                FailedDeliveriesFragment.newInstance()
            )
        } else {
            arrayListOf(
                HomeFragment.newInstance(),
                AliasFragment.newInstance(),
                RecipientsFragment.newInstance()
            )
        }



        viewPager.adapter = MainViewpagerAdapter(this, fragmentList)
        viewPager.offscreenPageLimit = if (resources.getBoolean(R.bool.isTablet)) 7 else 3
        // Allow swiping through the pages
        viewPager.isUserInputEnabled = true
        viewPager.setPageTransformer { page, position ->
            val normalizedposition = abs(abs(position) - 1)
            page.alpha = normalizedposition
        }


        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        navView.menu.findItem(R.id.navigation_home).isChecked = true

                        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
                            changeTopBarTitle(
                                binding.mainAppBarInclude!!.mainTopBarTitle,
                                this@MainActivity.resources.getString(R.string.title_home)
                            )
                        }

                    }

                    1 -> {
                        navView.menu.findItem(R.id.navigation_alias).isChecked = true

                        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
                            changeTopBarTitle(
                                binding.mainAppBarInclude!!.mainTopBarTitle,
                                this@MainActivity.resources.getString(R.string.title_aliases)
                            )
                        }

                    }

                    2 -> {
                        navView.menu.findItem(R.id.navigation_recipients).isChecked = true

                        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
                            changeTopBarTitle(
                                binding.mainAppBarInclude!!.mainTopBarTitle,
                                this@MainActivity.resources.getString(R.string.title_recipients)
                            )
                        }

                    }

                    3 -> {
                        navView.menu.findItem(R.id.navigation_usernames).isChecked = true
                    }

                    4 -> {
                        navView.menu.findItem(R.id.navigation_domains).isChecked = true
                    }

                    5 -> {
                        navView.menu.findItem(R.id.navigation_rules).isChecked = true
                    }

                    6 -> {
                        hideFailedDeliveriesBadge()

                        navView.menu.findItem(R.id.navigation_failed_deliveries).isChecked = true
                    }
                }
                super.onPageSelected(position)
            }
        })

        if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            binding.navRail!!.setOnItemSelectedListener {
                navigateTo(it.itemId)
                false
            }
        } else {
            binding.navView!!.setOnItemSelectedListener {
                navigateTo(it.itemId)
                false
            }
        }

        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            binding.mainAppBarInclude!!.toolbar.setOnClickListener {
                val intent = Intent("scroll_up")
                sendBroadcast(intent)
                binding.mainAppBarInclude!!.appBar.setExpanded(true, true)
            }
        }

        checkForTargetExtrasAndStartupPage()
    }

    private fun checkForStartupPage() {
        val startupPageValue = SettingsManager(false, this).getSettingsString(PREFS.STARTUP_PAGE, "home")
        val startupPageOptions = this.resources.getStringArray(R.array.startup_page_options).toList()

        // Check if the value exists in the array, default (but dont reset) to home if not (this could occur if eg. a tablet backup (which has more options) gets restored on mobile)
        // Don't reset the value as this app could be opened in splitscreen, we don't want to reset the value then.
        if (startupPageOptions.contains(startupPageValue)) {
            goToTarget(startupPageValue.toString())
        }

    }

    private fun checkForTargetExtrasAndStartupPage() {
        val target = intent.getStringExtra("target")
        if (!target.isNullOrEmpty()) {
            goToTarget(target)
        } else {
            checkForStartupPage()
        }
    }

    private fun showChangeLog() {
        // Check the version code in the sharedpreferences, if the one in the preferences is older than the current one, the app got updated.
        // Show the changelog
        val settingsManager = SettingsManager(false, this)
        if (settingsManager.getSettingsInt(PREFS.VERSION_CODE) < BuildConfig.VERSION_CODE) {
            val addChangelogBottomDialogFragment: ChangelogBottomDialogFragment =
                ChangelogBottomDialogFragment.newInstance()
            addChangelogBottomDialogFragment.show(
                supportFragmentManager,
                "MainActivity:addChangelogBottomDialogFragment"
            )
        }

        // Write the current version code to prevent double triggering
        settingsManager.putSettingsInt(PREFS.VERSION_CODE, BuildConfig.VERSION_CODE)

        settingsManager.putSettingsInt(
            PREFS.TIMES_THE_APP_HAS_BEEN_OPENED,
            settingsManager.getSettingsInt(PREFS.TIMES_THE_APP_HAS_BEEN_OPENED) + 1)

        if (BuildConfig.DEBUG) {
            print("App has been opened ${settingsManager.getSettingsInt(PREFS.TIMES_THE_APP_HAS_BEEN_OPENED)} times")
        }
    }

    // Only gets calls on mobile (not tablet)
    private fun initialiseMainAppBar() {
        // Figure out the from name initials
        val usernameInitials = (this.application as AddyIoApp).userResource.username.take(2).uppercase(Locale.getDefault())
        binding.mainAppBarInclude!!.mainTopBarUserInitials.text = usernameInitials

        binding.mainAppBarInclude!!.mainTopBarUserInitials.setOnClickListener {
            if (!profileBottomDialogFragment.isAdded) {
                profileBottomDialogFragment.show(
                    supportFragmentManager,
                    "profileBottomDialogFragment"
                )
            }
        }

        binding.mainAppBarInclude!!.mainTopBarSearchIcon.setOnClickListener {
            openSearch()
        }

        binding.mainAppBarInclude!!.mainTopBarFailedDeliveriesIcon.setOnClickListener {
            hideFailedDeliveriesBadge()
            val intent = Intent(this, FailedDeliveriesActivity::class.java)
            startActivity(intent)
        }

        binding.mainAppBarInclude!!.mainTopBarAccountNotificationsIcon.setOnClickListener {
            hideAccountNotificationsBadge()
            val intent = Intent(this, AccountNotificationsActivity::class.java)
            startActivity(intent)
        }
    }

    fun openSearch() {
        if (!searchBottomDialogFragment.isAdded) {
            searchBottomDialogFragment.show(
                supportFragmentManager,
                "searchBottomDialogFragment"
            )
        }
    }


    private fun checkForPermissions() {
        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Notification permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !notificationManager.areNotificationsEnabled()) {
            profileBottomDialogFragment.permissionsRequired = true
            setAlertIconToProfile(permissionsRequired = true)
        } else {
            profileBottomDialogFragment.permissionsRequired = false
            setAlertIconToProfile(permissionsRequired = false)
        }
    }


    private suspend fun checkForUpdates() {
        val settingsManager = SettingsManager(false, this)
        if (settingsManager.getSettingsBool(PREFS.NOTIFY_UPDATES)) {
            Updater.isUpdateAvailable({ updateAvailable: Boolean, _: String?, _: Boolean, _: String? ->

                // Set the update status in profileBottomDialogFragment
                profileBottomDialogFragment.updateAvailable = updateAvailable

                // An update is available, set the update  profile bottomdialog fragment
                setAlertIconToProfile(updateAvailable = updateAvailable)
            }, this)
        }
    }

    private fun checkForCertificateExpiration(){
        val encryptedSettingsManager = SettingsManager(true, this)
        val alias = encryptedSettingsManager.getSettingsString(PREFS.CERTIFICATE_ALIAS)

        if (alias != null) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val chain = KeyChain.getCertificateChain(this@MainActivity, alias)
                    val expiryDateOfChain = chain?.firstOrNull()?.notAfter


                    if (expiryDateOfChain != null) {
                        val expiryDate = DateTimeUtils.convertDateToLocalTimeZoneDate(expiryDateOfChain) // Get the expiry date
                        val currentDateTime = LocalDateTime.now() // Get the current date
                        val deadLineDate = expiryDate?.minusDays(5) // Subtract 5 days from the expiry date
                        if (currentDateTime.isAfter(deadLineDate)) {
                            // The current date is suddenly after the deadline date. It will expire within 5 days
                            // Show the certificate is about to expire card
                            val text = PrettyTime().format(expiryDate)

                            withContext(Dispatchers.Main) {
                                MaterialDialogHelper.showMaterialDialog(
                                    context = this@MainActivity,
                                    title = this@MainActivity.resources.getString(R.string.certificate_about_to_expire),
                                    message = this@MainActivity.resources.getString(R.string.certificate_about_to_expire_desc, text),
                                    icon = R.drawable.ic_certificate,
                                    neutralButtonText = this@MainActivity.resources.getString(R.string.dismiss),
                                    positiveButtonText = this@MainActivity.resources.getString(R.string.certificate_about_to_expire_option_1),
                                    positiveButtonAction = {
                                        selectCertificate()
                                    }).show()
                            }

                        } else {
                            // The current date is not yet after the deadline date.
                        }
                    }
                }
            }
            // If expiryDate is null it will never expire, which I highly doubt will EVER happen

        }
    }


    private suspend fun checkForApiExpiration() {
        networkHelper.getApiTokenDetails { apiTokenDetails, error ->
            if (apiTokenDetails?.expires_at != null) {

                val expiryDate = DateTimeUtils.convertStringToLocalTimeZoneDate(apiTokenDetails.expires_at) // Get the expiry date
                val currentDateTime = LocalDateTime.now() // Get the current date
                val deadLineDate = expiryDate?.minusDays(5) // Subtract 5 days from the expiry date
                if (currentDateTime.isAfter(deadLineDate)) {
                    // The current date is suddenly after the deadline date. It will expire within 5 days
                    // Show the api is about to expire card
                    val text = PrettyTime().format(expiryDate)
                    MaterialDialogHelper.showMaterialDialog(
                        context = this@MainActivity,
                        title = this@MainActivity.resources.getString(R.string.api_token_about_to_expire),
                        message = this@MainActivity.resources.getString(R.string.api_token_about_to_expire_desc, text),
                        icon = R.drawable.ic_letters_case,
                        neutralButtonText = this@MainActivity.resources.getString(R.string.dismiss),
                        positiveButtonText = this@MainActivity.resources.getString(R.string.api_token_about_to_expire_option_1),
                        positiveButtonAction = {
                            verifyNewApiToken()
                        },

                        ).show()

                } else {
                    // The current date is not yet before the deadline date. It will expire within 5 days
                }
            }
            // If expires_at is null it will never expire

        }

    }

    private var subscriptionResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            if (data?.getBooleanExtra("hasNewSubscription", false) == true) {
                refreshAllData()
            }
        }
    }

    private fun checkForSubscriptionExpiration() {
        // Only check on hosted instance
        if (AddyIo.isUsingHostedInstance) {
            lifecycleScope.launch {
                networkHelper.getUserResource { user: UserResource?, _: String? ->
                    if (user?.subscription_ends_at != null) {
                        val expiryDate = DateTimeUtils.convertStringToLocalTimeZoneDate(user.subscription_ends_at) // Get the expiry date
                        val currentDateTime = LocalDateTime.now() // Get the current date
                        val deadLineDate = expiryDate?.minusDays(7) // Subtract 7 days from the expiry date
                        if (currentDateTime.isAfter(deadLineDate)) {
                            // The current date is suddenly after the deadline date. It will expire within 7 days
                            val text = PrettyTime().format(expiryDate)
                            val dialog = MaterialDialogHelper.showMaterialDialog(
                                context = this@MainActivity,
                                title = this@MainActivity.resources.getString(R.string.subscription_about_to_expire),
                                message = this@MainActivity.resources.getString(R.string.subscription_about_to_expire_desc, text),
                                icon = R.drawable.ic_credit_card,
                                neutralButtonText = this@MainActivity.resources.getString(R.string.dismiss),
                            )
                            // Only show the renew button when not-google play version
                            // https://support.google.com/googleplay/android-developer/answer/13321562
                                dialog.setPositiveButton(
                                    this@MainActivity.resources.getString(R.string.subscription_about_to_expire_option_1)
                                ) { _, _ ->
                                    if (BuildConfig.FLAVOR == "gplay") {
                                        val intent = Intent(this@MainActivity, ManageSubscriptionActivity::class.java)
                                        subscriptionResultLauncher.launch(intent)
                                    } else {
                                        val url = "${AddyIo.API_BASE_URL}/settings/subscription"
                                        val i = Intent(Intent.ACTION_VIEW)
                                        i.data = url.toUri()
                                        startActivity(i)
                                    }



                                }

                            dialog.show()
                        }
                    }
                }
            }
        }

    }


    private fun verifyNewApiToken() {
        addApiBottomDialogFragment = AddApiBottomDialogFragment.newInstance(AddyIo.API_BASE_URL)
        if (!addApiBottomDialogFragment.isAdded) {
            addApiBottomDialogFragment.show(
                supportFragmentManager,
                "addApiBottomDialogFragment"
            )
        }
    }

    private fun selectCertificate() {
        KeyChain.choosePrivateKeyAlias(this, object : KeyChainAliasCallback {
            override fun alias(alias: String?) {
                // If user denies access to the selected certificate
                if (alias == null) {
                    return
                }

                SettingsManager(true,this@MainActivity).putSettingsString(PREFS.CERTIFICATE_ALIAS, alias)
                SettingsManager(false,this@MainActivity).putSettingsBool(PREFS.NOTIFY_CERTIFICATE_EXPIRY, true) // Enable by default when a certificate has been selected

                // Since certificate expiry should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(this@MainActivity).scheduleBackgroundWorker()

                val notificationManager = this@MainActivity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
                    SnackbarHelper.createSnackbar(
                        this@MainActivity,
                        this@MainActivity.resources.getString(R.string.certificate_updated),
                        binding.mainContainer
                    ).show()
                    notificationManager.cancel(NotificationHelper.CERTIFICATE_EXPIRE_NOTIFICATION_ID)
                } else {
                    binding.navView.let {
                        SnackbarHelper.createSnackbar(
                            this@MainActivity,
                            this@MainActivity.resources.getString(R.string.certificate_updated),
                            it!!
                        ).apply {
                            anchorView = binding.navView
                        }.show()
                        notificationManager.cancel(NotificationHelper.CERTIFICATE_EXPIRE_NOTIFICATION_ID)
                    }
                }
            }
        }, null, null, null, null)
    }


    private var mUpdateAvailable = false
    private var mPermissionsRequired = false
    private fun setAlertIconToProfile(updateAvailable: Boolean? = null, permissionsRequired: Boolean? = null) {
        // Store the bools for comparison next time this method gets called
        if (updateAvailable != null) {
            mUpdateAvailable = updateAvailable
        }
        if (permissionsRequired != null) {
            mPermissionsRequired = permissionsRequired
        }

        val shouldShowDot = mUpdateAvailable || mPermissionsRequired


        if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            // If there is an update available or there are permissions required, show the dot
            if (shouldShowDot) {
                binding.navRail!!.headerView?.findViewById<ImageView>(R.id.navigation_rail_fab_settings)!!
                    .setColorFilter(ContextCompat.getColor(this, R.color.softRed), android.graphics.PorterDuff.Mode.SRC_IN)
            } else {
                binding.navRail!!.headerView?.findViewById<ImageView>(R.id.navigation_rail_fab_settings)!!.colorFilter = null
            }
        } else {
            // If there is an update available or there are permissions required, show the dot
            val animZoom = if (shouldShowDot && binding.mainAppBarInclude!!.mainTopBarUserInitialsUpdateIcon.visibility != View.VISIBLE) {
                // loading the animation of
                // zoom_in.xml file into a variable
                AnimationUtils.loadAnimation(
                    this,
                    R.anim.zoom_in
                )
            } else if (
            // If there is not update AND there are no permissions required, hide the dot
                !shouldShowDot &&
                binding.mainAppBarInclude!!.mainTopBarUserInitialsUpdateIcon.visibility != View.INVISIBLE
            ) {
                // loading the animation of
                // zoom_in.xml file into a variable
                AnimationUtils.loadAnimation(
                    this,
                    R.anim.zoom_out
                )
            } else {
                null
            }

            animZoom?.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                    binding.mainAppBarInclude!!.mainTopBarUserInitialsUpdateIcon.visibility = if (shouldShowDot) View.INVISIBLE else View.VISIBLE
                }

                override fun onAnimationEnd(p0: Animation?) {
                    binding.mainAppBarInclude!!.mainTopBarUserInitialsUpdateIcon.visibility = if (shouldShowDot) View.VISIBLE else View.INVISIBLE
                }

                override fun onAnimationRepeat(p0: Animation?) {
                    //
                }
            }
            )
            animZoom?.let { binding.mainAppBarInclude!!.mainTopBarUserInitialsUpdateIcon.startAnimation(it) }
        }
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
        val encryptedSettingsManager = SettingsManager(true, this)
        networkHelper.getAllFailedDeliveries { result, _ ->
            val currentFailedDeliveries =
                encryptedSettingsManager.getSettingsInt(PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT)
            if ((result?.size ?: 0) > currentFailedDeliveries) {
                if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
                    if (binding.mainAppBarInclude!!.mainTopBarFailedDeliveriesNewItemsIcon.visibility != View.VISIBLE) {
                        // loading the animation of
                        // zoom_in.xml file into a variable
                        val animZoomIn = AnimationUtils.loadAnimation(
                            this,
                            R.anim.zoom_in
                        )
                        animZoomIn.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(p0: Animation?) {
                                binding.mainAppBarInclude!!.mainTopBarFailedDeliveriesNewItemsIcon.visibility = View.VISIBLE
                            }

                            override fun onAnimationEnd(p0: Animation?) {
                                //
                            }

                            override fun onAnimationRepeat(p0: Animation?) {
                                //
                            }
                        }
                        )
                        binding.mainAppBarInclude!!.mainTopBarFailedDeliveriesNewItemsIcon.startAnimation(animZoomIn)
                    }
                } else {
                    val badge = binding.navRail!!.getOrCreateBadge(R.id.navigation_failed_deliveries)
                    badge.isVisible = true
                    // An icon only badge will be displayed unless a number or text is set:
                    badge.number = (result?.size?.minus(currentFailedDeliveries)) ?: 0  // or badge.text = "New"
                }
            } else {
                hideFailedDeliveriesBadge()
            }
        }
    }

    /*
     This method checks if there are new account notifications
     It does this by getting the current account notifications count, if that count is bigger than the account notifications in the cache that means there are new notifications

     As BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT is only updated in the service and in the AccountNotificationsActivity that means that the red
     indicator is only visible if:

     - The activity has not been opened since there were new items.
     - There are more account notifications than the server cached last time (in which case the user should have got a notification)
     */

    private suspend fun checkForNewAccountNotifications() {
        val encryptedSettingsManager = SettingsManager(true, this)
        networkHelper.getAllAccountNotifications { result, _ ->
            val currentAccountNotifications =
                encryptedSettingsManager.getSettingsInt(PREFS.BACKGROUND_SERVICE_CACHE_ACCOUNT_NOTIFICATIONS_COUNT)
            if ((result?.size ?: 0) > currentAccountNotifications) {
                if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {

                    if (binding.mainAppBarInclude!!.mainTopBarAccountNotificationsNewItemsIcon.visibility != View.VISIBLE) {
                        // loading the animation of
                        // zoom_in.xml file into a variable
                        val animZoomIn = AnimationUtils.loadAnimation(
                            this,
                            R.anim.zoom_in
                        )
                        animZoomIn.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(p0: Animation?) {
                                binding.mainAppBarInclude!!.mainTopBarAccountNotificationsNewItemsIcon.visibility = View.VISIBLE
                            }

                            override fun onAnimationEnd(p0: Animation?) {
                                //
                            }

                            override fun onAnimationRepeat(p0: Animation?) {
                                //
                            }
                        }
                        )
                        binding.mainAppBarInclude!!.mainTopBarAccountNotificationsNewItemsIcon.startAnimation(animZoomIn)
                    }
                } else {
                    binding.navRail!!.headerView?.findViewById<ImageView>(R.id.navigation_rail_fab_account_notifications)!!
                        .setColorFilter(ContextCompat.getColor(this, R.color.softRed), android.graphics.PorterDuff.Mode.SRC_IN)
                }
            } else {
                hideAccountNotificationsBadge()
            }

        }
    }

    private fun hideFailedDeliveriesBadge() {
        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {

            if (binding.mainAppBarInclude!!.mainTopBarFailedDeliveriesNewItemsIcon.visibility != View.INVISIBLE) {

                // loading the animation of
                // zoom_out.xml file into a variable
                val animZoomOut = AnimationUtils.loadAnimation(
                    this,
                    R.anim.zoom_out
                )
                animZoomOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(p0: Animation?) {
                        //
                    }

                    override fun onAnimationEnd(p0: Animation?) {
                        binding.mainAppBarInclude!!.mainTopBarFailedDeliveriesNewItemsIcon.visibility = View.INVISIBLE
                    }

                    override fun onAnimationRepeat(p0: Animation?) {
                        //
                    }
                }
                )
                binding.mainAppBarInclude!!.mainTopBarFailedDeliveriesNewItemsIcon.startAnimation(animZoomOut)
            }
        } else {
            binding.navRail!!.removeBadge(R.id.navigation_failed_deliveries)
        }
    }

    private fun hideAccountNotificationsBadge() {
        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {

            if (binding.mainAppBarInclude!!.mainTopBarAccountNotificationsNewItemsIcon.visibility != View.INVISIBLE) {

                // loading the animation of
                // zoom_out.xml file into a variable
                val animZoomOut = AnimationUtils.loadAnimation(
                    this,
                    R.anim.zoom_out
                )
                animZoomOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(p0: Animation?) {
                        //
                    }

                    override fun onAnimationEnd(p0: Animation?) {
                        binding.mainAppBarInclude!!.mainTopBarAccountNotificationsNewItemsIcon.visibility = View.INVISIBLE
                    }

                    override fun onAnimationRepeat(p0: Animation?) {
                        //
                    }
                }
                )
                binding.mainAppBarInclude!!.mainTopBarAccountNotificationsNewItemsIcon.startAnimation(animZoomOut)
            }
        } else {
            binding.navRail!!.headerView?.findViewById<ImageView>(R.id.navigation_rail_fab_account_notifications)!!.colorFilter = null
        }

    }

    fun navigateTo(fragment: Int) {
        when (fragment) {
            R.id.navigation_home -> viewPager.currentItem = 0
            R.id.navigation_alias -> viewPager.currentItem = 1
            R.id.navigation_recipients -> viewPager.currentItem = 2
            R.id.navigation_usernames -> {  // Only SW600DP>
                if (this.resources.getBoolean(R.bool.isTablet)) {
                    viewPager.currentItem = 3
                } else {
                    val intent = Intent(this, UsernamesSettingsActivity::class.java)
                    startActivity(intent)
                }
            }

            R.id.navigation_domains -> {  // Only SW600DP>
                if (this.resources.getBoolean(R.bool.isTablet)) {
                    viewPager.currentItem = 4
                } else {
                    val intent = Intent(this, DomainSettingsActivity::class.java)
                    startActivity(intent)
                }
            }

            R.id.navigation_rules -> {  // Only SW600DP>
                if (this.resources.getBoolean(R.bool.isTablet)) {
                    viewPager.currentItem = 5
                } else {
                    val intent = Intent(this, RulesSettingsActivity::class.java)
                    startActivity(intent)
                }
            }

            R.id.navigation_failed_deliveries -> {  // Only SW600DP>

                // Tell the fragment it is shown so it can mark the failed deliveries as read by updating the count in cache
                val failedDeliveriesFragment: FailedDeliveriesFragment? = (viewPager.adapter as MainViewpagerAdapter).getFragmentByTag("FailedDeliveriesFragment") as FailedDeliveriesFragment?
                failedDeliveriesFragment?.fragmentShown()
                hideFailedDeliveriesBadge()

                if (this.resources.getBoolean(R.bool.isTablet)) {
                    viewPager.currentItem = 6
                } else {
                    val intent = Intent(this, FailedDeliveriesActivity::class.java)
                    startActivity(intent)
                }
            }
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

        searchBottomDialogFragment.dismissAllowingStateLoss()
        val intent = Intent(this, SearchActivity::class.java)
        resultLauncher.launch(intent)
    }


    // When returning from the search activity, load the appropriate screen
    private var resultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data

                if (data != null) {
                    if (data.hasExtra("target")) {
                        data.extras?.getString("target")?.let { goToTarget(it) }
                    }
                }

            }
        }

    // TODO CHECK TABLET, doesnt work from search
    // Also gets called from the startupPage check
    private fun goToTarget(string: String) {
        when (string) {
            SearchActivity.SearchTargets.ALIASES.activity -> {
                navigateTo(R.id.navigation_alias)
            }

            SearchActivity.SearchTargets.RECIPIENTS.activity -> {
                navigateTo(R.id.navigation_recipients)
            }

            SearchActivity.SearchTargets.DOMAINS.activity -> {
                if (resources.getBoolean(R.bool.isTablet)) {
                    navigateTo(R.id.navigation_domains)
                } else {
                    val intent = Intent(this, DomainSettingsActivity::class.java)
                    startActivity(intent)
                }
            }

            SearchActivity.SearchTargets.USERNAMES.activity -> {
                if (resources.getBoolean(R.bool.isTablet)) {
                    navigateTo(R.id.navigation_usernames)
                } else {
                    val intent = Intent(this, UsernamesSettingsActivity::class.java)
                    startActivity(intent)
                }
            }

            SearchActivity.SearchTargets.RULES.activity -> {
                if (resources.getBoolean(R.bool.isTablet)) {
                    navigateTo(R.id.navigation_rules)
                } else {
                    val intent = Intent(this, RulesSettingsActivity::class.java)
                    startActivity(intent)
                }

            }

            SearchActivity.SearchTargets.FAILED_DELIVERIES.activity -> {
                if (resources.getBoolean(R.bool.isTablet)) {
                    navigateTo(R.id.navigation_failed_deliveries)
                } else {
                    hideFailedDeliveriesBadge()
                    val intent = Intent(this, FailedDeliveriesActivity::class.java)
                    startActivity(intent)
                }

            }
        }
    }


    private fun updateKey(apiKey: String) {
        val encryptedSettingsManager = SettingsManager(true, this)
        encryptedSettingsManager.putSettingsString(PREFS.API_KEY, apiKey)
        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.api_key_updated),
                binding.mainContainer
            ).show()

            notificationManager.cancel(NotificationHelper.API_KEY_EXPIRE_NOTIFICATION_ID)

        } else {
            binding.navView.let {
                SnackbarHelper.createSnackbar(
                    this,
                    this.resources.getString(R.string.api_key_updated),
                    it!!
                ).apply {
                    anchorView = binding.navView
                }.show()

                notificationManager.cancel(NotificationHelper.API_KEY_EXPIRE_NOTIFICATION_ID)
            }
        }


    }

    override fun onClickSave(baseUrl: String, apiKey: String) {
        addApiBottomDialogFragment.dismissAllowingStateLoss()
        updateKey(apiKey)

        // Send the new configuration to all the connected Wear devices
        try {
            Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
                for (node in nodes) {
                    val configuration = Gson().toJson(WearOSHelper(this).createWearOSConfiguration())
                    Wearable.getMessageClient(this).sendMessage(
                        node.id,
                        "/setup",
                        configuration.toByteArray()
                    )
                }

            }
        } catch (ex: Exception) {
            // WearAPI not available, not sending anything to nodes
            LoggingHelper(this).addLog(LOGIMPORTANCE.WARNING.int, ex.toString(), "MainActivity;onClickSave", null)
        }
    }
}