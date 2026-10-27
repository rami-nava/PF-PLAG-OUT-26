package com.example.plag_out

import com.example.plag_out.Service.todasLasPlagas
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.UsuarioRepository
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import com.example.plag_out.Service.todosLosTerrenos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.jan.supabase.exceptions.HttpRequestException
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import retrofit2.HttpException

const val REPORTES_PAGE_SIZE = 20

/**
 * Días entre "desde" y "hasta" como máximo. El backend acepta hasta 365 días entre el inicio de
 * "desde" y el final de "hasta", es decir 364 días de diferencia entre las fechas.
 */
const val RANGO_MAXIMO_DIAS = 364L

/**
 * Rango válido después de mover un extremo: el que se movió manda y el otro se acerca lo
 * necesario para que "desde" no pase a "hasta" ni el rango supere [RANGO_MAXIMO_DIAS].
 */
fun ajustarRangoFechas(desde: LocalDate, hasta: LocalDate, movioDesde: Boolean): Pair<LocalDate, LocalDate> =
    if (movioDesde) {
        val nuevoHasta = when {
            hasta.isBefore(desde) -> desde
            desde.plusDays(RANGO_MAXIMO_DIAS).isBefore(hasta) -> desde.plusDays(RANGO_MAXIMO_DIAS)
            else -> hasta
        }
        desde to nuevoHasta
    } else {
        val nuevoDesde = when {
            desde.isAfter(hasta) -> hasta
            hasta.minusDays(RANGO_MAXIMO_DIAS).isAfter(desde) -> hasta.minusDays(RANGO_MAXIMO_DIAS)
            else -> desde
        }
        nuevoDesde to hasta
    }

enum class AmbitoReportes(val valor: String) { PROPIOS("propios"), COMUNIDAD("comunidad") }


data class FiltrosReportes(
    val severidad: String? = null,
    val terrenoId: Int? = null,
    val plagaId: Int? = null
) {
    val activos: Boolean get() = severidad != null || terrenoId != null || plagaId != null
}

/** Lo cargado de una pestaña: cada una pagina por separado y se pide la primera vez que se abre. */
data class ListadoReportes(
    val reportes: List<ReporteDetalleResponse> = emptyList(),
    val total: Int? = null,
    val conteo: ConteoReportes? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val cargandoMas: Boolean = false,
    val error: String? = null,
    val errorCargarMas: String? = null,
    val sinConexion: Boolean = false,
    val desactualizado: Boolean = false
) {
    val cargado: Boolean get() = total != null
    val hayMas: Boolean get() = total != null && reportes.size < total
}

data class MisReportesUiState(
    val ambito: AmbitoReportes = AmbitoReportes.PROPIOS,
    val propios: ListadoReportes = ListadoReportes(),
    val comunidad: ListadoReportes = ListadoReportes(),
    val filtrosPropios: FiltrosReportes = FiltrosReportes(),
    val filtrosComunidad: FiltrosReportes = FiltrosReportes(),
    val terrenos: List<TerrenoResponse> = emptyList(),
    val plagas: List<PlagaResponse> = emptyList(),
    val fechaDesde: LocalDate = LocalDate.now().minusMonths(1),
    val fechaHasta: LocalDate = LocalDate.now(),
    val distanciaListadoKm: Int? = null,
    val radioNotificacionKm: Double = 20.0,
    val mostrarDialogoRadio: Boolean = false,
    val radioTemporalKm: Float = 20f,
    val guardandoRadio: Boolean = false,
    val eliminando: Set<Int> = emptySet()
) {
    fun listado(ambito: AmbitoReportes): ListadoReportes =
        if (ambito == AmbitoReportes.PROPIOS) propios else comunidad

    fun filtros(ambito: AmbitoReportes): FiltrosReportes =
        if (ambito == AmbitoReportes.PROPIOS) filtrosPropios else filtrosComunidad

    fun conteo(ambito: AmbitoReportes): ConteoReportes? = listado(ambito).conteo

    // Atajos a la pestaña seleccionada.
    val actual: ListadoReportes get() = listado(ambito)
    val filtrosActuales: FiltrosReportes get() = filtros(ambito)
    val reportes: List<ReporteDetalleResponse> get() = actual.reportes
    val isLoading: Boolean get() = actual.isLoading || !actual.cargado && actual.error == null
    val isRefreshing: Boolean get() = actual.isRefreshing
    val hayMas: Boolean get() = actual.hayMas
    val cargandoMas: Boolean get() = actual.cargandoMas
    val error: String? get() = actual.error
    val errorCargarMas: String? get() = actual.errorCargarMas
    val sinConexion: Boolean get() = actual.sinConexion
}

