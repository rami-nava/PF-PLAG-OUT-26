package com.example.plag_out.Service

import android.os.Build
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.plag_out.DispositivoRequest
import com.example.plag_out.SupabaseProvider
import com.example.plag_out.AlmacenamientoLocal.PreferenciasUsuario
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
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

    private var context: Context? = null
    fun configurar(context: Context) { this.context = context.applicationContext }

    private val coordinator by lazy {
        TokenRegistrationCoordinator(
            owner = { SupabaseProvider.client.auth.currentUserOrNull()?.id },
            enabled = { context?.let { PreferenciasUsuario.notificacionesActivadas(it) } == true },
            token = { obtenerTokenConReintentos() },
            rotate = { eliminarTokenLocal() },
            register = { token ->
                val response = gddService.registrarDispositivo(DispositivoRequest(token, PLATAFORMA))
                if (response.code() == 409 && response.errorBody()?.string()?.contains("device_token_conflict") != true) 500
                else response.code()
            },
            unregister = { token -> gddService.eliminarDispositivo(token); Unit },
        )
    }

    fun iniciarSesion() = coordinator.beginSession()
    fun invalidarSesion() = coordinator.invalidateSession()

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun registrar() = withContext(Dispatchers.IO) {
        try { coordinator.register() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { Log.w(TAG, "Registro FCM pendiente; se reintentará con la sesión vigente") }
    }

    // Read the current token instead of replaying a potentially stale onNewToken callback.
    @RequiresApi(Build.VERSION_CODES.O)
    @Suppress("UNUSED_PARAMETER")
    suspend fun registrar(token: String) = registrar()

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun desregistrar(remote: Boolean = true) {
        try { coordinator.unregister(remote) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { Log.w(TAG, "Retiro FCM pendiente") }
    }

    private suspend fun eliminarTokenLocal(): Unit = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().deleteToken()
            .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
            .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
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
                .addOnSuccessListener { token -> if (cont.isActive) cont.resume(token) }
                .addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
        }
}
