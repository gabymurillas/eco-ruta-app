package com.ecoruta.dispositivo.domain.system

import kotlinx.coroutines.flow.StateFlow

/** Decides between sending now and queueing. */
interface ConnectivityObserver {
    /** True while the device has a validated internet connection. */
    val isOnline: StateFlow<Boolean>
}
