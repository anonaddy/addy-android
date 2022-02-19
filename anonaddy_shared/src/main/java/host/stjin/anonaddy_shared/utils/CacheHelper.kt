package host.stjin.anonaddy_shared.utils

import android.content.Context
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.UserResource

object CacheHelper {
    fun getBackgroundServiceCacheMostActiveAliasesData(context: Context): ArrayList<Aliases>? {
        val aliasesJson =
            SettingsManager(encrypt = true, context).getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_MOST_ACTIVE_ALIASES_DATA)
        return aliasesJson?.let { GsonTools.jsonToAliasObject(context, it) }
    }

    fun getBackgroundServiceCacheFavoriteAliasesData(context: Context): ArrayList<Aliases>? {
        val aliasesJson =
            SettingsManager(encrypt = true, context).getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_FAVORITE_ALIASES_DATA)
        return aliasesJson?.let { GsonTools.jsonToAliasObject(context, it) }
    }

    fun getBackgroundServiceCacheUserResource(context: Context): UserResource? {
        val userResourceJson =
            SettingsManager(encrypt = true, context).getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_USER_RESOURCE)
        return userResourceJson?.let { GsonTools.jsonToUserResourceObject(context, it) }
    }

}