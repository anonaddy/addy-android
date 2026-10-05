package host.stjin.anonaddy.ui.base

import android.app.ActivityManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.appbar.AppBarLayout
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.databinding.CustomToolbarOneHandedBinding
import host.stjin.anonaddy.ui.customviews.refreshlayout.RefreshLayout
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy.utils.ToolbarUtils
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.utils.LoggingHelper


abstract class BaseActivity : AppCompatActivity() {

    open val requiresAuthentication: Boolean = true
    private var isAuthenticating: Boolean = false

    companion object SecurityStatus {
        // This variable becomes true when the user authenticates. It will only switch back to false whenever the app is closed.
        // That way all the protected parts of the app stay available until the user explicitly closed them.
        var isSessionAuthenticated = false
    }


    /**
     * Oh, the screen stretches far, to the edge it does reach,
     * But what of my content, behind bars does it breach?
     * With each system update, my woes do renew,
     * For edge-to-edge display, if only it knew,
     * The dance of the pixels, the navigation's hue,
     * A developer's lament, in code and view.
     * https://developer.android.com/develop/ui/views/layout/edge-to-edge#kts
     */
    private var isDynamicColorsEnabled: Boolean = false

    val isTablet: Boolean
        get() = resources.getBoolean(R.bool.isTablet)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        isDynamicColorsEnabled = ServiceLocator.settingsManager.getSettingsBool(SettingsManager.PREFS.DYNAMIC_COLORS)

