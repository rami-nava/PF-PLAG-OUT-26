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
    val nivelRiesgoPromedio: Float,
    val cultivosAfectados: List<String> = emptyList()
)

/**
 * Desglose de cantidad de monitoreos de una plaga dentro de un cultivo en una campaña.
 */
data class ResumenPlagaEnCultivoCampaña(
    val plagaNombre: String,
    val plagaCientifico: String? = null,
    val totalMonitoreos: Int,
    val porcentaje: Float
)

/**
 * Total de monitoreos de un cultivo y su desglose por plaga dentro de una campaña agrícola
 * (incluye tanto monitoreos activos como finalizados).
 */
data class ResumenCultivoCampaña(
    val cultivoNombre: String,
    val cultivoCientifico: String? = null,
    val totalMonitoreos: Int,
    val plagas: List<ResumenPlagaEnCultivoCampaña> = emptyList()
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
    val riesgoAlto: Int,
    val cultivos: List<ResumenCultivoCampaña> = emptyList()
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
    val totalLotes: Int,
    // Fuera del gráfico y de los totales, como en el panel.
    val lotesEsperandoBiofix: Int = 0,
    val hectareasEsperandoBiofix: Float = 0f,
    val lotesSinMonitoreo: Int = 0,
    val hectareasSinMonitoreo: Float = 0f
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
    val diasDesdeSiembra: Long?
)

/**
 * Etapa térmica de la plaga en un lote: es el nivel de alerta del ciclo (0 huevo, 1 eclosión
 * cerca, 2 eclosionó), calculado por el backend con parámetros citados. No hay "ventanas" por
 * porcentaje ni recomendación de aplicar: eso se decide monitoreando el lote.
 */
enum class EstadoVentanaControl {
    HUEVO,
    ECLOSION_CERCA,
    ECLOSIONADA
}

fun etapaDeNivel(nivelAlerta: Int): EstadoVentanaControl = when {
    nivelAlerta >= 2 -> EstadoVentanaControl.ECLOSIONADA
    nivelAlerta == 1 -> EstadoVentanaControl.ECLOSION_CERCA
    else -> EstadoVentanaControl.HUEVO
}

