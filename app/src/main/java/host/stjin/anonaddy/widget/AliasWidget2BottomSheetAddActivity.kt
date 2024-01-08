package host.stjin.anonaddy.widget

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.ui.alias.AddAliasBottomDialogFragment
import kotlinx.coroutines.launch

class AliasWidget2BottomSheetAddActivity : BaseActivity(), AddAliasBottomDialogFragment.AddAliasBottomDialogListener {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * This activity can be called by an URI or Widget/Notification Intent.
         * Protect this part
         */

        // FIXME Adding a 100 ms delay, otherwise the biometric prompt return error 10 cancelled when being called from widget.
        Handler(Looper.getMainLooper()).postDelayed({
            // Unauthenticated, clear settings
            lifecycleScope.launch {
                isAuthenticated { isAuthenticated ->
                    if (isAuthenticated) {
                        val addAliasBottomDialogFragment: AddAliasBottomDialogFragment =
                            AddAliasBottomDialogFragment.newInstance()
                        // Main fragment (the one with the text and loading indicator)
                        if (!addAliasBottomDialogFragment.isAdded) {
                            addAliasBottomDialogFragment.show(
                                supportFragmentManager,
                                "addAliasBottomDialogFragment"
                            )
                        }
                    }
                }
            }
        }, 100)


    }

    override fun onAdded() {
        finish()
    }

    override fun onCancel() {
        finish()
    }
}