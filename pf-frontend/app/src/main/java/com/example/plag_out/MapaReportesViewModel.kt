package com.example.plag_out

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Desde este zoom el mapa pide los reportes uno por uno; por debajo, solo cuántos hay por zona.
 * A zoom 11 la pantalla de un teléfono abarca unos 100 km: pocos reportes, y ya se distinguen lotes.
 */
const val ZOOM_REPORTES_INDIVIDUALES = 11.0


const val CELDA_GRUPO_SERVIDOR_PX = 64.0

/**
 * Máximo de reportes sueltos por respuesta. Si en la zona pedida hay más, el servidor responde
 * grupos aunque el zoom pida reportes: así los números del mapa son siempre exactos y los pines
 * sueltos aparecen recién cuando entran todos.
 */
const val LIMITE_REPORTES_MAPA = 500

enum class NivelMapa(val parametro: String) { GRUPOS("grupos"), REPORTES("reportes") }

enum class AmbitoMapa(val parametro: String?) { TODOS(null), PROPIOS("propios"), COMUNIDAD("comunidad") }

/** null en cualquier campo = sin ese filtro. */
data class FiltrosMapa(
    val ambito: AmbitoMapa = AmbitoMapa.TODOS,
    val radioKm: Int? = null,
    val dias: Int? = null,
    val severidad: String? = null,
    val plaga: String? = null,
    val cultivo: String? = null
) {
    val activos: Int
        get() = listOf(
            ambito != AmbitoMapa.TODOS, radioKm != null, dias != null,
            severidad != null, plaga != null, cultivo != null
        ).count { it }
}

/** Zona del mapa en grados, más el zoom con el que se está mirando. */
data class VistaMapa(
    val sur: Double,
    val oeste: Double,
    val norte: Double,
    val este: Double,
    val zoom: Double
) {
    /**
     * Se pide un margen alrededor de lo visible para que un desplazamiento chico no dispare otro
     * pedido: mientras la vista siga adentro de lo ya cargado, no hace falta ir al servidor.
     */
    fun ampliada(margen: Double = 0.5): VistaMapa {
        val alto = norte - sur
        val ancho = este - oeste
        return copy(
            sur = (sur - alto * margen).coerceAtLeast(-85.0),
            norte = (norte + alto * margen).coerceAtMost(85.0),
            oeste = (oeste - ancho * margen).coerceAtLeast(-180.0),
            este = (este + ancho * margen).coerceAtMost(180.0)
        )
    }

    fun contiene(otra: VistaMapa): Boolean =
        otra.sur >= sur && otra.norte <= norte && otra.oeste >= oeste && otra.este <= este
}

fun nivelParaZoom(zoom: Double): NivelMapa =
    if (zoom >= ZOOM_REPORTES_INDIVIDUALES) NivelMapa.REPORTES else NivelMapa.GRUPOS


fun celdaGradosParaZoom(zoom: Int, ladoPx: Double = CELDA_GRUPO_SERVIDOR_PX): Double =
    360.0 / (256.0 * 2.0.pow(zoom)) * ladoPx

/** Lo que ya está cargado: sirve para saber si una vista nueva necesita otro pedido. */
private data class ConsultaMapa(
    val area: VistaMapa,
    /** Lo que se pidió según el zoom. */
    val nivel: NivelMapa,
    val zoomEntero: Int,
    val filtros: FiltrosMapa,
    /** Lo que respondió el servidor: puede ser GRUPOS aunque se hayan pedido reportes. */
    val recibido: NivelMapa = nivel
) {
    fun cubre(vista: VistaMapa, nivel: NivelMapa, zoomEntero: Int, filtros: FiltrosMapa): Boolean =
        this.nivel == nivel &&
            this.filtros == filtros &&
            (recibido == NivelMapa.REPORTES || this.zoomEntero == zoomEntero) &&
            area.contiene(vista)
}

data class MapaReportesUiState(
    val cargando: Boolean = true,
    val actualizando: Boolean = false,
    val nivel: NivelMapa = NivelMapa.GRUPOS,
    val grupos: List<GrupoReportesMapa> = emptyList(),
    val reportes: List<ReporteDetalleResponse> = emptyList(),
    val total: Int = 0,
    val filtros: FiltrosMapa = FiltrosMapa(),
    val catalogoPlagas: List<String> = emptyList(),
    val plagasVistas: List<String> = emptyList(),
    val cultivos: List<String> = emptyList(),
    val error: String? = null
)


