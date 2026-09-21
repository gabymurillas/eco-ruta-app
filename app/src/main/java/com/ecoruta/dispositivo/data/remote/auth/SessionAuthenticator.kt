package com.ecoruta.dispositivo.data.remote.auth

import com.ecoruta.dispositivo.data.local.prefs.DevicePreferences
import com.ecoruta.dispositivo.data.remote.api.SupabaseAuthApi
import com.ecoruta.dispositivo.data.remote.dto.RefreshRequestDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Renews the access token when the backend answers 401, so an unattended device keeps
 * transmitting for weeks. Refreshes are serialized: concurrent 401s trigger a single refresh.
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

        val refreshed = runCatching { authApi.refresh(RefreshRequestDto(session.refreshToken)) }
            .getOrNull() ?: return null
        preferences.updateTokens(refreshed.accessToken, refreshed.refreshToken)
        return response.request.withToken(refreshed.accessToken)
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header(SupabaseHeadersInterceptor.HEADER_AUTHORIZATION, "Bearer $token").build()

    private fun Response.retryCount(): Int = generateSequence(priorResponse) { it.priorResponse }.count()

    private companion object {
        const val MAX_RETRIES = 2
    }
}
