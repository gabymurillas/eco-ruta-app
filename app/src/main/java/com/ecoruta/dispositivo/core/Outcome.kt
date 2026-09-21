package com.ecoruta.dispositivo.core

/** Result of an operation that can fail in an expected, recoverable way. */
sealed interface Outcome<out T> {
    data class Success<out T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}

/** Expected failures the app knows how to react to. Anything else is [Unexpected]. */
sealed interface AppError {
    // Transport / backend
    data object Offline : AppError
    data object Unauthorized : AppError
    data object Conflict : AppError
    data class Rejected(val httpCode: Int) : AppError
    data object Server : AppError
    data class Unexpected(val cause: Throwable? = null) : AppError

    // Domain
    data object InvalidPairingCode : AppError
    data object NotPaired : AppError
    data object NoActiveTrip : AppError
    data object NoRouteAssigned : AppError
    data object LocationUnavailable : AppError
}

/** Worth trying again later: the cause is transient, not a rejected request. */
val AppError.isRetryable: Boolean get() = this is AppError.Offline || this is AppError.Server

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T> Outcome<T>.onSuccess(action: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) action(value)
    return this
}

inline fun <T> Outcome<T>.onFailure(action: (AppError) -> Unit): Outcome<T> {
    if (this is Outcome.Failure) action(error)
    return this
}

fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Success)?.value
