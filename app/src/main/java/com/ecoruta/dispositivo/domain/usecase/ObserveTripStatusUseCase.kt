package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.model.GpsStatus
import com.ecoruta.dispositivo.domain.model.TripStatus
import com.ecoruta.dispositivo.domain.policy.TransmissionPolicy
import com.ecoruta.dispositivo.domain.repository.TelemetryRepository
import com.ecoruta.dispositivo.domain.system.BatteryStatusSource
import com.ecoruta.dispositivo.domain.system.ConnectivityObserver
import com.ecoruta.dispositivo.domain.system.LocationSettingsObserver
import com.ecoruta.dispositivo.domain.system.TelemetryStatusStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import kotlin.time.Duration.Companion.seconds

/** Live summary of GPS, connectivity, battery and delivery state for the trip screen. */
class ObserveTripStatusUseCase(
    private val statusStore: TelemetryStatusStore,
    private val telemetry: TelemetryRepository,
    private val connectivity: ConnectivityObserver,
    private val locationSettings: LocationSettingsObserver,
    private val battery: BatteryStatusSource,
    private val policy: TransmissionPolicy,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<TripStatus> = combine(
        gpsStatus(),
        statusStore.lastSentAt,
        connectivity.isOnline,
        battery.observe(),
        telemetry.pendingCount,
    ) { gps, lastSentAt, online, batteryStatus, pending ->
        TripStatus(
            gps = gps,
            isOnline = online,
            battery = batteryStatus,
            lastSentAt = lastSentAt,
            pendingPositions = pending,
        )
    }.combine(locationSettings.isLocationEnabled) { status, enabled -> status.copy(isLocationEnabled = enabled) }

    /** Re-evaluated on every tick so a fix going stale is noticed even when nothing else changes. */
    private fun gpsStatus(): Flow<GpsStatus> =
        combine(statusStore.lastFix, ticker()) { fix, _ -> policy.gpsStatusOf(fix, clock.instant()) }
            .distinctUntilChanged()

    private fun ticker(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(TICK)
        }
    }

    private companion object {
        val TICK = 5.seconds
    }
}
