package com.example.plag_out.Service

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.plag_out.DispositivoRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Registra y desregistra el token FCM del dispositivo contra el backend.
 */
object FcmTokenRegistrar {

    private const val TAG = "FcmTokenRegistrar"
    private const val PLATAFORMA = "android"
    private const val MAX_INTENTOS = 4
    private const val ESPERA_INICIAL_MS = 1_000L

    private val gddService: GDDService @RequiresApi(Build.VERSION_CODES.O)
    get() = RetrofitClient.gddService

    /** Obtiene el token FCM actual y lo registra (upsert) en el backend. */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun registrar() = withContext(Dispatchers.IO) {
        try {
            val token = obtenerTokenConReintentos()
            val response = gddService.registrarDispositivo(
                DispositivoRequest(fcm_token = token, plataforma = PLATAFORMA)
            )
            if (response.isSuccessful) {
                Log.d(TAG, "Dispositivo registrado: id=${response.body()?.id}")
            } else {
                Log.e(
                    TAG,
                    "Error al registrar dispositivo: ${response.code()} ${response.errorBody()?.string()}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al registrar el token FCM", e)
        }
    }

    /**
     * Registra un token puntual (usado desde onNewToken, donde FCM ya nos entrega
     * el token nuevo y no hace falta volver a pedirlo).
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun registrar(token: String) = withContext(Dispatchers.IO) {
        try {
            val response = gddService.registrarDispositivo(
                DispositivoRequest(fcm_token = token, plataforma = PLATAFORMA)
            )
            if (!response.isSuccessful) {
                Log.e(
                    TAG,
                    "Error al registrar token nuevo: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al registrar token puntual", e)
        }
    }

    /**
     * Elimina el token del dispositivo en el backend (logout). Best-effort: si
     * falla, el backend termina limpiando los tokens muertos al fallar el envío.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun desregistrar() {
        try {
            val token = obtenerToken()
            val response = gddService.eliminarDispositivo(token)
            if (!response.isSuccessful) {
                Log.w(TAG, "No se pudo eliminar el dispositivo: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al eliminar el token FCM", e)
        }
    }

    /**
     * Pide el token reintentando con backoff exponencial.
     *
     * FCM devuelve SERVICE_NOT_AVAILABLE (envuelto en IOException) cuando Play
     * Services todavía no pudo hablar con sus servidores.
     */
    private suspend fun obtenerTokenConReintentos(): String {
        var espera = ESPERA_INICIAL_MS
        repeat(MAX_INTENTOS - 1) { intento ->
            try {
                return obtenerToken()
            } catch (e: IOException) {
                Log.w(TAG, "FCM no disponible (intento ${intento + 1}/$MAX_INTENTOS), reintento en ${espera}ms: ${e.message}")
                delay(espera)
                espera *= 2
            }
        }
        // Último intento: si vuelve a fallar, que la excepción suba al catch de registrar()
        return obtenerToken()
    }

    /** Envuelve el Task<String> de FCM en una función suspend. */
    private suspend fun obtenerToken(): String =
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> cont.resume(token) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
