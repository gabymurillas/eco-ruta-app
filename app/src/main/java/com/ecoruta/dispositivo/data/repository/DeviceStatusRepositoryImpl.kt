package com.ecoruta.dispositivo.data.repository

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.data.mapper.toDto
import com.ecoruta.dispositivo.data.remote.api.SupabaseRestApi
import com.ecoruta.dispositivo.data.remote.safeApiCall
import com.ecoruta.dispositivo.domain.model.DeviceStatus
import com.ecoruta.dispositivo.domain.repository.DeviceStatusRepository

/** Sends the heartbeat through the `registrar_salud_del_dispositivo` RPC. */
class DeviceStatusRepositoryImpl(
    private val api: SupabaseRestApi,
) : DeviceStatusRepository {

    override suspend fun report(status: DeviceStatus): Outcome<Unit> =
        safeApiCall { api.reportDeviceHealth(status.toDto()) }
}
