package com.ecoruta.dispositivo.data.remote.auth

import android.util.Log
import com.ecoruta.dispositivo.data.local.prefs.DevicePreferences
import com.ecoruta.dispositivo.data.remote.api.SupabaseAuthApi
import com.ecoruta.dispositivo.data.remote.dto.RefreshRequestDto
import com.ecoruta.dispositivo.data.remote.dto.TokenResponseDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import java.io.IOException

/**
 * Renews the access token when the backend answers 401, so an unattended device keeps
 * transmitting for weeks. Refreshes are serialized: concurrent 401s trigger a single refresh.
 *
 * If the refresh token itself is refused, the pairing is over (the admin paired the unit again, or
 * the token was revoked) and no retry can fix it. The credentials are then dropped so the device
 * shows the pairing screen instead of claiming to transmit while every request fails.
 */
class SessionAuthenticator(
    private val preferences: DevicePreferences,
    private val authApi: SupabaseAuthApi,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.retryCount() >= MAX_RETRIES) return null
        return synchronized(lock) { runBlocking { retryWithFreshToken(response) } }
    }

    private suspend fun retryWithFreshToken(response: Response): Request? {
        val session = preferences.session.first() ?: return null
        val failedToken = response.request.header(SupabaseHeadersInterceptor.HEADER_AUTHORIZATION)
            ?.removePrefix("Bearer ")

        // Another call already refreshed while this one was waiting on the lock.
        if (session.accessToken != failedToken) return response.request.withToken(session.accessToken)

        val refreshed = refresh(session.refreshToken) ?: return null
        preferences.updateTokens(refreshed.accessToken, refreshed.refreshToken)
        return response.request.withToken(refreshed.accessToken)
    }

    /** Null when the refresh failed. Only a refusal from the auth server ends the session. */
    private suspend fun refresh(refreshToken: String): TokenResponseDto? = try {
        authApi.refresh(RefreshRequestDto(refreshToken))
    } catch (e: HttpException) {
        if (e.code() in REFRESH_REFUSED) {
            Log.e(TAG, "Refresh token refused (${e.code()}): the device must be paired again")
            preferences.clearTokens()
        }
        null
    } catch (e: IOException) {
        null // offline: keep the session, the next request tries again
    } catch (e: SerializationException) {
        null
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header(SupabaseHeadersInterceptor.HEADER_AUTHORIZATION, "Bearer $token").build()

    private fun Response.retryCount(): Int = generateSequence(priorResponse) { it.priorResponse }.count()

    private companion object {
        const val TAG = "SessionAuthenticator"
        const val MAX_RETRIES = 2

        /** GoTrue answers 400 (invalid_grant) or 401 for a revoked, unknown or reused refresh token. */
        val REFRESH_REFUSED = setOf(400, 401)
    }
}
