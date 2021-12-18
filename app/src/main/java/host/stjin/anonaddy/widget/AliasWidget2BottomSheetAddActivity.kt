package host.stjin.anonaddy.widget

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import host.stjin.anonaddy.BaseActivity
import host.stjin.anonaddy.ui.alias.AddAliasBottomDialogFragment
import kotlinx.coroutines.launch

class AliasWidget2BottomSheetAddActivity : BaseActivity(), AddAliasBottomDialogFragment.AddAliasBottomDialogListener {

    private val addAliasBottomDialogFragment: AddAliasBottomDialogFragment =
        AddAliasBottomDialogFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * This activity can be called by an URI or Widget/Notification Intent.
         * Protect this part
         */
        lifecycleScope.launch {
            isAuthenticated { isAuthenticated ->
                if (isAuthenticated) {
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
    }

    override fun onAdded() {
        finish()
    }

    override fun onCancel() {
        finish()
    }
}