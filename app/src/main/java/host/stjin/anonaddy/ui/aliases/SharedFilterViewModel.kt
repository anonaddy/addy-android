package host.stjin.anonaddy.ui.aliases

import androidx.lifecycle.ViewModel
import host.stjin.anonaddy_shared.models.AliasSortFilter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SharedFilterViewModel : ViewModel() {
    private val _filterEvents = MutableSharedFlow<AliasSortFilter>(extraBufferCapacity = 1)
    val filterEvents = _filterEvents.asSharedFlow()

    fun applyFilter(filter: AliasSortFilter) {
        _filterEvents.tryEmit(filter)
    }
}
