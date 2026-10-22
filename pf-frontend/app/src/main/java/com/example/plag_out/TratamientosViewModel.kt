package com.example.plag_out

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.TratamientoPendiente
import com.example.plag_out.AlmacenamientoLocal.TratamientoPendienteDao
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import com.google.gson.JsonObject
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** Lo que muestra la lista: lo que ya está en el backend y lo que espera en la bandeja. */
sealed interface ItemTratamiento {
    val clave: String

    data class Enviado(val tratamiento: TratamientoResponse, val edicionPendiente: TratamientoPendiente?) : ItemTratamiento {
        override val clave get() = "t${tratamiento.id}"
    }

    data class SinEnviar(val fila: TratamientoPendiente, val alta: TratamientoRequest) : ItemTratamiento {
        override val clave get() = "p${fila.idempotency_key}"
        val requiereRevision get() = fila.estado == "requiere_revision"
    }
}

data class TratamientosUIState(
    val monitoreoId: Int? = null,
    val tratamientos: List<TratamientoResponse> = emptyList(),
    val pendientes: List<TratamientoPendiente> = emptyList(),
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null
) {
    val items: List<ItemTratamiento>
        get() {
            val altas = pendientes.filter { it.tratamiento_id == null }.mapNotNull { fila ->
                runCatching { gsonTratamientos.fromJson(fila.payload, TratamientoRequest::class.java) }.getOrNull()
                    ?.let { ItemTratamiento.SinEnviar(fila, it) }
            }
            val ediciones = pendientes.filter { it.tratamiento_id != null }.associateBy { it.tratamiento_id }
            return altas + tratamientos.map { ItemTratamiento.Enviado(it, ediciones[it.id]) }
        }
}