class MisReportesViewModel(
    private val gddService: GDDService = RetrofitClient.gddService,
    private val usuarioRepository: UsuarioRepository? = null,
    private val reportesDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow(MisReportesUiState())
    val state: StateFlow<MisReportesUiState> = _state.asStateFlow()
    private val cargas = mutableMapOf<AmbitoReportes, Job>()
    private val consultas = mutableMapOf<AmbitoReportes, ConsultaReportes>()
    private val siguienteOffset = mutableMapOf<AmbitoReportes, Int>()
    private val cargasResumen = mutableMapOf<AmbitoReportes, Job>()
    private var cargaOpciones: Job? = null

    init {
        cargarRadioNotificacion()
    }

    fun cargarRadioNotificacion() {
        viewModelScope.launch {
            val cacheado = withContext(Dispatchers.IO) {
                usuarioRepository?.obtenerUsuario()
            }
            if (cacheado != null) {
                _state.value = _state.value.copy(
                    radioNotificacionKm = cacheado.radio_notificacion_km,
                    radioTemporalKm = cacheado.radio_notificacion_km.toFloat()
                )
            } else {
                try {
                    val response = withContext(Dispatchers.IO) { gddService.getUsuarioActual() }
                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()!!
                        _state.value = _state.value.copy(
                            radioNotificacionKm = user.radio_notificacion_km,
                            radioTemporalKm = user.radio_notificacion_km.toFloat()
                        )
                        withContext(Dispatchers.IO) { usuarioRepository?.guardarUsuario(user) }
                    }
                } catch (e: Exception) {
                    Log.w("MIS_REPORTES", "No se pudo precargar radio del usuario: ${e.message}")
                }
            }
        }
    }

    fun abrirConfiguracionRadio() {
        _state.value = _state.value.copy(
            mostrarDialogoRadio = true,
            radioTemporalKm = _state.value.radioNotificacionKm.toFloat()
        )
    }

    fun onRadioTemporalChange(nuevoRadio: Float) {
        _state.value = _state.value.copy(radioTemporalKm = nuevoRadio)
    }

    fun cerrarConfiguracionRadio() {
        _state.value = _state.value.copy(mostrarDialogoRadio = false)
    }

    fun guardarRadioNotificacion(onSuccess: (() -> Unit)? = null) {
        val nuevoRadio = _state.value.radioTemporalKm.toDouble()
        val radioAnterior = _state.value.radioNotificacionKm
        _state.value = _state.value.copy(guardandoRadio = true)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.actualizarUsuario(
                        UpdateUserRequest(radio_notificacion_km = nuevoRadio)
                    )
                }
                if (response.isSuccessful) {
                    val usuarioActualizado = response.body()
                    val radioFinal = usuarioActualizado?.radio_notificacion_km ?: nuevoRadio
                    if (radioFinal != radioAnterior && _state.value.distanciaListadoKm == null) {
                        reiniciarComunidad()
                    }
                    _state.value = _state.value.copy(
                        radioNotificacionKm = radioFinal,
                        radioTemporalKm = radioFinal.toFloat(),
                        guardandoRadio = false,
                        mostrarDialogoRadio = false
                    )
                    if (usuarioActualizado != null) {
                        withContext(Dispatchers.IO) { usuarioRepository?.guardarUsuario(usuarioActualizado) }
                    } else {
                        val local = withContext(Dispatchers.IO) { usuarioRepository?.obtenerUsuario() }
                        if (local != null) {
                            withContext(Dispatchers.IO) {
                                usuarioRepository?.guardarUsuario(local.copy(radio_notificacion_km = radioFinal))
                            }
                        }
                    }
                    Avisos.exito("Radio de notificación actualizado.")
                    onSuccess?.invoke()
                } else {
                    _state.value = _state.value.copy(guardandoRadio = false)
                    Avisos.error(
                        errorDeRespuesta(
                            response, ERRORES_PERFIL,
                            mapOf(500 to "No se pudo actualizar el radio de notificación. Intentá de nuevo.")
                        ).mensaje
                    )
                }
            } catch (e: Exception) {
                Log.e("MIS_REPORTES", "Error al guardar radio de notificación: ${e.message}")
                _state.value = _state.value.copy(guardandoRadio = false)
                Avisos.error(errorDeExcepcion(e).mensaje)
            }
        }
    }


    fun cargarReportes(forzar: Boolean = false, avisarSiFalla: Boolean = false) {
        val ambito = _state.value.ambito
        if (forzar || !_state.value.listado(ambito).cargado && cargas[ambito]?.isActive != true) {
            cargarPrimeraPagina(ambito, avisarSiFalla)
        }
        if (cargaOpciones == null) cargarOpcionesFiltros()
    }

    fun seleccionarAmbito(ambito: AmbitoReportes) {
        if (_state.value.ambito == ambito) return
        _state.value = _state.value.copy(ambito = ambito)
        val listado = _state.value.listado(ambito)
        if ((!listado.cargado || listado.desactualizado) && cargas[ambito]?.isActive != true) {
            cargarPrimeraPagina(ambito, avisarSiFalla = false)
        }
    }


    fun refrescar() {
        val ambito = _state.value.ambito
        val otro = if (ambito == AmbitoReportes.PROPIOS) AmbitoReportes.COMUNIDAD else AmbitoReportes.PROPIOS
        if (_state.value.listado(otro).cargado) actualizar(otro) { it.copy(desactualizado = true) }
        cargarPrimeraPagina(ambito, avisarSiFalla = true)
        cargarOpcionesFiltros()
    }


    fun actualizarFiltros(filtros: FiltrosReportes) {
        val ambito = _state.value.ambito
        if (_state.value.filtros(ambito) == filtros) return
        _state.value = if (ambito == AmbitoReportes.PROPIOS) {
            _state.value.copy(filtrosPropios = filtros)
        } else {
            _state.value.copy(filtrosComunidad = filtros)
        }
        cargas[ambito]?.cancel()
        actualizar(ambito) { ListadoReportes() }
        cargarPrimeraPagina(ambito, avisarSiFalla = false)
    }


    fun limpiarFiltros() {
        val ambito = _state.value.ambito
        if (ambito == AmbitoReportes.COMUNIDAD && _state.value.distanciaListadoKm != null) {
            _state.value = _state.value.copy(distanciaListadoKm = null, filtrosComunidad = FiltrosReportes())
            reiniciarComunidad()
        } else {
            actualizarFiltros(FiltrosReportes())
        }
    }

    private fun cargarPrimeraPagina(ambito: AmbitoReportes, avisarSiFalla: Boolean) {
        cargas[ambito]?.cancel()
        // Las páginas siguientes usan la misma consulta aunque el usuario cambie algo: cambiarla
        // vuelve a empezar desde la primera.
        val consulta = consultaActual(ambito)
        consultas[ambito] = consulta
        val hayResultadosAnteriores = _state.value.listado(ambito).reportes.isNotEmpty()
        actualizar(ambito) {
            it.copy(
                isLoading = !hayResultadosAnteriores,
                isRefreshing = hayResultadosAnteriores,
                cargandoMas = false,
                error = null,
                errorCargarMas = null
            )
        }
        cargarConteo(consulta)
        cargas[ambito] = viewModelScope.launch {
            try {
                val pagina = pedirPagina(consulta, offset = 0)
                val items = pagina.items.orEmpty()
                siguienteOffset[ambito] = items.size
                actualizar(ambito) {
                    it.copy(
                        reportes = items.distinctBy { r -> r.id },
                        total = pagina.total,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        sinConexion = false,
                        desactualizado = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MIS_REPORTES", "No se pudo cargar la primera página de ${ambito.valor}", e)
                val fallo = falloDeCarga(e)
                // Un refresco fallido deja la lista anterior tal cual.
                actualizar(ambito) {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = fallo.mensaje,
                        sinConexion = e is IOException || e is HttpRequestException
                    )
                }
                if (avisarSiFalla) Avisos.error(fallo.mensaje)
            }
        }
    }

    fun cargarMas() {
        val ambito = _state.value.ambito
        val actual = _state.value.listado(ambito)
        if (!actual.hayMas || actual.cargandoMas || actual.isLoading || actual.isRefreshing ||
            cargas[ambito]?.isActive == true
        ) return
        val consulta = consultas[ambito] ?: return
        val offset = siguienteOffset[ambito] ?: 0
        actualizar(ambito) { it.copy(cargandoMas = true, errorCargarMas = null) }
        cargas[ambito] = viewModelScope.launch {
            try {
                val pagina = pedirPagina(consulta, offset)
                val items = pagina.items.orEmpty()
                val cargados = _state.value.listado(ambito).reportes
                val ids = cargados.mapTo(HashSet()) { it.id }
                val nuevos = items.filterNot { it.id in ids }.distinctBy { it.id }
                // Una página llena sin nada nuevo es un servidor que ignora el offset: seguir
                // pidiendo sería un bucle. Se corta con error y queda el reintento manual.
                if (items.size == REPORTES_PAGE_SIZE && nuevos.isEmpty()) {
                    throw IllegalStateException("Report pagination did not advance")
                }
                // Avanzar por filas recibidas, no por IDs únicos: pueden repetirse entre páginas.
                // Se suma sobre el valor actual para conservar el corrimiento de un borrado en curso.
                siguienteOffset[ambito] = Math.addExact(siguienteOffset[ambito] ?: 0, items.size)
                actualizar(ambito) {
                    it.copy(
                        reportes = it.reportes + nuevos,
                        // Si el servidor se quedó sin filas antes de llegar al total, no hay más.
                        total = if (items.size < REPORTES_PAGE_SIZE) it.reportes.size + nuevos.size else pagina.total,
                        cargandoMas = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MIS_REPORTES", "No se pudo cargar la página de ${ambito.valor} en $offset", e)
                actualizar(ambito) {
                    it.copy(
                        cargandoMas = false,
                        errorCargarMas = falloDeCarga(e).mensaje,
                        sinConexion = e is IOException || e is HttpRequestException
                    )
                }
            }
        }
    }

    private suspend fun pedirPagina(consulta: ConsultaReportes, offset: Int): Pagina<ReporteDetalleResponse> =
        withContext(reportesDispatcher) {
            val response = gddService.getReportes(
                ambito = consulta.ambito.valor,
                fechaDesde = consulta.desde,
                fechaHasta = consulta.hasta,
                limit = REPORTES_PAGE_SIZE,
                offset = offset,
                distanciaKm = consulta.distanciaKm,
                severidad = consulta.filtros.severidad,
                terrenoId = consulta.filtros.terrenoId,
                plagaId = consulta.filtros.plagaId
            )
            if (!response.isSuccessful) throw HttpException(response)
            val pagina = response.body() ?: throw IllegalStateException("Report response has no body")
            if (pagina.items.orEmpty().size > REPORTES_PAGE_SIZE) {
                throw IllegalStateException("Report page exceeds the limit")
            }
            pagina
        }


    private fun cargarConteo(consulta: ConsultaReportes) {
        val ambito = consulta.ambito
        // Al refrescar se muestran los totales anteriores hasta que lleguen los nuevos.
        cargasResumen[ambito]?.cancel()
        cargasResumen[ambito] = viewModelScope.launch {
            try {
                val response = withContext(reportesDispatcher) {
                    gddService.getResumenReportes(
                        ambito = ambito.valor,
                        fechaDesde = consulta.desde,
                        fechaHasta = consulta.hasta,
                        distanciaKm = consulta.distanciaKm,
                        severidad = consulta.filtros.severidad,
                        terrenoId = consulta.filtros.terrenoId,
                        plagaId = consulta.filtros.plagaId
                    )
                }
                if (response.isSuccessful) actualizar(ambito) { it.copy(conteo = response.body()) }
                else Log.w("MIS_REPORTES", "Resumen de ${ambito.valor} respondió ${response.code()}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("MIS_REPORTES", "No se pudo cargar el resumen de ${ambito.valor}: ${e.message}")
            }
        }
    }

    private fun cargarOpcionesFiltros() {
        cargaOpciones?.cancel()
        cargaOpciones = viewModelScope.launch {
            val terrenos = runCatching {
                withContext(reportesDispatcher) { gddService.todosLosTerrenos() }.body()
            }.onFailure { if (it is CancellationException) throw it }.getOrNull()
            val plagas = runCatching {
                withContext(reportesDispatcher) { gddService.todasLasPlagas() }.body()
            }.onFailure { if (it is CancellationException) throw it }.getOrNull()
            _state.value = _state.value.copy(
                terrenos = terrenos.orEmpty().sortedBy { it.terreno_nombre.lowercase() },
                plagas = plagas.orEmpty().sortedBy { it.nombre.lowercase() }
            )
        }
    }

    private fun consultaActual(ambito: AmbitoReportes): ConsultaReportes {
        val zona = ZoneId.systemDefault()
        val estado = _state.value
        return ConsultaReportes(
            ambito = ambito,
            desde = estado.fechaDesde.atStartOfDay(zona).toInstant().toString(),
            hasta = estado.fechaHasta.plusDays(1).atStartOfDay(zona).toInstant().minusNanos(1).toString(),
            // La distancia filtra los reportes de otros productores; los propios van todos.
            distanciaKm = if (ambito == AmbitoReportes.COMUNIDAD) estado.distanciaListadoKm else null,
            filtros = estado.filtros(ambito)
        )
    }

    private fun actualizar(ambito: AmbitoReportes, cambio: (ListadoReportes) -> ListadoReportes) {
        val estado = _state.value
        _state.value = if (ambito == AmbitoReportes.PROPIOS) {
            estado.copy(propios = cambio(estado.propios))
        } else {
            estado.copy(comunidad = cambio(estado.comunidad))
        }
    }

    private fun falloDeCarga(e: Exception) =
        (e as? HttpException)?.response()?.let { errorDeRespuesta(it) } ?: errorDeExcepcion(e)

    fun eliminarReporte(reporteId: Int, onResultado: (error: String?) -> Unit) {
        if (reporteId in _state.value.eliminando) return
        _state.value = _state.value.copy(eliminando = _state.value.eliminando + reporteId)
        viewModelScope.launch {
            val error = try {
                val response = withContext(reportesDispatcher) { gddService.deleteReporte(reporteId) }
                if (response.isSuccessful) {
                    quitarReporteEliminado(reporteId)
                    null
                } else {
                    Log.e("MIS_REPORTES", "Error al eliminar el reporte $reporteId: ${response.code()}")
                    errorDeRespuesta(
                        response,
                        porCodigo = mapOf(
                            404 to "La alerta ya no existe.",
                            500 to "No se pudo eliminar la alerta. Intentá de nuevo."
                        )
                    ).mensaje
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MIS_REPORTES", "Error al eliminar el reporte $reporteId", e)
                errorDeExcepcion(e).mensaje
            }
            _state.value = _state.value.copy(eliminando = _state.value.eliminando - reporteId)
            onResultado(error)
        }
    }


    private fun quitarReporteEliminado(reporteId: Int) {
        val ambito = AmbitoReportes.PROPIOS
        val eliminado = _state.value.propios.reportes.firstOrNull { it.id == reporteId } ?: return
        // En el servidor los siguientes se corren un lugar: sin esto, la próxima página salta uno.
        // Si alguno se repite, lo descarta la deduplicación por id.
        siguienteOffset[ambito] = ((siguienteOffset[ambito] ?: 0) - 1).coerceAtLeast(0)
        val nivel = eliminado.nivel_severidad.lowercase()
        fun menosUno(valor: Int, de: String) = if (nivel == de) (valor - 1).coerceAtLeast(0) else valor
        actualizar(ambito) {
            it.copy(
                reportes = it.reportes.filter { r -> r.id != reporteId },
                total = it.total?.let { t -> (t - 1).coerceAtLeast(0) },
                conteo = it.conteo?.let { c ->
                    c.copy(
                        total = (c.total - 1).coerceAtLeast(0),
                        alto = menosUno(c.alto, "alto"),
                        medio = menosUno(c.medio, "medio"),
                        bajo = menosUno(c.bajo, "bajo")
                    )
                }
            )
        }
    }

    fun actualizarDistanciaListado(distanciaKm: Int?) {
        if (_state.value.distanciaListadoKm == distanciaKm) return
        _state.value = _state.value.copy(distanciaListadoKm = distanciaKm)
        reiniciarComunidad()
    }

    private fun reiniciarComunidad() {
        cargas[AmbitoReportes.COMUNIDAD]?.cancel()
        cargasResumen[AmbitoReportes.COMUNIDAD]?.cancel()
        _state.value = _state.value.copy(comunidad = ListadoReportes())
        if (_state.value.ambito == AmbitoReportes.COMUNIDAD) {
            cargarPrimeraPagina(AmbitoReportes.COMUNIDAD, avisarSiFalla = false)
        }
    }

    fun limpiar() {
        cargas.values.forEach { it.cancel() }
        cargas.clear()
        cargasResumen.values.forEach { it.cancel() }
        cargasResumen.clear()
        cargaOpciones?.cancel()
        cargaOpciones = null
        _state.value = MisReportesUiState()
    }

    fun actualizarFechas(desde: LocalDate, hasta: LocalDate) {
        cargas.values.forEach { it.cancel() }
        cargasResumen.values.forEach { it.cancel() }
        _state.value = _state.value.copy(
            fechaDesde = desde,
            fechaHasta = hasta,
            propios = ListadoReportes(),
            comunidad = ListadoReportes()
        )
        cargarPrimeraPagina(_state.value.ambito, avisarSiFalla = false)
    }
}

private data class ConsultaReportes(
    val ambito: AmbitoReportes,
    val desde: String,
    val hasta: String,
    val distanciaKm: Int?,
    val filtros: FiltrosReportes
)

class MisReportesViewModelFactory(
    private val gddService: GDDService = RetrofitClient.gddService,
    private val usuarioRepository: UsuarioRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MisReportesViewModel(gddService, usuarioRepository) as T
    }
}
