package com.example.plag_out

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import com.example.plag_out.Service.FcmTokenRegistrar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.example.plag_out.Service.PlagOutMessagingService

/**
 * Se ejecuta cada vez que arranca el proceso, incluso cuando quien lo levanta es un push de
 * FCM con la app cerrada. Acá se crea el canal de notificaciones para que exista antes de que
 * el SDK de Firebase tenga que mostrar un aviso: si no existe, el SDK usa
 * `fcm_fallback_notification_channel` y la alerta pierde la importancia alta.
 */
class PlagOutApplication : Application() {
    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        com.example.plag_out.Service.FcmTokenRegistrar.configurar(this)
        PlagOutMessagingService.crearCanal(this)
        PresenceRetryScheduler.start(this)
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    notificationScope.launch { FcmTokenRegistrar.registrar() }
                }
            }
        )
    }
}
