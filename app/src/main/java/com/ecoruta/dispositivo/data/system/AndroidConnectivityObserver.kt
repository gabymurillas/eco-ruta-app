package com.ecoruta.dispositivo.data.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.ecoruta.dispositivo.domain.system.ConnectivityObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

/**
 * Online means a *validated* connection: a captive portal or dead mobile data counts as offline,
 * so positions are queued instead of failing one request at a time.
 */
class AndroidConnectivityObserver(
    context: Context,
    scope: CoroutineScope,
) : ConnectivityObserver {

    private val manager = context.getSystemService(ConnectivityManager::class.java)

    override val isOnline: StateFlow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasValidatedInternet())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, initialValue = manager.hasValidatedInternet())

    private fun ConnectivityManager.hasValidatedInternet(): Boolean =
        getNetworkCapabilities(activeNetwork)?.hasValidatedInternet() == true

    private fun NetworkCapabilities.hasValidatedInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
