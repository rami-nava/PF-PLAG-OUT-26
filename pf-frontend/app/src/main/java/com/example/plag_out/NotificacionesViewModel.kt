package com.example.plag_out

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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

data class NotificacionesUIState(
    val isLoading: Boolean = false,
    /** Solo las no leídas: es lo único que devuelve el backend. */
    val notificaciones: List<NotificacionResponse> = emptyList(),
    val error: String? = null
) {
    /** Número del badge del timbre. */
    val noLeidas: Int get() = notificaciones.size
}

/**
 * Notificaciones in-app. A diferencia del resto de la app no cachea en Room: el backend
 * devuelve únicamente las no leídas, así que la lista es efímera por definición y un caché
 * solo serviría para mostrar como pendiente algo que ya se leyó en otro dispositivo. Sin
 * red, el timbre simplemente no muestra badge.
 */
class NotificacionesViewModel(
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow(NotificacionesUIState())
    val state: StateFlow<NotificacionesUIState> = _state.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    fun cargar() {
        if (_state.value.isLoading) return

        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { gddService.getNotificaciones() }
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    _state.value = NotificacionesUIState(notificaciones = body)
                } else {
                    // El endpoint puede no existir todavía: sin badge y sin molestar al usuario.
                    _state.value = _state.value.copy(isLoading = false, error = null)
                    Log.e("NOTIFICACIONES", "Error al cargar: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = null)
                Log.e("NOTIFICACIONES", "Error al cargar: ${e.message}")
            }
        }
    }

    /**
     * Marca una notificación como leída. Se saca de la lista al instante (optimista) porque el
     * gesto que la dispara —tap o swipe— ya la hizo desaparecer en pantalla; si el PATCH falla
     * se la devuelve a su lugar.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun marcarLeida(notificacion: NotificacionResponse) {
        val previas = _state.value.notificaciones
        if (previas.none { it.id == notificacion.id }) return

        _state.value = _state.value.copy(
            notificaciones = previas.filterNot { it.id == notificacion.id }
        )

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.marcarNotificacionLeida(notificacion.id)
                }
                if (!response.isSuccessful) {
                    restaurar(notificacion, previas)
                    Log.e("NOTIFICACIONES", "Error al marcar leída: ${response.code()}")
                }
            } catch (e: Exception) {
                restaurar(notificacion, previas)
                Log.e("NOTIFICACIONES", "Error al marcar leída: ${e.message}")
            }
        }
    }

    /** Devuelve la notificación a la lista respetando el orden que traía el backend. */
    private fun restaurar(notificacion: NotificacionResponse, previas: List<NotificacionResponse>) {
        val actuales = _state.value.notificaciones
        if (actuales.any { it.id == notificacion.id }) return
        val ordenPrevio = previas.map { it.id }
        _state.value = _state.value.copy(
            notificaciones = (actuales + notificacion).sortedBy { ordenPrevio.indexOf(it.id) },
            error = "No se pudo marcar como leída. Revisá tu conexión."
        )
    }

    fun descartarError() {
        _state.value = _state.value.copy(error = null)
    }

    /** Cierre de sesión: las notificaciones son del usuario anterior. */
    fun limpiar() {
        _state.value = NotificacionesUIState()
    }
}

class NotificacionesViewModelFactory(
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NotificacionesViewModel(gddService) as T
    }
}
