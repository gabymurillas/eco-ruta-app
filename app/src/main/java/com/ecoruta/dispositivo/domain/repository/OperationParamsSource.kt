package com.ecoruta.dispositivo.domain.repository

import com.ecoruta.dispositivo.domain.model.OperationParams

/**
 * Read-only view of the parameters currently in force. Split from [ParametersRepository] so the
 * rules that only read them (the transmission policy, retention) cannot trigger a refresh.
 */
interface OperationParamsSource {
    val current: OperationParams
}
