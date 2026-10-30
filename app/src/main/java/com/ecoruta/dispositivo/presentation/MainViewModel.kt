package com.ecoruta.dispositivo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.dispositivo.domain.model.DeviceState
import com.ecoruta.dispositivo.domain.model.ThemeMode
import com.ecoruta.dispositivo.domain.usecase.ObserveDeviceStateUseCase
import com.ecoruta.dispositivo.domain.usecase.ObserveThemeModeUseCase
import com.ecoruta.dispositivo.domain.usecase.ResumeTelemetryUseCase
import com.ecoruta.dispositivo.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Root state: the device state that picks the screen, and the theme mode chosen on screen. */
class MainViewModel(
    observeDeviceState: ObserveDeviceStateUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val resumeTelemetry: ResumeTelemetryUseCase,
) : ViewModel() {

    /** Null until the persisted state has been read for the first time. */
    val deviceState: StateFlow<DeviceState?> = observeDeviceState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initialValue = null)

    val themeMode: StateFlow<ThemeMode> = observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initialValue = ThemeMode.SYSTEM)

    /**
     * Called on every launch once location is granted. After a reboot or process death mid-trip,
     * capture must come back without anyone touching it; starting the service sooner would crash.
     */
    fun onLocationPermissionGranted() {
        viewModelScope.launch { resumeTelemetry() }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
