package host.stjin.anonaddy.ui
import host.stjin.anonaddy_shared.utils.GsonTools

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.security.KeyChain
import android.security.KeyChainAliasCallback
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.wearable.Wearable
import com.google.android.material.button.MaterialButton
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.databinding.ActivityMainBinding
import host.stjin.anonaddy.interfaces.Refreshable
import host.stjin.anonaddy.notifications.NotificationHelper
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.accountnotifications.AccountNotificationsActivity
import host.stjin.anonaddy.ui.aliases.AliasesFragment
import host.stjin.anonaddy.ui.appsettings.update.ChangelogBottomDialogFragment
import host.stjin.anonaddy.ui.base.SharedScrollViewModel
import host.stjin.anonaddy.ui.blocklist.BlocklistFragment
import host.stjin.anonaddy.ui.customviews.refreshlayout.RefreshLayout
import host.stjin.anonaddy.ui.domains.DomainsFragment
import host.stjin.anonaddy.ui.faileddeliveries.FailedDeliveriesActivity
import host.stjin.anonaddy.ui.faileddeliveries.FailedDeliveriesFragment
import host.stjin.anonaddy.ui.home.HomeFragment
import host.stjin.anonaddy.ui.recipients.RecipientsFragment
import host.stjin.anonaddy.ui.rules.RulesFragment
import host.stjin.anonaddy.ui.setup.AddApiBottomDialogFragment
import host.stjin.anonaddy.ui.usernames.UsernamesFragment
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.SnackbarHelper
import host.stjin.anonaddy.utils.WearOSHelper
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.managers.SettingsManager.PREFS
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.DateTimeUtils
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ocpsoft.prettytime.PrettyTime
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private object MainActivityTimeClass {
    private var lastGeneralRefresh = Date()

    fun updateLastGeneralRefresh() {
        lastGeneralRefresh = Date()
    }

    fun isPast5Minutes(): Boolean {
        val fiveMinutesInMillis = 5 * 60 * 1000
        return Date().time - lastGeneralRefresh.time > fiveMinutesInMillis
    }
}

class MainActivity : BaseActivity(), AddApiBottomDialogFragment.AddApiBottomDialogListener {

    private val sharedScrollViewModel: SharedScrollViewModel by viewModels()
    private val viewModel: MainViewModel by viewModels()
    private lateinit var navigator: MainNavigator

    lateinit var viewPager: ViewPager2

    private lateinit var binding: ActivityMainBinding

