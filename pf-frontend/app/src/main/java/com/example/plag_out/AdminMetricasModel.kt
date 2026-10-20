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
 * DTOs para la respuesta del endpoint dedicado de dashboards /dashboards/metricas.
 */
data class DashboardMetricasResponse(
    val total_hectareas_historicas: Float = 0f,
    val total_monitoreos_historicos: Int = 0,
    val plaga_historica_lider: String? = null,
    val semaforo_hectareas: SemaforoHectareasDTO? = null,
    val plagas_por_cultivo: List<ResumenCultivoPlagasDTO> = emptyList(),
    val lotes_criticos: List<LoteEnRiesgoCriticoDTO> = emptyList(),
    val ventanas_control: List<VentanaControlBiologicoDTO> = emptyList(),
    val eficacia_tratamientos: EficaciaTratamientosDTO? = null,
    val campanas: List<CampañaHistoricaDTO> = emptyList()
)

data class SemaforoHectareasDTO(
    val hectareas_verde: Float = 0f,
    val hectareas_amarillo: Float = 0f,
    val hectareas_rojo: Float = 0f,
    val total_hectareas: Float = 0f,
    val lotes_verde: Int = 0,
    val lotes_amarillo: Int = 0,
    val lotes_rojo: Int = 0,
    val total_lotes: Int = 0
) {
    fun toDomain() = SemaforoHectareas(
        hectareasVerde = hectareas_verde,
        hectareasAmarillo = hectareas_amarillo,
        hectareasRojo = hectareas_rojo,
        totalHectareas = total_hectareas,
        lotesVerde = lotes_verde,
        lotesAmarillo = lotes_amarillo,
        lotesRojo = lotes_rojo,
        totalLotes = total_lotes
    )
}

data class ParPlagaFrecuenciaDTO(
    val plaga_nombre: String,
    val plaga_cientifico: String? = null,
    val total_lotes: Int = 0,
    val porcentaje: Float = 0f,
    val casos_alerta: Int = 0
) {
    fun toDomain() = ParPlagaFrecuencia(
        plagaNombre = plaga_nombre,
        plagaCientifico = plaga_cientifico,
        totalLotes = total_lotes,
        porcentaje = porcentaje,
        casosAlerta = casos_alerta
    )
}

data class ResumenCultivoPlagasDTO(
    val cultivo_nombre: String,
    val cultivo_cientifico: String? = null,
    val total_lotes: Int = 0,
    val total_hectareas: Float = 0f,
    val plagas: List<ParPlagaFrecuenciaDTO> = emptyList(),
    val plaga_mas_frecuente: String? = null,
    val lotes_en_riesgo_alto: Int = 0,
    val dias_desde_siembra: Long? = null
) {
    fun toDomain() = ResumenCultivoPlagas(
        cultivoNombre = cultivo_nombre,
        cultivoCientifico = cultivo_cientifico,
        totalLotes = total_lotes,
        totalHectareas = total_hectareas,
        plagas = plagas.map { it.toDomain() },
        plagaMasFrecuente = plaga_mas_frecuente,
        lotesEnRiesgoAlto = lotes_en_riesgo_alto,
        diasDesdeSiembra = dias_desde_siembra
    )
}

data class LoteEnRiesgoCriticoDTO(
    val monitoreo_id: Int,
    val terreno_id: Int,
    val terreno_nombre: String,
    val terreno_area: Float,
    val cultivo_nombre: String,
    val plaga_nombre: String,
    val plaga_cientifico: String? = null,
    val nivel_alerta: Int,
    val progreso_gdd: Float,
    val gdd_acumulado: Float,
    val gdd_objetivo: Float,
    val urgencia_accion: String,
    val dias_desde_siembra: Long? = null
) {
    fun toDomain() = LoteEnRiesgoCritico(
        monitoreoId = monitoreo_id,
        terrenoId = terreno_id,
        terrenoNombre = terreno_nombre,
        terrenoArea = terreno_area,
        cultivoNombre = cultivo_nombre,
        plagaNombre = plaga_nombre,
        plagaCientifico = plaga_cientifico,
        nivelAlerta = nivel_alerta,
        progresoGdd = progreso_gdd,
        gddAcumulado = gdd_acumulado,
        gddObjetivo = gdd_objetivo,
        urgenciaAccion = urgencia_accion,
        diasDesdeSiembra = dias_desde_siembra
    )
}

