package com.ecoruta.dispositivo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.dispositivo.domain.model.DeviceState
import com.ecoruta.dispositivo.domain.usecase.ObserveDeviceStateUseCase
import com.ecoruta.dispositivo.domain.usecase.ResumeTelemetryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    observeDeviceState: ObserveDeviceStateUseCase,
    private val resumeTelemetry: ResumeTelemetryUseCase,
) : ViewModel() {

    /** Null until the persisted state has been read for the first time. */
    val deviceState: StateFlow<DeviceState?> = observeDeviceState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initialValue = null)

    init {
        // After a reboot or process death mid-trip, capture must come back without anyone touching it.
        viewModelScope.launch { resumeTelemetry() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
