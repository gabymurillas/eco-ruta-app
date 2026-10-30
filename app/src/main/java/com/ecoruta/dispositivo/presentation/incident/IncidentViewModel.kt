package com.ecoruta.dispositivo.presentation.incident

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.IncidentType
import com.ecoruta.dispositivo.domain.usecase.ReportIncidentUseCase
import com.ecoruta.dispositivo.presentation.common.toMessageRes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the incident screen shows. */
data class IncidentUiState(
    val selectedType: IncidentType? = null,
    val description: String = "",
    val isSending: Boolean = false,
    @StringRes val errorRes: Int? = null,
) {
    /** A type, a note, or both: the note alone goes out as [IncidentType.OTHER]. */
    val canSend: Boolean get() = (selectedType != null || description.isNotBlank()) && !isSending
}

/** One-off effects the screen reacts to exactly once. */
sealed interface IncidentEvent {
    data object Sent : IncidentEvent
}

/** Holds the incident being written and sends it. Returns to the trip only once it is accepted. */
class IncidentViewModel(private val reportIncident: ReportIncidentUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(IncidentUiState())
    val uiState: StateFlow<IncidentUiState> = _uiState.asStateFlow()

    private val _events = Channel<IncidentEvent>(Channel.BUFFERED)
    val events: Flow<IncidentEvent> = _events.receiveAsFlow()

    /** Tapping the chosen option again clears it, so a note can still go out on its own. */
    fun onTypeSelected(type: IncidentType) {
        _uiState.update { state -> state.copy(selectedType = type.takeIf { it != state.selectedType }, errorRes = null) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onSend() {
        val current = _uiState.value
        if (!current.canSend) return
        val type = current.selectedType ?: IncidentType.OTHER

        _uiState.update { it.copy(isSending = true, errorRes = null) }
        viewModelScope.launch {
            when (val result = reportIncident(type, current.description)) {
                is Outcome.Success -> _events.send(IncidentEvent.Sent)
                is Outcome.Failure -> _uiState.update { it.copy(errorRes = result.error.toMessageRes()) }
            }
            _uiState.update { it.copy(isSending = false) }
        }
    }
}
