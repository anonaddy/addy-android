package host.stjin.anonaddy_shared.utils

import android.content.Context
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.UserResource

object CacheHelper {
    private fun getEncryptedSettingsManager(context: Context): SettingsManager {
        return SettingsManager(true, context)
    }

    fun getBackgroundServiceCacheMostActiveAliasesData(context: Context): ArrayList<Aliases>? {
        val aliasesJson =
            getEncryptedSettingsManager(context).getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_MOST_ACTIVE_ALIASES_DATA)
        return aliasesJson?.let { GsonTools.jsonToAliasObject(context, it) }
    }

    fun getBackgroundServiceCacheLastUpdatedAliasesData(context: Context): ArrayList<Aliases>? {
        val aliasesJson =
            getEncryptedSettingsManager(context).getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_LAST_UPDATED_ALIASES_DATA)
        return aliasesJson?.let { GsonTools.jsonToAliasObject(context, it) }
    }

    fun getBackgroundServiceCachePinnedAliasesData(context: Context): ArrayList<Aliases>? {
        val aliasesJson =
            getEncryptedSettingsManager(context).getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_PINNED_ALIASES_DATA)
        return aliasesJson?.let { GsonTools.jsonToAliasObject(context, it) }
    }

    fun getBackgroundServiceCacheUserResource(context: Context): UserResource? {
        val userResourceJson =
            getEncryptedSettingsManager(context).getSettingsString(SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_USER_RESOURCE)
        return userResourceJson?.let { GsonTools.jsonToUserResourceObject(context, it) }
    }

}
