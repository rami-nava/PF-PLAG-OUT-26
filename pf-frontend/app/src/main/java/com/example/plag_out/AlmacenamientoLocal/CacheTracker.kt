package com.example.plag_out.AlmacenamientoLocal

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Registra cuándo se consultó por última vez cada recurso al backend, para
 * decidir si alcanza con el caché de Room o hay que volver a pedir los datos.
 */
object CacheTracker {

    const val TERRENOS = "terrenos"
    const val PLANTACIONES = "plantaciones"
    const val MONITOREOS = "monitoreos"

    private const val PREFS = "cache_tracker"

    private val ZONA_ARGENTINA: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")

    /**
     * Hora argentina a la que el workflow del backend termina de recalcular los GDD del día.
     * Corre a las 06:00, así que se toma un margen hasta las 06:30.
     */
    private val HORA_ACTUALIZACION_BACKEND: LocalTime = LocalTime.of(6, 30)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Si el recurso ya se consultó al menos una vez (terrenos/plantaciones). */
    fun yaConsultado(context: Context, clave: String): Boolean =
        prefs(context).getLong(clave, 0L) != 0L

    /**
     * Si la última consulta es posterior al último recálculo de GDD del backend (monitoreos).
     * El workflow corre todos los días a las 06:00 de Argentina, así que el caché sigue
     * vigente hasta las 06:30 del día siguiente: una consulta anterior a ese corte ya
     * quedó desactualizada.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun consultadoTrasUltimaActualizacion(context: Context, clave: String): Boolean {
        val ultimaConsulta = prefs(context).getLong(clave, 0L)
        if (ultimaConsulta == 0L) return false
        val ahora = ZonedDateTime.now(ZONA_ARGENTINA)
        // Corte de hoy si ya pasaron las 06:30; si no, el de ayer.
        val corteDeHoy = ahora.with(HORA_ACTUALIZACION_BACKEND)
        val ultimoCorte = if (ahora.isBefore(corteDeHoy)) corteDeHoy.minusDays(1) else corteDeHoy
        val fechaUltimaConsulta = Instant.ofEpochMilli(ultimaConsulta).atZone(ZONA_ARGENTINA)
        return !fechaUltimaConsulta.isBefore(ultimoCorte)
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
