package host.stjin.anonaddy.widget

import android.os.Bundle
import host.stjin.anonaddy.ui.base.BaseActivity
import host.stjin.anonaddy.ui.aliases.AddAliasBottomDialogFragment

class AliasWidget2BottomSheetAddActivity : BaseActivity(), AddAliasBottomDialogFragment.AddAliasBottomDialogListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireAuthentication {
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

    override fun onAdded() {
        finish()
    }

    override fun onCancel() {
        finish()
    }
}