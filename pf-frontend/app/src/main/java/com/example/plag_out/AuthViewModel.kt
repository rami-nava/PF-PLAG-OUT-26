package com.example.plag_out

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.CacheTracker
import com.example.plag_out.AlmacenamientoLocal.PreferenciasUsuario
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
import com.example.plag_out.AlmacenamientoLocal.PlantacionRepository
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import com.example.plag_out.AlmacenamientoLocal.UsuarioRepository
import com.example.plag_out.Service.FcmTokenRegistrar
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class LoginState(
    val email: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null
)

data class CrearCuentaState(
    val nombre: String = "",
    val apellido: String = "",
    val cargosDisponibles: List<CargoResponse> = emptyList(),
    val cargo: CargoResponse? = null,
    val email: String = "",
    val password: String = "",
    val repetirPassword: String = "",
    val cargando: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val supabaseClient: SupabaseClient,
    private val context: Context,
    private val monitoreoRepository: MonitoreoRepository,
    private val terrenoRepository: TerrenoRepository,
    private val plantacionRepository: PlantacionRepository,
    private val usuarioRepository: UsuarioRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState

    private val _crearCuentaState = MutableStateFlow(CrearCuentaState())
    val crearCuentaState: StateFlow<CrearCuentaState> = _crearCuentaState

    // ---------- LOGIN ----------

    fun actualizarEmail(valor: String) {
        _loginState.value = _loginState.value.copy(email = valor, error = null)
    }

    fun actualizarPassword(valor: String) {
        _loginState.value = _loginState.value.copy(password = valor, error = null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun iniciarSesion(onSuccess: () -> Unit) {
        val email = _loginState.value.email
        val password = _loginState.value.password

        _loginState.value = _loginState.value.copy(cargando = true, error = null)

        viewModelScope.launch {
            try {
                supabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                val token = supabaseClient.auth.currentAccessTokenOrNull()
                if(token != null) Log.d("AuthViewModel", "Login exitoso.")

                // Sincronizar con el backend de Plag-Out después de un login exitoso
                val user = supabaseClient.auth.currentUserOrNull()
                val metadata = user?.userMetadata
                val nombre = metadata?.get("nombre")?.jsonPrimitive?.contentOrNull ?: ""
                val apellido = metadata?.get("apellido")?.jsonPrimitive?.contentOrNull ?: ""
                val cargoId = metadata?.get("cargo")?.jsonPrimitive?.contentOrNull ?: ""

                if (user != null) {
                    try {
                        val response = gddService.createUser(
                            CreateUserRequest(
                                email = user.email ?: "",
                                nombre = nombre,
                                apellido = apellido,
                                cargo_id = cargoId.toIntOrNull() ?: 1
                            )
                        )
                        if (response.isSuccessful) {
                            Log.d(
                                "AuthViewModel",
                                if(response.body()?.created == false) "El usuario ya existe en el backend"
                                        else "Usuario creado exitosamente en el backend: ${response.body()?.usuario_id}"
                            )
                        } else {
                            Log.e(
                                "AuthViewModel",
                                "Error al crear usuario en backend: ${response.code()} ${
                                    response.errorBody()?.string()
                                }"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Excepción al sincronizar usuario con el backend", e)
                    }
                } else {
                    Log.w(
                        "AuthViewModel",
                        "No se encontró el usuario en Supabase después del login"
                    )
                }

                // Registrar el token FCM del dispositivo para poder recibir alertas, salvo que el
                // usuario haya apagado las notificaciones desde su perfil.
                if (PreferenciasUsuario.notificacionesActivadas(context)) {
                    FcmTokenRegistrar.registrar()
                }

                _loginState.value = _loginState.value.copy(cargando = false)
                onSuccess()
            } catch (e: RestException) {
                _loginState.value = _loginState.value.copy(
                    cargando = false,
                    error = mapearErrorLogin(e)
                )
            } catch (e: Exception) {
                _loginState.value = _loginState.value.copy(
                    cargando = false,
                    error = "Ocurrió un error inesperado. Intentá de nuevo."
                )
            }
        }
    }

    private fun mapearErrorLogin(e: RestException): String {
        return when {
            e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                "Correo o contraseña incorrectos"

            e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                "Confirmá tu correo antes de iniciar sesión"

            else -> "No se pudo iniciar sesión. Intentá de nuevo."
        }
    }

    // ---------- CERRAR SESIÓN ----------

    /**
     * Cierra la sesión de Supabase y borra todo el almacenamiento local del
     * dispositivo (token JWT, caché de Room y marcas de consulta), para que
     * el próximo usuario que inicie sesión no vea datos ajenos.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun cerrarSesion(onComplete: () -> Unit) {
        viewModelScope.launch {
            // Desregistrar el token FCM antes del signOut, mientras el JWT sigue válido
            FcmTokenRegistrar.desregistrar()
            try {
                supabaseClient.auth.signOut()
            } catch (e: Exception) {
                // Sin conexión el signOut remoto puede fallar: lo local se limpia igual
                Log.e("AuthViewModel", "Error al cerrar sesión en Supabase", e)
            }
            withContext(Dispatchers.IO) {
                monitoreoRepository.borrarTodos()
                plantacionRepository.borrarTodos()
                terrenoRepository.borrarTodos()
                usuarioRepository.borrarTodos()
            }
            CacheTracker.limpiarTodo(context)
            _loginState.value = LoginState()
            _crearCuentaState.value = CrearCuentaState()
            onComplete()
        }
    }

    // ---------- CAMBIAR CONTRASEÑA ----------

    /**
     * Cambia la contraseña del usuario logueado. La sesión sigue siendo válida después del cambio,
     * así que no hace falta volver a iniciar sesión.
     */
    fun cambiarPassword(nueva: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                supabaseClient.auth.updateUser { password = nueva }
                onSuccess()
            } catch (e: RestException) {
                onError(mapearErrorPassword(e))
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error al cambiar la contraseña", e)
                onError("No se pudo cambiar la contraseña. Revisá tu conexión.")
            }
        }
    }

    private fun mapearErrorPassword(e: RestException): String {
        return when {
            e.message?.contains("Password should be", ignoreCase = true) == true ->
                "La contraseña no cumple los requisitos mínimos"

            e.message?.contains("should be different", ignoreCase = true) == true ->
                "La contraseña nueva tiene que ser distinta de la actual"

            e.message?.contains("reauthentication", ignoreCase = true) == true ->
                "Por seguridad, volvé a iniciar sesión antes de cambiar la contraseña"

            else -> "No se pudo cambiar la contraseña. Intentá de nuevo."
        }
    }

    // ---------- CREAR CUENTA ----------

    fun actualizarNombre(valor: String) {
        _crearCuentaState.value = _crearCuentaState.value.copy(nombre = valor, error = null)
    }

    fun actualizarApellido(valor: String) {
        _crearCuentaState.value = _crearCuentaState.value.copy(apellido = valor, error = null)
    }

    fun actualizarCargo(valor: CargoResponse) {
        _crearCuentaState.value = _crearCuentaState.value.copy(cargo = valor, error = null)
    }

    fun actualizarEmailRegistro(valor: String) {
        _crearCuentaState.value = _crearCuentaState.value.copy(email = valor, error = null)
    }

    fun actualizarPasswordRegistro(valor: String) {
        _crearCuentaState.value = _crearCuentaState.value.copy(password = valor, error = null)
    }

    fun actualizarRepetirPassword(valor: String) {
        _crearCuentaState.value =
            _crearCuentaState.value.copy(repetirPassword = valor, error = null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun cargarCargos() {
        viewModelScope.launch {
            try {
                val response = gddService.getCargos()
                if (response.isSuccessful()) {
                    val cargos = response.body() ?: emptyList()
                    _crearCuentaState.value =
                        _crearCuentaState.value.copy(cargosDisponibles = cargos)
                }
            } catch (e: Exception) {
                _crearCuentaState.value = _crearCuentaState.value.copy(
                    cargando = false,
                    error = "No se pudieron cargar los cargos"
                )
                Log.e("CARGOS", "Error: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun crearCuenta(onSuccess: () -> Unit) {
        val state = _crearCuentaState.value

        if (state.password != state.repetirPassword) {
            _crearCuentaState.value = state.copy(error = "Las contraseñas no coinciden")
            return
        }

        _crearCuentaState.value = state.copy(cargando = true, error = null)

        viewModelScope.launch {
            try {
                // 1. Crear usuario en Supabase (esto enviará el mail de confirmación)
                supabaseClient.auth.signUpWith(Email) {
                    email = state.email
                    password = state.password
                    data = buildJsonObject {
                        put("nombre", state.nombre)
                        put("apellido", state.apellido)
                        put("cargo", state.cargo?.id.toString())
                    }
                }

                _crearCuentaState.value = _crearCuentaState.value.copy(cargando = false)
                onSuccess()

            } catch (e: RestException) {
                _crearCuentaState.value = _crearCuentaState.value.copy(
                    cargando = false,
                    error = mapearErrorRegistro(e)
                )
            } catch (e: Exception) {
                Log.e("Supabase", "Error creando cuenta", e)
                _crearCuentaState.value = _crearCuentaState.value.copy(
                    cargando = false,
                    error = "No se pudo crear la cuenta. Intentá de nuevo."
                )
            }
        }
    }

    private fun mapearErrorRegistro(e: RestException): String {
        return when {
            e.message?.contains("already registered", ignoreCase = true) == true ->
                "Ya existe una cuenta con este correo"

            e.message?.contains("Password should be", ignoreCase = true) == true ->
                "La contraseña no cumple los requisitos mínimos"

            else -> "No se pudo crear la cuenta. Intentá de nuevo."
        }
    }

    class AuthViewModelFactory(
        private val client: SupabaseClient,
        private val context: Context,
        private val monitoreoRepository: MonitoreoRepository,
        private val terrenoRepository: TerrenoRepository,
        private val plantacionRepository: PlantacionRepository,
        private val usuarioRepository: UsuarioRepository,
        private val gddService: GDDService = RetrofitClient.gddService
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(
                client, context,
                monitoreoRepository, terrenoRepository, plantacionRepository, usuarioRepository, gddService
            ) as T
        }
    }
}

