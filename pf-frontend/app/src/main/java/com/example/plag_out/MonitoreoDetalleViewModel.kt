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
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.roundToInt

const val MAX_CARACTERES_OBSERVACIONES = 500

data class MonitoreoDetalleUIState(
    val isLoading: Boolean = true,
    val monitoreo: MonitoreoResponse? = null,
    /** Valor en vivo del slider mientras el bottom sheet de umbral está abierto; null = cerrado. */
    val umbralEditado: Int? = null,
    val guardandoUmbral: Boolean = false,
    val umbralMlEditado: Int? = null,
    val guardandoUmbralMl: Boolean = false,
    val observacionesEditadas: String? = null,
    val guardandoObservaciones: Boolean = false,
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

    /** Carga en vuelo: se cancela si llega otra, para que gane siempre la respuesta más nueva. */
    private var cargaEnCurso: Job? = null

    /**
     * Offline-first, igual que el resto de la app: primero se sirve del caché de Room
     * (instantáneo), después intenta refrescar contra el backend.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun cargar(monitoreoId: Int) {
        // Antes se salía cuando ya había un monitoreo cargado con ese id: eso dejaba el dato
        // congelado durante toda la vida del ViewModel (el caché rancio sólo se iba cuando se
        // reiniciaba la app) y además volvía inútil el botón "Reintentar". Ahora cada entrada a
        // la pantalla vuelve a pedir el monitoreo; se cancela la carga anterior para no tener dos
        // respuestas compitiendo y que gane la más nueva.
        cargaEnCurso?.cancel()
        cargaEnCurso = viewModelScope.launch {
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

    fun abrirEditorUmbralMl() {
        val monitoreo = _state.value.monitoreo ?: return
        val recomendado = monitoreo.umbral_alerta_ml_recomendado ?: return
        if (monitoreo.modelo_alerta_ml_id == null) return
        val inicial = monitoreo.umbral_alerta_ml?.roundToInt()
            ?: monitoreo.umbral_alerta_ml_efectivo?.roundToInt()
            ?: ceil(recomendado.toDouble()).toInt()
        _state.value = _state.value.copy(
            umbralMlEditado = inicial.coerceIn(ceil(recomendado.toDouble()).toInt(), 100)
        )
    }

    fun actualizarUmbralMlEditado(valor: Int) {
        _state.value = _state.value.copy(umbralMlEditado = valor)
    }

    fun cancelarEdicionUmbralMl() {
        _state.value = _state.value.copy(umbralMlEditado = null)
    }

    fun abrirEditorObservaciones() {
        _state.value = _state.value.copy(observacionesEditadas = _state.value.monitoreo?.observaciones.orEmpty())
    }

    fun actualizarObservacionesEditadas(texto: String) {
        _state.value = _state.value.copy(observacionesEditadas = texto.take(MAX_CARACTERES_OBSERVACIONES))
    }

    fun cancelarEdicionObservaciones() {
        _state.value = _state.value.copy(observacionesEditadas = null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun guardarObservaciones(onSuccess: () -> Unit) {
        val monitoreo = _state.value.monitoreo ?: return
        val nueva = (_state.value.observacionesEditadas ?: return).trim()

        if (nueva == monitoreo.observaciones.orEmpty()) {
            _state.value = _state.value.copy(observacionesEditadas = null)
            onSuccess()
            return
        }

        _state.value = _state.value.copy(guardandoObservaciones = true, error = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.actualizarMonitoreo(
                        monitoreo.monitoreo_id,
                        // Cadena vacía (no null) para que el backend distinga "borrar la nota" de
                        // "este PATCH no toca la nota".
                        UpdateMonitoreoRequest(observaciones = nueva)
                    )
                }
                if (response.isSuccessful) {
                    val actualizado = response.body() ?: monitoreo.copy(observaciones = nueva.ifBlank { null })
                    withContext(Dispatchers.IO) { repository.guardarMonitoreo(actualizado) }
                    CacheTracker.invalidar(context, CacheTracker.MONITOREOS)
                    _state.value = _state.value.copy(
                        monitoreo = actualizado,
                        guardandoObservaciones = false,
                        observacionesEditadas = null
                    )
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(
                        guardandoObservaciones = false,
                        error = mensajeDeError(response.code())
                    )
                    Log.e("MONITOREO_DETALLE", "Error al guardar la nota: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    guardandoObservaciones = false,
                    error = "No se pudo guardar la nota. Revisá tu conexión."
                )
                Log.e("MONITOREO_DETALLE", "Error al guardar la nota: ${e.message}")
            }
        }
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
                    // El listado sirve del caché hasta el corte diario de GDD: sin invalidar,
                    // seguiría mostrando el umbral viejo aunque Room ya tenga el nuevo.
                    CacheTracker.invalidar(context, CacheTracker.MONITOREOS)
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun guardarUmbralMl(onSuccess: () -> Unit) {
        val monitoreo = _state.value.monitoreo ?: return
        val nuevo = _state.value.umbralMlEditado ?: return
        val recomendado = monitoreo.umbral_alerta_ml_recomendado ?: return
        if (monitoreo.modelo_alerta_ml_id == null) return

        val minimo = ceil(recomendado.toDouble()).toInt()
        if (nuevo !in minimo..100) {
            _state.value = _state.value.copy(error = "El threshold ML debe estar entre $minimo y 100%.")
            return
        }
        actualizarUmbralMl(monitoreo, JsonObject().apply { addProperty("umbral_alerta_ml", nuevo) }, onSuccess)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun usarUmbralMlRecomendado(onSuccess: () -> Unit) {
        val monitoreo = _state.value.monitoreo ?: return
        if (monitoreo.modelo_alerta_ml_id == null || monitoreo.umbral_alerta_ml_recomendado == null) return
        actualizarUmbralMl(
            monitoreo,
            JsonObject().apply { add("umbral_alerta_ml", JsonNull.INSTANCE) },
            onSuccess
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun actualizarUmbralMl(
        monitoreo: MonitoreoResponse,
        body: JsonObject,
        onSuccess: () -> Unit
    ) {
        _state.value = _state.value.copy(guardandoUmbralMl = true, error = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.actualizarUmbralAlertaMl(monitoreo.monitoreo_id, body)
                }
                if (response.isSuccessful) {
                    val actualizado = response.body() ?: monitoreo.copy(
                        umbral_alerta_ml = body.get("umbral_alerta_ml")?.takeUnless { it.isJsonNull }?.asFloat,
                        umbral_alerta_ml_efectivo = body.get("umbral_alerta_ml")
                            ?.takeUnless { it.isJsonNull }?.asFloat
                            ?: monitoreo.umbral_alerta_ml_recomendado
                    )
                    withContext(Dispatchers.IO) { repository.guardarMonitoreo(actualizado) }
                    CacheTracker.invalidar(context, CacheTracker.MONITOREOS)
                    _state.value = _state.value.copy(
                        monitoreo = actualizado,
                        guardandoUmbralMl = false,
                        umbralMlEditado = null
                    )
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(
                        guardandoUmbralMl = false,
                        error = if (response.code() == 422) {
                            "Ese threshold ML no es válido para el modelo disponible."
                        } else mensajeDeError(response.code())
                    )
                    Log.e("MONITOREO_DETALLE", "Error al guardar threshold ML: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    guardandoUmbralMl = false,
                    error = "No se pudo actualizar el threshold ML. Revisá tu conexión."
                )
                Log.e("MONITOREO_DETALLE", "Error al guardar threshold ML: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun finalizarMonitoreo(observaciones: String? = null, onSuccess: () -> Unit) {
        val monitoreo = _state.value.monitoreo ?: return
        val nota = observaciones?.trim()?.take(MAX_CARACTERES_OBSERVACIONES)
            ?.takeIf { it != monitoreo.observaciones.orEmpty() }

        _state.value = _state.value.copy(finalizando = true, error = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.actualizarMonitoreo(
                        monitoreo.monitoreo_id,
                        UpdateMonitoreoRequest(activo = false, observaciones = nota)
                    )
                }
                if (response.isSuccessful) {
                    val actualizado = response.body()
                        ?: monitoreo.copy(
                            activo = false,
                            observaciones = nota?.ifBlank { null } ?: monitoreo.observaciones
                        )
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
