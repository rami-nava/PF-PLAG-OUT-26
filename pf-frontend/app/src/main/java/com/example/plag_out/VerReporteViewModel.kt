package com.example.plag_out

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.plag_out.TerrenoResponse
import kotlinx.serialization.json.Json

sealed class VerReporteUiState {
    object Cargando : VerReporteUiState()
    data class Exito(
        val detalle: ReporteDetalleResponse,
        val terrenoReferencia: TerrenoResponse? = null
    ) : VerReporteUiState()
    data class Error(val mensaje: String) : VerReporteUiState()
}

class VerReporteViewModel(
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow<VerReporteUiState>(VerReporteUiState.Cargando)
    val state: StateFlow<VerReporteUiState> = _state.asStateFlow()

    /**
     * Carga el detalle del reporte.
     *
     * Orden de intentos:
     * 1. GET /reportes/{reporteId} — fuente primaria. [plaga_nombre] viene del servidor.
     * 2. Si la llamada falla (excepcion de red, timeout, codigo != 2xx) -> decodifica
     *    [reporteJsonFallback] y usa [ReporteNavPayload.plaga_nombre] capturado en la UI.
     * 3. Si el JSON de fallback tambien es nulo/corrupto -> [VerReporteUiState.Error].
     */
    fun cargar(reporteId: Int, reporteJsonFallback: String?) {
        viewModelScope.launch {
            _state.value = VerReporteUiState.Cargando

            // Intento 1: API
            try {
                val res = withContext(Dispatchers.IO) { gddService.getReporte(reporteId) }
                if (res.isSuccessful && res.body() != null) {
                    val detalle = res.body()!!
                    var terrenoRef: TerrenoResponse? = null
                    
                    if (!detalle.es_propio && detalle.terreno_mas_cercano_id != null) {
                        try {
                            val resTerrenos = withContext(Dispatchers.IO) { gddService.getTerrenos() }
                            if (resTerrenos.isSuccessful && resTerrenos.body() != null) {
                                terrenoRef = resTerrenos.body()!!.find { it.terreno_id == detalle.terreno_mas_cercano_id }
                            }
                        } catch (e: Exception) {
                            Log.w("VER_REPORTE", "Fallo al obtener terrenos para referencia: ${e.message}")
                        }
                    }
                    
                    _state.value = VerReporteUiState.Exito(detalle, terrenoRef)
                    return@launch
                }
                Log.w("VER_REPORTE", "GET /reportes/$reporteId -> ${res.code()}, usando fallback")
            } catch (e: Exception) {
                Log.w("VER_REPORTE", "GET /reportes/$reporteId fallo: ${e.message}, usando fallback")
            }

            // Intento 2: fallback JSON de navegacion
            // plaga_nombre aqui siempre proviene de CrearReporteUIState.plagaSeleccionada.nombre
            usarFallback(reporteJsonFallback)
        }
    }

    private fun usarFallback(reporteJsonFallback: String?) {
        if (reporteJsonFallback.isNullOrBlank()) {
            _state.value = VerReporteUiState.Error("No se pudieron cargar los datos del reporte.")
            return
        }
        try {
            val payload = com.google.gson.Gson().fromJson(reporteJsonFallback, ReporteNavPayload::class.java)
            _state.value = VerReporteUiState.Exito(
                ReporteDetalleResponse(
                    id              = payload.id,
                    plaga_id        = 0,
                    plaga_nombre    = payload.plaga_nombre,
                    nivel_severidad = payload.nivel_severidad,
                    latitud         = payload.latitud,
                    longitud        = payload.longitud,
                    timestamp_ms    = payload.timestamp_ms
                )
            )
        } catch (e: Exception) {
            Log.e("VER_REPORTE", "Error deserializando fallback JSON: ${e.message}")
            _state.value = VerReporteUiState.Error("No se pudieron cargar los datos del reporte.")
        }
    }
}

class VerReporteViewModelFactory(
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return VerReporteViewModel(gddService) as T
    }
}
