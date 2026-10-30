package com.ecoruta.dispositivo.data.system

import com.ecoruta.dispositivo.domain.model.LocationFix
import com.ecoruta.dispositivo.domain.system.TelemetryStatusStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

/** Process-wide and in memory on purpose: it only feeds the screen, and a restart starts it fresh. */
class InMemoryTelemetryStatusStore : TelemetryStatusStore {

    private val _lastFix = MutableStateFlow<LocationFix?>(null)
    private val _lastSentAt = MutableStateFlow<Instant?>(null)

    override val lastFix: StateFlow<LocationFix?> = _lastFix.asStateFlow()
    override val lastSentAt: StateFlow<Instant?> = _lastSentAt.asStateFlow()

    override fun onFix(fix: LocationFix) {
        _lastFix.value = fix
    }

    override fun onSent(at: Instant) {
        _lastSentAt.value = at
    }
}