class TratamientosViewModel(
    private val service: GDDService = RetrofitClient.gddService,
    private val dao: TratamientoPendienteDao,
    private val owner: () -> String? = { SupabaseProvider.client.auth.currentUserOrNull()?.id },
    // El envío va atado a la sesión que guardó la fila, como el biofix.
    private val servicioDe: (String) -> GDDService = RetrofitClient::forPresenceRetry,
    private val nuevaClave: () -> String = { UUID.randomUUID().toString() }
) : ViewModel() {

    private val _state = MutableStateFlow(TratamientosUIState())
    val state: StateFlow<TratamientosUIState> = _state.asStateFlow()

    private var pendientesJob: Job? = null
    private var eventosJob: Job? = null

    fun cargar(monitoreoId: Int) {
        val cambio = _state.value.monitoreoId != monitoreoId
        if (cambio) {
            _state.value = TratamientosUIState(monitoreoId = monitoreoId)
            val usuario = owner()
            pendientesJob?.cancel()
            eventosJob?.cancel()
            if (usuario != null) {
                pendientesJob = viewModelScope.launch {
                    dao.delMonitoreo(usuario, monitoreoId).collect { filas ->
                        _state.value = _state.value.copy(pendientes = filas)
                    }
                }
                // El worker mandó algo de este monitoreo: traer la cifra que calculó el backend.
                eventosJob = viewModelScope.launch {
                    PresenceRetryEvents.events.collect { evento ->
                        if (evento.owner == usuario && evento.kind == TIPO_TRATAMIENTO && evento.id == monitoreoId) {
                            recargar(monitoreoId)
                        }
                    }
                }
            }
        }
        recargar(monitoreoId)
    }

    private fun recargar(monitoreoId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(cargando = true, error = null)
            try {
                val response = service.getTratamientos(monitoreoId)
                val lista = response.body()
                _state.value = if (response.isSuccessful && lista != null) {
                    _state.value.copy(tratamientos = lista, cargando = false)
                } else {
                    _state.value.copy(cargando = false, error = errorDeRespuesta(response).mensaje)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("TRATAMIENTOS", "Sin conexión al cargar: ${e.javaClass.simpleName}")
                // Sin conexión igual se ven los pendientes de la bandeja.
                _state.value = _state.value.copy(cargando = false, error = errorDeExcepcion(e).mensaje)
            }
        }
    }

    /** Alta nueva: primero a la bandeja (no se pierde sin señal) y después al backend. */
    fun registrar(form: FormularioTratamiento, onListo: (EnvioTratamiento) -> Unit = {}) {
        val monitoreoId = _state.value.monitoreoId ?: return
        val usuario = owner() ?: return
        val clave = nuevaClave()
        guardarYEnviar(
            TratamientoPendiente(clave, usuario, monitoreoId, payload = gsonTratamientos.toJson(form.aRequest(clave))),
            onListo
        )
    }

    /** Corregir o completar (p. ej. el conteo posterior) lo que ya está o lo que espera envío. */
    fun editar(item: ItemTratamiento, form: FormularioTratamiento, onListo: (EnvioTratamiento?) -> Unit = {}) {
        val usuario = owner() ?: return
        when (item) {
            is ItemTratamiento.SinEnviar -> {
                // El alta todavía no salió: se reemplaza con la misma clave y viaja todo junto.
                val alta = form.aRequest(item.fila.idempotency_key)
                guardarYEnviar(item.fila.copy(payload = gsonTratamientos.toJson(alta), estado = "pendiente"), onListo)
            }
            is ItemTratamiento.Enviado -> {
                val cambios = cambiosDeTratamiento(item.tratamiento, form)
                if (cambios == null) {
                    onListo(null)
                    return
                }
                // Una edición anterior sin enviar se combina: lo último gana.
                val anterior = item.edicionPendiente
                val combinados = anterior?.let { gsonTratamientos.fromJson(it.payload, JsonObject::class.java) } ?: JsonObject()
                cambios.entrySet().forEach { (campo, valor) -> combinados.add(campo, valor) }
                guardarYEnviar(
                    TratamientoPendiente(
                        idempotency_key = anterior?.idempotency_key ?: nuevaClave(),
                        owner_id = usuario,
                        monitoreo_id = item.tratamiento.monitoreo_id,
                        tratamiento_id = item.tratamiento.id,
                        payload = combinados.toString()
                    ),
                    onListo
                )
            }
        }
    }

    private fun guardarYEnviar(fila: TratamientoPendiente, onListo: (EnvioTratamiento) -> Unit) {
        if (_state.value.guardando) return
        _state.value = _state.value.copy(guardando = true, error = null)
        viewModelScope.launch {
            try {
                dao.guardar(fila)
                val resultado = if (owner() == fila.owner_id) {
                    enviarTratamientoPendiente(fila, servicioDe(fila.owner_id), dao)
                } else EnvioTratamiento.Reintentar
                if (resultado is EnvioTratamiento.Enviado) reemplazar(resultado.tratamiento)
                _state.value = _state.value.copy(guardando = false)
                onListo(resultado)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("TRATAMIENTOS", "No se pudo guardar: ${e.javaClass.simpleName}")
                _state.value = _state.value.copy(guardando = false, error = errorDeExcepcion(e).mensaje)
            }
        }
    }

    private fun reemplazar(tratamiento: TratamientoResponse) {
        val actuales = _state.value.tratamientos
        val nuevos = if (actuales.any { it.id == tratamiento.id }) actuales.map { if (it.id == tratamiento.id) tratamiento else it }
        else listOf(tratamiento) + actuales
        _state.value = _state.value.copy(tratamientos = nuevos.sortedWith(ORDEN))
    }

    /** Lo que no se envió se descarta en el teléfono; lo enviado se borra en el backend. */
    fun eliminar(item: ItemTratamiento, onListo: (String?) -> Unit = {}) {
        val usuario = owner() ?: return
        viewModelScope.launch {
            when (item) {
                is ItemTratamiento.SinEnviar -> {
                    dao.borrar(usuario, item.fila.idempotency_key)
                    onListo(null)
                }
                is ItemTratamiento.Enviado -> try {
                    val response = service.eliminarTratamiento(item.tratamiento.id)
                    if (response.isSuccessful || response.code() == 404) {
                        item.edicionPendiente?.let { dao.borrar(usuario, it.idempotency_key) }
                        _state.value = _state.value.copy(
                            tratamientos = _state.value.tratamientos.filterNot { it.id == item.tratamiento.id }
                        )
                        onListo(null)
                    } else onListo(errorDeRespuesta(response).mensaje)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    onListo("Necesitás conexión para eliminar un tratamiento ya enviado.")
                }
            }
        }
    }

    fun descartarError() {
        _state.value = _state.value.copy(error = null)
    }

    companion object {
        const val TIPO_TRATAMIENTO = "tratamiento"
        private val ORDEN = compareByDescending<TratamientoResponse> { it.fecha_aplicacion }.thenByDescending { it.id }
    }
}

class TratamientosViewModelFactory(private val dao: TratamientoPendienteDao) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = TratamientosViewModel(dao = dao) as T
}
