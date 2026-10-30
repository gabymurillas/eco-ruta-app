package com.ecoruta.dispositivo.presentation.trip

import com.ecoruta.dispositivo.domain.model.GpsStatus
import com.ecoruta.dispositivo.domain.model.TripStatus

/**
 * The single line at the top of the trip screen: the worst thing currently true, so a glance tells
 * the driver whether the unit is being seen. Ordered from worst to best.
 */
enum class TripHeadline(val severity: Severity) {
    /** Location is switched off in settings: nothing at all is captured. */
    LOCATION_OFF(Severity.ERROR),

    /** No fix, or the last one is older than the signal-loss threshold. */
    SEARCHING_GPS(Severity.ERROR),

    /** Positions are captured and stored, but cannot be sent yet. */
    OFFLINE(Severity.WARNING),

    /** Sending, but readings are too inaccurate to trigger stop alerts. */
    WEAK_GPS(Severity.WARNING),
    TRANSMITTING(Severity.OK),
    ;

    enum class Severity { OK, WARNING, ERROR }

    companion object {
        fun of(status: TripStatus): TripHeadline = when {
            !status.isLocationEnabled -> LOCATION_OFF
            status.gps == GpsStatus.NO_SIGNAL -> SEARCHING_GPS
            !status.isOnline -> OFFLINE
            status.gps == GpsStatus.WEAK -> WEAK_GPS
            else -> TRANSMITTING
        }
    }
}