data class VentanaControlBiologicoDTO(
    val monitoreo_id: Int,
    val terreno_nombre: String,
    val cultivo_nombre: String,
    val plaga_nombre: String,
    val plaga_cientifico: String? = null,
    val progreso_gdd: Float,
    val estadio_estimado: String,
    val en_ventana_optima: Boolean,
    val estado_ventana: String,
    val recomendacion_tecnica: String
) {
    fun toDomain() = VentanaControlBiologico(
        monitoreoId = monitoreo_id,
        terrenoNombre = terreno_nombre,
        cultivoNombre = cultivo_nombre,
        plagaNombre = plaga_nombre,
        plagaCientifico = plaga_cientifico,
        progresoGdd = progreso_gdd,
        estadioEstimado = estadio_estimado,
        enVentanaOptima = en_ventana_optima,
        estadoVentana = try { EstadoVentanaControl.valueOf(estado_ventana) } catch (e: Exception) { EstadoVentanaControl.INCUBACION_ESPERA },
        recomendacionTecnica = recomendacion_tecnica
    )
}

data class EficaciaTratamientosDTO(
    val total_tratamientos_archivados: Int = 0,
    val tratamientos_exitosos: Int = 0,
    val tasa_eficacia_porcentaje: Float = 0f,
    val monitoreos_con_rebrote: Int = 0
) {
    fun toDomain() = EficaciaTratamientos(
        totalTratamientosArchivados = total_tratamientos_archivados,
        tratamientosExitosos = tratamientos_exitosos,
        tasaEficaciaPorcentaje = tasa_eficacia_porcentaje,
        monitoreosConRebrote = monitoreos_con_rebrote
    )
}

data class ResumenPlagaCampañaDTO(
    val plaga_nombre: String,
    val plaga_cientifico: String? = null,
    val total_monitoreos: Int = 0,
    val porcentaje_incidencia: Float = 0f,
    val en_riesgo_alto: Int = 0,
    val porcentaje_riesgo_alto: Float = 0f,
    val nivel_riesgo_promedio: Float = 0f
) {
    fun toDomain() = ResumenPlagaCampaña(
        plagaNombre = plaga_nombre,
        plagaCientifico = plaga_cientifico,
        totalMonitoreos = total_monitoreos,
        porcentajeIncidencia = porcentaje_incidencia,
        enRiesgoAlto = en_riesgo_alto,
        porcentajeRiesgoAlto = porcentaje_riesgo_alto,
        nivelRiesgoPromedio = nivel_riesgo_promedio
    )
}

data class CampañaHistoricaDTO(
    val nombre: String,
    val ano_inicio: Int,
    val total_monitoreos: Int = 0,
    val hectareas_monitoreadas: Float = 0f,
    val plaga_predominante: ResumenPlagaCampañaDTO? = null,
    val plagas: List<ResumenPlagaCampañaDTO> = emptyList(),
    val riesgo_bajo: Int = 0,
    val riesgo_moderado: Int = 0,
    val riesgo_alto: Int = 0
) {
    fun toDomain() = CampañaHistorica(
        nombre = nombre,
        añoInicio = ano_inicio,
        totalMonitoreos = total_monitoreos,
        hectareasMonitoreadas = hectareas_monitoreadas,
        plagaPredominante = plaga_predominante?.toDomain(),
        plagas = plagas.map { it.toDomain() },
        riesgoBajo = riesgo_bajo,
        riesgoModerado = riesgo_moderado,
        riesgoAlto = riesgo_alto
    )
}

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

        val urgencia = when {
            nivelAlerta >= 2 -> "⚠️ Superó umbral - Aplicar / Revisar hoy"
            progreso >= 80f -> "👀 Eclosión inminente (${progreso.toInt()}% térmico)"
            progreso >= 45f -> "🌱 Ventana activa de desarrollo"
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
                "No pulverizar aún: plaga en fase de huevo o no susceptible.",
                false
            )
            progreso <= 85f -> Quad(
                estadioReal ?: "Larva chica (L1 - L3)",
                EstadoVentanaControl.VENTANA_OPTIMA_ABIERTA,
                "Ventana óptima de control: máxima susceptibilidad a insecticidas y biológicos.",
                true
            )
            else -> Quad(
                estadioReal ?: "Larva avanzada / Adulto",
                EstadoVentanaControl.VENTANA_CERRADA_AVANZADO,
                "Ventana en cierre: superó el umbral térmico de eclosión y la plaga está en fase avanzada.",
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

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
