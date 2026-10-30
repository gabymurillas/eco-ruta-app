package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.model.Position
import com.ecoruta.dispositivo.domain.policy.TransmissionPolicy
import com.ecoruta.dispositivo.domain.repository.SessionRepository
import com.ecoruta.dispositivo.domain.repository.TelemetryRepository
import com.ecoruta.dispositivo.domain.repository.TripRepository
import com.ecoruta.dispositivo.domain.system.LocationSource
import com.ecoruta.dispositivo.domain.system.TelemetryStatusStore
import kotlinx.coroutines.flow.first
import java.time.Instant

/** Turns the raw GPS stream into positions queued for delivery. Runs until cancelled. */
class CaptureTelemetryUseCase(
    private val location: LocationSource,
    private val sessions: SessionRepository,
    private val trips: TripRepository,
    private val telemetry: TelemetryRepository,
    private val statusStore: TelemetryStatusStore,
    private val policy: TransmissionPolicy,
) {
    suspend operator fun invoke() {
        val session = sessions.current() ?: return
        var lastRecordedAt: Instant? = null

        location.updates(policy.locationRequestInterval).collect { fix ->
            statusStore.onFix(fix)
            val tripId = trips.activeTripId.first() ?: return@collect
            if (!policy.shouldRecord(fix, lastRecordedAt)) return@collect

            lastRecordedAt = fix.capturedAt
            telemetry.enqueue(
                Position(
                    unitId = session.unitId,
                    tripId = tripId,
                    point = fix.point,
                    speedKmh = fix.speedKmh,
                    accuracyMeters = fix.accuracyMeters,
                    capturedAt = fix.capturedAt,
                ),
            )
        }
    }
}