        if (requiresAuthentication) {
            val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
            if (encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED)) {
                if (!isSessionAuthenticated) {
                    isAuthenticated(shouldFinishOnError = true) {
                        // Session authenticated
                    }
                }
            }
        }
    }


    /*
    This method forces the use of dark/light/auto mode
     */

    fun checkForDarkModeAndSetFlags() {
        val settingsManager = ServiceLocator.settingsManager
        val mode = when (settingsManager.getSettingsInt(SettingsManager.PREFS.DARK_MODE, -1)) {
            0 -> AppCompatDelegate.MODE_NIGHT_NO
            1 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }


    // This logic is for the refreshlayout, when the home, alias or recipient fragment is scrolled they will fire the setHasReachedTopOfNsv() method
    // in their respective classes. That method will set this value, the setter then checks if the appbar is expanded and will set that result in the
    // RefreshLayout. If the value is true it means that the top of the shown fragment is reached as well as the appbar expanded. Continuing to scroll
    // up will then trigger a refresh action. Else it won't do anything
    var hasReachedTopOfNsv: Boolean = true
        set(value) {
            field = value
            refreshLayout?.shouldShowRefreshLayoutOnScroll = value && appBarIsExpanded
        }


    // This value holds the status if the app bar is expanded or not, used for the refreshlayouts
    private var appBarIsExpanded: Boolean = true

    private var refreshLayout: RefreshLayout? = null
    fun setupRefreshLayout(
        appBarLayout: AppBarLayout,
        refreshLayout: RefreshLayout
    ) {
        this.refreshLayout = refreshLayout

        appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
            this.appBarIsExpanded = (verticalOffset == 0)

            if (this.refreshLayout != null) {
                // AppBar expanded or collapsed, set shouldShowRefreshLayoutOnScroll
                this.refreshLayout!!.shouldShowRefreshLayoutOnScroll = hasReachedTopOfNsv && appBarIsExpanded
            }
        }
    }

    fun changeTopBarTitle(title: TextView, text: String) {
        title.text = text
    }

    fun changeTopBarSubTitle(subtitle: TextView, title: TextView, text: String?) {
        ToolbarUtils.changeTopBarSubTitle(subtitle, title, text)
    }

    fun shimmerTopBarSubTitle(shimmerFrameLayout: ShimmerFrameLayout, shimmer: Boolean) {
        ToolbarUtils.shimmerTopBarSubTitle(shimmerFrameLayout, shimmer)
    }

    fun setupToolbar(
        title: Int,
        nestedScrollView: NestedScrollView?,
        customToolbarOneHandedBinding: CustomToolbarOneHandedBinding? = null,
        image: Int? = null,
        customBackPressedMethod: (() -> Unit)? = null,
        showBackButton: Boolean = true
    ) {
        ToolbarUtils.setupToolbar(
            this,
            onBackPressedDispatcher,
            title,
            nestedScrollView,
            customToolbarOneHandedBinding,
            image,
            customBackPressedMethod,
            showBackButton
        ) {
            this.appBarLayout = it
        }

        this.nestedScrollView = nestedScrollView
    }

    fun toolbarSetAction(customToolbarOneHandedBinding: CustomToolbarOneHandedBinding, icon: Int, onClickListener: View.OnClickListener?) {
        ToolbarUtils.setupToolbarAction(customToolbarOneHandedBinding.customToolbarOneHandedActionButton, icon, onClickListener)
    }

    fun toolbarSetSecondAction(customToolbarOneHandedBinding: CustomToolbarOneHandedBinding, icon: Int, onClickListener: View.OnClickListener?) {
        ToolbarUtils.setupToolbarAction(customToolbarOneHandedBinding.customToolbarOneHandedActionButton2, icon, onClickListener)
    }

    private var nestedScrollView: NestedScrollView? = null
    private var appBarLayout: AppBarLayout? = null

    override fun onResume() {
        super.onResume()
        val currentDynamicColors = ServiceLocator.settingsManager.getSettingsBool(SettingsManager.PREFS.DYNAMIC_COLORS)
        if (isDynamicColorsEnabled != currentDynamicColors) {
            recreate()
        }
    }


    private val authCallbacks = mutableListOf<(Boolean) -> Unit>()

    fun requireAuthentication(shouldFinishOnError: Boolean = true, action: () -> Unit) {
        isAuthenticated(shouldFinishOnError) { isAuthenticated ->
            if (isAuthenticated) {
                action()
            }
        }
    }

    /*
    This method is getting called in multiple places to check if the user is Authenticated to use the app.
    It only gives a callback when the user is authenticated
     */
    fun isAuthenticated(shouldFinishOnError: Boolean = true, callback: (Boolean) -> Unit) {
        val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
        if (encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED)) {
            if (!isSessionAuthenticated) {
                authCallbacks.add(callback)
                if (isAuthenticating) return
                isAuthenticating = true
                val executor = ContextCompat.getMainExecutor(this)
                val biometricPrompt = BiometricPrompt(
                    this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {
                            super.onAuthenticationError(errorCode, errString)
                            isAuthenticating = false
                            val callbacks = ArrayList(authCallbacks)
                            authCallbacks.clear()
                            LoggingHelper(this@BaseActivity).addLog(LOGIMPORTANCE.WARNING.int, "$errorCode $errString", "isAuthenticated", null)

                            when (errorCode) {
                                BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                                    MaterialDialogHelper.showMaterialDialog(
                                        context = this@BaseActivity,
                                        message = getString(R.string.authentication_splash_error_unavailable),
                                        icon = R.drawable.ic_fingerprint,
                                        neutralButtonText = getString(R.string.try_again),
                                        neutralButtonAction = {
                                            isAuthenticated(shouldFinishOnError, callback)
                                        },
                                        positiveButtonText = getString(R.string.reset_app),
                                        positiveButtonAction = {
                                            (getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                                        }
                                    ).setCancelable(false).show()
                                }

                                BiometricPrompt.ERROR_USER_CANCELED,
                                BiometricPrompt.ERROR_CANCELED -> {
                                    if (shouldFinishOnError) {
                                        finish()
                                    }
                                }

                                else -> {
                                    Toast.makeText(
                                        this@BaseActivity, getString(
                                            R.string.authentication_error_s,
                                            errString
                                        ), Toast.LENGTH_LONG
                                    ).show()
                                    if (shouldFinishOnError) {
                                        finish()
                                    }
                                }
                            }
                            callbacks.forEach { it(false) }
                        }

                        override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult
                        ) {
                            super.onAuthenticationSucceeded(result)
                            isAuthenticating = false
                            isSessionAuthenticated = true
                            val callbacks = ArrayList(authCallbacks)
                            authCallbacks.clear()
                            callbacks.forEach { it(true) }
                        }

                    })

                val promptInfo =
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(resources.getString(R.string.addyio_locked))
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
