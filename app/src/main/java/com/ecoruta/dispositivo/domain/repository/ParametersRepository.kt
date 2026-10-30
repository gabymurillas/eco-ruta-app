package com.ecoruta.dispositivo.domain.repository

/** The backend's `parametros`, cached on the device so it keeps its calibration offline. */
interface ParametersRepository : OperationParamsSource {
    /**
     * Refreshes [current] from the backend. When offline it falls back to the last values that
     * were fetched, and to the built-in defaults on a device that has never been online.
     */
    suspend fun refresh()
}
