package host.stjin.anonaddy.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy.SettingsManager
import host.stjin.anonaddy.models.*
import host.stjin.anonaddy.ui.appsettings.ChangelogBottomDialogFragment
import host.stjin.anonaddy.ui.domains.DomainSettingsActivity
import host.stjin.anonaddy.ui.rules.RulesSettingsActivity
import host.stjin.anonaddy.ui.search.SearchActivity
import host.stjin.anonaddy.ui.search.SearchBottomDialogFragment
import host.stjin.anonaddy.ui.usernames.UsernamesSettingsActivity
import kotlinx.android.synthetic.main.main_top_bar_not_user.*

class MainActivity : BaseActivity(), SearchBottomDialogFragment.AddSearchBottomDialogListener {


    private val SEARCH_CONSTANT: Int = 1
    private val searchBottomDialogFragment: SearchBottomDialogFragment =
        SearchBottomDialogFragment.newInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkForDarkModeAndSetFlags()

        val settingsManager = SettingsManager(true, this)
        // First check for biometrics with a fallback on screen lock
        if (settingsManager.getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED)) {
            verifyBiometrics()
        } else {
            loadMainActivity()
        }
    }

    private fun loadMainActivity() {
        setContentView(R.layout.activity_main)
        showChangeLog()

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        navView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            changeTopBarTitle(destination.label.toString())
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

    private fun verifyBiometrics() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)

                    when (errorCode) {
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                            // The user has removed the screen lock completely.
                            // Unlock the app and continue
                            SettingsManager(true, this@MainActivity).putSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED, false)
                            Toast.makeText(
                                this@MainActivity, resources.getString(
                                    R.string.authentication_error_11
                                ), Toast.LENGTH_LONG
                            ).show()
                            loadMainActivity()
                        }
                        BiometricPrompt.ERROR_USER_CANCELED -> {
                            finish()
                        }
                        BiometricPrompt.ERROR_CANCELED -> {
                            finish()
                        }
                        else -> {
                            Toast.makeText(
                                this@MainActivity, resources.getString(
                                    R.string.authentication_error_s,
                                    errString
                                ), Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                    }
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    loadMainActivity()
                }

            })

        val promptInfo =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(resources.getString(R.string.anonaddy_locked))
                .setDeviceCredentialAllowed(true)
                .setConfirmationRequired(false)
                .build()

        biometricPrompt.authenticate(promptInfo)

    }

    private fun initialiseMainAppBar() {
        main_top_bar_user_initials.setOnClickListener {
            val i = Intent(Intent(this, DialogActivity::class.java))
            val options = ActivityOptions
                .makeSceneTransitionAnimation(
                    this as Activity?,
                    Pair.create(
                        findViewById(R.id.main_top_bar_user_initials),
                        "background_transition"
                    ),
                    Pair.create(
                        findViewById(R.id.main_top_bar_user_initials), "image_transition"
                    )
                )
            startActivity(i, options.toBundle())
        }

        main_top_bar_search_icon.setOnClickListener {
            if (!searchBottomDialogFragment.isAdded) {
                searchBottomDialogFragment.show(
                    supportFragmentManager,
                    "searchBottomDialogFragment"
                )
            }
        }
    }

    @SuppressLint("SwitchIntDef")
    fun checkForDarkModeAndSetFlags() {
        val settingsManager = SettingsManager(false, this)
        when (settingsManager.getSettingsInt(SettingsManager.PREFS.DARK_MODE, -1)) {
            0 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            1 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            -1 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    private fun changeTopBarTitle(title: String) {
        main_top_bar_not_title.text = title
    }

    fun switchFragments(fragment: Int) {
        val navController = findNavController(R.id.nav_host_fragment)
        navController.navigate(fragment)
    }

    override fun onSearch(
        filteredAliases: ArrayList<Aliases>,
        filteredRecipients: ArrayList<Recipients>,
        filteredDomains: ArrayList<Domains>,
        filteredUsernames: ArrayList<Usernames>,
        filteredRules: ArrayList<Rules>
    ) {

        SearchActivity.FilteredLists.filteredAliases = filteredAliases
        SearchActivity.FilteredLists.filteredRecipients = filteredRecipients
        SearchActivity.FilteredLists.filteredDomains = filteredDomains
        SearchActivity.FilteredLists.filteredUsernames = filteredUsernames
        SearchActivity.FilteredLists.filteredRules = filteredRules

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
                    }
                }
            }
        }
    }

}