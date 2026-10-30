package com.ecoruta.dispositivo.domain.model

/** Quality of the latest fix, as judged by the server's thresholds (see TransmissionPolicy). */
enum class GpsStatus {
    /** No fix yet, or the last one is older than `sin_senal_umbral_s`. */
    NO_SIGNAL,

    /** Accuracy worse than `exactitud_maxima_m`: stored, but it evaluates no geofence. */
    WEAK,
    GOOD,
}
