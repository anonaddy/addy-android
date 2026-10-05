package host.stjin.anonaddy.ui.aliases

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.network.NetworkResult

class ManageAliasViewModel(application: Application) : BaseViewModel(application) {

    var alias by mutableStateOf<Aliases?>(null)
        private set

    var isAliasActive by mutableStateOf(false)

    var isAliasPinned by mutableStateOf(false)

    var isChangingActivationStatus by mutableStateOf(false)

    var isChangingPinnedStatus by mutableStateOf(false)

    private val aliasRepository = ServiceLocator.aliasRepository

    fun setInitialAlias(initialAlias: Aliases) {
        alias = initialAlias
        isAliasActive = initialAlias.active
        isAliasPinned = initialAlias.pinned
    }

    suspend fun getSpecificAlias(id: String): NetworkResult<Aliases> {
        val result = aliasRepository.getSpecificAlias(id)
        if (result is NetworkResult.Success) {
            setInitialAlias(result.data)
        }
        return result
    }

    private fun updateLocalCache(aliasId: String, active: Boolean? = null, pinned: Boolean? = null) {
        val context = getApplication<Application>()
        val cached = host.stjin.anonaddy_shared.utils.CacheHelper.getBackgroundServiceCacheLastUpdatedAliasesData(context)
        if (cached != null) {
            val updatedList = ArrayList(cached.map { item ->
                if (item.id == aliasId) {
                    item.copy(
                        active = active ?: item.active,
                        pinned = pinned ?: item.pinned
                    )
                } else {
                    item
                }
            })
            val data = ServiceLocator.aliasRepository.gson.toJson(updatedList)
            ServiceLocator.encryptedSettingsManager.putSettingsString(
                host.stjin.anonaddy_shared.managers.SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_LAST_UPDATED_ALIASES_DATA,
                data
            )
        }
    }

    suspend fun activateAlias(): Pair<Boolean, String?> {
        val currentAlias = alias ?: return Pair(false, null)
        val result = aliasRepository.activateSpecificAlias(currentAlias.id)
        isChangingActivationStatus = false
        val success = if (result is NetworkResult.Success) {
            isAliasActive = true
            alias = currentAlias.copy(active = true)
            updateLocalCache(currentAlias.id, active = true)
            true
        } else {
            false
        }
        BackgroundWorkerHelper(getApplication()).scheduleBackgroundWorker()
        val errorMsg = if (!success) {
            getApplication<Application>().resources.getString(R.string.error_edit_active) + "\n" + (result.errorOrNull() ?: "")
        } else null
        return Pair(success, errorMsg)
    }

    suspend fun deactivateAlias(): Pair<Boolean, String?> {
        val currentAlias = alias ?: return Pair(false, null)
        val result = aliasRepository.deactivateSpecificAlias(currentAlias.id)
        isChangingActivationStatus = false
        val success = if (result is NetworkResult.Success && result.data == "204") {
            isAliasActive = false
            alias = currentAlias.copy(active = false)
            updateLocalCache(currentAlias.id, active = false)
            true
        } else {
            false
        }
        BackgroundWorkerHelper(getApplication()).scheduleBackgroundWorker()
        val errorMsg = if (!success) {
            getApplication<Application>().resources.getString(R.string.error_edit_active) + "\n" + (result.errorOrNull() ?: "")
        } else null
        return Pair(success, errorMsg)
    }

    suspend fun pinAlias(): Pair<Boolean, String?> {
        val currentAlias = alias ?: return Pair(false, null)
        val result = aliasRepository.pinSpecificAlias(currentAlias.id)
        isChangingPinnedStatus = false
        val success = if (result is NetworkResult.Success) {
            isAliasPinned = true
            alias = currentAlias.copy(pinned = true)
            updateLocalCache(currentAlias.id, pinned = true)
            true
        } else {
            false
        }
        BackgroundWorkerHelper(getApplication()).scheduleBackgroundWorker()
        val errorMsg = if (!success) {
            getApplication<Application>().resources.getString(R.string.error_edit_pinned) + "\n" + (result.errorOrNull() ?: "")
        } else null
        return Pair(success, errorMsg)
    }

    suspend fun unpinAlias(): Pair<Boolean, String?> {
        val currentAlias = alias ?: return Pair(false, null)
        val result = aliasRepository.unpinSpecificAlias(currentAlias.id)
        isChangingPinnedStatus = false
        val success = if (result is NetworkResult.Success && result.data == "204") {
            isAliasPinned = false
            alias = currentAlias.copy(pinned = false)
            updateLocalCache(currentAlias.id, pinned = false)
            true
        } else {
            false
        }
        BackgroundWorkerHelper(getApplication()).scheduleBackgroundWorker()
        val errorMsg = if (!success) {
            getApplication<Application>().resources.getString(R.string.error_edit_pinned) + "\n" + (result.errorOrNull() ?: "")
        } else null
        return Pair(success, errorMsg)
    }
}
