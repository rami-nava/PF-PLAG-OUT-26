package com.example.plag_out

import android.app.Application
import com.example.plag_out.Service.PlagOutMessagingService

/**
 * Se ejecuta cada vez que arranca el proceso, incluso cuando quien lo levanta es un push de
 * FCM con la app cerrada. Acá se crea el canal de notificaciones para que exista antes de que
 * el SDK de Firebase tenga que mostrar un aviso: si no existe, el SDK usa
 * `fcm_fallback_notification_channel` y la alerta pierde la importancia alta.
 */
class PlagOutApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PlagOutMessagingService.crearCanal(this)
    }
}
