package host.stjin.anonaddy

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
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
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.appbar.AppBarLayout
import host.stjin.anonaddy.databinding.CustomToolbarOneHandedBinding
import host.stjin.anonaddy.ui.customviews.refreshlayout.RefreshLayout
import host.stjin.anonaddy_shared.managers.SettingsManager


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


    // This logic is for the refreshlayout, when the home, alias or recipient fragment is scrolled they will fire the setHasReachedTopOfNsv() method
    // in their respective classes. That method will set this value, the setter then checks if the appbar is expanded and will set that result in the
    // RefreshLayout. If the value is true it means that the top of the shown fragment is reached as well as the appbar expanded. Continuing to scroll
    // up will then trigger a refresh action. Else it won't do anything
    var hasReachedTopOfNsv: Boolean = true
        set(value) {
            field = value

            if (this.refreshLayout != null) {
                // hasReachedTopOfNsv, set shouldShowRefreshLayoutOnScroll
                this.refreshLayout!!.shouldShowRefreshLayoutOnScroll = value && appBarIsExpanded
            }
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

    fun changeTopBarSubTitle(subtitle: TextView, title: TextView, smallTitle: TextView, text: String?) {

        // Prevent lagging animation by not setting text multiple times
        if (subtitle.text == text || subtitle.text.isNullOrEmpty() && text == null) {
            return
        }

        if (text == null) {
            ObjectAnimator.ofFloat(title, "translationY", 0f).apply {
                duration = 300
                start()
            }
            ObjectAnimator.ofFloat(smallTitle, "translationY", 0f).apply {
                duration = 300
                start()
            }

            ObjectAnimator.ofFloat(subtitle, "alpha", 0f).apply {
                duration = 300
                start()
            }
        } else {
            ObjectAnimator.ofFloat(title, "translationY", -12f).apply {
                duration = 300
                start()
            }
            ObjectAnimator.ofFloat(smallTitle, "translationY", -8f).apply {
                duration = 300
                start()
            }
            ObjectAnimator.ofFloat(subtitle, "alpha", 0.7f).apply {
                duration = 300
                start()
            }
        }

        subtitle.text = text
    }

    fun shimmerTopBarSubTitle(shimmerFrameLayout: ShimmerFrameLayout, shimmer: Boolean) {
        if (shimmer) {
            shimmerFrameLayout.startShimmer()
        } else {
            shimmerFrameLayout.stopShimmer()
        }
    }

    fun setupToolbar(
        title: Int,
        nestedScrollView: NestedScrollView?,
        customToolbarOneHandedBinding: CustomToolbarOneHandedBinding? = null,
        image: Int? = null,
        customBackPressedMethod: (() -> Unit)? = null
    ) {
        customToolbarOneHandedBinding?.customToolbarOneHandedMaterialtoolbar?.setNavigationIcon(R.drawable.ic_arrow_back) // need to set the icon here to have a navigation icon. You can simple create an vector image by "Vector Asset" and using here
        customToolbarOneHandedBinding?.customToolbarOneHandedMaterialtoolbar?.setNavigationOnClickListener {
            if (customBackPressedMethod != null) {
                customBackPressedMethod.invoke()
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        customToolbarOneHandedBinding?.customToolbarOneHandedMaterialtoolbar?.title = this.resources.getString(title)

        if (customToolbarOneHandedBinding?.customToolbarOneHandedImage != null && image != null) {
            customToolbarOneHandedBinding.customToolbarOneHandedImage.setImageDrawable(ContextCompat.getDrawable(this, image))
        }

        customToolbarOneHandedBinding?.customToolbarOneHandedMaterialtoolbar?.setOnClickListener {
            val intent = Intent("scroll_up")
            sendBroadcast(intent)
        }



        this.nestedScrollView = nestedScrollView
        this.appBarLayout = customToolbarOneHandedBinding?.customToolbarAppbar
    }

    fun toolbarSetAction(customToolbarOneHandedBinding: CustomToolbarOneHandedBinding, icon: Int, onClickListener: View.OnClickListener?) {
        customToolbarOneHandedBinding.customToolbarOneHandedActionButton.setImageDrawable(ContextCompat.getDrawable(this, icon))

        if (onClickListener != null) {
            customToolbarOneHandedBinding.customToolbarOneHandedActionButton.animate()?.alpha(1.0f)
        } else {
            customToolbarOneHandedBinding.customToolbarOneHandedActionButton.animate()?.alpha(0.0f)
        }

        customToolbarOneHandedBinding.customToolbarOneHandedActionButton.setOnClickListener(onClickListener)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mScrollUpBroadcastReceiver, IntentFilter("scroll_up"), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(mScrollUpBroadcastReceiver, IntentFilter("scroll_up"))
        }
    }


    /*
    This method is getting called in multiple places to check if the user is Authenticated to use the app.
    It only gived a callback when the user is authenticated
     */
    fun isAuthenticated(shouldFinishOnError: Boolean = true, callback: (Boolean) -> Unit) {
        val encryptedSettingsManager = SettingsManager(true, this)
        if (encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.BIOMETRIC_ENABLED)) {
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

    /**
     * bottomViewToShiftUp should be the last view in a NSV or CL to add a margin bottom to
     */

    private var paddingHasBeenSet = false

    fun drawBehindNavBar(
        root: View? = null,
        topViewsToShiftDownUsingMargin: ArrayList<View>? = null,
        topViewsToShiftDownUsingPadding: ArrayList<View>? = null,
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

                        if (topViewsToShiftDownUsingPadding != null) {
                            for (view in topViewsToShiftDownUsingPadding) {
                                view.paddingBottom.plus(insets.top)
                                    .let { view.setPadding(view.paddingLeft, it, view.paddingRight, view.paddingBottom) }
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
