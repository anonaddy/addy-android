package host.stjin.anonaddy.ui.appsettings.wearos

import android.os.Bundle
import host.stjin.anonaddy.ui.base.BaseActivity

class SetupWearOSBottomSheetActivity : BaseActivity(), SetupWearOSBottomDialogFragment.AddSetupWearOSBottomDialogListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val nodeId = intent.getStringExtra("nodeId")
        val nodeDisplayName = intent.getStringExtra("nodeDisplayName")
        if (supportFragmentManager.findFragmentByTag("setupWearOSBottomDialogFragment") == null) {
            val setupWearOSBottomDialogFragment: SetupWearOSBottomDialogFragment =
                SetupWearOSBottomDialogFragment.newInstance(nodeId, nodeDisplayName)
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
