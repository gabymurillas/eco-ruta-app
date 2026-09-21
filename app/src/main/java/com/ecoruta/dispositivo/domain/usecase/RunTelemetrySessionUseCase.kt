package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.repository.ParametersRepository
import com.ecoruta.dispositivo.domain.repository.TripRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Everything the device does while it is paired, for as long as the service lives.
 *
 * The heartbeat runs continuously, trip or not: the admin panel flags a unit as down when it goes
 * quiet, and an idle unit must not look dead. Position capture only runs while a trip is open.
 */
class RunTelemetrySessionUseCase(
    private val captureTelemetry: CaptureTelemetryUseCase,
    private val sendHeartbeat: SendHeartbeatUseCase,
    private val trips: TripRepository,
    private val parameters: ParametersRepository,
) {
    suspend operator fun invoke(): Unit = coroutineScope {
        // The very first heartbeat must not wait: a paired unit that never reports shows as down.
        launch { runHeartbeat() }
        launch {
            // Settle on the server's calibration before the GPS is requested at its emission interval.
            parameters.refresh()
            captureWhileTripIsOpen()
        }
    }

    private suspend fun runHeartbeat() {
        while (true) { // ends when the service cancels the session; delay() is cancellable
            sendHeartbeat() // best effort: a missed beat is simply replaced by the next one
            delay(parameters.current.stoppedInterval)
        }
    }

    private suspend fun captureWhileTripIsOpen() {
        trips.activeTripId
            .map { it != null }
            .distinctUntilChanged()
            .collectLatest { tripIsOpen -> if (tripIsOpen) captureTelemetry() }
    }
}
