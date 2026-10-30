package com.ecoruta.dispositivo.domain.system

import com.ecoruta.dispositivo.domain.model.LocationFix
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/** GPS readings, independent of the provider (fused today; the platform one is planned as fallback). */
interface LocationSource {
    /** Cold flow of GPS readings, requested at [interval]. Cancelling the collector stops updates. */
    fun updates(interval: Duration): Flow<LocationFix>

    /** Most recent known position, or null if the provider has none. */
    suspend fun lastKnown(): LocationFix?
}
