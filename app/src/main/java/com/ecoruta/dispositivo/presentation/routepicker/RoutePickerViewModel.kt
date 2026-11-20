package com.ecoruta.dispositivo.presentation.routepicker

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.DeviceState
import com.ecoruta.dispositivo.domain.model.RouteSign
import com.ecoruta.dispositivo.domain.usecase.ChangeRouteSignUseCase
import com.ecoruta.dispositivo.domain.usecase.ObserveDeviceStateUseCase
import com.ecoruta.dispositivo.domain.usecase.ObserveReadinessUseCase
import com.ecoruta.dispositivo.domain.usecase.ObserveRouteSignUseCase
import com.ecoruta.dispositivo.domain.usecase.RefreshRouteSignsUseCase
import com.ecoruta.dispositivo.presentation.common.toMessageRes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the route picker shows. A null id is the unit's usual route. */
data class RoutePickerUiState(
    val normalRouteName: String = "",
    val options: List<RouteSign> = emptyList(),
    /** What the web shows now. */
    val currentId: String? = null,
    /** What the driver has ticked, not yet confirmed. */
    val selectedId: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isOnline: Boolean = true,
    /** The list comes from the cache because the backend could not be reached. */
    val isStale: Boolean = false,
    @StringRes val errorRes: Int? = null,
) {
    /** Only a real change, and only with network: a change is never queued. */
    val canConfirm: Boolean get() = isOnline && !isLoading && !isSaving && selectedId != currentId
}

sealed interface RoutePickerEvent {
    /** The backend accepted the change: the screen closes. */
    data object Changed : RoutePickerEvent
}

class RoutePickerViewModel(
    private val observeDeviceState: ObserveDeviceStateUseCase,
    observeReadiness: ObserveReadinessUseCase,
    private val observeRouteSign: ObserveRouteSignUseCase,
    private val refreshSigns: RefreshRouteSignsUseCase,
    private val changeSign: ChangeRouteSignUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutePickerUiState())
    val uiState: StateFlow<RoutePickerUiState> = _uiState.asStateFlow()

    private val _events = Channel<RoutePickerEvent>(Channel.BUFFERED)
    val events: Flow<RoutePickerEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            // Cache first, so the list shows at once and with no signal; then the fresh one.
            val current = observeRouteSign().first()?.id
            val cached = refreshSigns.cached()
            _uiState.update {
                it.copy(
                    normalRouteName = normalRouteName(),
                    options = cached?.options.orEmpty(),
                    currentId = current,
                    selectedId = current,
                    isLoading = cached == null,
                )
            }
            reload()
        }
        viewModelScope.launch {
            observeReadiness().collect { readiness -> _uiState.update { it.copy(isOnline = readiness.isOnline) } }
        }
    }

    fun onSelect(signId: String?) {
        _uiState.update { it.copy(selectedId = signId, errorRes = null) }
    }

    fun onConfirm() {
        val state = _uiState.value
        if (!state.canConfirm) return
        _uiState.update { it.copy(isSaving = true, errorRes = null) }
        viewModelScope.launch {
            when (val result = changeSign(state.selectedId)) {
                is Outcome.Success -> _events.send(RoutePickerEvent.Changed)
                is Outcome.Failure -> {
                    // Withdrawn by the admin: the use case refreshed the cache, show the new list.
                    val fresh = refreshSigns.cached()
                    _uiState.update { ui ->
                        ui.copy(
                            isSaving = false,
                            errorRes = result.error.toMessageRes(),
                            options = fresh?.options ?: ui.options,
                            selectedId = ui.selectedId.takeIf { id -> id == null || fresh?.options?.any { it.id == id } != false },
                        )
                    }
                }
            }
        }
    }

    private suspend fun reload() {
        val result = refreshSigns()
        val current = observeRouteSign().first()?.id
        _uiState.update { ui ->
            when (result) {
                is Outcome.Success -> ui.copy(
                    options = result.value.options,
                    currentId = current,
                    // Keep what the driver ticked unless it no longer exists.
                    selectedId = ui.selectedId.takeIf { id -> id == null || result.value.options.any { it.id == id } } ?: current,
                    isLoading = false,
                    isStale = false,
                )
                is Outcome.Failure -> ui.copy(isLoading = false, isStale = true)
            }
        }
    }

    private suspend fun normalRouteName(): String = when (val state = observeDeviceState().first()) {
        is DeviceState.Idle -> state.session.routeName
        is DeviceState.OnTrip -> state.session.routeName
        is DeviceState.Unpaired -> ""
    }
}
