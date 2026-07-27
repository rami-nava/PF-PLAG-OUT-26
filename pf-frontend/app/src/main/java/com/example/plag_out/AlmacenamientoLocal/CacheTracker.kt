package com.example.plag_out.AlmacenamientoLocal

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Registra cuándo se consultó por última vez cada recurso al backend, para
 * decidir si alcanza con el caché de Room o hay que volver a pedir los datos.
 */
object CacheTracker {

    const val TERRENOS = "terrenos"
    const val PLANTACIONES = "plantaciones"
    const val MONITOREOS = "monitoreos"

    private const val PREFS = "cache_tracker"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Si el recurso ya se consultó al menos una vez (terrenos/plantaciones). */
    fun yaConsultado(context: Context, clave: String): Boolean =
        prefs(context).getLong(clave, 0L) != 0L

    /**
     * Si la última consulta fue hoy según la hora argentina (monitoreos):
     * el backend recalcula los GDD todos los días a las 00:00 de Argentina,
     * así que una consulta de un día anterior ya está desactualizada.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun consultadoHoy(context: Context, clave: String): Boolean {
        val ultimaConsulta = prefs(context).getLong(clave, 0L)
        if (ultimaConsulta == 0L) return false
        val zonaArgentina = ZoneId.of("America/Argentina/Buenos_Aires")
        val fechaUltimaConsulta = Instant.ofEpochMilli(ultimaConsulta).atZone(zonaArgentina).toLocalDate()
        return fechaUltimaConsulta == LocalDate.now(zonaArgentina)
    }

    fun marcarConsultado(context: Context, clave: String) {
        prefs(context).edit().putLong(clave, System.currentTimeMillis()).apply()
    }

    fun invalidar(context: Context, clave: String) {
        prefs(context).edit().remove(clave).apply()
    }

    fun limpiarTodo(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
