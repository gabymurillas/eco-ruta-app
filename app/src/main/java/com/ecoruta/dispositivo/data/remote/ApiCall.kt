package com.ecoruta.dispositivo.data.remote

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/** Runs a network call and translates every expected failure into an [AppError]. */
suspend fun <T> safeApiCall(block: suspend () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    Outcome.Failure(e.toAppError())
} catch (e: IOException) {
    Outcome.Failure(AppError.Offline)
} catch (e: SerializationException) {
    Outcome.Failure(AppError.Unexpected(e))
}

private fun HttpException.toAppError(): AppError = when (val httpCode = code()) {
    HTTP_UNAUTHORIZED -> AppError.Unauthorized
    HTTP_CONFLICT -> AppError.Conflict
    in HTTP_CLIENT_ERRORS -> AppError.Rejected(httpCode)
    else -> AppError.Server
}

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_CONFLICT = 409
private val HTTP_CLIENT_ERRORS = 400..499
