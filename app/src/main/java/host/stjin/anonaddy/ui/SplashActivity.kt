package host.stjin.anonaddy.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.R
import host.stjin.anonaddy.databinding.ActivitySplashBinding
import host.stjin.anonaddy.ui.setup.SetupActivity
import host.stjin.anonaddy.utils.MaterialDialogHelper
import host.stjin.anonaddy_shared.AddyIo
import host.stjin.anonaddy_shared.AddyIo.API_BASE_URL
import host.stjin.anonaddy_shared.AddyIoApp
import host.stjin.anonaddy_shared.NetworkHelper
import host.stjin.anonaddy_shared.controllers.LauncherIconController
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.models.UserResourceExtended
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity(), UnsupportedBottomDialogFragment.UnsupportedBottomDialogListener {


    lateinit var networkHelper: NetworkHelper

    private val unsupportedBottomDialogFragment: UnsupportedBottomDialogFragment =
        UnsupportedBottomDialogFragment.newInstance()

    // True if there is UI stuff to be done, this var is used for Android 12 devices to keep showing the splashscreen until the app is done loading
    // Pre Android 12 devices will see a progressbar
    private var loadingDone = false

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            skipAndroid12SplashScreenAnimation()
        }

        binding = ActivitySplashBinding.inflate(layoutInflater)
        val view = binding.root

        LauncherIconController(this).tryFixLauncherIconIfNeeded()

        // Set dark mode on the splashactivity to prevent Main- and later activities from restarting and repeating calls
        checkForDarkModeAndSetFlags()

        setContentView(view)

        var closeSplashScreen = false

        Handler(Looper.getMainLooper()).postDelayed({
            // Unauthenticated, clear settings
            closeSplashScreen = true
        }, 700) // 700 is the length of splash

        // Set up an OnPreDrawListener to the root view.
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    // Check if the initial data is ready.
                    return if (closeSplashScreen) {
                        // The content is ready; start drawing.
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        // The content is not ready; suspend.
                        false
                    }
                }
            }
        )

        drawBehindNavBar(
            binding.root,
            topViewsToShiftDownUsingPadding = arrayListOf(binding.activitySplashErrorLl1),
            bottomViewsToShiftUpUsingPadding = arrayListOf(binding.activitySplashErrorLl2)
        )

        playAnimation(true, R.drawable.ic_loading_logo_splash)


        // This is prone to fail when users have restored the app data from any restore app as the
        // encryption key has changed. So we catch this once in the app and that's at launch
        val settingsManager = try {
            SettingsManager(true, this)
        } catch (e: Exception) {
            null
        }

        if (settingsManager == null) {
            showErrorScreen(this.resources.getString(R.string.app_data_corrupted))
            Handler(Looper.getMainLooper()).postDelayed({
                // Clear settings
                SettingsManager(false, this).clearSettingsAndCloseApp()
            }, 15000)
            return
        }

        /**
         * MIGRATE FROM APP.ANONADDY.COM TO APP.ADDY.IO
         */
        //migrateFromAnonAddyToAddyIo() // TODO ENABLE in 4.8.1

        // This helper inits the BASE_URL var
        networkHelper = NetworkHelper(this)


        // Open setup
        if (settingsManager.getSettingsString(SettingsManager.PREFS.API_KEY) == null) {
            loadingDone = true
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
            finish()
            return
        } else {
            lifecycleScope.launch {
                loadDataAndStartApp()
            }
        }

    }

    private fun migrateFromAnonAddyToAddyIo() {

        val encryptedSettingsManager = SettingsManager(true, this)

        val baseUrl = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.BASE_URL)
        if (baseUrl == "https://app.anonaddy.com") {
            // Change baseUrl to app.addy.io
            encryptedSettingsManager.putSettingsString(SettingsManager.PREFS.BASE_URL, API_BASE_URL)
        }
    }

    fun playAnimation(playOnLoop: Boolean, animationDrawable: Int, callback: (() -> Unit)? = null) {
        val animated = this.let { AnimatedVectorDrawableCompat.create(it, animationDrawable) }
        if (playOnLoop || callback != null) {
            animated?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    if (playOnLoop) binding.activitySplashAnimatedLogo.post { animated.start() }
                    callback?.let { it() }
                }

            })
        }
        binding.activitySplashAnimatedLogo.setImageDrawable(animated)
        animated?.start()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun skipAndroid12SplashScreenAnimation() {
        // Add a callback that called when the splash screen is animating to
        // the app content.
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.remove()
        }

    }

    private suspend fun loadDataAndStartApp() {
        // The default instance at addy.io does NOT return its version
        // However, assume that the creator of addy.io keeps the main version up-to-date :P
        // So set the versioncode to 9999 so it will always pass the min version check
        if (API_BASE_URL == this.resources.getString(R.string.default_base_url)) {
            AddyIo.VERSIONMAJOR = 9999
            AddyIo.VERSIONSTRING = this.resources.getString(R.string.latest)

            lifecycleScope.launch {
                loadUserResourceIntoMemory()
            }
        } else {
            networkHelper.getAddyIoInstanceVersion { version, error ->
                if (version != null) {
                    AddyIo.VERSIONMAJOR = version.major
                    AddyIo.VERSIONMINOR = version.minor
                    AddyIo.VERSIONPATCH = version.patch
                    AddyIo.VERSIONSTRING = version.version.toString()
                    if (instanceHasTheMinimumRequiredVersion()) {
                        lifecycleScope.launch {
                            loadUserResourceIntoMemory()
                        }
                    } else {
                        loadingDone = true
                        if (!unsupportedBottomDialogFragment.isAdded) {
                            unsupportedBottomDialogFragment.show(
                                supportFragmentManager,
                                "unsupportedBottomDialogFragment"
                            )
                        }
                    }
                } else {
                    showErrorScreen(error)
                }
            }
        }
    }

    private fun instanceHasTheMinimumRequiredVersion(): Boolean {
        if (AddyIo.VERSIONMAJOR > AddyIo.MINIMUMVERSIONCODEMAJOR) {
            return true
        } else if (AddyIo.VERSIONMAJOR >= AddyIo.MINIMUMVERSIONCODEMAJOR) {
            if (AddyIo.VERSIONMINOR > AddyIo.MINIMUMVERSIONCODEMINOR) {
                return true
            } else if (AddyIo.VERSIONMINOR >= AddyIo.MINIMUMVERSIONCODEMINOR) {
                if (AddyIo.VERSIONPATCH >= AddyIo.MINIMUMVERSIONCODEPATCH) {
                    return true
                }
            }
        }
        return false
    }


    private suspend fun loadUserResourceIntoMemory() {
        networkHelper.getUserResource { user: UserResource?, error: String? ->
            if (user != null) {
                (this.application as AddyIoApp).userResource = user
                lifecycleScope.launch {
                    getDefaultRecipientAddress(user.default_recipient_id)
                }
            } else {
                showErrorScreen(error)
            }
        }
    }

    private suspend fun getDefaultRecipientAddress(recipientId: String) {
        networkHelper.getSpecificRecipient({ recipient, error ->
            if (recipient != null) {
                (this.application as AddyIoApp).userResourceExtended = UserResourceExtended(recipient.email)
                loadingDone = true
                val intent = Intent(this, MainActivity::class.java)
                // Widgets pass a target to splashActivity, so always pass a target to MainActivity (onCreate will check if there are any pending targets)
                intent.putExtra("target", this.intent.getStringExtra("target"))
                startActivity(intent)
                finish()
            } else {
                showErrorScreen(error)
            }
        }, recipientId)
    }

    private fun showErrorScreen(error: String?) {
        loadingDone = true

        playAnimation(false, R.drawable.ic_loading_logo_error_splash) {
            binding.activitySplashErrorLl1.animate().alpha(1.0f)
            binding.activitySplashErrorLl2.animate().alpha(1.0f)
        }

        binding.activitySplashErrorTryAgain.setOnClickListener {
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.activitySplashErrorResetApp.setOnClickListener {
            SettingsManager(true, this).clearSettingsAndCloseApp()
        }

        binding.activitySplashErrorMessage.setOnClickListener {
            showErrorMessage(error)
        }
    }

    private fun showErrorMessage(error: String?) {
        MaterialDialogHelper.showMaterialDialog(
            context = this,
            title = resources.getString(R.string.error_details),
            message = error ?: resources.getString(R.string.no_error_message),
            neutralButtonText = resources.getString(R.string.close),
            positiveButtonText = resources.getString(R.string.copy_to_clipboard),
            positiveButtonAction = {
                val clipboard: ClipboardManager =
                    this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("error", error)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, resources.getString(R.string.error_copied_to_clipboard), Toast.LENGTH_LONG).show()
            }
        ).show()
    }

    override fun onClickHowToUpdate() {
        unsupportedBottomDialogFragment.dismissAllowingStateLoss()
        val url = "https://github.com/anonaddy/anonaddy/blob/master/SELF-HOSTING.md#updating"
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
        finish()
    }

    override fun onClickIgnore() {
        unsupportedBottomDialogFragment.dismissAllowingStateLoss()
        lifecycleScope.launch {
            loadUserResourceIntoMemory()
        }
    }

}