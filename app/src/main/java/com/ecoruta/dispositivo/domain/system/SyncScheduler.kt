package com.ecoruta.dispositivo.domain.system

/** Schedules deferred work that must survive process death and wait for connectivity. */
interface SyncScheduler {
    /** Retries delivery of queued positions and trip closes as soon as the network is available. */
    fun scheduleSync()
}
