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
    private const val ULTIMA_RUTA = "ultima_ruta_navegada"
    private const val MAPA_LAT = "mapa_latitud"
    private const val MAPA_LON = "mapa_longitud"
    private const val MAPA_ZOOM = "mapa_zoom"
    private const val MAPA_MODO = "mapa_modo"

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

    /**
     * Última pantalla/ruta navegada por el usuario. Se persiste para que si el usuario
     * sale de la app o el proceso se descarga de memoria, al regresar vuelva al mismo lugar.
     */
    fun ultimaRuta(context: Context): String? =
        prefs(context).getString(ULTIMA_RUTA, null)

    fun guardarUltimaRuta(context: Context, ruta: String) {
        prefs(context).edit().putString(ULTIMA_RUTA, ruta).apply()
    }

    fun limpiarUltimaRuta(context: Context) {
        prefs(context).edit().remove(ULTIMA_RUTA).apply()
    }

    /**
     * Guarda la última posición de la cámara del mapa (latitud, longitud, zoom y modo de visualización)
     * para reanudar en el mismo punto al volver a entrar a la app o al alternar entre aplicaciones.
     */
    fun guardarCamaraMapa(context: Context, lat: Double, lon: Double, zoom: Double, modo: String? = null) {
        val editor = prefs(context).edit()
            .putString(MAPA_LAT, lat.toString())
            .putString(MAPA_LON, lon.toString())
            .putString(MAPA_ZOOM, zoom.toString())
        if (modo != null) {
            editor.putString(MAPA_MODO, modo)
        }
        editor.apply()
    }

    fun obtenerCamaraMapa(context: Context): Triple<Double, Double, Double>? {
        val p = prefs(context)
        val lat = p.getString(MAPA_LAT, null)?.toDoubleOrNull() ?: return null
        val lon = p.getString(MAPA_LON, null)?.toDoubleOrNull() ?: return null
        val zoom = p.getString(MAPA_ZOOM, null)?.toDoubleOrNull() ?: return null
        return Triple(lat, lon, zoom)
    }

    fun guardarModoMapa(context: Context, modo: String) {
        prefs(context).edit().putString(MAPA_MODO, modo).apply()
    }

    fun obtenerModoMapa(context: Context): String? =
        prefs(context).getString(MAPA_MODO, null)

    fun limpiarCamaraMapa(context: Context) {
        prefs(context).edit()
            .remove(MAPA_LAT)
            .remove(MAPA_LON)
            .remove(MAPA_ZOOM)
            .remove(MAPA_MODO)
            .apply()
    }
}