data class VentanaControlBiologico(
    val monitoreoId: Int,
    val terrenoNombre: String,
    val cultivoNombre: String,
    val plagaNombre: String,
    val plagaCientifico: String?,
    val progresoGdd: Float,
    val estadioEstimado: String,
    /** Ya eclosionó: toca recorrer el lote. */
    val requiereRecorrida: Boolean,
    val estadoVentana: EstadoVentanaControl,
    val recomendacionTecnica: String
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
    // Solo en las métricas globales: los del productor se ven en el detalle de cada monitoreo.
    val tratamientos: TratamientosAdmin? = null,
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
        val fecha = m.fecha_inicio ?: LocalDate.now()
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
            val enAlto = items.count { (nivelAlertaResuelto(it) ?: -1) >= 2 }
            val pctAlto = if (totalPlaga > 0) (enAlto * 100f / totalPlaga) else 0f
            val niveles = items.mapNotNull { nivelAlertaResuelto(it) }
            val promedioNivel = if (niveles.isNotEmpty()) niveles.average().toFloat() else 0f
            val nombreCientifico = items.firstNotNullOfOrNull { it.plaga_nombre_cientifico }
            val cultivosAfectados = items
                .map { it.cultivo_nombre.trim() }
                .filter { it.isNotEmpty() }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { it.key }

            ResumenPlagaCampaña(
                plagaNombre = nombrePlaga,
                plagaCientifico = nombreCientifico,
                totalMonitoreos = totalPlaga,
                porcentajeIncidencia = incidencia,
                enRiesgoAlto = enAlto,
                porcentajeRiesgoAlto = pctAlto,
                nivelRiesgoPromedio = promedioNivel,
                cultivosAfectados = cultivosAfectados
            )
        }.sortedWith(compareByDescending<ResumenPlagaCampaña> { it.totalMonitoreos }.thenBy { it.plagaNombre })

        val resumenCultivos = listaMonitoreos
            .filter { it.cultivo_nombre.isNotBlank() }
            .groupBy { it.cultivo_nombre.trim() }
            .map { (nombreCultivo, msCultivo) ->
                val totalMsCultivo = msCultivo.size
                val plagasDelCultivo = msCultivo
                    .groupBy { it.plaga_nombre.trim().ifEmpty { "Sin especificar" } }
                    .map { (nombrePlaga, msPlagaCultivo) ->
                        val cientifico = msPlagaCultivo.firstNotNullOfOrNull { it.plaga_nombre_cientifico }
                        ResumenPlagaEnCultivoCampaña(
                            plagaNombre = nombrePlaga,
                            plagaCientifico = cientifico,
                            totalMonitoreos = msPlagaCultivo.size,
                            porcentaje = if (totalMsCultivo > 0) (msPlagaCultivo.size * 100f / totalMsCultivo) else 0f
                        )
                    }
                    .sortedWith(compareByDescending<ResumenPlagaEnCultivoCampaña> { it.totalMonitoreos }.thenBy { it.plagaNombre })

                ResumenCultivoCampaña(
                    cultivoNombre = nombreCultivo,
                    totalMonitoreos = totalMsCultivo,
                    plagas = plagasDelCultivo
                )
            }
            .sortedWith(compareByDescending<ResumenCultivoCampaña> { it.totalMonitoreos }.thenBy { it.cultivoNombre })

        val plagaLider = resumenPlagas.firstOrNull()

        // Los que esperan el biofix no tienen nivel: no cuentan en ningún riesgo.
        val rBajo = listaMonitoreos.count { nivelAlertaResuelto(it) == 0 }
        val rModerado = listaMonitoreos.count { nivelAlertaResuelto(it) == 1 }
        val rAlto = listaMonitoreos.count { (nivelAlertaResuelto(it) ?: -1) >= 2 }

        CampañaHistorica(
            nombre = nombreCampaña,
            añoInicio = extraerAñoInicioCampaña(nombreCampaña),
            totalMonitoreos = totalCampaña,
            hectareasMonitoreadas = hectareasCampaña,
            plagaPredominante = plagaLider,
            plagas = resumenPlagas,
            riesgoBajo = rBajo,
            riesgoModerado = rModerado,
            riesgoAlto = rAlto,
            cultivos = resumenCultivos
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

    // Lotes cuyos monitoreos esperan todos el biofix no tienen nivel: quedan fuera del semáforo.
    val alertaPorTerreno = activos.groupBy { it.terreno_id }
        .mapNotNull { (terreno, ms) -> ms.mapNotNull(::nivelAlertaVigente).maxOrNull()?.let { terreno to it } }
        .toMap()

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

    // Lo que queda fuera del gráfico, como en el panel: lotes con monitoreos activos que
    // esperan todos el biofix, y lotes registrados sin ningún monitoreo activo.
    val conMonitoreo = activos.map { it.terreno_id }.toSet()
    val esperando = conMonitoreo - alertaPorTerreno.keys
    val sinMonitoreo = terrenos.filter { it.terreno_id !in conMonitoreo }

    return SemaforoHectareas(
        hectareasVerde = haVerde,
        hectareasAmarillo = haAmarillo,
        hectareasRojo = haRojo,
        totalHectareas = totalHa,
        lotesVerde = lVerde,
        lotesAmarillo = lAmarillo,
        lotesRojo = lRojo,
        totalLotes = totalL,
        lotesEsperandoBiofix = esperando.size,
        hectareasEsperandoBiofix = esperando.sumOf { (terrenosMap[it]?.terreno_area ?: 0f).toDouble() }.toFloat(),
        lotesSinMonitoreo = sinMonitoreo.size,
        hectareasSinMonitoreo = sinMonitoreo.sumOf { it.terreno_area.toDouble() }.toFloat()
    )
}

/** Cuántos monitoreos activos hay en cada etapa térmica (Control del panel). */
fun conteoPorEtapa(ventanas: List<VentanaControlBiologico>): Map<EstadoVentanaControl, Int> =
    EstadoVentanaControl.entries.associateWith { etapa -> ventanas.count { it.estadoVentana == etapa } }

/** Texto de cada etapa, el mismo del panel (VENTANA en src/lib/metricas.ts). */
fun textoDeEtapa(etapa: EstadoVentanaControl): String = recomendacion(etapa)

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
            val enAlerta = items.count { (nivelAlertaResuelto(it) ?: -1) >= 2 }
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
        val enAlertaTotal = lista.count { (nivelAlertaResuelto(it) ?: -1) >= 2 }

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
    val activos = monitoreos.filter { it.activo && cicloVigente(it) != null }
    val terrenosMap = terrenos.associateBy { it.terreno_id }
    val plantacionesMap = plantaciones.associateBy { it.plantacion_id }

    return activos.map { m ->
        val area = terrenosMap[m.terreno_id]?.terreno_area ?: 0f
        val plantacion = plantacionesMap[m.plantacion_id]
        val diasSiembra = plantacion?.fecha_siembra?.let {
            ChronoUnit.DAYS.between(it, fechaReferencia).coerceAtLeast(0)
        }

        val ciclo = cicloVigente(m)
        val nivelAlerta = ciclo?.nivel_alerta ?: 0
        val progreso = ciclo?.progreso ?: 0f
        val gddAcumulado = ciclo?.gdd_acumulado ?: 0f

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
            diasDesdeSiembra = diasSiembra
        )
    }.sortedWith(
        compareByDescending<LoteEnRiesgoCritico> { it.nivelAlerta }
            .thenByDescending { it.progresoGdd }
    )
}

