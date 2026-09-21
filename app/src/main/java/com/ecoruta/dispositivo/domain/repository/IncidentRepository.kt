package com.ecoruta.dispositivo.domain.repository

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.Incident

interface IncidentRepository {
    suspend fun report(incident: Incident): Outcome<Unit>
}
