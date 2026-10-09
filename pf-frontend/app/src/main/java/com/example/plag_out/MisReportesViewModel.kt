package com.example.plag_out

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.UsuarioRepository
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import retrofit2.HttpException

private const val REPORTES_PAGE_SIZE = 100

data class MisReportesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val reportes: List<ReporteDetalleResponse> = emptyList(),
    val error: String? = null,
    val fechaDesde: LocalDate = LocalDate.now().minusMonths(1),
    val fechaHasta: LocalDate = LocalDate.now(),
    val distanciaListadoKm: Int? = null,
    val radioNotificacionKm: Double = 20.0,
    val mostrarDialogoRadio: Boolean = false,
    val radioTemporalKm: Float = 20f,
    val guardandoRadio: Boolean = false
)

class MisReportesViewModel(
    private val gddService: GDDService = RetrofitClient.gddService,
    private val usuarioRepository: UsuarioRepository? = null,
    private val reportesDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow(MisReportesUiState())
    val state: StateFlow<MisReportesUiState> = _state.asStateFlow()
    private var cargaReportes: Job? = null

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
                    // El backend filtra los reportes de la comunidad de GET /reportes con este radio:
                    // sin recargar, la lista seguiría mostrando los del radio anterior.
                    if (radioFinal != radioAnterior) cargarReportes(forzar = true)
                    onSuccess?.invoke()
                } else {
                    _state.value = _state.value.copy(
                        guardandoRadio = false,
                        error = "No se pudo actualizar el radio de notificación."
                    )
                }
            } catch (e: Exception) {
                Log.e("MIS_REPORTES", "Error al guardar radio de notificación: ${e.message}")
                _state.value = _state.value.copy(
                    guardandoRadio = false,
                    error = "Error de red al actualizar el radio."
                )
            }
        }
    }

    fun cargarReportes(forzar: Boolean = false) {
        if (!forzar && cargaReportes?.isActive == true) return
        cargaReportes?.cancel()
        // Todos los requests de una carga usan el mismo rango, incluso si el usuario lo cambia.
        val zona = ZoneId.systemDefault()
        val desdeStr = _state.value.fechaDesde.atStartOfDay(zona).toInstant().toString()
        val hastaStr = _state.value.fechaHasta.plusDays(1).atStartOfDay(zona)
            .toInstant().minusNanos(1).toString()
        val distanciaKm = _state.value.distanciaListadoKm
        val hayResultadosAnteriores = _state.value.reportes.isNotEmpty()
        _state.value = _state.value.copy(
            isLoading = !hayResultadosAnteriores,
            isRefreshing = hayResultadosAnteriores,
            error = null
        )
        cargaReportes = viewModelScope.launch {
            try {
                val reportes = withContext(reportesDispatcher) {
                    val completos = linkedMapOf<Int, ReporteDetalleResponse>()
                    var offset = 0
                    while (true) {
                        val response = gddService.getReportes(
                            fechaDesde = desdeStr,
                            fechaHasta = hastaStr,
                            limit = REPORTES_PAGE_SIZE,
                            offset = offset,
                            distanciaKm = distanciaKm
                        )
                        if (!response.isSuccessful) throw HttpException(response)
                        val pagina = response.body() ?: throw IOException("Report response has no body")
                        if (pagina.size > REPORTES_PAGE_SIZE ||
                            (pagina.isNotEmpty() && pagina.none { it.id !in completos })) {
                            throw IOException("Report pagination did not advance")
                        }
                        pagina.forEach { completos[it.id] = it }
                        if (pagina.size < REPORTES_PAGE_SIZE) break
                        // Avanzar por filas recibidas, no por IDs únicos: pueden repetirse entre páginas.
                        offset = Math.addExact(offset, pagina.size)
                    }
                    completos.values.toList()
                }
                // Publicar únicamente una carga completa; nunca reemplazarla con una página parcial.
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    reportes = reportes,
                    error = null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MIS_REPORTES", "No se pudo completar la carga de reportes", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = "No se pudieron cargar todos los reportes. Reintentá."
                )
            }
        }
    }

    fun refrescar() {
        cargarReportes(forzar = true)
    }

    fun actualizarDistanciaListado(distanciaKm: Int?) {
        if (_state.value.distanciaListadoKm == distanciaKm) return
        _state.value = _state.value.copy(distanciaListadoKm = distanciaKm, reportes = emptyList())
        cargarReportes(forzar = true)
    }

    fun limpiar() {
        cargaReportes?.cancel()
        cargaReportes = null
        _state.value = MisReportesUiState()
    }
    
    fun actualizarFechas(desde: LocalDate, hasta: LocalDate) {
        _state.value = _state.value.copy(
            fechaDesde = desde,
            fechaHasta = hasta,
            reportes = emptyList()
        )
        cargarReportes(forzar = true)
    }
}

class MisReportesViewModelFactory(
    private val gddService: GDDService = RetrofitClient.gddService,
    private val usuarioRepository: UsuarioRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MisReportesViewModel(gddService, usuarioRepository) as T
    }
}
