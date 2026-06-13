package com.example.forgeplan.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

/**
 * Observa o estado da rede e dispara SyncManager.syncIfOnline() sempre que
 * a app deteta que ficou online (ligação com acesso à internet).
 *
 * Uso (no ForgePlanApplication.onCreate()):
 *
 *     ConnectivityObserver.register(applicationContext)
 *
 * Não é necessário fazer unregister manualmente em apps simples (o callback
 * vive enquanto o processo da app viver), mas se quiseres parar de observar
 * em algum momento, chama ConnectivityObserver.unregister(context).
 */
object ConnectivityObserver {

    private var registered = false
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun register(context: Context) {
        if (registered) return

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Ficámos online -> tenta sincronizar pendentes
                SyncManager.syncIfOnline(context)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                ) {
                    SyncManager.syncIfOnline(context)
                }
            }
        }

        cm.registerNetworkCallback(request, cb)
        callback = cb
        registered = true
    }

    fun unregister(context: Context) {
        val cb = callback ?: return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            cm.unregisterNetworkCallback(cb)
        } catch (e: Exception) {
            // já não estava registado
        }
        callback = null
        registered = false
    }
}