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

data class MisReportesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val reportes: List<ReporteDetalleResponse> = emptyList(),
    /**
     * Catálogo completo de plagas del backend. El mapa filtra contra esto y no solo contra las
     * plagas que aparecen en los reportes: "no hay reportes de esta plaga en mi zona" también es
     * una respuesta, y sin el catálogo esa pregunta no se puede ni formular.
     */
    val catalogoPlagas: List<String> = emptyList(),
    val error: String? = null
)

class MisReportesViewModel(
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow(MisReportesUiState())
    val state: StateFlow<MisReportesUiState> = _state.asStateFlow()

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
                val response = withContext(Dispatchers.IO) {
                    gddService.getReportes()
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

    /**
     * Se pide una sola vez y se degrada en silencio: si el backend no responde, el mapa igual
     * puede filtrar por las plagas que ya aparecen en los reportes.
     */
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
                    _state.value = _state.value.copy(catalogoPlagas = nombres)
                } else {
                    Log.w("MIS_REPORTES", "Catálogo de plagas no disponible: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.w("MIS_REPORTES", "No se pudo cargar el catálogo de plagas: ${e.message}")
            }
        }
    }

    fun limpiar() {
        _state.value = MisReportesUiState()
    }
}

class MisReportesViewModelFactory(
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MisReportesViewModel(gddService) as T
    }
}
