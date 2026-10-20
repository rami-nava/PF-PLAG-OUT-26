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
    // null: sin ciclos archivados la tasa es desconocida (solo en las métricas globales).
    val tasaEficaciaPorcentaje: Float?,
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
            val enAlto = items.count { nivelAlertaResuelto(it) >= 2 }
            val pctAlto = if (totalPlaga > 0) (enAlto * 100f / totalPlaga) else 0f
            val promedioNivel = if (totalPlaga > 0) items.map { nivelAlertaResuelto(it).coerceAtLeast(0) }.average().toFloat() else 0f
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

        val rBajo = listaMonitoreos.count { nivelAlertaResuelto(it) <= 0 }
        val rModerado = listaMonitoreos.count { nivelAlertaResuelto(it) == 1 }
        val rAlto = listaMonitoreos.count { nivelAlertaResuelto(it) >= 2 }

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
        .mapValues { (_, ms) -> ms.maxOfOrNull { nivelAlertaResuelto(it) } ?: 0 }

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

    // Preferir monitoreos activos de la campaña en curso
    val monitoreosParaAnalisis = monitoreos.filter { it.activo }.ifEmpty { monitoreos }
    val porCultivo = monitoreosParaAnalisis.groupBy { it.cultivo_nombre.trim() }

    return porCultivo.map { (cultivo, lista) ->
        val lotesUnicos = lista.map { it.terreno_id }.distinct()
        val totalHa = lotesUnicos.mapNotNull { terrenosMap[it]?.terreno_area }.sum()
        val totalLotes = lotesUnicos.size

        val plantacionesDelCultivo = plantacionesPorCultivo[cultivo].orEmpty()
        val plantacionInfo = plantacionesDelCultivo.firstOrNull()
        val plantacionesActivas = plantacionesDelCultivo.filter { it.activa }
        val fechaSiembra = plantacionesActivas.map { it.fecha_siembra }.minOrNull()
            ?: plantacionesDelCultivo.map { it.fecha_siembra }.maxOrNull()
        val diasSiembra = fechaSiembra?.let { ChronoUnit.DAYS.between(it, fechaReferencia).coerceAtLeast(0) }

        val porPlaga = lista.groupBy { it.plaga_nombre.trim() }
        val plagasDesglose = porPlaga.map { (plaga, items) ->
            val lotesPlaga = items.map { it.terreno_id }.distinct().size
            val pct = if (totalLotes > 0) (lotesPlaga * 100f / totalLotes) else 0f
            val enAlerta = items.count { nivelAlertaResuelto(it) >= 2 }
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
        val enAlertaTotal = lista.count { nivelAlertaResuelto(it) >= 2 }

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

        val nivelAlerta = nivelAlertaResuelto(m)
        val progreso = progresoResuelto(m)
        val gddAcumulado = gddAcumuladoResuelto(m)

        val urgencia = textoUrgencia(
            when {
                nivelAlerta >= 2 -> "supero_umbral"
                progreso >= 80f -> "eclosion_inminente"
                progreso >= 45f -> "ventana_activa"
                else -> "controlada"
            },
            progreso
        )

        LoteEnRiesgoCritico(
            monitoreoId = m.monitoreo_id,
            terrenoId = m.terreno_id,
            terrenoNombre = m.terreno_nombre,
            terrenoArea = area,
            cultivoNombre = m.cultivo_nombre,
            plagaNombre = m.plaga_nombre,
            plagaCientifico = m.plaga_nombre_cientifico,
            nivelAlerta = nivelAlerta,
            progresoGdd = progreso,
            gddAcumulado = gddAcumulado,
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
        val progreso = progresoResuelto(m)
        val cicloActivo = m.ciclos?.firstOrNull { it.estado.equals("activo", ignoreCase = true) }
            ?: m.ciclos?.lastOrNull()
        val estadioReal = cicloActivo?.estadio_biologico?.takeIf { it.isNotBlank() }

        val (estadio, estado, recomendacion, enOptima) = when {
            progreso < 40f -> Quad(
                estadioReal ?: "Incubación (Huevo)",
                EstadoVentanaControl.INCUBACION_ESPERA,
                recomendacion(EstadoVentanaControl.INCUBACION_ESPERA),
                false
            )
            progreso <= 85f -> Quad(
                estadioReal ?: "Larva chica (L1 - L3)",
                EstadoVentanaControl.VENTANA_OPTIMA_ABIERTA,
                recomendacion(EstadoVentanaControl.VENTANA_OPTIMA_ABIERTA),
                true
            )
            else -> Quad(
                estadioReal ?: "Larva avanzada / Adulto",
                EstadoVentanaControl.VENTANA_CERRADA_AVANZADO,
                recomendacion(EstadoVentanaControl.VENTANA_CERRADA_AVANZADO),
                false
            )
        }

        VentanaControlBiologico(
            monitoreoId = m.monitoreo_id,
            terrenoNombre = m.terreno_nombre,
            cultivoNombre = m.cultivo_nombre,
            plagaNombre = m.plaga_nombre,
            plagaCientifico = m.plaga_nombre_cientifico,
            progresoGdd = progreso,
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
            if (nivelAlertaResuelto(m) < 2) {
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

/**
 * Extrae el nivel de alerta real de un monitoreo a partir de su ciclo activo o archivado más reciente.
 */
fun nivelAlertaResuelto(monitoreo: MonitoreoResponse): Int {
    val ciclo = monitoreo.ciclos?.firstOrNull { it.estado.equals("activo", ignoreCase = true) }
        ?: monitoreo.ciclos?.lastOrNull()
    return ciclo?.nivel_alerta ?: monitoreo.nivel_alerta
}

/**
 * Extrae el porcentaje de progreso térmico real del monitoreo a partir de su ciclo activo o archivado.
 */
fun progresoResuelto(monitoreo: MonitoreoResponse): Float {
    val ciclo = monitoreo.ciclos?.firstOrNull { it.estado.equals("activo", ignoreCase = true) }
        ?: monitoreo.ciclos?.lastOrNull()
    return ciclo?.progreso ?: monitoreo.progreso
}

/**
 * Extrae el acumulado térmico GDD real del monitoreo a partir de su ciclo activo o archivado.
 */
fun gddAcumuladoResuelto(monitoreo: MonitoreoResponse): Float {
    val ciclo = monitoreo.ciclos?.firstOrNull { it.estado.equals("activo", ignoreCase = true) }
        ?: monitoreo.ciclos?.lastOrNull()
    return ciclo?.gdd_acumulado ?: monitoreo.gdd_acumulado
}

// ── Métricas globales del admin (GET /admin/metricas) ───────────────────────
// El backend ya las calcula sobre todos los productores; acá solo se pasan a los modelos
// de la pantalla. Los lotes llegan anónimos, así que se numeran en el orden del ranking.

fun textoUrgencia(urgencia: String, progreso: Float): String = when (urgencia) {
    "supero_umbral" -> "⚠️ Superó umbral - Aplicar / Revisar hoy"
    "eclosion_inminente" -> "👀 Eclosión inminente (${progreso.toInt()}% térmico)"
    "ventana_activa" -> "🌱 Ventana activa de desarrollo"
    else -> "✅ Población controlada"
}

private fun estadoVentana(estado: String): EstadoVentanaControl = when (estado) {
    "ventana_optima" -> EstadoVentanaControl.VENTANA_OPTIMA_ABIERTA
    "ventana_cerrada" -> EstadoVentanaControl.VENTANA_CERRADA_AVANZADO
    else -> EstadoVentanaControl.INCUBACION_ESPERA
}

private fun recomendacion(estado: EstadoVentanaControl): String = when (estado) {
    EstadoVentanaControl.INCUBACION_ESPERA ->
        "No pulverizar aún: plaga en fase de huevo o no susceptible."
    EstadoVentanaControl.VENTANA_OPTIMA_ABIERTA ->
        "Ventana óptima de control: máxima susceptibilidad a insecticidas y biológicos."
    EstadoVentanaControl.VENTANA_CERRADA_AVANZADO ->
        "Ventana en cierre: superó el umbral térmico de eclosión y la plaga está en fase avanzada."
}

private fun nombreLoteAnonimo(posicion: Int) = "Lote anónimo ${posicion + 1}"

fun AdminMetricasUIState.conMetricasGlobales(metricas: MetricasAdmin): AdminMetricasUIState {
    val campañas = metricas.campanias.map { campaña ->
        val plagas = campaña.plagas.map { plaga ->
            ResumenPlagaCampaña(
                plagaNombre = plaga.plaga,
                plagaCientifico = plaga.plaga_cientifico,
                totalMonitoreos = plaga.monitoreos,
                porcentajeIncidencia = plaga.incidencia,
                enRiesgoAlto = plaga.riesgo_alto,
                // Sin monitoreos medidos no hay porcentaje: se muestra 0.
                porcentajeRiesgoAlto = plaga.porcentaje_riesgo_alto ?: 0f,
                nivelRiesgoPromedio = plaga.nivel_promedio ?: 0f
            )
        }
        CampañaHistorica(
            nombre = campaña.nombre,
            añoInicio = campaña.anio_inicio,
            totalMonitoreos = campaña.monitoreos,
            hectareasMonitoreadas = campaña.hectareas,
            plagaPredominante = plagas.firstOrNull(),
            plagas = plagas,
            riesgoBajo = campaña.riesgo_bajo,
            riesgoModerado = campaña.riesgo_moderado,
            riesgoAlto = campaña.riesgo_alto
        )
    }
    val semaforo = metricas.semaforo
    return copy(
        campañas = campañas,
        campañaSeleccionada = campañas.firstOrNull { it.nombre == campañaSeleccionada?.nombre }
            ?: campañas.firstOrNull(),
        totalHectareasHistoricas = metricas.historico.hectareas,
        totalMonitoreosHistoricos = metricas.historico.monitoreos,
        plagaHistoricaLider = metricas.historico.plaga_lider,
        semaforoHectareas = SemaforoHectareas(
            hectareasVerde = semaforo.verde.hectareas,
            hectareasAmarillo = semaforo.amarillo.hectareas,
            hectareasRojo = semaforo.rojo.hectareas,
            totalHectareas = semaforo.total_hectareas,
            lotesVerde = semaforo.verde.lotes,
            lotesAmarillo = semaforo.amarillo.lotes,
            lotesRojo = semaforo.rojo.lotes,
            totalLotes = semaforo.total_lotes
        ),
        plagasPorCultivo = metricas.cultivos.map { cultivo ->
            ResumenCultivoPlagas(
                cultivoNombre = cultivo.cultivo,
                cultivoCientifico = cultivo.cultivo_cientifico,
                totalLotes = cultivo.lotes,
                totalHectareas = cultivo.hectareas,
                plagas = cultivo.plagas.map { plaga ->
                    ParPlagaFrecuencia(
                        plagaNombre = plaga.plaga,
                        plagaCientifico = plaga.plaga_cientifico,
                        totalLotes = plaga.lotes,
                        porcentaje = plaga.porcentaje,
                        casosAlerta = plaga.casos_alerta
                    )
                },
                plagaMasFrecuente = cultivo.plaga_mas_frecuente,
                lotesEnRiesgoAlto = cultivo.monitoreos_riesgo_alto,
                fechaSiembraReciente = cultivo.fecha_siembra,
                diasDesdeSiembra = cultivo.dias_desde_siembra
            )
        },
        lotesCriticos = metricas.lotes_criticos.mapIndexed { posicion, lote ->
            LoteEnRiesgoCritico(
                // Sin ids reales: los lotes son anónimos. Negativos para no chocar con ninguno.
                monitoreoId = -(posicion + 1),
                terrenoId = -(posicion + 1),
                terrenoNombre = nombreLoteAnonimo(posicion),
                terrenoArea = lote.hectareas,
                cultivoNombre = lote.cultivo,
                plagaNombre = lote.plaga,
                plagaCientifico = lote.plaga_cientifico,
                nivelAlerta = lote.nivel_alerta,
                progresoGdd = lote.progreso,
                gddAcumulado = lote.gdd_acumulado,
                gddObjetivo = lote.gdd_objetivo,
                urgenciaAccion = textoUrgencia(lote.urgencia, lote.progreso),
                diasDesdeSiembra = lote.dias_desde_siembra
            )
        },
        ventanasControl = metricas.ventanas.mapIndexed { posicion, ventana ->
            val estado = estadoVentana(ventana.estado)
            VentanaControlBiologico(
                monitoreoId = -(posicion + 1),
                terrenoNombre = "Lote anónimo",
                cultivoNombre = ventana.cultivo,
                plagaNombre = ventana.plaga,
                plagaCientifico = ventana.plaga_cientifico,
                progresoGdd = ventana.progreso,
                estadioEstimado = ventana.estadio,
                enVentanaOptima = estado == EstadoVentanaControl.VENTANA_OPTIMA_ABIERTA,
                estadoVentana = estado,
                recomendacionTecnica = recomendacion(estado)
            )
        },
        eficaciaTratamientos = EficaciaTratamientos(
            totalTratamientosArchivados = metricas.eficacia.ciclos_archivados,
            tratamientosExitosos = metricas.eficacia.exitosos,
            tasaEficaciaPorcentaje = metricas.eficacia.tasa,
            monitoreosConRebrote = metricas.eficacia.monitoreos_con_rebrote
        )
    )
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
