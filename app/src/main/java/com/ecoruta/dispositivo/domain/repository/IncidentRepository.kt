package com.ecoruta.dispositivo.domain.repository

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.Incident

/** Where incidents reported from the road go. Online only: an incident is about *now*. */
interface IncidentRepository {
    suspend fun report(incident: Incident): Outcome<Unit>
}
