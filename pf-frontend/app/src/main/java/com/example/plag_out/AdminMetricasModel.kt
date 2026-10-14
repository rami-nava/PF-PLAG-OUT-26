package com.example.plag_out

import java.time.LocalDate

/**
 * Resumen de una plaga específica dentro de una campaña agrícola.
 */
data class ResumenPlagaCampaña(
    val plagaNombre: String,
    val plagaCientifico: String? = null,
    val totalMonitoreos: Int,
    val porcentajeIncidencia: Float,
    val enRiesgoAlto: Int,
    val porcentajeRiesgoAlto: Float,
    val nivelRiesgoPromedio: Float
)

/**
 * Agrupa los datos y métricas históricas de una campaña agrícola determinada (ej. "2025/2026").
 * En la agricultura regional, la campaña se computa del 1 de julio al 30 de junio del año siguiente.
 */
data class CampañaHistorica(
    val nombre: String,
    val añoInicio: Int,
    val totalMonitoreos: Int,
    val hectareasMonitoreadas: Float,
    val plagaPredominante: ResumenPlagaCampaña?,
    val plagas: List<ResumenPlagaCampaña>,
    val riesgoBajo: Int,
    val riesgoModerado: Int,
    val riesgoAlto: Int
) {
    val totalConRiesgo: Int get() = riesgoBajo + riesgoModerado + riesgoAlto
    val porcentajeRiesgoBajo: Float get() = if (totalConRiesgo > 0) (riesgoBajo * 100f / totalConRiesgo) else 0f
    val porcentajeRiesgoModerado: Float get() = if (totalConRiesgo > 0) (riesgoModerado * 100f / totalConRiesgo) else 0f
    val porcentajeRiesgoAlto: Float get() = if (totalConRiesgo > 0) (riesgoAlto * 100f / totalConRiesgo) else 0f
    val cantidadPlagasDistintas: Int get() = plagas.size
}

/**
 * Estado observable de la pantalla de métricas de administración.
 */
data class AdminMetricasUIState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val campañas: List<CampañaHistorica> = emptyList(),
    val campañaSeleccionada: CampañaHistorica? = null,
    val totalHectareasHistoricas: Float = 0f,
    val totalMonitoreosHistoricos: Int = 0,
    val plagaHistoricaLider: String? = null,
    val error: String? = null
)

/**
 * Determina a qué campaña agrícola pertenece una fecha dada.
 * Si la fecha es de julio en adelante (mes >= 7), pertenece al ciclo `año / (año + 1)`.
 * Si es anterior a julio (mes < 7), pertenece al ciclo `(año - 1) / año`.
 */
fun determinarCampaña(fecha: LocalDate): String {
    val año = fecha.year
    return if (fecha.monthValue >= 7) {
        "$año/${año + 1}"
    } else {
        "${año - 1}/$año"
    }
}

/**
 * Extrae el año de inicio numérico de una etiqueta de campaña "2025/2026" -> 2025.
 */
fun extraerAñoInicioCampaña(nombreCampaña: String): Int =
    nombreCampaña.substringBefore('/').toIntOrNull() ?: 0

/**
 * Agrupa y procesa la lista de monitoreos históricos en campañas agrícolas,
 * calculando para cada una sus plagas predominantes, porcentajes y severidad.
 */
fun agruparPorCampaña(
    monitoreos: List<MonitoreoResponse>,
    terrenos: List<TerrenoResponse> = emptyList()
): List<CampañaHistorica> {
    if (monitoreos.isEmpty()) return emptyList()

    val terrenosMap = terrenos.associateBy { it.terreno_id }

    // Agrupar cada monitoreo según su fecha (fecha_inicio o fecha_actualizacion)
    val monitoreosPorCampaña = monitoreos.groupBy { m ->
        val fecha = m.fecha_inicio ?: m.fecha_actualizacion
        determinarCampaña(fecha)
    }

    return monitoreosPorCampaña.map { (nombreCampaña, listaMonitoreos) ->
        val totalCampaña = listaMonitoreos.size

        // Hectáreas monitoreadas en esta campaña (suma del área de terrenos únicos monitoreados)
        val hectareasCampaña = listaMonitoreos
            .map { it.terreno_id }
            .distinct()
            .mapNotNull { terrenosMap[it]?.terreno_area }
            .sum()

        // Agrupación de plagas dentro de esta campaña
        val porPlaga = listaMonitoreos.groupBy { it.plaga_nombre.trim() }

        val resumenPlagas = porPlaga.map { (nombrePlaga, items) ->
            val totalPlaga = items.size
            val incidencia = if (totalCampaña > 0) (totalPlaga * 100f / totalCampaña) else 0f
            val enAlto = items.count { it.nivel_alerta >= 2 }
            val pctAlto = if (totalPlaga > 0) (enAlto * 100f / totalPlaga) else 0f
            val promedioNivel = if (totalPlaga > 0) items.map { it.nivel_alerta.coerceAtLeast(0) }.average().toFloat() else 0f
            val nombreCientifico = items.firstNotNullOfOrNull { it.plaga_nombre_cientifico }

            ResumenPlagaCampaña(
                plagaNombre = nombrePlaga,
                plagaCientifico = nombreCientifico,
                totalMonitoreos = totalPlaga,
                porcentajeIncidencia = incidencia,
                enRiesgoAlto = enAlto,
                porcentajeRiesgoAlto = pctAlto,
                nivelRiesgoPromedio = promedioNivel
            )
        }.sortedByDescending { it.totalMonitoreos }

        val plagaLider = resumenPlagas.firstOrNull()

        // Distribución de niveles de riesgo ambiental (IRA)
        val rBajo = listaMonitoreos.count { it.nivel_alerta == 0 }
        val rModerado = listaMonitoreos.count { it.nivel_alerta == 1 }
        val rAlto = listaMonitoreos.count { it.nivel_alerta >= 2 }

        CampañaHistorica(
            nombre = nombreCampaña,
            añoInicio = extraerAñoInicioCampaña(nombreCampaña),
            totalMonitoreos = totalCampaña,
            hectareasMonitoreadas = hectareasCampaña,
            plagaPredominante = plagaLider,
            plagas = resumenPlagas,
            riesgoBajo = rBajo,
            riesgoModerado = rModerado,
            riesgoAlto = rAlto
        )
    }.sortedByDescending { it.añoInicio }
}
