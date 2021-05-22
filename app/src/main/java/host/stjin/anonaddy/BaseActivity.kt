package host.stjin.anonaddy

import android.annotation.SuppressLint
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar

abstract class BaseActivity : AppCompatActivity() {

    companion object SecurityStatus {
        // This variable becomes true when the user authenticates. It will only switch back to false whenever the app is closed.
        // That way all the protected parts of the app stay available until the user explicitly closed them.
        var isSessionAuthenticated = false
    }

    fun View.doOnApplyWindowInsets(f: (View, WindowInsets, InitialPadding) -> Unit) {
        // Create a snapshot of the view's padding state
        val initialPadding = recordInitialPaddingForView(this)
        // Set an actual OnApplyWindowInsetsListener which proxies to the given
        // lambda, also passing in the original padding state
        setOnApplyWindowInsetsListener { v, insets ->
            f(v, insets, initialPadding)
            // Always return the insets, so that children can also use them
            insets
        }
        // request some insets
        //requestApplyInsetsWhenAttached()
    }


    /*
    This method forces the use of dark/light/auto mode
     */

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

    data class InitialPadding(
        val left: Int, val top: Int,
        val right: Int, val bottom: Int
    )

    private fun recordInitialPaddingForView(view: View) = InitialPadding(
        view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom
    )

    fun setupToolbar(toolbar: MaterialToolbar, title: Int) {
        toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24) // need to set the icon here to have a navigation icon. You can simple create an vector image by "Vector Asset" and using here
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        toolbar.title = this.resources.getString(title)
    }


    /*
    This method is getting called in multiple places to check if the user is Authenticated to use the app.
    It only gived a callback when the user is authenticated
     */
    fun isAuthenticated(callback: (Boolean) -> Unit) {
        val settingsManager = SettingsManager(true, this)
        if (settingsManager.getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED)) {
            if (!isSessionAuthenticated) {
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
                                    SettingsManager(true, this@BaseActivity).putSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED, false)
                                    Toast.makeText(
                                        this@BaseActivity, resources.getString(
                                            R.string.authentication_error_11
                                        ), Toast.LENGTH_LONG
                                    ).show()
                                    isSessionAuthenticated = true
                                    callback(true)
                                }
                                BiometricPrompt.ERROR_USER_CANCELED -> {
                                    finish()
                                }
                                BiometricPrompt.ERROR_CANCELED -> {
                                    finish()
                                }
                                else -> {
                                    Toast.makeText(
                                        this@BaseActivity, resources.getString(
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
                            isSessionAuthenticated = true
                            callback(true)
                        }

                    })

                val promptInfo =
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(resources.getString(R.string.anonaddy_locked))
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                        .setConfirmationRequired(false)
                        .build()

                biometricPrompt.authenticate(promptInfo)
            } else {
                // Session was already authenticated.
                callback(true)
            }
        } else {
            isSessionAuthenticated = true
            callback(true)
        }

    }
}
