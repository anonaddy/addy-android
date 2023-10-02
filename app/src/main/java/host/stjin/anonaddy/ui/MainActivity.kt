package host.stjin.anonaddy.ui


import android.animation.ObjectAnimator
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.launch
import org.ocpsoft.prettytime.PrettyTime
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.abs


class MainActivity : BaseActivity(), SearchBottomDialogFragment.AddSearchBottomDialogListener, AddApiBottomDialogFragment.AddApiBottomDialogListener {


    private val searchBottomDialogFragment: SearchBottomDialogFragment =
        SearchBottomDialogFragment.newInstance()

    private val profileBottomDialogFragment: ProfileBottomDialogFragment =
        ProfileBottomDialogFragment.newInstance()


    private var addApiBottomDialogFragment: AddApiBottomDialogFragment =
        AddApiBottomDialogFragment.newInstance()


    private lateinit var networkHelper: NetworkHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            drawBehindNavBar(
                binding.root,
                arrayListOf(binding.root),
                bottomViewsToShiftUpUsingPadding = arrayListOf(binding.navRail!!, binding.activityMainViewpagerSw600dp!!)
            )
        } else {
            drawBehindNavBar(
                binding.root,
                arrayListOf(binding.root),
                bottomViewsToShiftUpUsingPadding = arrayListOf(binding.navView!!, binding.activityMainViewpager!!)
            )
        }


        networkHelper = NetworkHelper(this@MainActivity)

        isAuthenticated { isAuthenticated ->
            if (isAuthenticated) {
                lifecycleScope.launch {
                    loadMainActivity()
                    // No need to check for updates on recreation of the activity
                    if (savedInstanceState == null) {
                        checkForUpdates()
                        checkForApiExpiration()
                        checkForSubscriptionExpiration()
                        // Schedule the background worker (in case this has not been done before) (this will cancel if already scheduled)
                        BackgroundWorkerHelper(this@MainActivity).scheduleBackgroundWorker()

                        checkForNewFailedDeliveries()
                    }
                }
            }
        }

        if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            setOnBigScreenClickListener()
        }

        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            setAppBar()
        }

        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            setRefreshLayout()
        }

    }

    private fun setOnBigScreenClickListener() {
        binding.navRail!!.headerView?.findViewById<FloatingActionButton>(R.id.navigation_rail_fab_settings)!!.setOnClickListener {
            val intent = Intent(this, AppSettingsActivity::class.java)
            startActivity(intent)
        }
    }

    // This only applies to <sw600Dp devices
    private fun setRefreshLayout() {
        binding.refreshLayout!!.setOnRefreshListener(object : RefreshLayout.OnRefreshListener {
            override fun refresh() {
                changeTopBarSubTitle(
                    binding.mainAppBarInclude!!.mainTopBarSubtitle,
                    binding.mainAppBarInclude!!.mainTopBarTitle,
                    binding.mainAppBarInclude!!.mainTopBarTitleSmall,
                    this@MainActivity.resources.getString(R.string.refreshing_data)
                )
                shimmerTopBarSubTitle(binding.mainAppBarInclude!!.mainTopBarSubtitleShimmerframelayout, true)

                lifecycleScope.launch {
                    checkForNewFailedDeliveries()
                }

                // Check for updates and check API expiration key
                lifecycleScope.launch {
                    checkForUpdates()
                    checkForApiExpiration()
                    checkForSubscriptionExpiration()
                }


                // Refresh all data in child fragments
                val homeFragment: HomeFragment = supportFragmentManager.fragments[0] as HomeFragment
                val aliasFragment: AliasFragment = supportFragmentManager.fragments[1] as AliasFragment
                val recipientsFragment: RecipientsFragment = supportFragmentManager.fragments[2] as RecipientsFragment
                homeFragment.getDataFromWeb(this@MainActivity, null)
                aliasFragment.getDataFromWeb(this@MainActivity, null)
                recipientsFragment.getDataFromWeb(null)


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
                        binding.mainAppBarInclude!!.mainTopBarTitleSmall,
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
                            binding.mainAppBarInclude!!.mainTopBarTitleSmall,
                            this@MainActivity.resources.getString(R.string.release_to_refresh)
                        )
                    } else {
                        changeTopBarSubTitle(
                            binding.mainAppBarInclude!!.mainTopBarSubtitle,
                            binding.mainAppBarInclude!!.mainTopBarTitle,
                            binding.mainAppBarInclude!!.mainTopBarTitleSmall,
                            this@MainActivity.resources.getString(R.string.pull_down_to_refresh)
                        )
                    }
                } else {
                    changeTopBarSubTitle(
                        binding.mainAppBarInclude!!.mainTopBarSubtitle,
                        binding.mainAppBarInclude!!.mainTopBarTitle,
                        binding.mainAppBarInclude!!.mainTopBarTitleSmall,
                        null
                    )
                }

            }

            override fun cancel() {
                changeTopBarSubTitle(
                    binding.mainAppBarInclude!!.mainTopBarSubtitle,
                    binding.mainAppBarInclude!!.mainTopBarTitle,
                    binding.mainAppBarInclude!!.mainTopBarTitleSmall,
                    null
                )
            }
        })
    }

    private fun setAppBar() {
        var collapsingToolbarExpanded = false
        binding.mainAppBarInclude!!.appBar.addOnOffsetChangedListener { _, verticalOffset ->
            if (verticalOffset == -binding.mainAppBarInclude!!.collapsingToolbar.height + binding.mainAppBarInclude!!.toolbar.height) {
                if (!collapsingToolbarExpanded) {
                    collapsingToolbarExpanded = true

                    // FADE
                    ObjectAnimator.ofFloat(binding.mainAppBarInclude!!.mainTopBarTitle, "alpha", 0f).apply {
                        duration = 300
                        start()
                    }
                    ObjectAnimator.ofFloat(binding.mainAppBarInclude!!.mainTopBarTitleSmall, "alpha", 1f).apply {
                        duration = 300
                        start()
                    }

                    // MOVE
                    ObjectAnimator.ofFloat(binding.mainAppBarInclude!!.mainTopBarTitle, "translationY", -32f).apply {
                        duration = 300
                        start()
                    }
                }
            } else {
                if (collapsingToolbarExpanded) {
                    collapsingToolbarExpanded = false

                    // FADE
                    ObjectAnimator.ofFloat(binding.mainAppBarInclude!!.mainTopBarTitle, "alpha", 1f).apply {
                        duration = 300
                        start()
                    }

                    ObjectAnimator.ofFloat(binding.mainAppBarInclude!!.mainTopBarTitleSmall, "alpha", 0f).apply {
                        duration = 300
                        start()
                    }

                    // MOVE
                    ObjectAnimator.ofFloat(binding.mainAppBarInclude!!.mainTopBarTitle, "translationY", 0f).apply {
                        duration = 300
                        start()
                    }

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            initialiseMainAppBar()
        }
        checkForPermissions()
    }


    private lateinit var binding: ActivityMainBinding
    private fun loadMainActivity() {
        showChangeLog()

        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            setupRefreshLayout(binding.mainAppBarInclude!!.appBar, binding.refreshLayout!!)
        }

        val navView = if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) binding.navRail!! else binding.navView!!
        val viewPager =
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
                                this@MainActivity.resources.getString(R.string.title_dashboard)
                            )
                            changeTopBarTitle(
                                binding.mainAppBarInclude!!.mainTopBarTitleSmall,
                                this@MainActivity.resources.getString(R.string.title_dashboard)
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
                            changeTopBarTitle(
                                binding.mainAppBarInclude!!.mainTopBarTitleSmall,
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
                            changeTopBarTitle(
                                binding.mainAppBarInclude!!.mainTopBarTitleSmall,
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
                        navView.menu.findItem(R.id.navigation_failed_deliveries).isChecked = true
                    }
                }
                super.onPageSelected(position)
            }
        })

        if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            binding.navRail!!.setOnItemSelectedListener {
                switchFragments(it.itemId)
                false
            }
        } else {
            binding.navView!!.setOnItemSelectedListener {
                switchFragments(it.itemId)
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

        checkForTargetExtras()
    }

    private fun checkForTargetExtras() {
        val target = intent.getStringExtra("target")
        if (!target.isNullOrEmpty()) {
            goToTarget(target)
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
            val intent = Intent(this, FailedDeliveriesActivity::class.java)
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
        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
        Updater.isUpdateAvailable({ updateAvailable: Boolean, _: String?, _: Boolean ->

            // Set the update status in profileBottomDialogFragment
            profileBottomDialogFragment.updateAvailable = updateAvailable

            // An update is available, set the update  profile bottomdialog fragment
            setAlertIconToProfile(updateAvailable = updateAvailable)
        }, this)
    }


    private suspend fun checkForApiExpiration() {
        networkHelper.getApiTokenDetails { apiTokenDetails, error ->
            if (apiTokenDetails?.expires_at != null) {

                val expiryDate = DateTimeUtils.turnStringIntoLocalDateTime(apiTokenDetails.expires_at) // Get the expiry date
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

    private suspend fun checkForSubscriptionExpiration() {
        // Only check on hosted instance
        if (AddyIo.VERSIONMAJOR == 9999) {
            lifecycleScope.launch {
                networkHelper.getUserResource { user: UserResource?, _: String? ->
                    if (user?.subscription_ends_at != null) {
                        val expiryDate = DateTimeUtils.turnStringIntoLocalDateTime(user.subscription_ends_at) // Get the expiry date
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
                            if (BuildConfig.FLAVOR != "gplay") {
                                dialog.setPositiveButton(
                                    this@MainActivity.resources.getString(R.string.subscription_about_to_expire_option_1)
                                ) { _, _ ->
                                    val url = "${AddyIo.API_BASE_URL}/settings/subscription"
                                    val i = Intent(Intent.ACTION_VIEW)
                                    i.data = Uri.parse(url)
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
            val animZoom =
                if (shouldShowDot && binding.navRail!!.headerView?.findViewById<ImageView>(R.id.navigation_rail_update_icon)!!.visibility != View.VISIBLE) {
                    // loading the animation of
                    // zoom_in.xml file into a variable
                    AnimationUtils.loadAnimation(
                        this,
                        R.anim.zoom_in
                    )
                } else if (
                // If there is not update AND there are no permissions required, hide the dot
                    !shouldShowDot &&
                    binding.navRail!!.headerView?.findViewById<ImageView>(R.id.navigation_rail_update_icon)!!.visibility != View.INVISIBLE
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
                    binding.navRail!!.headerView?.findViewById<ImageView>(R.id.navigation_rail_update_icon)!!.visibility =
                        if (shouldShowDot) View.INVISIBLE else View.VISIBLE
                }

                override fun onAnimationEnd(p0: Animation?) {
                    binding.navRail!!.headerView?.findViewById<ImageView>(R.id.navigation_rail_update_icon)!!.visibility =
                        if (shouldShowDot) View.VISIBLE else View.INVISIBLE
                }

                override fun onAnimationRepeat(p0: Animation?) {
                    //
                }
            }
            )
            animZoom?.let { binding.navRail!!.headerView?.findViewById<ImageView>(R.id.navigation_rail_update_icon)!!.startAnimation(it) }
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
        networkHelper.getAllFailedDeliveries({ result, _ ->
            val currentFailedDeliveries =
                encryptedSettingsManager.getSettingsInt(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT)
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
        }, show404Toast = false)
    }

    fun switchFragments(fragment: Int) {
        val viewPager =
            if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) binding.activityMainViewpagerSw600dp!! else binding.activityMainViewpager!!

        when (fragment) {
            R.id.navigation_home -> viewPager.currentItem = 0
            R.id.navigation_alias -> viewPager.currentItem = 1
            R.id.navigation_recipients -> viewPager.currentItem = 2
            R.id.navigation_usernames -> viewPager.currentItem = 3 // Only SW600DP>
            R.id.navigation_domains -> viewPager.currentItem = 4 // Only SW600DP>
            R.id.navigation_rules -> viewPager.currentItem = 5 // Only SW600DP>
            R.id.navigation_failed_deliveries -> viewPager.currentItem = 6 // Only SW600DP>
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
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data

                if (data != null) {
                    if (data.hasExtra("target")) {
                        data.extras?.getString("target")?.let { goToTarget(it) }
                    }
                }

            }
        }

    // TODO CHECK TABLET
    private fun goToTarget(string: String) {
        when (string) {
            SearchActivity.SearchTargets.ALIASES.activity -> {
                switchFragments(R.id.navigation_alias)
            }

            SearchActivity.SearchTargets.RECIPIENTS.activity -> {
                switchFragments(R.id.navigation_recipients)
            }

            SearchActivity.SearchTargets.DOMAINS.activity -> {
                if (resources.getBoolean(R.bool.isTablet)) {
                    switchFragments(R.id.navigation_domains)
                } else {
                    val intent = Intent(this, DomainSettingsActivity::class.java)
                    startActivity(intent)
                }
            }

            SearchActivity.SearchTargets.USERNAMES.activity -> {
                if (resources.getBoolean(R.bool.isTablet)) {
                    switchFragments(R.id.navigation_usernames)
                } else {
                    val intent = Intent(this, UsernamesSettingsActivity::class.java)
                    startActivity(intent)
                }
            }

            SearchActivity.SearchTargets.RULES.activity -> {
                if (resources.getBoolean(R.bool.isTablet)) {
                    switchFragments(R.id.navigation_rules)
                } else {
                    val intent = Intent(this, RulesSettingsActivity::class.java)
                    startActivity(intent)
                }

            }

            SearchActivity.SearchTargets.FAILED_DELIVERIES.activity -> {
                if (resources.getBoolean(R.bool.isTablet)) {
                    switchFragments(R.id.navigation_failed_deliveries)
                } else {
                    val intent = Intent(this, FailedDeliveriesActivity::class.java)
                    startActivity(intent)
                }

            }
        }
    }


    private fun updateKey(apiKey: String) {
        val encryptedSettingsManager = SettingsManager(true, this)
        encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.API_KEY, apiKey)

        if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            SnackbarHelper.createSnackbar(
                this,
                this.resources.getString(R.string.api_key_updated),
                binding.mainContainer
            ).show()

            val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

                val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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