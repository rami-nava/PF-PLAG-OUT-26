package com.example.plag_out

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.UsuarioRepository
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class MisReportesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val reportes: List<ReporteDetalleResponse> = emptyList(),
    val error: String? = null,
    val fechaDesde: LocalDate = LocalDate.now().minusMonths(1),
    val fechaHasta: LocalDate = LocalDate.now(),
    val radioNotificacionKm: Double = 20.0,
    val mostrarDialogoRadio: Boolean = false,
    val radioTemporalKm: Float = 20f,
    val guardandoRadio: Boolean = false
)

class MisReportesViewModel(
    private val gddService: GDDService = RetrofitClient.gddService,
    private val usuarioRepository: UsuarioRepository? = null
) : ViewModel() {

    private val _state = MutableStateFlow(MisReportesUiState())
    val state: StateFlow<MisReportesUiState> = _state.asStateFlow()

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
        viewModelScope.launch {
            if (_state.value.reportes.isEmpty() || forzar) {
                if (forzar && _state.value.reportes.isNotEmpty()) {
                    _state.value = _state.value.copy(isRefreshing = true, error = null)
                } else {
                    _state.value = _state.value.copy(isLoading = true, error = null)
                }
            }

            try {
                val sdfIn = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val sdfOut = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                sdfOut.timeZone = java.util.TimeZone.getTimeZone("UTC")
                
                val desdeDate = sdfIn.parse(_state.value.fechaDesde)
                val hastaDate = sdfIn.parse(_state.value.fechaHasta)
                
                val cal = java.util.Calendar.getInstance()
                cal.time = hastaDate!!
                cal.add(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.add(java.util.Calendar.MINUTE, 59)
                cal.add(java.util.Calendar.SECOND, 59)
                
                val desdeStr = sdfOut.format(desdeDate!!)
                val hastaStr = sdfOut.format(cal.time)

                val response = withContext(Dispatchers.IO) {
                    gddService.getReportes(fechaDesde = desdeStr, fechaHasta = hastaStr)
                }
                if (response.isSuccessful && response.body() != null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        reportes = response.body()!!,
                        error = null
                    )
                } else {
                    Log.w("MIS_REPORTES", "Error al obtener reportes: ${response.code()}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = "No se pudieron obtener los reportes del servidor."
                    )
                }
            } catch (e: Exception) {
                Log.e("MIS_REPORTES", "Excepción cargando reportes: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = "Error de conexión al cargar reportes."
                )
            }
        }
    }

    fun refrescar() {
        cargarReportes(forzar = true)
    }

    fun limpiar() {
        _state.value = MisReportesUiState()
    }
    
    fun actualizarFechas(desde: LocalDate, hasta: LocalDate) {
        _state.value = _state.value.copy(fechaDesde = desde, fechaHasta = hasta)
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
