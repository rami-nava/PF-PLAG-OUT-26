package com.example.plag_out

import com.example.plag_out.Service.todasLasPlagas
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
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
const val ZOOM_MAXIMO_SERVIDOR = 22


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

/** La latitud máxima que acepta GET /mapa/reportes. */
const val LATITUD_MAXIMA_MAPA = 85.0

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
            sur = (sur - alto * margen).coerceAtLeast(-LATITUD_MAXIMA_MAPA),
            norte = (norte + alto * margen).coerceAtMost(LATITUD_MAXIMA_MAPA),
            oeste = (oeste - ancho * margen).coerceAtLeast(-180.0),
            este = (este + ancho * margen).coerceAtMost(180.0)
        )
    }

    /**
     * Lo que acepta el backend: latitudes dentro de ±85 (osmdroid llega a ±85,0511) y longitudes
     * dentro de ±180 sin cruzar el antimeridiano. Con la vista sobre el antimeridiano osmdroid
     * devuelve oeste > este; con zoom muy lejano, más de una vuelta. En los dos casos se pide todo
     * el ancho del mundo en vez de un rango que el servidor rechaza con 422.
     *
     * Cuando el mundo es casi tan alto como la pantalla (zoom 3 en un teléfono), osmdroid devuelve
     * el sur por encima del norte (por ejemplo sur 81,9 y norte 79,1). La latitud no da la vuelta:
     * eso significa que se ve todo el alto, no una franja del Ártico.
     */
    fun normalizada(): VistaMapa {
        val valida = listOf(sur, oeste, norte, este).all { it.isFinite() }
        val vueltaEntera = !valida || oeste > este || este - oeste >= 360.0
        val altoEntero = !valida || sur > norte
        return copy(
            sur = if (altoEntero) -LATITUD_MAXIMA_MAPA else sur.coerceIn(-LATITUD_MAXIMA_MAPA, LATITUD_MAXIMA_MAPA),
            norte = if (altoEntero) LATITUD_MAXIMA_MAPA else norte.coerceIn(-LATITUD_MAXIMA_MAPA, LATITUD_MAXIMA_MAPA),
            oeste = if (vueltaEntera) -180.0 else oeste.coerceIn(-180.0, 180.0),
            este = if (vueltaEntera) 180.0 else este.coerceIn(-180.0, 180.0)
        )
    }

    fun contiene(otra: VistaMapa): Boolean =
        otra.sur >= sur && otra.norte <= norte && otra.oeste >= oeste && otra.este <= este
}

/** Reintentos automáticos de un pedido del mapa ante una falla pasajera, además del primer intento. */
const val REINTENTOS_MAPA = 3
const val ESPERA_BASE_MAPA_MS = 1_000L
const val ESPERA_MAXIMA_MAPA_MS = 30_000L

/** Sin red, timeout, demasiados pedidos o el servidor caído: puede andar si se espera. */
fun esFallaPasajera(codigo: Int): Boolean = codigo == 408 || codigo == 429 || codigo >= 500

/**
 * Cuánto esperar después de [fallosSeguidos] fallas pasajeras: lo que pide el servidor en
 * `Retry-After` (en segundos) o, si no lo dice, 1 s, 2 s, 4 s… con tope local de 30 s.
 */
fun esperaTrasFallos(fallosSeguidos: Int, retryAfter: String?): Long {
    val propia = ESPERA_BASE_MAPA_MS shl (fallosSeguidos - 1).coerceIn(0, 10)
    return esperaPedidaPorServidor(retryAfter) ?: propia.coerceAtMost(ESPERA_MAXIMA_MAPA_MS)
}

private fun esperaPedidaPorServidor(retryAfter: String?): Long? =
    retryAfter?.trim()?.toIntOrNull()?.takeIf { it >= 0 }?.times(1_000L)

/** Un intento que falló de una forma que puede andar si se espera. */
private class FallaPasajera(val codigo: Int?, val retryAfter: String?) : Exception()

