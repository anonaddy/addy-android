package host.stjin.anonaddy.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.setup.SetupActivity
import host.stjin.anonaddy.widget.AliasWidget2BottomSheetAddActivity
import host.stjin.anonaddy_shared.managers.SettingsManager

class AddAliasTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val apiKey = ServiceLocator.encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.API_KEY)
        if (apiKey.isNullOrEmpty()) {
            tile.state = Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.not_configured)
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = null
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val apiKey = ServiceLocator.encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.API_KEY)
        if (apiKey.isNullOrEmpty()) {
            val intent = Intent(this, SetupActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            launchActivityAndCollapse(intent)
            return
        }

        val intent = Intent(this, AliasWidget2BottomSheetAddActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (isLocked) {
            unlockAndRun {
                launchActivityAndCollapse(intent)
            }
        } else {
            launchActivityAndCollapse(intent)
        }
    }

    private fun launchActivityAndCollapse(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
