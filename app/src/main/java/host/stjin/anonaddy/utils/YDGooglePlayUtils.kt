package host.stjin.anonaddy.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils


object YDGooglePlayUtils {
    private var GOOGLE_PLAY = "com.android.vending"
    private var FDROID = "org.fdroid.fdroid"
    fun isInstalledViaGooglePlay(ctx: Context): Boolean {
        return isInstalledVia(ctx, GOOGLE_PLAY)
    }

    fun isInstalledViaFDroid(ctx: Context): Boolean {
        return isInstalledVia(ctx, FDROID)
    }

    fun isSideloaded(ctx: Context): Boolean {
        val installer = getInstallerPackageName(ctx)
        return TextUtils.isEmpty(installer)
    }

    private fun isInstalledVia(ctx: Context, required: String): Boolean {
        val installer = getInstallerPackageName(ctx)
        return required == installer
    }

    fun getInstallerPackageName(ctx: Context): String? {
        try {
            val packageName = ctx.packageName
            val pm = ctx.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(packageName)
                return info.installingPackageName
            }
            @Suppress("DEPRECATION")
            return pm.getInstallerPackageName(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
        }
        return ""
    }

    fun getInstallerApplicationName(ctx: Context, packageName: String): String {
        val packageManager: PackageManager = ctx.packageManager
        val applicationInfo: ApplicationInfo? = try {
            packageManager.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        return (if (applicationInfo != null) packageManager.getApplicationLabel(applicationInfo) else packageName) as String
    }
}