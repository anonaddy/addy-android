package host.stjin.anonaddy.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object ComponentUtils {
    fun getComponentState(context: Context, packageName: String?, componentClassName: String?): Boolean {
        val pm = context.applicationContext.packageManager
        val componentName = ComponentName(packageName!!, componentClassName!!)
        return pm.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ||
                pm.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    fun setComponentState(context: Context, packageName: String?, componentClassName: String?, enabled: Boolean) {
        val pm = context.applicationContext.packageManager
        val componentName = ComponentName(packageName!!, componentClassName!!)
        val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        pm.setComponentEnabledSetting(
            componentName,
            state,
            PackageManager.DONT_KILL_APP
        )
    }
}