class MapaReportesViewModel(
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow(MapaReportesUiState())
    val state: StateFlow<MapaReportesUiState> = _state.asStateFlow()

    private var vista: VistaMapa? = null
    private var cargado: ConsultaMapa? = null
    private var pedido: Job? = null

    fun actualizarVista(nueva: VistaMapa) {
        vista = nueva
        consultar()
    }

    fun actualizarFiltros(filtros: FiltrosMapa) {
        if (filtros == _state.value.filtros) return
        _state.update { it.copy(filtros = filtros) }
        consultar()
    }

    fun refrescar() {
        cargado = null
        consultar()
    }

    private fun consultar() {
        val v = vista ?: return
        val filtros = _state.value.filtros
        val nivel = nivelParaZoom(v.zoom)
        val zoomEntero = floor(v.zoom).toInt()
        if (cargado?.cubre(v, nivel, zoomEntero, filtros) == true) return

        val consulta = ConsultaMapa(v.ampliada(), nivel, zoomEntero, filtros)
        pedido?.cancel()
        pedido = viewModelScope.launch {
            _state.update { it.copy(actualizando = !it.cargando, error = null) }
            try {
                val area = consulta.area
                val response = withContext(Dispatchers.IO) {
                    gddService.getReportesMapa(
                        sur = area.sur, oeste = area.oeste, norte = area.norte, este = area.este,
                        zoom = zoomEntero,
                        nivel = nivel.parametro,
                        celdaGrados = celdaGradosParaZoom(zoomEntero),
                        limite = if (nivel == NivelMapa.REPORTES) LIMITE_REPORTES_MAPA else null,
                        ambito = filtros.ambito.parametro,
                        radioKm = filtros.radioKm,
                        dias = filtros.dias,
                        severidad = filtros.severidad,
                        plaga = filtros.plaga,
                        cultivo = filtros.cultivo
                    )
                }
                val body = response.body()
                when {
                    response.isSuccessful && body != null -> {
                        val recibido = aplicarRespuesta(body, nivel)
                        cargado = consulta.copy(recibido = recibido)
                    }
                    else -> {
                        Log.w("MAPA_REPORTES", "Error al obtener reportes del mapa: ${response.code()}")
                        val mensaje = if (response.code() == 404 || response.code() == 405) {
                            "El mapa de reportes todavía no está disponible en el servidor."
                        } else {
                            "No se pudieron obtener los reportes del servidor."
                        }
                        _state.update { it.copy(cargando = false, actualizando = false, error = mensaje) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MAPA_REPORTES", "Excepción cargando el mapa: ${e.message}", e)
                _state.update {
                    it.copy(cargando = false, actualizando = false, error = "Error de conexión al cargar reportes.")
                }
            }
        }
    }

    private fun aplicarRespuesta(body: ReportesMapaResponse, pedido: NivelMapa): NivelMapa {
        val nivel = NivelMapa.entries.firstOrNull { it.parametro.equals(body.nivel, ignoreCase = true) } ?: pedido
        val grupos = if (nivel == NivelMapa.GRUPOS) {
            body.grupos.orEmpty().filter { it.cantidad > 0 && coordenadasValidas(it.latitud, it.longitud) }
        } else emptyList()
        val reportes = if (nivel == NivelMapa.REPORTES) {
            body.reportes.orEmpty().filter { it.latitud != null && it.longitud != null }
        } else emptyList()
        val total = body.total ?: if (nivel == NivelMapa.GRUPOS) grupos.sumOf { it.cantidad } else reportes.size

        _state.update { s ->
            s.copy(
                cargando = false,
                actualizando = false,
                nivel = nivel,
                grupos = grupos,
                reportes = reportes,
                total = total,
                plagasVistas = acumular(s.plagasVistas, reportes.map { it.plaga_nombre }),
                cultivos = acumular(s.cultivos, body.cultivos.orEmpty() + reportes.mapNotNull { it.cultivo_nombre }),
                error = null
            )
        }
        return nivel
    }

    fun cargarCatalogoPlagas() {
        if (_state.value.catalogoPlagas.isNotEmpty()) return
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { gddService.getPlagas() }
                val nombres = response.body()
                    ?.map { it.nombre }
                    ?.filter { it.isNotBlank() }
                    ?.distinct()
                    ?: emptyList()
                if (response.isSuccessful && nombres.isNotEmpty()) {
                    _state.update { it.copy(catalogoPlagas = nombres) }
                } else {
                    Log.w("MAPA_REPORTES", "Catálogo de plagas no disponible: ${response.code()}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("MAPA_REPORTES", "No se pudo cargar el catálogo de plagas: ${e.message}")
            }
        }
    }

    private fun acumular(previos: List<String>, nuevos: List<String>): List<String> {
        val agregados = nuevos.filter { it.isNotBlank() && it !in previos }.distinct()
        return if (agregados.isEmpty()) previos else (previos + agregados).sortedBy { it.lowercase() }
    }

    private fun coordenadasValidas(lat: Double, lon: Double): Boolean =
        lat in -90.0..90.0 && lon in -180.0..180.0
}

fun distanciaAlLoteMasCercano(
    reporte: ReporteDetalleResponse,
    terrenos: List<TerrenoResponse>
): Float? {
    reporte.distancia_km?.let { return it }
    val lat = reporte.latitud ?: return null
    val lon = reporte.longitud ?: return null
    if (terrenos.isEmpty()) return null
    return terrenos.minOf { t ->
        distanciaKm(lat, lon, t.terreno_latitud.toDouble(), t.terreno_longitud.toDouble())
    }
}

private fun distanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val radioTierra = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return (2 * radioTierra * atan2(sqrt(a), sqrt(1 - a))).toFloat()
}

class MapaReportesViewModelFactory(
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapaReportesViewModel(gddService) as T
    }
}
