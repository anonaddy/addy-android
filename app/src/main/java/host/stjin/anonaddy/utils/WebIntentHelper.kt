package host.stjin.anonaddy.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.core.net.toUri
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R
import host.stjin.anonaddy_shared.AddyIo


class WebIntentHelper(private val context: Context) {
    fun requestSupportedLinks(enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(
                    Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                    "package:${context.packageName}".toUri()
                )
                context.startActivity(intent)
                val msgRes = if (enable) R.string.webintent_manage_default_apps_enable else R.string.webintent_manage_default_apps_disable
                Toast.makeText(context, context.resources.getString(msgRes), Toast.LENGTH_LONG).show()
                return
            } catch (_: Exception) {
                // Fallback to application details settings
            }
        }

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        }
        context.startActivity(intent)
        val msgRes = if (enable) R.string.webintent_manage_default_apps_pre_sdk24_enable else R.string.webintent_manage_default_apps_pre_sdk24_disable
        Toast.makeText(context, context.resources.getString(msgRes), Toast.LENGTH_LONG).show()
    }


    //If new URLS are added here, also add them to the manifest
    private fun isOurAppDefault(context: Context): Boolean {
        // Only /deactivate for now
        val browserIntent = Intent(Intent.ACTION_VIEW, (AddyIo.API_BASE_URL + "/deactivate").toUri())
        val resolveInfo = context.packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
        var defaultBrowserPkg: String? = null
        if (resolveInfo != null) {
            if (resolveInfo.activityInfo != null) {
                defaultBrowserPkg = resolveInfo.activityInfo.packageName
            }
        }
        return TextUtils.equals(context.packageName, defaultBrowserPkg)
    }

    // Check if the set baseURL is either verified or associated with the app
    fun isCurrentDomainAssociated(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(DomainVerificationManager::class.java)
            val userState = manager.getDomainVerificationUserState(context.packageName)
            val verifiedDomains = userState?.hostToStateMap
                ?.filterValues { it == DomainVerificationUserState.DOMAIN_STATE_VERIFIED }
            val selectedDomains = userState?.hostToStateMap
                ?.filterValues { it == DomainVerificationUserState.DOMAIN_STATE_SELECTED }

            var baseUrl = AddyIo.API_BASE_URL
            // Remove http,https prefix
            baseUrl = baseUrl.replace("https://", "").replace("http://", "")

            verifiedDomains?.contains(baseUrl) == true || selectedDomains?.contains(baseUrl) == true
        } else {
            isOurAppDefault(context)
        }
    }
}