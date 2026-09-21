package com.ecoruta.dispositivo.presentation.common

import androidx.annotation.StringRes
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.core.AppError

/** Maps a domain error to the user-facing message that explains it. */
@StringRes
fun AppError.toMessageRes(): Int = when (this) {
    AppError.Offline -> R.string.error_offline
    AppError.Unauthorized -> R.string.error_unauthorized
    AppError.Conflict -> R.string.error_trip_already_open
    is AppError.Rejected -> R.string.error_rejected
    AppError.Server -> R.string.error_server
    AppError.InvalidPairingCode -> R.string.error_invalid_pairing_code
    AppError.NotPaired -> R.string.error_not_paired
    AppError.NoActiveTrip -> R.string.error_no_active_trip
    AppError.NoRouteAssigned -> R.string.error_no_route_assigned
    AppError.LocationUnavailable -> R.string.error_location_unavailable
    is AppError.Unexpected -> R.string.error_unexpected
}