/**
 * Etapa térmica de cada lote activo, con lo eclosionado primero.
 */
fun calcularVentanasControlAgronomo(
    monitoreos: List<MonitoreoResponse>
): List<VentanaControlBiologico> {
    val activos = monitoreos.filter { it.activo && cicloVigente(it) != null }

    return activos.map { m ->
        val cicloActivo = cicloVigente(m)
        val progreso = cicloActivo?.progreso ?: 0f
        val estadioReal = cicloActivo?.estadio_biologico?.takeIf { it.isNotBlank() }

        val estado = etapaDeNivel(cicloActivo?.nivel_alerta ?: 0)
        val estadio = estadioReal ?: estadioPorDefecto(estado)
        val recomendacion = recomendacion(estado)

        VentanaControlBiologico(
            monitoreoId = m.monitoreo_id,
            terrenoNombre = m.terreno_nombre,
            cultivoNombre = m.cultivo_nombre,
            plagaNombre = m.plaga_nombre,
            plagaCientifico = m.plaga_nombre_cientifico,
            progresoGdd = progreso,
            estadioEstimado = estadio,
            requiereRecorrida = estado == EstadoVentanaControl.ECLOSIONADA,
            estadoVentana = estado,
            recomendacionTecnica = recomendacion
        )
    }.sortedWith(
        compareByDescending<VentanaControlBiologico> { it.estadoVentana.ordinal }
            .thenByDescending { it.progresoGdd }
    )
}

/**
 * El ciclo que describe el monitoreo: el activo o, si no hay, el de biofix más reciente (mismo
 * criterio que el backend). Null mientras espera el biofix: todavía no se midió nada.
 */
fun cicloQueDescribe(monitoreo: MonitoreoResponse): GddCicloResponse? {
    val ciclos = monitoreo.ciclos.orEmpty()
    return ciclos.firstOrNull { it.estado.equals("activo", ignoreCase = true) }
        ?: ciclos.maxWithOrNull(compareBy<GddCicloResponse> { it.fecha_biofix }.thenBy { it.id })
}

fun nivelAlertaResuelto(monitoreo: MonitoreoResponse): Int? = cicloQueDescribe(monitoreo)?.nivel_alerta

fun cicloVigente(monitoreo: MonitoreoResponse): GddCicloResponse? =
    ciclosActivos(monitoreo).maxWithOrNull(compareBy<GddCicloResponse> { it.nivel_alerta }.thenBy { it.progreso })

fun nivelAlertaVigente(monitoreo: MonitoreoResponse): Int? = cicloVigente(monitoreo)?.nivel_alerta

fun progresoResuelto(monitoreo: MonitoreoResponse): Float? = cicloQueDescribe(monitoreo)?.progreso

fun gddAcumuladoResuelto(monitoreo: MonitoreoResponse): Float? = cicloQueDescribe(monitoreo)?.gdd_acumulado


// ── Métricas globales del admin (GET /admin/metricas) ───────────────────────
// El backend ya las calcula sobre todos los productores; acá solo se pasan a los modelos
// de la pantalla. Los lotes llegan anónimos, así que se numeran en el orden del ranking.

private fun estadoVentana(estado: String): EstadoVentanaControl = when (estado) {
    "eclosionada" -> EstadoVentanaControl.ECLOSIONADA
    "eclosion_cerca" -> EstadoVentanaControl.ECLOSION_CERCA
    else -> EstadoVentanaControl.HUEVO
}

private fun estadioPorDefecto(estado: EstadoVentanaControl): String = when (estado) {
    EstadoVentanaControl.HUEVO -> "Huevo"
    EstadoVentanaControl.ECLOSION_CERCA -> "Huevo, eclosión cerca"
    EstadoVentanaControl.ECLOSIONADA -> "Larva o ninfa"
}

