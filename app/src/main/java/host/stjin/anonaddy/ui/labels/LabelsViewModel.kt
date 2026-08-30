package host.stjin.anonaddy.ui.labels

import android.app.Application
import androidx.lifecycle.viewModelScope
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.ui.base.BaseViewModel
import host.stjin.anonaddy_shared.models.UiState
import host.stjin.anonaddy_shared.models.Labels
import host.stjin.anonaddy_shared.network.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LabelsViewModel(application: Application) : BaseViewModel(application) {

    private val labelRepository = ServiceLocator.labelRepository

    private val _labelsState = MutableStateFlow<UiState<List<Labels>>>(UiState.Loading)
    val labelsState: StateFlow<UiState<List<Labels>>> = _labelsState.asStateFlow()

    fun loadLabels(search: String? = null, forceRefresh: Boolean = false): Job {
        if (!forceRefresh && _labelsState.value is UiState.Success && search == null) {
            return Job().apply { complete() }
        }

        return viewModelScope.launch {
            _labelsState.value = UiState.Loading
            when (val result = labelRepository.getAllLabels(search = search)) {
                is NetworkResult.Success -> {
                    _labelsState.value = UiState.Success(result.data.data)
                }
                is NetworkResult.Error -> {
                    _labelsState.value = UiState.Error(result.error, result.statusCode)
                }
            }
        }
    }

    suspend fun deleteLabel(labelId: String): NetworkResult<String> {
        return labelRepository.deleteLabel(labelId)
    }

    suspend fun addNewLabel(newLabelEntry: host.stjin.anonaddy_shared.models.NewLabelEntry): NetworkResult<Labels> {
        return labelRepository.addNewLabel(newLabelEntry)
    }

    suspend fun updateLabel(labelId: String, newLabelEntry: host.stjin.anonaddy_shared.models.NewLabelEntry): NetworkResult<Labels> {
        return labelRepository.updateLabel(labelId, newLabelEntry)
    }

    suspend fun getAllLabels() = labelRepository.getAllLabels()
}
