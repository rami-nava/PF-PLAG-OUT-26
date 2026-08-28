package com.example.plag_out.Service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.plag_out.AlmacenamientoLocal.PreferenciasUsuario
import com.example.plag_out.MainActivity
import com.example.plag_out.NotificacionesEventBus
import com.example.plag_out.R
import com.example.plag_out.SupabaseProvider
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Recibe los push de FCM y refresca el token del dispositivo.
 */
class PlagOutMessagingService : FirebaseMessagingService() {
    //Se llaman de manera automatica cuando el sistema lo precisa

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewToken(token: String) {
        // Solo tiene sentido registrar si hay sesión: si no, el login lo hará luego.
        val haySesion = runCatching {
            SupabaseProvider.client.auth.currentAccessTokenOrNull() != null
        }.getOrDefault(false)
        // Y si el usuario apagó las notificaciones en su perfil, un token nuevo no debe volver a
        // suscribir el dispositivo por la puerta de atrás.
        if (haySesion && PreferenciasUsuario.notificacionesActivadas(this)) {
            scope.launch { FcmTokenRegistrar.registrar(token) }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val titulo = message.notification?.title ?: data["titulo"] ?: "Plag-Out"
        val cuerpo = message.notification?.body ?: data["cuerpo"] ?: ""

        mostrarNotificacion(titulo, cuerpo, data)
        // Si la app está abierta y alguien está escuchando, refresca la campana ya mismo en vez
        // de esperar al próximo ON_RESUME.
        NotificacionesEventBus.avisarNuevaNotificacion()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun mostrarNotificacion(titulo: String, cuerpo: String, data: Map<String, String>) {
        crearCanal()

        // Intent de tap: reabre MainActivity con el id para hacer deep-link a la entidad del
        // aviso. Cada tipo de alerta trae la suya: GDD el monitoreo, reporte cercano el reporte
        // y biofixel cultivo que arrancó a acumular.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data["tipo"]?.let { putExtra(EXTRA_TIPO, it) }
            data["monitoreo_id"]?.let { putExtra(EXTRA_MONITOREO_ID, it) }
            data["reporte_id"]?.let { putExtra(EXTRA_REPORTE_ID, it) }
            data["plantacion_id"]?.let { putExtra(EXTRA_PLANTACION_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_plagout)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // POST_NOTIFICATIONS puede estar denegado (Android 13+): notify() no lanza,
        // simplemente no muestra nada, así que no hace falta chequear acá.
        val id = data["monitoreo_id"]?.toIntOrNull()
            ?: data["reporte_id"]?.toIntOrNull()
            ?: data["plantacion_id"]?.toIntOrNull()
            ?: data["entidad_id"]?.toIntOrNull()
            ?: System.currentTimeMillis().toInt()
        NotificationManagerCompat.from(this).notify(id, notification)
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Un solo canal para los tres tipos de aviso (GDD, reportes cercanos, biofix): el
            // nombre es genérico a propósito, porque es lo que el usuario ve en los ajustes del
            // sistema. El id no se toca —cambiarlo crearía un canal nuevo y perdería la
            // configuración que el usuario ya tenga— y es el mismo que declara el manifest
            // como default_notification_channel_id de FCM.
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas y avisos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Riesgo por GDD, reportes de plagas cercanos e inicio de " +
                        "acumulación (biofix) de tus cultivos"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "alertas_gdd"
        const val EXTRA_TIPO = "tipo"
        const val EXTRA_MONITOREO_ID = "monitoreo_id"
        const val EXTRA_REPORTE_ID = "reporte_id"
        const val EXTRA_PLANTACION_ID = "plantacion_id"
        const val EXTRA_ENTIDAD_ID = "entidad_id"
    }
}