// Las larvas chicas son las más sensibles al control, y aplicar se decide con el umbral de
// daño del cultivo (Gamundi y Perotti, INTA 2007), no con el modelo.
private fun recomendacion(estado: EstadoVentanaControl): String = when (estado) {
    EstadoVentanaControl.HUEVO ->
        "Según los grados-día acumulados todavía no eclosionó."
    EstadoVentanaControl.ECLOSION_CERCA ->
        "Pasó el umbral de aviso del productor: conviene planificar la recorrida del lote."
    EstadoVentanaControl.ECLOSIONADA ->
        "Recorrer el lote: las larvas chicas son las más sensibles al control y la aplicación se decide con el umbral de daño del cultivo."
}

private fun nombreLoteAnonimo(posicion: Int) = "Lote anónimo ${posicion + 1}"

fun AdminMetricasUIState.conMetricasGlobales(metricas: MetricasAdmin): AdminMetricasUIState {
    val campañas = metricas.campanias.map { campaña ->
        val cultivosCampaña = campaña.cultivos.orEmpty().map { c ->
            ResumenCultivoCampaña(
                cultivoNombre = c.cultivo,
                cultivoCientifico = c.cultivo_cientifico,
                totalMonitoreos = c.monitoreos,
                plagas = c.plagas.orEmpty().map { pc ->
                    ResumenPlagaEnCultivoCampaña(
                        plagaNombre = pc.plaga,
                        plagaCientifico = pc.plaga_cientifico,
                        totalMonitoreos = pc.monitoreos,
                        porcentaje = pc.porcentaje
                    )
                }
            )
        }
        val plagas = campaña.plagas.map { plaga ->
            ResumenPlagaCampaña(
                plagaNombre = plaga.plaga,
                plagaCientifico = plaga.plaga_cientifico,
                totalMonitoreos = plaga.monitoreos,
                porcentajeIncidencia = plaga.incidencia,
                enRiesgoAlto = plaga.riesgo_alto,
                // Sin monitoreos medidos no hay porcentaje: se muestra 0.
                porcentajeRiesgoAlto = plaga.porcentaje_riesgo_alto ?: 0f,
                nivelRiesgoPromedio = plaga.nivel_promedio ?: 0f,
                cultivosAfectados = plaga.cultivos.orEmpty()
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
            riesgoAlto = campaña.riesgo_alto,
            cultivos = cultivosCampaña
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
            totalLotes = semaforo.total_lotes,
            lotesEsperandoBiofix = semaforo.esperando_biofix.lotes,
            hectareasEsperandoBiofix = semaforo.esperando_biofix.hectareas,
            lotesSinMonitoreo = semaforo.sin_monitoreo_activo?.lotes ?: 0,
            hectareasSinMonitoreo = semaforo.sin_monitoreo_activo?.hectareas ?: 0f
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
                requiereRecorrida = estado == EstadoVentanaControl.ECLOSIONADA,
                estadoVentana = estado,
                recomendacionTecnica = recomendacion(estado)
            )
        },
        tratamientos = metricas.tratamientos
    )
}


private fun formatearDecimalAr(valor: Float): String =
    if (valor % 1f == 0f) valor.toInt().toString() else String.format(java.util.Locale.US, "%.1f", valor).replace('.', ',')


fun textoLotesRegistrados(semaforo: SemaforoHectareas): String {
    val registrados = semaforo.totalLotes + semaforo.lotesEsperandoBiofix + semaforo.lotesSinMonitoreo
    return "$registrados ${if (registrados == 1) "lote registrado" else "lotes registrados"}"
}

/** Una viñeta por grupo de lotes: los del gráfico y los que quedan fuera, con su superficie. */
fun puntosLotesDelSemaforo(semaforo: SemaforoHectareas): List<String> {
    fun punto(lotes: Int, estado: String, hectareas: Float) =
        "$lotes $estado · ${formatearDecimalAr(hectareas)} ha"
    return listOf(
        punto(semaforo.totalLotes, "con ciclos activos", semaforo.totalHectareas),
        punto(semaforo.lotesEsperandoBiofix, "esperando biofix", semaforo.hectareasEsperandoBiofix),
        punto(semaforo.lotesSinMonitoreo, "sin monitoreos activos", semaforo.hectareasSinMonitoreo)
    )
}

/** "87,5 %" o "—" cuando no hay cifra. */
fun formatearPorcentajeTratamiento(valor: Float?): String = valor?.let { "${formatearDecimalAr(it)} %" } ?: "—"
