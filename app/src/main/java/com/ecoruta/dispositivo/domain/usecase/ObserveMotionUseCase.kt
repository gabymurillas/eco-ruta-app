package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.policy.MotionDetector
import com.ecoruta.dispositivo.domain.system.TelemetryStatusStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import java.time.Clock
import kotlin.time.Duration.Companion.seconds

/** True while the unit is moving, from the readings of the running trip (see [MotionDetector]). */
class ObserveMotionUseCase(
    private val statusStore: TelemetryStatusStore,
    private val detector: MotionDetector,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<Boolean> =
        // Re-evaluated every second too, so "stopped" is confirmed even if no new reading arrives.
        combine(statusStore.lastFix, ticker()) { fix, _ -> fix }
            .scan(MotionDetector.State()) { state, fix -> detector.next(state, fix, clock.instant()) }
            .map { it.isMoving }
            .distinctUntilChanged()

    private fun ticker(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(TICK)
        }
    }

    private companion object {
        val TICK = 1.seconds
    }
}
