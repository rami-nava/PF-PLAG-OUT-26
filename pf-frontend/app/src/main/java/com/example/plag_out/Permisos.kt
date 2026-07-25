package com.example.plag_out

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Utilidades de permisos compartidas por la sección de permisos del perfil.
 */
class EstadoPermiso internal constructor(
    private val context: Context,
    private val permiso: String,
    otorgadoInicial: Boolean
) {
    var otorgado by mutableStateOf(otorgadoInicial)
        private set

    /** Vuelve a leer el permiso del sistema. */
    fun revalidar() {
        otorgado = permisoOtorgado(context, permiso)
    }
}

/**
 * Estado de un permiso, revalidado cada vez que la pantalla vuelve a primer plano.
 */
@Composable
fun rememberEstadoPermiso(permiso: String): EstadoPermiso {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val estado = remember(permiso) {
        EstadoPermiso(context, permiso, permisoOtorgado(context, permiso))
    }

    DisposableEffect(lifecycleOwner, estado) {
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) estado.revalidar()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return estado
}

fun permisoOtorgado(context: Context, permiso: String): Boolean =
    ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED

/**
 * Si el sistema ya no va a volver a mostrar el diálogo del permiso (el usuario lo denegó de forma
 * permanente). En ese caso el único camino es mandarlo a los ajustes.
 */
fun denegadoPermanentemente(context: Context, permiso: String): Boolean {
    val activity = context.buscarActivity() ?: return false
    return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permiso)
}

/** Abre la ficha de la app en los ajustes del sistema (para permisos ya denegados). */
fun abrirAjustesDeApp(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    context.startActivity(intent)
}

/** Abre directamente los ajustes de notificaciones de la app. */
fun abrirAjustesDeNotificaciones(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } else {
        abrirAjustesDeApp(context)
    }
}

/** El context de Compose puede venir envuelto: hay que desenvolverlo para llegar a la Activity. */
private fun Context.buscarActivity(): Activity? {
    var actual: Context? = this
    while (actual is ContextWrapper) {
        if (actual is Activity) return actual
        actual = actual.baseContext
    }
    return null
}
