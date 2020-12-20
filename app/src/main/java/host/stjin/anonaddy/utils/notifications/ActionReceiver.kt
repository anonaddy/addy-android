package host.stjin.anonaddy.utils.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import host.stjin.anonaddy.service.AliasWatcher
import host.stjin.anonaddy.ui.alias.manage.ManageAliasActivity


class ActionReceiver : BroadcastReceiver() {

    object NOTIFICATION_ACTIONS {
        const val STOP_WATCHING = "stop_watching"
        const val EDIT_ALIAS = "edit_alias"
    }

    override fun onReceive(context: Context, intent: Intent) {
        //Toast.makeText(context,"received",Toast.LENGTH_SHORT).show();
        val action = intent.action
        val extra = intent.getStringExtra("extra")

        if (action == NOTIFICATION_ACTIONS.STOP_WATCHING) {
            extra?.let { AliasWatcher(context).removeAliasToWatch(it) }
        } else if (action == NOTIFICATION_ACTIONS.EDIT_ALIAS) {
            val manageAliasIntent = Intent(context, ManageAliasActivity::class.java)
            manageAliasIntent.putExtra("alias_id", extra)
            manageAliasIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ContextCompat.startActivity(context, manageAliasIntent, null)
        }
        //This is used to close the notification tray
        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        context.sendBroadcast(it)
    }

}