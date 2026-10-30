package com.ecoruta.dispositivo.data

import com.ecoruta.dispositivo.data.remote.api.SupabaseAuthApi
import com.ecoruta.dispositivo.data.remote.auth.SessionAuthenticator
import com.ecoruta.dispositivo.data.remote.dto.PairRequestDto
import com.ecoruta.dispositivo.data.remote.dto.PairResponseDto
import com.ecoruta.dispositivo.data.remote.dto.RefreshRequestDto
import com.ecoruta.dispositivo.data.remote.dto.TokenResponseDto
import com.ecoruta.dispositivo.fakes.TEST_SESSION
import com.ecoruta.dispositivo.fakes.testPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.HttpException
import java.io.IOException

class SessionAuthenticatorTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferences by lazy { testPreferences(folder.root, storeScope) }
    private val authApi = FakeAuthApi()
    private val authenticator by lazy { SessionAuthenticator(preferences, authApi) }

    @After
    fun tearDown() {
        storeScope.cancel()
    }

    @Test
    fun `an expired token is renewed and the request retried with the new one`() = runTest {
        preferences.saveSession(TEST_SESSION)
        authApi.result = { TokenResponseDto(accessToken = "access-2", refreshToken = "refresh-2") }

        val retry = authenticator.authenticate(null, unauthorized(token = TEST_SESSION.accessToken))

        assertEquals("Bearer access-2", retry?.header("Authorization"))
        assertEquals("refresh-2", preferences.session.first()?.refreshToken)
    }

    @Test
    fun `a refused refresh token ends the session so the device asks to be paired again`() = runTest {
        preferences.saveSession(TEST_SESSION)
        authApi.result = { throw HttpException(retrofit2.Response.error<Any>(400, "".toResponseBody())) }

        val retry = authenticator.authenticate(null, unauthorized(token = TEST_SESSION.accessToken))

        assertNull(retry)
        assertNull(preferences.session.first())
        assertEquals(TEST_SESSION.unitId, preferences.lastPairedUnitId()) // same unit can pick up again
    }

    @Test
    fun `no network while refreshing keeps the session for the next attempt`() = runTest {
        preferences.saveSession(TEST_SESSION)
        authApi.result = { throw IOException("no route to host") }

        val retry = authenticator.authenticate(null, unauthorized(token = TEST_SESSION.accessToken))

        assertNull(retry)
        assertNotNull(preferences.session.first())
    }

    @Test
    fun `a server error while refreshing keeps the session too`() = runTest {
        preferences.saveSession(TEST_SESSION)
        authApi.result = { throw HttpException(retrofit2.Response.error<Any>(503, "".toResponseBody())) }

        authenticator.authenticate(null, unauthorized(token = TEST_SESSION.accessToken))

        assertNotNull(preferences.session.first())
    }

    private fun unauthorized(token: String): Response = Response.Builder()
        .request(Request.Builder().url("https://example.test/rest/v1/viajes").header("Authorization", "Bearer $token").build())
        .protocol(Protocol.HTTP_1_1)
        .code(401)
        .message("Unauthorized")
        .build()
}

private class FakeAuthApi : SupabaseAuthApi {
    var result: () -> TokenResponseDto = { error("unset") }

    override suspend fun pair(body: PairRequestDto): PairResponseDto = error("unused")

    override suspend fun refresh(body: RefreshRequestDto): TokenResponseDto = result()
}
