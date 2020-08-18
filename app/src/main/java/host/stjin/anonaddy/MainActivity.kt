package host.stjin.anonaddy

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import host.stjin.anonaddy.models.User
import host.stjin.anonaddy.ui.DialogActivity
import kotlinx.android.synthetic.main.main_top_bar_not_user.*

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsManager = SettingsManager(true, this)
        // First check for biometrics with a fallback on screen lock
        if (settingsManager.getSettingsBool("biometric_enabled")) {
            verifyBiometrics()
        } else {
            loadMainActivity()
        }
    }

    private fun loadMainActivity() {
        setContentView(R.layout.activity_main)
        checkForDarkModeAndSetFlags()

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        navView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            changeTopBarTitle(destination.label.toString())
        }

        changeTopBarNotification(true)
        initialiseMainAppBar()
    }

    private fun verifyBiometrics() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)

                    if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS) {
                        // The user has removed the screen lock completely.
                        // Unlock the app and continue
                        SettingsManager(true, applicationContext).putSettingsBool("biometric_enabled", false)
                        Toast.makeText(
                            applicationContext, resources.getString(
                                R.string.authentication_error_11
                            ), Toast.LENGTH_LONG
                        ).show()
                        loadMainActivity()
                    } else {
                        Toast.makeText(
                            applicationContext, resources.getString(
                                R.string.authentication_error_s,
                                errString
                            ), Toast.LENGTH_LONG
                        ).show()
                        finish()
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
        main_top_bar_user_initials.text = User.userResource.username.first().toString()
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
    }

    @SuppressLint("SwitchIntDef")
    fun checkForDarkModeAndSetFlags() {
        val settingsManager = SettingsManager(false, this)
        when (settingsManager.getSettingsInt("dark_mode", -1)) {
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

    fun changeTopBarInitials(initials: String) {
        main_top_bar_user_initials.text = initials
    }

    private fun changeTopBarNotification(newNotifications: Boolean) {
        main_top_bar_not_new_icon.visibility = if (newNotifications) View.VISIBLE else View.GONE
    }


    fun switchFragments(fragment: Int) {
        val navController = findNavController(R.id.nav_host_fragment)
        navController.navigate(fragment)
    }

}