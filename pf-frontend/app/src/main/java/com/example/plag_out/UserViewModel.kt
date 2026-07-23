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

        _state.value = _state.value.copy(isLoading = true, error = null)
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
                        error = "No se pudo cargar tu perfil."
                    )
                    Log.e("USUARIO", "Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "No se pudo cargar tu perfil. Revisá tu conexión."
                )
                Log.e("USUARIO", "Error: ${e.message}")
            }
        }
    }

    /** Cierre de sesión: descarta en memoria el perfil del usuario anterior. */
    fun limpiar() {
        _state.value = UserUIState()
    }
}
