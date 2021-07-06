package host.stjin.anonaddy.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils

object YDGooglePlayUtils {
    var GOOGLE_PLAY = "com.android.vending"
    var AMAZON = "com.amazon.venezia"
    fun isInstalledViaGooglePlay(ctx: Context): Boolean {
        return isInstalledVia(ctx, GOOGLE_PLAY)
    }

    fun isInstalledViaAmazon(ctx: Context): Boolean {
        return isInstalledVia(ctx, AMAZON)
    }

    fun isSideloaded(ctx: Context): Boolean {
        val installer = getInstallerPackageName(ctx)
        return TextUtils.isEmpty(installer)
    }

    fun isInstalledVia(ctx: Context, required: String): Boolean {
        val installer = getInstallerPackageName(ctx)
        return required == installer
    }

    private fun getInstallerPackageName(ctx: Context): String? {
        try {
            val packageName = ctx.packageName
            val pm = ctx.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(packageName)
                if (info != null) {
                    return info.installingPackageName
                }
            }
            return pm.getInstallerPackageName(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return ""
    }
}