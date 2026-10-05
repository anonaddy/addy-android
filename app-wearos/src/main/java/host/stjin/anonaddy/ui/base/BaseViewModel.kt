package host.stjin.anonaddy.ui.base

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import host.stjin.anonaddy_shared.models.UserResource
import host.stjin.anonaddy_shared.utils.CacheHelper

abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {
    var userResource by mutableStateOf<UserResource?>(null)
        protected set

    init {
        refreshUserResourceFromCache()
    }

    fun refreshUserResourceFromCache() {
        userResource = CacheHelper.getBackgroundServiceCacheUserResource(getApplication())
    }
}