/**
 * La vista a partir de la caja de osmdroid. Si el mundo entero es más bajo que el mapa, se ve todo
 * el alto, pero osmdroid devuelve una franja cualquiera (en el emulador, a zoom 3: de 74 a 84,5).
 * Ahí se pide todo el alto en vez de esa franja.
 */
fun vistaDeOsmdroid(
    sur: Double, oeste: Double, norte: Double, este: Double, zoom: Double,
    altoMundoPx: Double, altoMapaPx: Double
): VistaMapa =
    if (altoMapaPx > 0 && altoMundoPx <= altoMapaPx) {
        VistaMapa(-LATITUD_MAXIMA_MAPA, oeste, LATITUD_MAXIMA_MAPA, este, zoom)
    } else {
        VistaMapa(sur, oeste, norte, este, zoom)
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
    private val gddService: GDDService = RetrofitClient.gddService,
    private val esperar: suspend (Long) -> Unit = { delay(it) },
    private val ahora: () -> Long = { SystemClock.elapsedRealtime() }
) : ViewModel() {

    private val _state = MutableStateFlow(MapaReportesUiState())
    val state: StateFlow<MapaReportesUiState> = _state.asStateFlow()

    private var vista: VistaMapa? = null
    private var cargado: ConsultaMapa? = null
    private var pedido: Job? = null
    /** Lo que está pidiendo [pedido] mientras no vuelve. */
    private var enCurso: ConsultaMapa? = null

    /**
     * Fallas pasajeras seguidas, de este pedido o de los anteriores: moverse durante una caída no
     * reinicia la espera, así que el mapa no inunda al servidor con un pedido por gesto.
     */
    private var fallosSeguidos = 0
    /** Antes de este instante ([ahora]) no sale ningún pedido. */
    private var pausaHasta = 0L
    /** La pausa la pidió el servidor con Retry-After: ni "Reintentar" se la saltea. */
    private var pausaDelServidor = false

    fun limpiar() {
        pedido?.cancel()
        pedido = null
        vista = null
        cargado = null
        enCurso = null
        fallosSeguidos = 0
        pausaHasta = 0L
        pausaDelServidor = false
        _state.value = MapaReportesUiState()
    }

    fun actualizarVista(nueva: VistaMapa) {
        vista = nueva.normalizada()
        consultar()
    }

    fun actualizarFiltros(filtros: FiltrosMapa) {
        if (filtros == _state.value.filtros) return
        _state.update { it.copy(filtros = filtros) }
        consultar()
    }

    /** "Reintentar": pide ya, salvo que el servidor haya dicho cuánto esperar. */
    fun refrescar() {
        cargado = null
        if (!pausaDelServidor) pausaHasta = 0L
        consultar()
    }

    private fun consultar() {
        val v = vista ?: return
        val filtros = _state.value.filtros
        val nivel = nivelParaZoom(v.zoom)
        val zoomEntero = floor(v.zoom).toInt().coerceIn(0, ZOOM_MAXIMO_SERVIDOR)
        if (cargado?.cubre(v, nivel, zoomEntero, filtros) == true) {
            // Al volver al área en caché ya no sirve la consulta iniciada durante el paneo.
            if (enCurso != null) {
                pedido?.cancel()
                pedido = null
                enCurso = null
                _state.update { it.copy(actualizando = false) }
            }
            return
        }
        // Un paneo lento que no sale del área que ya se está pidiendo (lo visible más el margen)
        // no cancela ese pedido: si cada gesto lo cancelara, nada llegaría hasta que el usuario
        // se detenga. Cuando vuelve se revisa si alcanza para la vista de ese momento.
        if (pedido?.isActive == true && enCurso?.cubre(v, nivel, zoomEntero, filtros) == true) return

        val consulta = ConsultaMapa(v.ampliada(), nivel, zoomEntero, filtros)
        pedido?.cancel()
        enCurso = consulta
        pedido = viewModelScope.launch {
            _state.update { it.copy(actualizando = !it.cargando, error = null) }
            try {
                var reintentos = 0
                while (true) {
                    val falta = pausaHasta - ahora()
                    if (falta > 0) esperar(falta)
                    try {
                        val body = pedir(consulta)
                        fallosSeguidos = 0
                        pausaHasta = 0L
                        pausaDelServidor = false
                        val recibido = aplicarRespuesta(body, nivel)
                        cargado = consulta.copy(recibido = recibido)
                        enCurso = null
                        // Mientras volvía la vista pudo cambiar de zoom, y el servidor pudo
                        // responder grupos en vez de reportes: si no alcanza, se pide lo que falta.
                        consultar()
                        return@launch
                    } catch (e: FallaPasajera) {
                        fallosSeguidos++
                        pausaHasta = ahora() + esperaTrasFallos(fallosSeguidos, e.retryAfter)
                        pausaDelServidor = esperaPedidaPorServidor(e.retryAfter) != null
                        if (reintentos++ >= REINTENTOS_MAPA) {
                            fallo(
                                if (e.codigo == null) "Error de conexión al cargar alertas."
                                else "No se pudieron obtener las alertas del servidor."
                            )
                            return@launch
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: RespuestaInvalida) {
                Log.w("MAPA_REPORTES", "Error al obtener reportes del mapa: ${e.codigo}")
                fallo(
                    if (e.codigo == 404 || e.codigo == 405) {
                        "El mapa de alertas todavía no está disponible en el servidor."
                    } else {
                        "No se pudieron obtener las alertas del servidor."
                    }
                )
            } catch (e: Exception) {
                Log.e("MAPA_REPORTES", "Excepción cargando el mapa: ${e.message}", e)
                fallo("Error de conexión al cargar alertas.")
            }
        }
    }

    /** Un intento. Lanza [FallaPasajera] si conviene reintentar y [RespuestaInvalida] si no. */
    private suspend fun pedir(consulta: ConsultaMapa): ReportesMapaResponse {
        val area = consulta.area
        val filtros = consulta.filtros
        val response = try {
            withContext(Dispatchers.IO) {
                gddService.getReportesMapa(
                    sur = area.sur, oeste = area.oeste, norte = area.norte, este = area.este,
                    zoom = consulta.zoomEntero,
                    nivel = consulta.nivel.parametro,
                    celdaGrados = celdaGradosParaZoom(consulta.zoomEntero),
                    limite = if (consulta.nivel == NivelMapa.REPORTES) LIMITE_REPORTES_MAPA else null,
                    ambito = filtros.ambito.parametro,
                    radioKm = filtros.radioKm,
                    dias = filtros.dias,
                    severidad = filtros.severidad,
                    plaga = filtros.plaga,
                    cultivo = filtros.cultivo
                )
            }
        } catch (e: IOException) {
            Log.w("MAPA_REPORTES", "Sin conexión cargando el mapa: ${e.message}")
            throw FallaPasajera(codigo = null, retryAfter = null)
        }
        val body = response.body()
        if (response.isSuccessful && body != null) return body
        val codigo = response.code()
        if (esFallaPasajera(codigo)) {
            Log.w("MAPA_REPORTES", "Falla pasajera del mapa: $codigo")
            throw FallaPasajera(codigo, response.headers()["Retry-After"])
        }
        throw RespuestaInvalida(codigo)
    }

    private class RespuestaInvalida(val codigo: Int) : Exception()

    /**
     * Sin respuesta no quedan en pantalla las alertas de otra zona o de otros filtros: los chips ya
     * muestran los nuevos. Y lo que estaba cargado deja de servir para no volver a pedir.
     */
    private fun fallo(mensaje: String) {
        cargado = null
        _state.update {
            it.copy(
                cargando = false, actualizando = false, error = mensaje,
                grupos = emptyList(), reportes = emptyList(), total = 0
            )
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
                val response = withContext(Dispatchers.IO) { gddService.todasLasPlagas() }
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
