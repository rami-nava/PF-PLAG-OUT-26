package com.example.plag_out.Service

import android.content.Context
import android.util.Log
import com.example.plag_out.DispositivoRequest
import com.example.plag_out.SupabaseProvider
import com.example.plag_out.AlmacenamientoLocal.PreferenciasUsuario
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import com.google.android.gms.tasks.Task
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Registra y desregistra el dispositivo contra el backend por su Firebase installation ID (FID).
 *
 * Desde firebase-messaging 25.1.0 el registro es por FID: getToken()/deleteToken() quedan
 * deshabilitados (flag firebase_messaging_installation_id_enabled del manifest), el SDK registra
 * con FirebaseInstallations.getId() y avisa los cambios en onRegistered().
 */
object FcmTokenRegistrar {

    private const val TAG = "FcmTokenRegistrar"
    private const val PLATAFORMA = "android"
    private const val MAX_INTENTOS = 4
    private const val ESPERA_INICIAL_MS = 1_000L

    private val gddService: GDDService
        get() = RetrofitClient.gddService

    private var context: Context? = null
    fun configurar(context: Context) { this.context = context.applicationContext }

    private val coordinator by lazy {
        TokenRegistrationCoordinator(
            owner = { SupabaseProvider.client.auth.currentUserOrNull()?.id },
            enabled = { context?.let { PreferenciasUsuario.notificacionesActivadas(it) } == true },
            token = { obtenerFidConReintentos() },
            rotate = { rotarFid() },
            register = { fid ->
                val response = gddService.registrarDispositivo(DispositivoRequest(fid, PLATAFORMA))
                if (response.code() == 409 && response.errorBody()?.string()?.contains("device_token_conflict") != true) 500
                else response.code()
            },
            unregister = { token -> gddService.eliminarDispositivo(token); Unit },
        )
    }

    fun iniciarSesion() = coordinator.beginSession()
    fun invalidarSesion() = coordinator.invalidateSession()

    suspend fun registrar() = withContext(Dispatchers.IO) {
        try { coordinator.register() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { Log.w(TAG, "Registro FCM pendiente; se reintentará con la sesión vigente") }
    }

    // Read the current FID instead of replaying a potentially stale onRegistered callback.
    @Suppress("UNUSED_PARAMETER")
    suspend fun registrar(fid: String) = registrar()

    suspend fun desregistrar(remote: Boolean = true) {
        try { coordinator.unregister(remote) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { Log.w(TAG, "Retiro FCM pendiente") }
    }

    // Una vez por proceso alcanza: los cambios posteriores del FID llegan por onRegistered().
    @Volatile private var registradoEnFcm = false

    /**
     * Un FID nuevo: se da de baja el actual en FCM y se borra la instalación, así el próximo
     * usuario (o el reintento tras un 409 device_token_conflict) no hereda el anterior.
     */
    private suspend fun rotarFid() {
        runCatching { FirebaseMessaging.getInstance().unregister().esperar() }
            .onFailure { Log.w(TAG, "No se pudo dar de baja el FID en FCM: ${it.javaClass.simpleName}") }
        registradoEnFcm = false
        FirebaseInstallations.getInstance().delete().esperar()
    }

    /**
     * Registra el dispositivo en FCM y devuelve su FID, reintentando con backoff exponencial.
     *
     * FCM devuelve SERVICE_NOT_AVAILABLE (envuelto en IOException) cuando Play
     * Services todavía no pudo hablar con sus servidores.
     */
    private suspend fun obtenerFidConReintentos(): String {
        var espera = ESPERA_INICIAL_MS
        repeat(MAX_INTENTOS - 1) { intento ->
            try {
                return obtenerFid()
            } catch (e: IOException) {
                Log.w(TAG, "FCM no disponible (intento ${intento + 1}/$MAX_INTENTOS), reintento en ${espera}ms: ${e.message}")
                delay(espera)
                espera *= 2
            }
        }
        // Último intento: si vuelve a fallar, que la excepción suba al catch de registrar()
        return obtenerFid()
    }

    private suspend fun obtenerFid(): String {
        if (!registradoEnFcm) {
            FirebaseMessaging.getInstance().register().esperar()
            registradoEnFcm = true
        }
        return FirebaseInstallations.getInstance().id.esperar()
    }

    /** Envuelve un Task de Play Services en una función suspend. */
    private suspend fun <T> Task<T>.esperar(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { valor -> if (cont.isActive) cont.resume(valor) }
            addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
        }
}
