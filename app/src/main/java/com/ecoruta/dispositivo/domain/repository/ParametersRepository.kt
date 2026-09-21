package com.ecoruta.dispositivo.domain.repository

import com.ecoruta.dispositivo.domain.model.OperationParams

/** Read-only view of the parameters currently in force. */
interface OperationParamsSource {
    val current: OperationParams
}

interface ParametersRepository : OperationParamsSource {
    /**
     * Refreshes [current] from the backend. When offline it falls back to the last values that
     * were fetched, and to the built-in defaults on a device that has never been online.
     */
    suspend fun refresh()
}
