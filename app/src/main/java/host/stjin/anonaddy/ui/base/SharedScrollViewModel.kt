package host.stjin.anonaddy.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SharedScrollViewModel : ViewModel() {
    private val _scrollEvents = MutableSharedFlow<Int?>(extraBufferCapacity = 1)
    val scrollEvents = _scrollEvents.asSharedFlow()

    fun triggerScrollUp(tabId: Int? = null) {
        viewModelScope.launch {
            _scrollEvents.emit(tabId)
        }
    }
}
