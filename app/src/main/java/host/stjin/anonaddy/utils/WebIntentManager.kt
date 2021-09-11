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
import host.stjin.anonaddy.AnonAddy
import host.stjin.anonaddy.BuildConfig
import host.stjin.anonaddy.R


class WebIntentManager(private val context: Context) {
    fun requestSupportedLinks(enable: Boolean) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val intent = Intent(
                    Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
                if (enable) {
                    Toast.makeText(context, context.resources.getString(R.string.webintent_manage_default_apps_enable), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, context.resources.getString(R.string.webintent_manage_default_apps_disable), Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                intent.data = uri
                context.startActivity(intent)
                if (enable) {
                    Toast.makeText(context, context.resources.getString(R.string.webintent_manage_default_apps_pre_sdk24_enable), Toast.LENGTH_LONG)
                        .show()
                } else {
                    Toast.makeText(context, context.resources.getString(R.string.webintent_manage_default_apps_pre_sdk24_disable), Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }


    //If new URLS are added here, also add them to the manifest
    private fun isOurAppDefault(context: Context): Boolean {
        // Only /deactivate for now
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(AnonAddy.API_BASE_URL + "/deactivate"))
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
            val unapprovedDomains = userState?.hostToStateMap
                ?.filterValues { it == DomainVerificationUserState.DOMAIN_STATE_NONE }

            var baseUrl = AnonAddy.API_BASE_URL
            // Remove http,https prefix
            baseUrl = baseUrl.replace("https://", "").replace("http://", "")

            verifiedDomains?.contains(baseUrl) ?: false || selectedDomains?.contains(baseUrl) ?: false
        } else {
            isOurAppDefault(context)
        }
    }

    // Check if the current baseURL can be associated with the app (This class is A12 only, so we just check if the baseURL is the app.anonaddy.com domain)
    fun canBaseURLBeAssociated(): Boolean {
        var baseUrl = AnonAddy.API_BASE_URL
        // Remove http,https prefix
        baseUrl = baseUrl.replace("https://", "").replace("http://", "")

        return baseUrl == "app.anonaddy.com"
    }
}