package com.example.plag_out

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pestañas / perspectivas del tablero fitosanitario.
 */
enum class PestañaDashboard {
    PRODUCTOR,
    AGRONOMO,
    HISTORICO
}

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
 * Semáforo y distribución de hectáreas en riesgo para el productor.
 */
data class SemaforoHectareas(
    val hectareasVerde: Float,
    val hectareasAmarillo: Float,
    val hectareasRojo: Float,
    val totalHectareas: Float,
    val lotesVerde: Int,
    val lotesAmarillo: Int,
    val lotesRojo: Int,
    val totalLotes: Int
) {
    val porcentajeVerde: Float get() = if (totalHectareas > 0f) (hectareasVerde * 100f / totalHectareas) else 0f
    val porcentajeAmarillo: Float get() = if (totalHectareas > 0f) (hectareasAmarillo * 100f / totalHectareas) else 0f
    val porcentajeRojo: Float get() = if (totalHectareas > 0f) (hectareasRojo * 100f / totalHectareas) else 0f
}

/**
 * Desglose de plagas incidentes en un cultivo.
 */
data class ParPlagaFrecuencia(
    val plagaNombre: String,
    val plagaCientifico: String?,
    val totalLotes: Int,
    val porcentaje: Float,
    val casosAlerta: Int
)

data class ResumenCultivoPlagas(
    val cultivoNombre: String,
    val cultivoCientifico: String?,
    val totalLotes: Int,
    val totalHectareas: Float,
    val plagas: List<ParPlagaFrecuencia>,
    val plagaMasFrecuente: String?,
    val lotesEnRiesgoAlto: Int,
    val fechaSiembraReciente: LocalDate? = null,
    val diasDesdeSiembra: Long? = null
)

/**
 * Lote en estado crítico o prioritario de atención para el productor.
 */
data class LoteEnRiesgoCritico(
    val monitoreoId: Int,
    val terrenoId: Int,
    val terrenoNombre: String,
    val terrenoArea: Float,
    val cultivoNombre: String,
    val plagaNombre: String,
    val plagaCientifico: String?,
    val nivelAlerta: Int,
    val progresoGdd: Float,
    val gddAcumulado: Float,
    val gddObjetivo: Float,
    val urgenciaAccion: String,
    val diasDesdeSiembra: Long?
)

/**
 * Estado fenológico de una plaga y evaluación de ventana de aplicación para el agrónomo.
 */
enum class EstadoVentanaControl {
    INCUBACION_ESPERA,
    VENTANA_OPTIMA_ABIERTA,
    VENTANA_CERRADA_AVANZADO
}

data class VentanaControlBiologico(
    val monitoreoId: Int,
    val terrenoNombre: String,
    val cultivoNombre: String,
    val plagaNombre: String,
    val plagaCientifico: String?,
    val progresoGdd: Float,
    val estadioEstimado: String,
    val enVentanaOptima: Boolean,
    val estadoVentana: EstadoVentanaControl,
    val recomendacionTecnica: String
)

/**
 * Tasa de eficacia observada tras archivar tratamientos en ciclos previos.
 */
data class EficaciaTratamientos(
    val totalTratamientosArchivados: Int,
    val tratamientosExitosos: Int,
    val tasaEficaciaPorcentaje: Float,
    val monitoreosConRebrote: Int
)

/**
 * Estado observable de la pantalla de métricas y dashboards.
 */
data class AdminMetricasUIState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val pestañaActual: PestañaDashboard = PestañaDashboard.PRODUCTOR,
    // Perspectiva Histórica
    val campañas: List<CampañaHistorica> = emptyList(),
    val campañaSeleccionada: CampañaHistorica? = null,
    val totalHectareasHistoricas: Float = 0f,
    val totalMonitoreosHistoricos: Int = 0,
    val plagaHistoricaLider: String? = null,
    // Perspectiva Productor
    val semaforoHectareas: SemaforoHectareas? = null,
    val plagasPorCultivo: List<ResumenCultivoPlagas> = emptyList(),
    val lotesCriticos: List<LoteEnRiesgoCritico> = emptyList(),
    // Perspectiva Agrónomo
    val ventanasControl: List<VentanaControlBiologico> = emptyList(),
    val eficaciaTratamientos: EficaciaTratamientos? = null,
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
 * Agrupa y procesa la lista de monitoreos históricos en campañas agrícolas.
 */
