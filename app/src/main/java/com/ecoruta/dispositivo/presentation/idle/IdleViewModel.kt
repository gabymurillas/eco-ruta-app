package com.ecoruta.dispositivo.presentation.idle

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.dispositivo.core.onFailure
import com.ecoruta.dispositivo.domain.model.DeviceState
import com.ecoruta.dispositivo.domain.usecase.ObserveDeviceStateUseCase
import com.ecoruta.dispositivo.domain.usecase.StartTripUseCase
import com.ecoruta.dispositivo.presentation.common.toMessageRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IdleUiState(
    val unitName: String = "",
    val routeName: String = "",
    val isStarting: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

class IdleViewModel(
    observeDeviceState: ObserveDeviceStateUseCase,
    private val startTrip: StartTripUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IdleUiState())
    val uiState: StateFlow<IdleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeDeviceState().filterIsInstance<DeviceState.Idle>().collect { idle ->
                _uiState.update {
                    it.copy(unitName = idle.session.unitName, routeName = idle.session.routeName)
                }
            }
        }
    }

    fun onStartTrip() {
        if (_uiState.value.isStarting) return

        _uiState.update { it.copy(isStarting = true, errorRes = null) }
        viewModelScope.launch {
            // On success the active trip is persisted and navigation moves to the trip screen.
            startTrip().onFailure { error ->
                _uiState.update { it.copy(errorRes = error.toMessageRes()) }
            }
            _uiState.update { it.copy(isStarting = false) }
        }
    }
}
