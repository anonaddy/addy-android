package host.stjin.anonaddy.ui.aliases

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import host.stjin.anonaddy.R
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.service.BackgroundWorkerHelper
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.Aliases
import host.stjin.anonaddy_shared.network.NetworkResult
import host.stjin.anonaddy_shared.utils.CacheHelper
import kotlinx.coroutines.launch

class CreateAliasViewModel(application: Application) : BaseViewModel(application) {

    var alias by mutableStateOf<Aliases?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val aliasRepository = ServiceLocator.aliasRepository
    private val userRepository = ServiceLocator.userRepository

    fun createAlias() {
        viewModelScope.launch {
            var userResource = CacheHelper.getBackgroundServiceCacheUserResource(getApplication())
            if (userResource?.default_alias_domain.isNullOrEmpty()) {
                val userResult = userRepository.cacheUserResourceForWidget()
                if (userResult is NetworkResult.Success) {
                    userResource = CacheHelper.getBackgroundServiceCacheUserResource(getApplication())
                }
            }

            val domain = userResource?.default_alias_domain
            if (domain.isNullOrEmpty()) {
                errorMessage = getApplication<Application>().resources.getString(R.string.error_adding_alias)
                return@launch
            }

            val format = if (userResource.default_alias_format == "custom") "random_characters" else userResource.default_alias_format
            val result = aliasRepository.addAlias(
                domain = domain,
                description = getApplication<Application>().resources.getString(R.string.created_on_wearos),
                format = format,
                aliasLocalPart = "",
                recipients = null,
                labels = null
            )
            when (result) {
                is NetworkResult.Success<Aliases> -> {
                    alias = result.data
                    val context = getApplication<Application>()
                    val cached = CacheHelper.getBackgroundServiceCacheLastUpdatedAliasesData(context) ?: arrayListOf()
                    cached.add(0, result.data)
                    val data = ServiceLocator.aliasRepository.gson.toJson(cached)
                    ServiceLocator.encryptedSettingsManager.putSettingsString(
                        host.stjin.anonaddy_shared.managers.SettingsManager.PREFS.BACKGROUND_SERVICE_CACHE_LAST_UPDATED_ALIASES_DATA,
                        data
                    )
                    BackgroundWorkerHelper(getApplication()).scheduleBackgroundWorker()
                }
                is NetworkResult.Error -> {
                    errorMessage = getApplication<Application>().resources.getString(R.string.error_adding_alias) + "\n" + result.error
                }
            }
        }
    }
}
