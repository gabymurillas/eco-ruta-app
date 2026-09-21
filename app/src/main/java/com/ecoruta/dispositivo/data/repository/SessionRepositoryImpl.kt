package com.ecoruta.dispositivo.data.repository

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.core.map
import com.ecoruta.dispositivo.core.onSuccess
import com.ecoruta.dispositivo.data.local.prefs.DevicePreferences
import com.ecoruta.dispositivo.data.mapper.toDomain
import com.ecoruta.dispositivo.data.remote.api.SupabaseAuthApi
import com.ecoruta.dispositivo.data.remote.dto.PairRequestDto
import com.ecoruta.dispositivo.data.remote.safeApiCall
import com.ecoruta.dispositivo.domain.model.DeviceSession
import com.ecoruta.dispositivo.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SessionRepositoryImpl(
    private val preferences: DevicePreferences,
    private val authApi: SupabaseAuthApi,
) : SessionRepository {

    override val session: Flow<DeviceSession?> = preferences.session

    override suspend fun current(): DeviceSession? = session.first()

    override suspend fun pair(code: String): Outcome<DeviceSession> =
        safeApiCall { authApi.pair(PairRequestDto(code)) }
            .map { it.toDomain() }
            .onSuccess { preferences.saveSession(it) }
            .withInvalidCodeMapping()

    /**
     * The backend answers 401 for an unknown, expired or already-used code, deliberately without
     * saying which. There is no session yet, so a 401 here can only mean a bad code.
     */
    private fun Outcome<DeviceSession>.withInvalidCodeMapping(): Outcome<DeviceSession> {
        val isBadCode = this is Outcome.Failure && (error is AppError.Unauthorized || error is AppError.Rejected)
        return if (isBadCode) Outcome.Failure(AppError.InvalidPairingCode) else this
    }
}
