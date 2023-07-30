package host.stjin.anonaddy_shared.controllers

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import host.stjin.anonaddy_shared.R

class LauncherIconController(private val context: Context) {

    fun tryFixLauncherIconIfNeeded() {
        for (icon in LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return
            }
        }
        setIcon(LauncherIcon.DEFAULT)
    }

    fun isEnabled(icon: LauncherIcon): Boolean {
        val i = context.packageManager.getComponentEnabledSetting(icon.getComponentName(context)!!)
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon === LauncherIcon.DEFAULT
    }

    fun setIcon(icon: LauncherIcon) {
        val pm = context.packageManager
        for (i in LauncherIcon.values()) {
            pm.setComponentEnabledSetting(
                i.getComponentName(context)!!,
                if (i === icon) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }


    }

    enum class LauncherIcon @JvmOverloads constructor(
        val key: String,
        val background: Int,
        val foreground: Int,
        val title: Int
    ) {
        DEFAULT(
            "DefaultIcon",
            R.color.ic_launcher_background,
            R.drawable.ic_launcher_foreground,
            R.string.AppIconDefault
        ),
        CLASSIC(
            "ClassicIcon",
            R.color.ic_launcher_classic_background,
            R.drawable.ic_launcher_classic_foreground,
            R.string.AppIconClassic
        ),
        GRADIENT(
            "GradientIcon",
            R.drawable.ic_launcher_gradient_background,
            R.mipmap.ic_launcher_gradient_foreground,
            R.string.AppIconGradient
        ),
        INVERSE_GRADIENT(
            "InverseGradientIcon",
            R.color.ic_launcher_inverse_gradient_background,
            R.mipmap.ic_launcher_inverse_gradient_foreground,
            R.string.AppIconInverseGradient
        );

        private var componentName: ComponentName? = null
        fun getComponentName(ctx: Context): ComponentName? {
            if (componentName == null) {
                componentName = ComponentName(ctx.packageName, "host.stjin.anonaddy.$key")
            }
            return componentName
        }
    }
}