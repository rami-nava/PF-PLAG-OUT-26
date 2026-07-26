package com.example.plag_out.AlmacenamientoLocal

import android.content.Context

/**
 * Preferencias que el usuario controla desde el perfil.
 *
 * Vive en su propio archivo de SharedPreferences
 */
object PreferenciasUsuario {

    private const val PREFS = "preferencias_usuario"
    private const val NOTIFICACIONES = "notificaciones_activadas"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun notificacionesActivadas(context: Context): Boolean =
        prefs(context).getBoolean(NOTIFICACIONES, true)

    fun setNotificacionesActivadas(context: Context, activadas: Boolean) {
        prefs(context).edit().putBoolean(NOTIFICACIONES, activadas).apply()
    }
}
