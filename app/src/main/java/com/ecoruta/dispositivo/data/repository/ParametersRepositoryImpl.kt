package com.ecoruta.dispositivo.data.repository

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.data.local.prefs.DevicePreferences
import com.ecoruta.dispositivo.data.mapper.toOperationParams
import com.ecoruta.dispositivo.data.remote.api.SupabaseRestApi
import com.ecoruta.dispositivo.data.remote.safeApiCall
import com.ecoruta.dispositivo.domain.model.OperationParams
import com.ecoruta.dispositivo.domain.repository.ParametersRepository
import kotlinx.coroutines.flow.MutableStateFlow

class ParametersRepositoryImpl(
    private val api: SupabaseRestApi,
    private val preferences: DevicePreferences,
) : ParametersRepository {

    private val state = MutableStateFlow(OperationParams())

    override val current: OperationParams get() = state.value

    override suspend fun refresh() {
        val values = when (val result = safeApiCall { api.getParameters() }) {
            is Outcome.Success -> result.value.associate { it.key to it.value }
                .also { preferences.saveParameters(it) }
            is Outcome.Failure -> preferences.cachedParameters()
        }
        if (values != null) state.value = values.toOperationParams()
    }
}
