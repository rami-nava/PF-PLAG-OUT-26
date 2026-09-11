package com.example.plag_out

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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

data class UserUIState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val usuario: UsuarioResponse? = null,
    val error: String? = null,
    val consentimientoModelo: ConsentimientoModeloResponse? = null,
    val cargandoConsentimiento: Boolean = false,
    val guardandoConsentimiento: Boolean = false,
    val consentimientoError: String? = null
)

class UserViewModel(
    private val usuarioRepository: UsuarioRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow(UserUIState())
    val state: StateFlow<UserUIState> = _state.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    fun refrescar() = getUsuario(forzar = true)

    @RequiresApi(Build.VERSION_CODES.O)
    fun getUsuario(forzar: Boolean = false) {
        if (!forzar && (_state.value.usuario != null || _state.value.isLoading)) return

        // Sin perfil todavía en memoria: se muestra el spinner hasta que aparezca el caché o
        // responda la red, para no mostrar el error un instante antes de leer Room.
        if (_state.value.usuario == null) {
            _state.value = _state.value.copy(isLoading = true, error = null)
        }

        viewModelScope.launch {
            // Mostrar el caché al instante (si existe), para que el perfil se vea aunque no haya red
            if (_state.value.usuario == null) {
                val cacheado = withContext(Dispatchers.IO) { usuarioRepository.obtenerUsuario() }
                if (cacheado != null) {
                    _state.value = _state.value.copy(usuario = cacheado, isLoading = false)
                }
            }

            // Si ya hay un perfil en pantalla (caché o refresco), no se tapa con un spinner:
            // se muestra como "refreshing" y el contenido viejo queda visible.
            val hayPerfil = _state.value.usuario != null
            _state.value = _state.value.copy(
                isLoading = !hayPerfil,
                isRefreshing = hayPerfil,
                error = null
            )
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.getUsuarioActual()
                }
                val usuario = response.body()
                if (response.isSuccessful && usuario != null) {
                    _state.value = _state.value.copy(
                        usuario = usuario,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                    withContext(Dispatchers.IO) { usuarioRepository.guardarUsuario(usuario) }
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = if (hayPerfil) null else "No se pudo cargar tu perfil."
                    )
                    Log.e("USUARIO", "Error: ${response.code()}")
                }
            } catch (e: Exception) {
                // Sin conexión: se queda con el caché ya mostrado (si lo hay)
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = if (hayPerfil) null else "No se pudo cargar tu perfil. Revisá tu conexión."
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
        _state.value = _state.value.copy(usuario = usuario)
        viewModelScope.launch(Dispatchers.IO) { usuarioRepository.guardarUsuario(usuario) }
    }

    fun cargarConsentimientoModelo() {
        if (_state.value.cargandoConsentimiento || _state.value.consentimientoModelo != null) return
        _state.value = _state.value.copy(cargandoConsentimiento = true, consentimientoError = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { gddService.getConsentimientoModelo() }
                val body = response.body()
                _state.value = if (response.isSuccessful && body != null) {
                    _state.value.copy(
                        consentimientoModelo = body,
                        cargandoConsentimiento = false,
                        consentimientoError = null
                    )
                } else {
                    _state.value.copy(
                        cargandoConsentimiento = false,
                        consentimientoError = "No se pudo cargar el consentimiento ML."
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    cargandoConsentimiento = false,
                    consentimientoError = "No se pudo cargar el consentimiento ML. Revisá tu conexión."
                )
            }
        }
    }

    fun actualizarConsentimientoModelo(consentido: Boolean) {
        if (_state.value.guardandoConsentimiento) return
        _state.value = _state.value.copy(guardandoConsentimiento = true, consentimientoError = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.actualizarConsentimientoModelo(
                        ConsentimientoModeloRequest(consentido, VERSION_CONSENTIMIENTO_ML)
                    )
                }
                val body = response.body()
                _state.value = when {
                    response.isSuccessful && body != null -> _state.value.copy(
                        consentimientoModelo = body,
                        guardandoConsentimiento = false,
                        consentimientoError = null
                    )
                    response.code() == 409 -> _state.value.copy(
                        guardandoConsentimiento = false,
                        consentimientoError = "Actualizá la app para revisar la versión vigente del consentimiento."
                    )
                    else -> _state.value.copy(
                        guardandoConsentimiento = false,
                        consentimientoError = "No se pudo actualizar el consentimiento ML."
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    guardandoConsentimiento = false,
                    consentimientoError = "No se pudo actualizar el consentimiento ML. Revisá tu conexión."
                )
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun eliminarCuenta(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { gddService.eliminarCuenta() }
                val resultado = response.body()
                if (response.isSuccessful && resultado != null && resultado.deleted) {
                    // La cuenta se fue del backend pero sobrevivió en Supabase Auth. Para el
                    // usuario la baja igual salió bien, así que no se le muestra nada: queda solo
                    // en el log, que es donde sirve (es un problema a resolver en el backend).
                    if (!resultado.auth_identity_deleted) {
                        Log.w("USUARIO", "Cuenta eliminada, pero la identidad de Supabase Auth sobrevivió")
                    }
                    onSuccess()
                } else {
                    Log.e("USUARIO", "Error al eliminar la cuenta: ${response.code()}")
                    onError("No se pudo eliminar tu cuenta. Intentá de nuevo más tarde.")
                }
            } catch (e: Exception) {
                Log.e("USUARIO", "Error al eliminar la cuenta: ${e.message}")
                onError("No se pudo eliminar tu cuenta. Revisá tu conexión.")
            }
        }
    }

    /** Cierre de sesión: descarta en memoria el perfil del usuario anterior. */
    fun limpiar() {
        _state.value = UserUIState()
    }
}

const val VERSION_CONSENTIMIENTO_ML = "report-ml-consent-v1"

class UserViewModelFactory(
    private val usuarioRepository: UsuarioRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UserViewModel(usuarioRepository, gddService) as T
    }
}
