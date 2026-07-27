package com.example.plag_out

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.CacheTracker
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MonitoreoDetalleUIState(
    val isLoading: Boolean = true,
    val monitoreo: MonitoreoResponse? = null,
    /** Valor en vivo del slider mientras el bottom sheet de umbral está abierto; null = cerrado. */
    val umbralEditado: Int? = null,
    val guardandoUmbral: Boolean = false,
    val finalizando: Boolean = false,
    /** Dispara la navegación de vuelta cuando el PATCH de finalizar tuvo éxito. */
    val finalizado: Boolean = false,
    val error: String? = null,
    /** Se sirvió del caché porque el fetch a la red falló, no hay conexión, o el endpoint no existe. */
    val datosDesactualizados: Boolean = false
)

class MonitoreoDetalleViewModel(
    private val context: Context,
    private val repository: MonitoreoRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow(MonitoreoDetalleUIState())
    val state: StateFlow<MonitoreoDetalleUIState> = _state.asStateFlow()

    /**
     * Offline-first, igual que el resto de la app: primero se sirve del caché de Room
     * (instantáneo), después intenta refrescar contra el backend.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun cargar(monitoreoId: Int) {
        if (_state.value.monitoreo?.monitoreo_id == monitoreoId && !_state.value.isLoading) return

        viewModelScope.launch {
            val cache = withContext(Dispatchers.IO) { repository.obtenerMonitoreo(monitoreoId) }
            if (cache != null) {
                _state.value = _state.value.copy(monitoreo = cache, isLoading = false)
            } else {
                _state.value = _state.value.copy(isLoading = true)
            }

            try {
                val response = withContext(Dispatchers.IO) { gddService.getMonitoreo(monitoreoId) }
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        withContext(Dispatchers.IO) { repository.guardarMonitoreo(body) }
                        _state.value = _state.value.copy(
                            monitoreo = body,
                            isLoading = false,
                            datosDesactualizados = false,
                            error = null
                        )
                    } else {
                        _state.value = _state.value.copy(isLoading = false, datosDesactualizados = cache != null)
                    }
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        datosDesactualizados = cache != null,
                        error = if (cache == null) mensajeDeError(response.code()) else null
                    )
                    Log.e("MONITOREO_DETALLE", "Error al cargar: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    datosDesactualizados = cache != null,
                    error = if (cache == null) "No se pudo cargar el monitoreo. Revisá tu conexión." else null
                )
                Log.e("MONITOREO_DETALLE", "Error al cargar: ${e.message}")
            }
        }
    }

    fun abrirEditorUmbral() {
        _state.value = _state.value.copy(umbralEditado = _state.value.monitoreo?.umbral_riesgo ?: 80)
    }

    fun actualizarUmbralEditado(valor: Int) {
        _state.value = _state.value.copy(umbralEditado = valor)
    }

    fun cancelarEdicionUmbral() {
        _state.value = _state.value.copy(umbralEditado = null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun guardarUmbral(onSuccess: () -> Unit) {
        val monitoreo = _state.value.monitoreo ?: return
        val nuevo = _state.value.umbralEditado ?: return

        if (nuevo !in 0..100) {
            _state.value = _state.value.copy(error = "El umbral debe estar entre 0 y 100.")
            return
        }
        if (nuevo == monitoreo.umbral_riesgo) {
            // Nada que guardar: no tiene sentido mandar un PATCH vacío.
            _state.value = _state.value.copy(umbralEditado = null)
            onSuccess()
            return
        }

        _state.value = _state.value.copy(guardandoUmbral = true, error = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.actualizarMonitoreo(monitoreo.monitoreo_id, UpdateMonitoreoRequest(umbral_riesgo = nuevo))
                }
                if (response.isSuccessful) {
                    val actualizado = response.body() ?: monitoreo.copy(umbral_riesgo = nuevo)
                    withContext(Dispatchers.IO) { repository.guardarMonitoreo(actualizado) }
                    _state.value = _state.value.copy(
                        monitoreo = actualizado,
                        guardandoUmbral = false,
                        umbralEditado = null
                    )
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(guardandoUmbral = false, error = mensajeDeError(response.code()))
                    Log.e("MONITOREO_DETALLE", "Error al guardar umbral: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    guardandoUmbral = false,
                    error = "No se pudo actualizar el umbral. Revisá tu conexión."
                )
                Log.e("MONITOREO_DETALLE", "Error al guardar umbral: ${e.message}")
            }
        }
    }

    /** Soft-close: marca `activo = false` para conservar el histórico en vez de borrar. */
    @RequiresApi(Build.VERSION_CODES.O)
    fun finalizarMonitoreo(onSuccess: () -> Unit) {
        val monitoreo = _state.value.monitoreo ?: return

        _state.value = _state.value.copy(finalizando = true, error = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.actualizarMonitoreo(monitoreo.monitoreo_id, UpdateMonitoreoRequest(activo = false))
                }
                if (response.isSuccessful) {
                    val actualizado = response.body() ?: monitoreo.copy(activo = false)
                    withContext(Dispatchers.IO) { repository.guardarMonitoreo(actualizado) }
                    CacheTracker.invalidar(context, CacheTracker.MONITOREOS)
                    _state.value = _state.value.copy(
                        monitoreo = actualizado,
                        finalizando = false,
                        finalizado = true
                    )
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(finalizando = false, error = mensajeDeError(response.code()))
                    Log.e("MONITOREO_DETALLE", "Error al finalizar: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    finalizando = false,
                    error = "No se pudo finalizar el monitoreo. Revisá tu conexión."
                )
                Log.e("MONITOREO_DETALLE", "Error al finalizar: ${e.message}")
            }
        }
    }

    private fun mensajeDeError(codigo: Int): String = when (codigo) {
        400, 422 -> "Revisá los datos ingresados."
        401, 403 -> "Tu sesión expiró. Volvé a iniciar sesión."
        else -> "No se pudieron guardar los cambios. Intentá de nuevo."
    }
}

class MonitoreoDetalleViewModelFactory(
    private val context: Context,
    private val repository: MonitoreoRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MonitoreoDetalleViewModel(context, repository, gddService) as T
    }
}