fun agruparPorCampaña(
    monitoreos: List<MonitoreoResponse>,
    terrenos: List<TerrenoResponse> = emptyList()
): List<CampañaHistorica> {
    if (monitoreos.isEmpty()) return emptyList()

    val terrenosMap = terrenos.associateBy { it.terreno_id }

    val monitoreosPorCampaña = monitoreos.groupBy { m ->
        val fecha = m.fecha_inicio ?: m.fecha_actualizacion
        determinarCampaña(fecha)
    }

    return monitoreosPorCampaña.map { (nombreCampaña, listaMonitoreos) ->
        val totalCampaña = listaMonitoreos.size

        val hectareasCampaña = listaMonitoreos
            .map { it.terreno_id }
            .distinct()
            .mapNotNull { terrenosMap[it]?.terreno_area }
            .sum()

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

/**
 * Calcula el semáforo de superficie de hectáreas en riesgo agrupando lotes por su peor nivel de alerta.
 */
fun calcularSemaforoHectareas(
    monitoreos: List<MonitoreoResponse>,
    terrenos: List<TerrenoResponse>
): SemaforoHectareas {
    val activos = monitoreos.filter { it.activo }
    val terrenosMap = terrenos.associateBy { it.terreno_id }

    val alertaPorTerreno = activos.groupBy { it.terreno_id }
        .mapValues { (_, ms) -> ms.maxOfOrNull { it.nivel_alerta } ?: 0 }

    var haVerde = 0f
    var haAmarillo = 0f
    var haRojo = 0f
    var lVerde = 0
    var lAmarillo = 0
    var lRojo = 0

    alertaPorTerreno.forEach { (terrenoId, nivel) ->
        val area = terrenosMap[terrenoId]?.terreno_area ?: 0f
        when {
            nivel >= 2 -> {
                haRojo += area
                lRojo++
            }
            nivel == 1 -> {
                haAmarillo += area
                lAmarillo++
            }
            else -> {
                haVerde += area
                lVerde++
            }
        }
    }

    val totalHa = haVerde + haAmarillo + haRojo
    val totalL = lVerde + lAmarillo + lRojo

    return SemaforoHectareas(
        hectareasVerde = haVerde,
        hectareasAmarillo = haAmarillo,
        hectareasRojo = haRojo,
        totalHectareas = totalHa,
        lotesVerde = lVerde,
        lotesAmarillo = lAmarillo,
        lotesRojo = lRojo,
        totalLotes = totalL
    )
}

/**
 * Agrupa las plagas que más afectaron a cada cultivo desde su fecha de siembra.
 */
fun calcularPlagasPorCultivo(
    monitoreos: List<MonitoreoResponse>,
    terrenos: List<TerrenoResponse> = emptyList(),
    plantaciones: List<PlantacionesResponse> = emptyList(),
    fechaReferencia: LocalDate = LocalDate.now()
): List<ResumenCultivoPlagas> {
    if (monitoreos.isEmpty()) return emptyList()

    val terrenosMap = terrenos.associateBy { it.terreno_id }
    val plantacionesPorCultivo = plantaciones.groupBy { it.cultivo_nombre.trim() }

    val porCultivo = monitoreos.groupBy { it.cultivo_nombre.trim() }

    return porCultivo.map { (cultivo, lista) ->
        val lotesUnicos = lista.map { it.terreno_id }.distinct()
        val totalHa = lotesUnicos.mapNotNull { terrenosMap[it]?.terreno_area }.sum()
        val totalLotes = lotesUnicos.size

        val plantacionInfo = plantacionesPorCultivo[cultivo]?.firstOrNull()
        val fechaSiembra = plantacionesPorCultivo[cultivo]?.map { it.fecha_siembra }?.maxOrNull()
        val diasSiembra = fechaSiembra?.let { ChronoUnit.DAYS.between(it, fechaReferencia).coerceAtLeast(0) }

        val porPlaga = lista.groupBy { it.plaga_nombre.trim() }
        val plagasDesglose = porPlaga.map { (plaga, items) ->
            val lotesPlaga = items.map { it.terreno_id }.distinct().size
            val pct = if (totalLotes > 0) (lotesPlaga * 100f / totalLotes) else 0f
            val enAlerta = items.count { it.nivel_alerta >= 2 }
            val cientifico = items.firstNotNullOfOrNull { it.plaga_nombre_cientifico }

            ParPlagaFrecuencia(
                plagaNombre = plaga,
                plagaCientifico = cientifico,
                totalLotes = lotesPlaga,
                porcentaje = pct,
                casosAlerta = enAlerta
            )
        }.sortedByDescending { it.totalLotes }

        val plagaLider = plagasDesglose.firstOrNull()?.plagaNombre
        val enAlertaTotal = lista.count { it.nivel_alerta >= 2 }

        ResumenCultivoPlagas(
            cultivoNombre = cultivo,
            cultivoCientifico = plantacionInfo?.cultivo_nombre_cientifico,
            totalLotes = totalLotes,
            totalHectareas = totalHa,
            plagas = plagasDesglose,
            plagaMasFrecuente = plagaLider,
            lotesEnRiesgoAlto = enAlertaTotal,
            fechaSiembraReciente = fechaSiembra,
            diasDesdeSiembra = diasSiembra
        )
    }.sortedByDescending { it.totalLotes }
}

/**
 * Obtiene el ranking de lotes en riesgo crítico y urgente para el productor.
 */
fun calcularRankingLotesCriticos(
    monitoreos: List<MonitoreoResponse>,
    terrenos: List<TerrenoResponse> = emptyList(),
    plantaciones: List<PlantacionesResponse> = emptyList(),
    fechaReferencia: LocalDate = LocalDate.now()
): List<LoteEnRiesgoCritico> {
    val activos = monitoreos.filter { it.activo }
    val terrenosMap = terrenos.associateBy { it.terreno_id }
    val plantacionesMap = plantaciones.associateBy { it.plantacion_id }

    return activos.map { m ->
        val area = terrenosMap[m.terreno_id]?.terreno_area ?: 0f
        val plantacion = plantacionesMap[m.plantacion_id]
        val diasSiembra = plantacion?.fecha_siembra?.let {
            ChronoUnit.DAYS.between(it, fechaReferencia).coerceAtLeast(0)
        }

        val urgencia = when {
            m.nivel_alerta >= 2 -> "⚠️ Superó umbral - Aplicar / Revisar hoy"
            m.progreso >= 80f -> "👀 Eclosión inminente (${m.progreso.toInt()}% térmico)"
            m.progreso >= 45f -> "🌱 Ventana activa de desarrollo"
            else -> "✅ Población controlada"
        }

        LoteEnRiesgoCritico(
            monitoreoId = m.monitoreo_id,
            terrenoId = m.terreno_id,
            terrenoNombre = m.terreno_nombre,
            terrenoArea = area,
            cultivoNombre = m.cultivo_nombre,
            plagaNombre = m.plaga_nombre,
            plagaCientifico = m.plaga_nombre_cientifico,
            nivelAlerta = m.nivel_alerta,
            progresoGdd = m.progreso,
            gddAcumulado = m.gdd_acumulado,
            gddObjetivo = m.gdd_objetivo,
            urgenciaAccion = urgencia,
            diasDesdeSiembra = diasSiembra
        )
    }.sortedWith(
        compareByDescending<LoteEnRiesgoCritico> { it.nivelAlerta }
            .thenByDescending { it.progresoGdd }
    )
}

/**
 * Determina las ventanas óptimas de aplicación fitosanitaria para el ingeniero agrónomo.
 */
fun calcularVentanasControlAgronomo(
    monitoreos: List<MonitoreoResponse>
): List<VentanaControlBiologico> {
    val activos = monitoreos.filter { it.activo }

    return activos.map { m ->
        val cicloActivo = m.ciclos?.lastOrNull()
        val estadioReal = cicloActivo?.estadio_biologico?.takeIf { it.isNotBlank() }

        val (estadio, estado, recomendacion, enOptima) = when {
            m.progreso < 40f -> Quad(
                estadioReal ?: "Incubación (Huevo)",
                EstadoVentanaControl.INCUBACION_ESPERA,
                "No pulverizar aún: plaga en fase de huevo o no susceptible.",
                false
            )
            m.progreso <= 85f -> Quad(
                estadioReal ?: "Larva chica (L1 - L3)",
                EstadoVentanaControl.VENTANA_OPTIMA_ABIERTA,
                "Ventana óptima de control: máxima susceptibilidad a insecticidas y biológicos.",
                true
            )
            else -> Quad(
                estadioReal ?: "Larva avanzada / Adulto",
                EstadoVentanaControl.VENTANA_CERRADA_AVANZADO,
                "Ventana en cierre: riesgo de daño económico consolidado en el cultivo.",
                false
            )
        }

        VentanaControlBiologico(
            monitoreoId = m.monitoreo_id,
            terrenoNombre = m.terreno_nombre,
            cultivoNombre = m.cultivo_nombre,
            plagaNombre = m.plaga_nombre,
            plagaCientifico = m.plaga_nombre_cientifico,
            progresoGdd = m.progreso,
            estadioEstimado = estadio,
            enVentanaOptima = enOptima,
            estadoVentana = estado,
            recomendacionTecnica = recomendacion
        )
    }.sortedWith(
        compareByDescending<VentanaControlBiologico> { it.enVentanaOptima }
            .thenByDescending { it.progresoGdd }
    )
}

/**
 * Evalúa la eficacia de tratamientos a partir de ciclos archivados y rebrotes.
 */
fun calcularEficaciaTratamientos(
    monitoreos: List<MonitoreoResponse>
): EficaciaTratamientos {
    var archivados = 0
    var exitosos = 0
    var rebrotes = 0

    monitoreos.forEach { m ->
        val ciclos = m.ciclos.orEmpty()
        val ciclosArchivados = ciclos.filter { it.estado.equals("archivado", ignoreCase = true) }
        archivados += ciclosArchivados.size

        if (ciclosArchivados.isNotEmpty()) {
            if (m.nivel_alerta < 2) {
                exitosos += ciclosArchivados.size
            } else {
                rebrotes++
            }
        }
    }

    val tasa = if (archivados > 0) (exitosos * 100f / archivados) else 100f

    return EficaciaTratamientos(
        totalTratamientosArchivados = archivados,
        tratamientosExitosos = exitosos,
        tasaEficaciaPorcentaje = tasa,
        monitoreosConRebrote = rebrotes
    )
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
