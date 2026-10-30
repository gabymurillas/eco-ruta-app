package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.repository.ParametersRepository
import com.ecoruta.dispositivo.domain.repository.TripRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Everything the device does while it is paired, for as long as the service lives.
 *
 * The heartbeat runs continuously, trip or not: the admin panel flags a unit as down when it goes
 * quiet, and an idle unit must not look dead. Position capture only runs while a trip is open.
 *
 * Neither loop may die. A failure in one (the GPS provider erroring, a permission revoked) is
 * reported and retried; it never cancels the other, so a broken GPS still leaves the unit visibly
 * alive on the panel instead of silently gone.
 */
class RunTelemetrySessionUseCase(
    private val captureTelemetry: CaptureTelemetryUseCase,
    private val sendHeartbeat: SendHeartbeatUseCase,
    private val trips: TripRepository,
    private val parameters: ParametersRepository,
    private val reportFailure: (Throwable) -> Unit = {},
) {
    suspend operator fun invoke(): Unit = coroutineScope {
        // The very first heartbeat must not wait: a paired unit that never reports shows as down.
        launch { runHeartbeat() }
        launch {
            // Settle on the server's calibration before the GPS is requested at its emission interval.
            guarded { parameters.refresh() }
            captureWhileTripIsOpen()
        }
    }

    private suspend fun runHeartbeat() {
        while (true) { // ends when the service cancels the session; delay() is cancellable
            guarded { sendHeartbeat() } // best effort: a missed beat is simply replaced by the next one
            delay(parameters.current.stoppedInterval)
        }
    }

    private suspend fun captureWhileTripIsOpen() {
        trips.activeTripId
            .map { it != null }
            .distinctUntilChanged()
            .collectLatest { tripIsOpen -> if (tripIsOpen) captureUntilTripCloses() }
    }

    /** Capture only stops when the trip closes (collectLatest cancels it); a failure restarts it. */
    private suspend fun captureUntilTripCloses() {
        while (true) {
            guarded { captureTelemetry() }
            delay(CAPTURE_RETRY_DELAY)
        }
    }

    private suspend fun guarded(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportFailure(e)
        }
    }

    private companion object {
        /** Short on purpose: every second without GPS is a gap in the unit's track. */
        val CAPTURE_RETRY_DELAY = 10.seconds
    }
}
