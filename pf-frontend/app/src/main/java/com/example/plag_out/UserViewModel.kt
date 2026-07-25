package com.example.plag_out

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UserUIState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val usuario: UsuarioResponse? = null,
    val error: String? = null
)

class UserViewModel(
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow(UserUIState())
    val state: StateFlow<UserUIState> = _state.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    fun refrescar() = getUsuario(forzar = true)

    @RequiresApi(Build.VERSION_CODES.O)
    fun getUsuario(forzar: Boolean = false) {
        if (!forzar && (_state.value.usuario != null || _state.value.isLoading)) return

        // Si ya hay un perfil en pantalla, el refresco no debe tapar los datos con un spinner:
        // se muestra como "refreshing" y el contenido viejo queda visible.
        val hayPerfil = _state.value.usuario != null
        _state.value = _state.value.copy(
            isLoading = !hayPerfil,
            isRefreshing = hayPerfil,
            error = null
        )
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.getUsuarioActual()
                }
                val usuario = response.body()
                if (response.isSuccessful && usuario != null) {
                    _state.value = UserUIState(usuario = usuario)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = "No se pudo cargar tu perfil."
                    )
                    Log.e("USUARIO", "Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = "No se pudo cargar tu perfil. Revisá tu conexión."
                )
                Log.e("USUARIO", "Error: ${e.message}")
            }
        }
    }

    /**
     * Refleja en el perfil un usuario ya actualizado (vuelta de la pantalla de edición), sin
     * volver a pedirlo al backend.
     */
    fun aplicarUsuario(usuario: UsuarioResponse) {
        _state.value = UserUIState(usuario = usuario)
    }

    /** Cierre de sesión: descarta en memoria el perfil del usuario anterior. */
    fun limpiar() {
        _state.value = UserUIState()
    }
}
