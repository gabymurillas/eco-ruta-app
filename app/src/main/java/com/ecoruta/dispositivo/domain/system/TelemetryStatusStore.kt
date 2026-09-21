package com.ecoruta.dispositivo.domain.system

import com.ecoruta.dispositivo.domain.model.LocationFix
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/** In-memory, process-wide view of what the capture pipeline is currently doing. */
interface TelemetryStatusStore {
    val lastFix: StateFlow<LocationFix?>
    val lastSentAt: StateFlow<Instant?>

    fun onFix(fix: LocationFix)

    fun onSent(at: Instant)
}
