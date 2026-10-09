package com.example.plag_out.AlmacenamientoLocal

import android.content.Context
import com.example.plag_out.ROL_ADMIN
import com.example.plag_out.ROL_USUARIO

/**
 * Preferencias que el usuario controla desde el perfil.
 *
 * Vive en su propio archivo de SharedPreferences
 */
object PreferenciasUsuario {

    private const val PREFS = "preferencias_usuario"
    private const val NOTIFICACIONES = "notificaciones_activadas"
    private const val ROL = "rol"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun notificacionesActivadas(context: Context): Boolean =
        prefs(context).getBoolean(NOTIFICACIONES, true)

    fun setNotificacionesActivadas(context: Context, activadas: Boolean) {
        prefs(context).edit().putBoolean(NOTIFICACIONES, activadas).apply()
    }

    /**
     * Rol de la cuenta logueada, copiado de `GET /usuarios/me`. Se guarda acá y no se lee de Room
     * porque el destino inicial del NavHost se decide de forma síncrona al arrancar. Es solo
     * para decidir qué mostrar: los permisos los valida el backend.
     */
    fun rol(context: Context): String =
        prefs(context).getString(ROL, null) ?: ROL_USUARIO

    fun esAdmin(context: Context): Boolean = rol(context) == ROL_ADMIN

    fun guardarRol(context: Context, rol: String?) {
        prefs(context).edit().putString(ROL, rol ?: ROL_USUARIO).apply()
    }

    fun limpiarRol(context: Context) {
        prefs(context).edit().remove(ROL).apply()
    }
}