    private var subscriptionResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            if (data?.getBooleanExtra("hasNewSubscription", false) == true) {
                refreshAllData()
            }
        }
    }

    private var isUpdateAvailable = false

    private var isPermissionsRequired = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        navigator = MainNavigator(this)

        lifecycleScope.launch {
            viewModel.updateAvailable.collect { available ->
                isUpdateAvailable = available
                setAlertIconToProfile(updateAvailable = available)
            }
        }

        requireAuthentication {
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

        if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            setOnBigScreenClickListener()
        }

        setRefreshLayout()

        if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            setRailVersion()
        }


        if (AddyIo.isUsingHostedInstance) {
            if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
                binding.navRail!!.headerView?.findViewById<MaterialButton>(R.id.navigation_rail_fab_account_notifications)!!.visibility =
                    View.VISIBLE
            } else {
                binding.mainAppBarInclude!!.mainTopBarAccountNotificationsIcon.visibility = View.VISIBLE
            }
        } else {
            if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
                binding.navRail!!.headerView?.findViewById<MaterialButton>(R.id.navigation_rail_fab_account_notifications)!!.visibility =
                    View.GONE
            } else {
                binding.mainAppBarInclude!!.mainTopBarAccountNotificationsIcon.visibility = View.GONE
            }

        }


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

    // Make sure the viewPager is ABOVE the bottomnavbar
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            // In onCreate or a setup method
            ViewCompat.setOnApplyWindowInsetsListener(binding.activityMainViewpager!!) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                // Add system navigation bar height to the existing margin
                view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = systemBars.bottom
                }
                insets
            }
        }

    }

    fun refreshAllData(onFinished: (() -> Unit)? = null) {
        // Refresh all data in child fragments

        // Get all fragments currently attached to MainActivity
        val activeFragments = supportFragmentManager.fragments

        lifecycleScope.launch {
            coroutineScope {
                // Loop through the fragments and refresh only those that implement the Refreshable interface
                for (fragment in activeFragments) {
                    if (fragment is Refreshable && fragment.isAdded) {
                        launch {
                            fragment.onRefreshData()
                        }
                    }
                }

                // Check for updates and check API expiration key
                launch {
                    checkForUpdates()
                    checkForApiExpiration()
                    checkForCertificateExpiration()
                    checkForSubscriptionExpiration()
                    checkForNewFailedDeliveries()
                    checkForNewAccountNotifications()
                }
            }

            MainActivityTimeClass.updateLastGeneralRefresh()
            onFinished?.invoke()
        }
    }

    fun navigateTo(fragment: Int) {
        navigator.navigateTo(fragment)
    }

    override fun onClickSave(baseUrl: String, apiKey: String) {
        (supportFragmentManager.findFragmentByTag("addApiBottomDialogFragment") as? AddApiBottomDialogFragment)?.dismissAllowingStateLoss()
        updateKey(apiKey)

        // Send the new configuration to all the connected Wear devices
        try {
            Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
                for (node in nodes) {
                    val configuration = GsonTools.gson.toJson(WearOSHelper.createWearOSConfiguration())
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

    // Only for Sw600>
    private fun setRailVersion() {
        val railVersionText =
            if (AddyIo.isUsingHostedInstance) this.resources.getString(R.string.hosted) else AddyIo.VERSIONSTRING
        binding.navRail!!.headerView?.findViewById<TextView>(R.id.navigation_rail_fab_version)!!.text = railVersionText

        val usernameInitials = (this.application as? AddyIoApp)?.userResourceOrNull?.username?.take(2)?.uppercase(Locale.getDefault()) ?: ""
        binding.navRail!!.headerView?.findViewById<MaterialButton>(R.id.main_top_bar_user_initials)!!.text = usernameInitials

    }

    private fun setOnBigScreenClickListener() {
        binding.navRail!!.headerView?.findViewById<MaterialButton>(R.id.main_top_bar_user_initials)!!.setOnClickListener {
            val profileBottomDialogFragment = ProfileBottomDialogFragment.newInstance(isUpdateAvailable, isPermissionsRequired)
            if (!profileBottomDialogFragment.isAdded) {
                profileBottomDialogFragment.show(
                    supportFragmentManager,
                    "profileBottomDialogFragment"
                )
            }
        }

        binding.navRail!!.headerView?.findViewById<MaterialButton>(R.id.navigation_rail_fab_account_notifications)!!.setOnClickListener {
            val intent = Intent(this, AccountNotificationsActivity::class.java)
            startActivity(intent)
        }

    }

    private fun setRefreshLayout() {
        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            binding.refreshLayout?.setOnRefreshListener(object : RefreshLayout.OnRefreshListener {
                override fun refresh() {
                    changeTopBarSubTitle(
                        binding.mainAppBarInclude!!.mainTopBarSubtitle,
                        binding.mainAppBarInclude!!.mainTopBarTitle,
                        this@MainActivity.resources.getString(R.string.refreshing_data)
                    )
                    shimmerTopBarSubTitle(binding.mainAppBarInclude!!.mainTopBarSubtitleShimmerframelayout, true)

                    refreshAllData {
                        binding.refreshLayout?.finishRefreshing()
                        shimmerTopBarSubTitle(binding.mainAppBarInclude!!.mainTopBarSubtitleShimmerframelayout, true)
                        changeTopBarSubTitle(
                            binding.mainAppBarInclude!!.mainTopBarSubtitle,
                            binding.mainAppBarInclude!!.mainTopBarTitle,
                            null
                        )
                    }

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
        } else {
            binding.swipeRefreshLayoutSw600dp?.setOnChildScrollUpCallback { _, _ ->
                !this@MainActivity.hasReachedTopOfNsv
            }
            binding.swipeRefreshLayoutSw600dp?.setOnRefreshListener {
                refreshAllData {
                    binding.swipeRefreshLayoutSw600dp?.isRefreshing = false
                }
            }
        }
    }

    private fun loadMainActivity() {
        showChangeLog()

        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            setupRefreshLayout(binding.mainAppBarInclude!!.appBar, binding.refreshLayout!!)
        }

        val navView = if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) binding.navRail!! else binding.navView!!
        viewPager =
            if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) binding.activityMainViewpagerSw600dp!! else binding.activityMainViewpager!!

        val fragmentList: ArrayList<Fragment> = if (resources.getBoolean(R.bool.isTablet)) {
            arrayListOf(
                HomeFragment.newInstance(),
                AliasesFragment.newInstance(),
                RecipientsFragment.newInstance(),
                UsernamesFragment.newInstance(),
                DomainsFragment.newInstance(),
                RulesFragment.newInstance(),
                BlocklistFragment.newInstance(),
                FailedDeliveriesFragment.newInstance()
            )
        } else {
            arrayListOf(
                HomeFragment.newInstance(),
                AliasesFragment.newInstance(),
                RecipientsFragment.newInstance()
            )
        }



        viewPager.adapter = MainViewpagerAdapter(this, fragmentList)
        viewPager.offscreenPageLimit = if (resources.getBoolean(R.bool.isTablet)) 8 else 3
        // Disallow swiping through the pages
        viewPager.isUserInputEnabled = false
        viewPager.setPageTransformer { page, position ->
            val normalizedposition = abs(abs(position) - 1)
            page.alpha = normalizedposition
        }


        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        navView.menu.findItem(R.id.navigation_home)?.isChecked = true

                        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
                            binding.mainAppBarInclude?.let {
                                changeTopBarTitle(
                                    it.mainTopBarTitle,
                                    this@MainActivity.resources.getString(R.string.title_home)
                                )
                            }
                        }

                    }

                    1 -> {
                        navView.menu.findItem(R.id.navigation_alias)?.isChecked = true

                        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
                            binding.mainAppBarInclude?.let {
                                changeTopBarTitle(
                                    it.mainTopBarTitle,
                                    this@MainActivity.resources.getString(R.string.title_aliases)
                                )
                            }
                        }

                    }

                    2 -> {
                        navView.menu.findItem(R.id.navigation_recipients)?.isChecked = true

                        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
                            binding.mainAppBarInclude?.let {
                                changeTopBarTitle(
                                    it.mainTopBarTitle,
                                    this@MainActivity.resources.getString(R.string.title_recipients)
                                )
                            }
                        }

                    }

                    3 -> {
                        navView.menu.findItem(R.id.navigation_usernames)?.isChecked = true
                    }

                    4 -> {
                        navView.menu.findItem(R.id.navigation_domains)?.isChecked = true
                    }

                    5 -> {
                        navView.menu.findItem(R.id.navigation_rules)?.isChecked = true
                    }

                    7 -> {
                        hideFailedDeliveriesBadge()

                        navView.menu.findItem(R.id.navigation_failed_deliveries)?.isChecked = true
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
                sharedScrollViewModel.triggerScrollUp()
                binding.mainAppBarInclude!!.appBar.setExpanded(true, true)
            }
        }

        checkForTargetExtrasAndStartupPage()
    }

    private fun checkForStartupPage() {
        val startupPageValue = ServiceLocator.settingsManager.getSettingsString(PREFS.STARTUP_PAGE, "home")
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
        val settingsManager = ServiceLocator.settingsManager
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
            settingsManager.getSettingsInt(PREFS.TIMES_THE_APP_HAS_BEEN_OPENED) + 1
        )

        if (BuildConfig.DEBUG) {
            print("App has been opened ${settingsManager.getSettingsInt(PREFS.TIMES_THE_APP_HAS_BEEN_OPENED)} times")
        }
    }

    // Only gets calls on mobile (not tablet)
    private fun initialiseMainAppBar() {
        // Figure out the from name initials
        val usernameInitials = (this.application as? AddyIoApp)?.userResourceOrNull?.username?.take(2)?.uppercase(Locale.getDefault()) ?: ""
        binding.mainAppBarInclude!!.mainTopBarUserInitials.text = usernameInitials

        binding.mainAppBarInclude!!.mainTopBarUserInitials.setOnClickListener {
            val profileBottomDialogFragment = ProfileBottomDialogFragment.newInstance(isUpdateAvailable, isPermissionsRequired)
            if (!profileBottomDialogFragment.isAdded) {
                profileBottomDialogFragment.show(
                    supportFragmentManager,
                    "profileBottomDialogFragment"
                )
            }
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

    private fun checkForPermissions() {
        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Notification permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !notificationManager.areNotificationsEnabled()) {
            setAlertIconToProfile(permissionsRequired = true)
        } else {
            setAlertIconToProfile(permissionsRequired = false)
        }
    }

    private fun checkForUpdates() {
        viewModel.checkForUpdates()
    }

    private fun checkForCertificateExpiration() {
        val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
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
        val result = ServiceLocator.userRepository.getApiTokenDetails()
        if (result is NetworkResult.Success && result.data.expires_at != null) {
            val apiTokenDetails = result.data
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
    }

    private fun checkForSubscriptionExpiration() {
        // Only check on hosted instance
        if (AddyIo.isUsingHostedInstance) {
            lifecycleScope.launch {
                val result = ServiceLocator.userRepository.getUserResource()
                if (result is NetworkResult.Success && result.data.subscription_ends_at != null) {
                    val user = result.data
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

    /*
        This method checks if there are new failed deliveries.
        As BACKGROUND_SERVICE_CACHE_FAILED_DELIVERIES_COUNT is only updated in the service and in the FailedDeliveriesActivity that means that the red
        indicator is only visible if:

        - The activity has not been opened since there were new items.
        - There are more failed deliveries than the server cached last time (in which case the user should have got a notification)
    */
    private suspend fun checkForNewFailedDeliveries() {
        val newDeliveriesCount = viewModel.getFailedDeliveriesCount()

        if (newDeliveriesCount > 0) {
            if (!isTablet) {
                setButtonAccentColor(binding.mainAppBarInclude!!.mainTopBarFailedDeliveriesIcon, true)
            } else {
                val badge = binding.navRail!!.getOrCreateBadge(R.id.navigation_failed_deliveries)
                badge.isVisible = true
                // An icon only badge will be displayed unless a number or text is set:
                badge.number = newDeliveriesCount  // or badge.text = "New"
            }
        } else {
            hideFailedDeliveriesBadge()
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
        val newNotificationsCount = viewModel.getNewAccountNotificationsCount()
        if (newNotificationsCount > 0) {
            if (!isTablet) {
                setButtonAccentColor(binding.mainAppBarInclude!!.mainTopBarAccountNotificationsIcon, true)
            } else {
                setButtonAccentColor(binding.navRail!!.headerView?.findViewById(R.id.navigation_rail_fab_account_notifications)!!, true)
            }
        } else {
            hideAccountNotificationsBadge()
        }
    }

    private fun verifyNewApiToken() {
        val addApiBottomDialogFragment = AddApiBottomDialogFragment.newInstance(AddyIo.API_BASE_URL)
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

                ServiceLocator.encryptedSettingsManager.putSettingsString(PREFS.CERTIFICATE_ALIAS, alias)
                ServiceLocator.settingsManager.putSettingsBool(
                    PREFS.NOTIFY_CERTIFICATE_EXPIRY,
                    true
                ) // Enable by default when a certificate has been selected

                // Since certificate expiry should be monitored in the background, call scheduleBackgroundWorker. This method will schedule the service if its required
                BackgroundWorkerHelper(this@MainActivity).scheduleBackgroundWorker()

                val notificationManager = this@MainActivity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                if (isTablet) {
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

    private fun setAlertIconToProfile(updateAvailable: Boolean? = null, permissionsRequired: Boolean? = null) {
        // Store the bools for comparison next time this method gets called
        if (updateAvailable != null) {
            isUpdateAvailable = updateAvailable
        }
        if (permissionsRequired != null) {
            isPermissionsRequired = permissionsRequired
        }

        val shouldShowDot = isUpdateAvailable || isPermissionsRequired

        if (this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            // If there is an update available or there are permissions required, show the dot
            setButtonAccentColor(binding.navRail!!.headerView?.findViewById(R.id.main_top_bar_user_initials)!!, shouldShowDot)
        } else {
            setButtonAccentColor(binding.mainAppBarInclude!!.mainTopBarUserInitials, shouldShowDot)
        }
    }

    private fun hideFailedDeliveriesBadge() {
        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            setButtonAccentColor(binding.mainAppBarInclude!!.mainTopBarFailedDeliveriesIcon, false)
        } else {
            binding.navRail?.removeBadge(R.id.navigation_failed_deliveries)
        }
    }


    private fun setButtonAccentColor(button: MaterialButton, shouldAccent: Boolean) {
        if (shouldAccent) {
            button.setTextColor(ContextCompat.getColor(this, R.color.softRed))
            button.icon?.colorFilter = android.graphics.PorterDuffColorFilter(ContextCompat.getColor(this, R.color.softRed), android.graphics.PorterDuff.Mode.SRC_IN)
        } else {
            val typedValue = TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
            val defaultColor = typedValue.data
            button.setTextColor(defaultColor)
            button.icon?.colorFilter = null

        }
    }

    private fun hideAccountNotificationsBadge() {
        if (!this@MainActivity.resources.getBoolean(R.bool.isTablet)) {
            setButtonAccentColor(binding.mainAppBarInclude!!.mainTopBarAccountNotificationsIcon, false)
        } else {
            binding.navRail?.headerView?.findViewById<MaterialButton>(R.id.navigation_rail_fab_account_notifications)?.icon?.colorFilter = null
        }

    }

    // Also gets called from the startupPage check
    private fun goToTarget(string: String) {
        when (string) {
            ActivityTargets.ALIASES.activity -> {
                navigateTo(R.id.navigation_alias)
            }

            ActivityTargets.RECIPIENTS.activity -> {
                navigateTo(R.id.navigation_recipients)
            }

            ActivityTargets.DOMAINS.activity -> {
                navigateTo(R.id.navigation_domains)
            }

            ActivityTargets.USERNAMES.activity -> {
                navigateTo(R.id.navigation_usernames)
            }

            ActivityTargets.RULES.activity -> {
                navigateTo(R.id.navigation_rules)
            }

            ActivityTargets.FAILED_DELIVERIES.activity -> {
                navigateTo(R.id.navigation_failed_deliveries)
            }
        }
    }

    private fun updateKey(apiKey: String) {
        val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
        encryptedSettingsManager.putSettingsString(PREFS.API_KEY, apiKey)
        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (isTablet) {
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
}
