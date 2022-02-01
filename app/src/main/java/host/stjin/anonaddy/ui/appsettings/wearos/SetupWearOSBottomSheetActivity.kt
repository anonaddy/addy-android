package host.stjin.anonaddy.ui.appsettings.wearos

import android.os.Bundle
import host.stjin.anonaddy.BaseActivity

class SetupWearOSBottomSheetActivity : BaseActivity(), SetupWearOSBottomDialogFragment.AddSetupWearOSBottomDialogListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val nodeId = intent.getStringExtra("nodeId")
        val nodeDisplayName = intent.getStringExtra("nodeDisplayName")
        val setupWearOSBottomDialogFragment: SetupWearOSBottomDialogFragment =
            SetupWearOSBottomDialogFragment.newInstance(this, nodeId,nodeDisplayName)

        if (!setupWearOSBottomDialogFragment.isAdded) {
            setupWearOSBottomDialogFragment.show(
                supportFragmentManager,
                "setupWearOSBottomDialogFragment"
            )
        }
    }

    override fun onDismissed() {
        finish()
    }
}