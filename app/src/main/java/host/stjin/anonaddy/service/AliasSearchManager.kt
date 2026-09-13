package host.stjin.anonaddy.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appsearch.app.AppSearchSchema
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.GenericDocument
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.RemoveByDocumentIdRequest
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.app.SetSchemaRequest
import androidx.appsearch.platformstorage.PlatformStorage
import androidx.concurrent.futures.await
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.aliases.manage.ManageAliasActivity
import host.stjin.anonaddy_shared.managers.SettingsManager
import host.stjin.anonaddy_shared.models.AliasSortFilter
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.models.LOGIMPORTANCE
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.LoggingHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AliasSearchManager(private val context: Context) {

    companion object {
        const val DATABASE_NAME = "addy_aliases"
        const val NAMESPACE = "aliases"
        const val SCHEMA_TYPE = "AliasDocument"
    }

    private var appSearchSession: AppSearchSession? = null
    private val sessionMutex = Mutex()

    val isEnabled: Boolean
        get() {
            val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
            if (encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.PRIVACY_MODE)) {
                return false
            }
            return encryptedSettingsManager.getSettingsBool(SettingsManager.PREFS.SYSTEM_SEARCH, true)
        }

    private suspend fun getAppSearchSession(): AppSearchSession? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        if (appSearchSession != null) return appSearchSession

        return sessionMutex.withLock {
            if (appSearchSession != null) return@withLock appSearchSession

            try {
                val searchContext = PlatformStorage.SearchContext.Builder(context, DATABASE_NAME).build()
                val session = PlatformStorage.createSearchSessionAsync(searchContext).await()

                val schema = AppSearchSchema.Builder(SCHEMA_TYPE)
                    .addProperty(
                        AppSearchSchema.StringPropertyConfig.Builder("email")
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                            .build()
                    )
                    .addProperty(
                        AppSearchSchema.StringPropertyConfig.Builder("localPart")
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                            .build()
                    )
                    .addProperty(
                        AppSearchSchema.StringPropertyConfig.Builder("domain")
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                            .build()
                    )
                    .addProperty(
                        AppSearchSchema.StringPropertyConfig.Builder("description")
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                            .build()
                    )
                    .addProperty(
                        AppSearchSchema.StringPropertyConfig.Builder("recipients")
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                            .build()
                    )
                    .addProperty(
                        AppSearchSchema.StringPropertyConfig.Builder("labels")
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                            .build()
                    )
                    .addProperty(
                        AppSearchSchema.LongPropertyConfig.Builder("active")
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build()
                    )
                    .build()

                val setSchemaRequest = SetSchemaRequest.Builder()
                    .addSchemas(schema)
                    .setSchemaTypeDisplayedBySystem(SCHEMA_TYPE, true)
                    .setForceOverride(true)
                    .build()

                session.setSchemaAsync(setSchemaRequest).await()
                appSearchSession = session
                session
            } catch (e: Exception) {
                LoggingHelper(context).addLog(
                    LOGIMPORTANCE.WARNING.int,
                    "Failed to initialize AppSearch: ${e.message}",
                    "AliasSearchManager.getAppSearchSession",
                    null
                )
                null
            }
        }
    }

    suspend fun indexAliases(aliases: List<Aliases>, isIncremental: Boolean = false) = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext

        val deletedAliases = aliases.filter { it.deleted_at != null }
        val validAliases = aliases.filter { it.deleted_at == null }

        if (deletedAliases.isNotEmpty()) {
            deindexAliases(deletedAliases.map { it.id })
        }

        if (validAliases.isEmpty()) return@withContext

        val session = getAppSearchSession()
        if (session != null) {
            try {
                // Put documents in chunks of 200 to avoid hitting single request size limits
                for (chunk in validAliases.chunked(200)) {
                    val putRequest = PutDocumentsRequest.Builder()
                    for (alias in chunk) {
                        val docBuilder = GenericDocument.Builder<GenericDocument.Builder<*>>(
                            NAMESPACE,
                            alias.id,
                            SCHEMA_TYPE
                        )
                            .setScore(if (alias.active) 1 else 0)
                            .setPropertyString("email", alias.email)
                            .setPropertyString("domain", alias.domain)
                            .setPropertyString("localPart", alias.local_part)
                            .setPropertyLong("active", if (alias.active) 1L else 0L)

                        alias.description?.let {
                            if (it.isNotBlank()) docBuilder.setPropertyString("description", it)
                        }

                        val recipientEmails = alias.recipients?.mapNotNull { it.email }?.filter { it.isNotBlank() }
                        if (!recipientEmails.isNullOrEmpty()) {
                            docBuilder.setPropertyString("recipients", *recipientEmails.toTypedArray())
                        }

                        val labelNames = alias.labels?.mapNotNull { it.name }?.filter { it.isNotBlank() }
                        if (!labelNames.isNullOrEmpty()) {
                            docBuilder.setPropertyString("labels", *labelNames.toTypedArray())
                        }

                        putRequest.addGenericDocuments(docBuilder.build())
                    }
                    session.putAsync(putRequest.build()).await()
                }
            } catch (e: Exception) {
                LoggingHelper(context).addLog(
                    LOGIMPORTANCE.WARNING.int,
                    "Failed to index aliases into AppSearch: ${e.message}",
                    "AliasSearchManager.indexAliases",
                    null
                )
            }
        }

        updateDynamicShortcuts(validAliases, isIncremental)
    }

    suspend fun indexAlias(alias: Aliases) {
        indexAliases(listOf(alias), isIncremental = true)
    }

    private fun createShortcutInfo(alias: Aliases): ShortcutInfoCompat {
        val intent = Intent(context, ManageAliasActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("addy://alias/${alias.id}")
            putExtra("alias_id", alias.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val shortLabel = alias.email
        val longLabel = if (!alias.description.isNullOrBlank()) {
            "${alias.email} (${alias.description})"
        } else {
            alias.email
        }

        return ShortcutInfoCompat.Builder(context, "alias_${alias.id}")
            .setShortLabel(shortLabel)
            .setLongLabel(longLabel)
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_email_at))
            .setIntent(intent)
            .setLongLived(true)
            .setCategories(setOf("host.stjin.anonaddy.category.ALIAS"))
            .build()
    }

    private fun updateDynamicShortcuts(aliases: List<Aliases>, isIncremental: Boolean = false) {
        try {
            if (isIncremental && aliases.size == 1) {
                val shortcut = createShortcutInfo(aliases.first())
                ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
            } else {
                val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
                val topAliases = aliases.take(maxShortcuts)
                val shortcuts = topAliases.map { createShortcutInfo(it) }
                ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
            }
        } catch (e: Exception) {
            LoggingHelper(context).addLog(
                LOGIMPORTANCE.WARNING.int,
                "Failed to update dynamic shortcuts: ${e.message}",
                "AliasSearchManager.updateDynamicShortcuts",
                null
            )
        }
    }

    suspend fun deindexAliases(ids: List<String>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext

        val session = getAppSearchSession()
        if (session != null) {
            try {
                val removeRequest = RemoveByDocumentIdRequest.Builder(NAMESPACE)
                    .addIds(ids)
                    .build()
                session.removeAsync(removeRequest).await()
            } catch (e: Exception) {
                LoggingHelper(context).addLog(
                    LOGIMPORTANCE.WARNING.int,
                    "Failed to deindex aliases from AppSearch: ${e.message}",
                    "AliasSearchManager.deindexAliases",
                    null
                )
            }
        }

        try {
            ShortcutManagerCompat.removeDynamicShortcuts(context, ids.map { "alias_$it" })
        } catch (_: Exception) {}
    }

    suspend fun deindexAlias(aliasId: String) {
        deindexAliases(listOf(aliasId))
    }

    suspend fun deleteAllIndexedAliases() = withContext(Dispatchers.IO) {
        val session = getAppSearchSession()
        if (session != null) {
            try {
                val searchSpec = SearchSpec.Builder()
                    .addFilterNamespaces(NAMESPACE)
                    .build()
                session.removeAsync("", searchSpec).await()
            } catch (e: Exception) {
                LoggingHelper(context).addLog(
                    LOGIMPORTANCE.WARNING.int,
                    "Failed to delete all indexed aliases from AppSearch: ${e.message}",
                    "AliasSearchManager.deleteAllIndexedAliases",
                    null
                )
            }
        }

        try {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
        } catch (_: Exception) {}
    }

    suspend fun syncAllAliases(force: Boolean = false) = withContext(Dispatchers.IO) {
        if (!isEnabled) {
            deleteAllIndexedAliases()
            return@withContext
        }

        val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
        if (encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.API_KEY) == null) {
            return@withContext
        }

        var page = 1
        val allAliases = mutableListOf<Aliases>()
        val aliasRepository = ServiceLocator.aliasRepository

        val filter = AliasSortFilter(
            onlyActiveAliases = false,
            onlyDeletedAliases = false,
            onlyPinnedAliases = false,
            onlyInactiveAliases = false,
            onlyWatchedAliases = false,
            sort = "created_at",
            sortDesc = false,
            filter = null,
            label = null
        )

        try {
            while (true) {
                val result = aliasRepository.getAliases(
                    aliasSortFilter = filter,
                    page = page,
                    size = 100
                )

                if (result is NetworkResult.Success) {
                    val data = result.data.data
                    allAliases.addAll(data)
                    val lastPage = result.data.meta?.last_page ?: 1
                    if (page < lastPage && page < 10) {
                        page++
                    } else {
                        break
                    }
                } else {
                    break
                }
            }

            if (allAliases.isNotEmpty()) {
                indexAliases(allAliases, isIncremental = false)
                val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
                encryptedSettingsManager.putSettingsString(
                    SettingsManager.PREFS.SYSTEM_SEARCH_LAST_SYNC,
                    (System.currentTimeMillis() / 1000).toString()
                )
            }
        } catch (e: Exception) {
            LoggingHelper(context).addLog(
                LOGIMPORTANCE.WARNING.int,
                "Failed to sync aliases for system search: ${e.message}",
                "AliasSearchManager.syncAllAliases",
                null
            )
        }
    }

    suspend fun syncAllAliasesIfNeeded() = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        val encryptedSettingsManager = ServiceLocator.encryptedSettingsManager
        val lastSyncStr = encryptedSettingsManager.getSettingsString(SettingsManager.PREFS.SYSTEM_SEARCH_LAST_SYNC)
        val lastSync = lastSyncStr?.toLongOrNull() ?: 0L
        val now = System.currentTimeMillis() / 1000
        val dayInSeconds = 86400L

        if (lastSync == 0L || (now - lastSync) > dayInSeconds) {
            syncAllAliases(force = false)
        }
    }
}
