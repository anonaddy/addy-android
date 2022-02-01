package host.stjin.anonaddy

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.appbar.AppBarLayout
import host.stjin.anonaddy.databinding.CustomToolbarOneHandedBinding
import host.stjin.anonaddy_shared.SettingsManager


abstract class BaseActivity : AppCompatActivity() {


    companion object SecurityStatus {
        // This variable becomes true when the user authenticates. It will only switch back to false whenever the app is closed.
        // That way all the protected parts of the app stay available until the user explicitly closed them.
        var isSessionAuthenticated = false
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

    fun setupToolbar(
        title: Int,
        nestedScrollView: NestedScrollView?,
        customToolbarOneHandedBinding: CustomToolbarOneHandedBinding? = null,
        image: Int? = null,
        showAction: Boolean = false
    ) {
        customToolbarOneHandedBinding?.customToolbarOneHandedMaterialtoolbar?.setNavigationIcon(R.drawable.ic_arrow_back) // need to set the icon here to have a navigation icon. You can simple create an vector image by "Vector Asset" and using here
        customToolbarOneHandedBinding?.customToolbarOneHandedMaterialtoolbar?.setNavigationOnClickListener {
            onBackPressed()
        }
        customToolbarOneHandedBinding?.customToolbarOneHandedMaterialtoolbar?.title = this.resources.getString(title)

        if (customToolbarOneHandedBinding?.customToolbarOneHandedImage != null && image != null) {
            customToolbarOneHandedBinding.customToolbarOneHandedImage.setImageDrawable(ContextCompat.getDrawable(this, image))
        }

        customToolbarOneHandedBinding?.customToolbarOneHandedMaterialtoolbar?.setOnClickListener {
            val intent = Intent("scroll_up")
            sendBroadcast(intent)
        }

        if (showAction) {
            customToolbarOneHandedBinding?.customToolbarOneHandedActions?.visibility = View.VISIBLE
        }

        this.nestedScrollView = nestedScrollView
        this.appBarLayout = customToolbarOneHandedBinding?.customToolbarAppbar
    }

    private var nestedScrollView: NestedScrollView? = null
    private var appBarLayout: AppBarLayout? = null
    private val mScrollUpBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            nestedScrollView?.post { nestedScrollView?.fullScroll(ScrollView.FOCUS_UP) }
            appBarLayout?.setExpanded(true, true)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mScrollUpBroadcastReceiver)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mScrollUpBroadcastReceiver, IntentFilter("scroll_up"))
    }

    /*
    This method is getting called in multiple places to check if the user is Authenticated to use the app.
    It only gived a callback when the user is authenticated
     */
    fun isAuthenticated(shouldFinishOnError: Boolean = true, callback: (Boolean) -> Unit) {
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
                                    if (shouldFinishOnError) {
                                        finish()
                                    }
                                }
                                BiometricPrompt.ERROR_CANCELED -> {
                                    if (shouldFinishOnError) {
                                        finish()
                                    }
                                }
                                else -> {
                                    Toast.makeText(
                                        this@BaseActivity, resources.getString(
                                            R.string.authentication_error_s,
                                            errString
                                        ), Toast.LENGTH_LONG
                                    ).show()
                                    if (shouldFinishOnError) {
                                        finish()
                                    }
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

    /**
     * bottomViewToShiftUp should be the last view in a NSV or CL to add a margin bottom to
     */

    private var paddingHasBeenSet = false

    fun drawBehindNavBar(
        root: View? = null,
        topViewsToShiftDownUsingMargin: ArrayList<View>? = null,
        bottomViewsToShiftUpUsingPadding: ArrayList<View>? = null,
        bottomViewsToShiftUpUsingMargin: ArrayList<View>? = null
    ) {

        if (!paddingHasBeenSet) {
            WindowCompat.setDecorFitsSystemWindows(window, false)

            root?.let { rootView ->
                ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, windowInsets ->
                    if (!paddingHasBeenSet) {
                        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

                        if (topViewsToShiftDownUsingMargin != null) {
                            for (view in topViewsToShiftDownUsingMargin) {
                                val params = view.layoutParams as ViewGroup.MarginLayoutParams
                                params.topMargin = view.paddingTop + insets.top
                                view.layoutParams = params
                            }
                        }

                        if (bottomViewsToShiftUpUsingMargin != null) {
                            for (view in bottomViewsToShiftUpUsingMargin) {
                                val params = view.layoutParams as ViewGroup.MarginLayoutParams
                                params.bottomMargin = view.paddingBottom + insets.bottom
                                view.layoutParams = params
                            }
                        }

                        if (bottomViewsToShiftUpUsingPadding != null) {
                            for (view in bottomViewsToShiftUpUsingPadding) {
                                view.paddingBottom.plus(insets.bottom)
                                    .let { view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, it) }
                            }
                        }

                        paddingHasBeenSet = true
                    }

                    // Return CONSUMED if you don't want want the window insets to keep being
                    // passed down to descendant views.
                    WindowInsetsCompat.CONSUMED
                }
            }
        }


    }

}
